"use client";

import { useEffect, useState } from "react";
import type { ComponentProps } from "react";
import { ChevronDown, Pencil, RefreshCw, Save, ShieldCheck, Trash2 } from "lucide-react";
import { usePlatform } from "@/features/platform/platform-context";
import {
  decideInterestModeration,
  deleteModerationRule,
  fetchAdminCatalog,
  fetchAdminContent,
  fetchAdminOmbudsman,
  invalidatePublicCache,
  archiveContentEntry,
  publishContentEntry,
  updateAdminOmbudsmanStatus,
  saveAdminCatalog,
  saveContentEntry,
  saveModerationRule,
  updateContentReportStatus
} from "@/shared/api/client";
import defaultContent from "@/content/default-content.json";
import type { OmbudsmanRequest } from "@/shared/api/types";
import { formatDateTime, statusLabel } from "@/shared/lib/format";
import { Button } from "@/shared/ui/button";
import { EmptyState } from "@/shared/ui/empty-state";

type AdminCatalog = {
  monetizationSettings?: { creditPurchasesEnabled?: boolean; boostPurchasesEnabled?: boolean };
  moderationSettings?: { userBlockListEnabled?: boolean };
  categories?: Array<Record<string, unknown>>;
  products?: Array<Record<string, unknown>>;
  updatedAt?: string;
};

type AdminContent = {
  entries?: Array<Record<string, unknown>>;
};

type FormSubmitHandler = NonNullable<ComponentProps<"form">["onSubmit"]>;

function normalizeAdminContent(payload: AdminContent | null): AdminContent {
  const remoteEntries = Array.isArray(payload?.entries) ? payload.entries : [];
  const entriesByKey = new Map<string, Record<string, unknown>>();
  remoteEntries.forEach((entry) => {
    const key = String(entry.key ?? "");
    if (key) {
      entriesByKey.set(key, entry);
    }
  });
  Object.entries(defaultContent.entries).forEach(([key, value]) => {
    const existing = entriesByKey.get(key);
    entriesByKey.set(key, {
      key,
      locale: "pt-BR",
      type: "TEXT",
      status: existing ? existing.status : "DEFAULT",
      ...existing,
      value: existing?.value ? existing.value : value,
      defaultValue: value
    });
  });
  return { entries: Array.from(entriesByKey.values()).sort((left, right) => String(left.key ?? "").localeCompare(String(right.key ?? ""))) };
}

export function AdminPage() {
  const { adminModeration, isLoadingPrivate, refreshPrivateData, setFeedback } = usePlatform();
  const [ruleForm, setRuleForm] = useState<{ id: string | null; term: string; riskLevel: string; active: boolean }>({ id: null, term: "", riskLevel: "HIGH", active: true });
  const [catalog, setCatalog] = useState<AdminCatalog | null>(null);
  const [content, setContent] = useState<AdminContent | null>(null);
  const [ombudsman, setOmbudsman] = useState<OmbudsmanRequest[]>([]);
  const [isAdminDataLoading, setIsAdminDataLoading] = useState(false);
  const [isContentOpen, setIsContentOpen] = useState(false);
  const [contentQuery, setContentQuery] = useState("");
  const [ombudsmanStatusFilter, setOmbudsmanStatusFilter] = useState("OPEN");
  const [busyAction, setBusyAction] = useState<string | null>(null);

  async function refreshAdminData() {
    setIsAdminDataLoading(true);
    try {
      const [catalogPayload, ombudsmanPayload] = await Promise.all([
        fetchAdminCatalog().catch(() => null),
        fetchAdminOmbudsman(ombudsmanStatusFilter === "ALL" ? "" : ombudsmanStatusFilter).catch(() => [])
      ]);
      setCatalog(catalogPayload as AdminCatalog | null);
      setOmbudsman(ombudsmanPayload);
    } finally {
      setIsAdminDataLoading(false);
    }
  }

  async function loadContentData(force = false) {
    if (!force && content?.entries?.length) {
      return;
    }
    setIsAdminDataLoading(true);
    try {
      const contentPayload = await fetchAdminContent().catch(() => null);
      setContent(normalizeAdminContent(contentPayload as AdminContent | null));
    } finally {
      setIsAdminDataLoading(false);
    }
  }

  useEffect(() => {
    if (adminModeration) {
      refreshAdminData().catch(() => {});
    }
  }, [adminModeration, ombudsmanStatusFilter]);

  async function decision(interestId: string, status: "APPROVED" | "REJECTED" | "HIDDEN") {
    setBusyAction(`decision:${interestId}:${status}`);
    try {
      await decideInterestModeration(interestId, { status });
      setFeedback({ type: "success", title: "Decisao aplicada", message: `Procura marcada como ${status}.` });
      await refreshAdminData();
      await refreshPrivateData();
    } finally {
      setBusyAction(null);
    }
  }

  const submitRule: FormSubmitHandler = async (event) => {
    event.preventDefault();
    if (!ruleForm.term.trim()) {
      return;
    }
    setBusyAction("rule:save");
    try {
      await saveModerationRule(ruleForm.id, { term: ruleForm.term.trim(), riskLevel: ruleForm.riskLevel, active: ruleForm.active });
      setRuleForm({ id: null, term: "", riskLevel: "HIGH", active: true });
      setFeedback({ type: "success", title: "Regra salva", message: "A regra de moderacao foi atualizada." });
      await refreshPrivateData();
    } finally {
      setBusyAction(null);
    }
  };

  function editRule(rule: Record<string, unknown>) {
    setRuleForm({
      id: rule.id ? String(rule.id) : null,
      term: String(rule.term ?? ""),
      riskLevel: String(rule.riskLevel ?? "HIGH"),
      active: rule.active !== false
    });
  }

  async function removeRule(ruleId: string) {
    if (!ruleId || !window.confirm("Deseja remover esta regra de moderacao?")) {
      return;
    }
    setBusyAction(`rule:delete:${ruleId}`);
    try {
      await deleteModerationRule(ruleId);
      await refreshPrivateData();
    } finally {
      setBusyAction(null);
    }
  }

  async function clearCache() {
    setBusyAction("cache:clear");
    try {
      await invalidatePublicCache("all");
      setFeedback({ type: "success", title: "Cache limpo", message: "Os caches publicos serao reconstruidos nas proximas leituras." });
    } finally {
      setBusyAction(null);
    }
  }

  async function saveCatalogSettings(nextCatalog = catalog) {
    if (!nextCatalog) {
      return;
    }
    setBusyAction("catalog:save");
    const payload = {
      monetizationSettings: nextCatalog.monetizationSettings ?? {},
      moderationSettings: nextCatalog.moderationSettings ?? {},
      categories: nextCatalog.categories ?? [],
      products: nextCatalog.products ?? []
    };
    try {
      const saved = await saveAdminCatalog(payload);
      setCatalog(saved as AdminCatalog);
      setFeedback({ type: "success", title: "Catalogo salvo", message: "Flags e catalogo operacional foram atualizados." });
    } finally {
      setBusyAction(null);
    }
  }

  function updateCatalogFlag(group: "monetizationSettings" | "moderationSettings", key: string, checked: boolean) {
    setCatalog((current) => current ? { ...current, [group]: { ...(current[group] ?? {}), [key]: checked } } : current);
  }

  function updateCatalogArrayItem(group: "products" | "categories", index: number, key: string, value: unknown) {
    setCatalog((current) => {
      if (!current) {
        return current;
      }
      const items = [...(current[group] ?? [])];
      items[index] = { ...(items[index] ?? {}), [key]: value };
      return { ...current, [group]: items };
    });
  }

  function updateContentEntry(index: number, key: string, value: unknown) {
    setContent((current) => {
      if (!current) {
        return current;
      }
      const entries = [...(current.entries ?? [])];
      entries[index] = { ...(entries[index] ?? {}), [key]: value };
      return { ...current, entries };
    });
  }

  async function saveContent(index: number) {
    const entry = content?.entries?.[index];
    if (!entry) {
      return;
    }
    setBusyAction(`content:save:${String(entry.key ?? index)}`);
    try {
      await saveContentEntry(entry.id ? String(entry.id) : null, entry);
      setFeedback({ type: "success", title: "Conteudo salvo", message: "A entrada do CRM foi atualizada." });
      await loadContentData(true);
    } finally {
      setBusyAction(null);
    }
  }

  async function publishContent(index: number) {
    const entry = content?.entries?.[index];
    if (!entry?.id) {
      return;
    }
    setBusyAction(`content:publish:${String(entry.key ?? index)}`);
    try {
      await publishContentEntry(String(entry.id));
      await loadContentData(true);
    } finally {
      setBusyAction(null);
    }
  }

  async function archiveContent(index: number) {
    const entry = content?.entries?.[index];
    if (!entry?.id) {
      return;
    }
    setBusyAction(`content:archive:${String(entry.key ?? index)}`);
    try {
      await archiveContentEntry(String(entry.id));
      await loadContentData(true);
    } finally {
      setBusyAction(null);
    }
  }

  async function updateOmbudsmanStatus(requestId: string, status: string) {
    setBusyAction(`ombudsman:${requestId}:${status}`);
    try {
      await updateAdminOmbudsmanStatus(requestId, status);
      setOmbudsman(await fetchAdminOmbudsman(ombudsmanStatusFilter === "ALL" ? "" : ombudsmanStatusFilter).catch(() => []));
    } finally {
      setBusyAction(null);
    }
  }

  async function updateReportStatus(reportId: string, status: "RESOLVED" | "DISMISSED") {
    setBusyAction(`report:${reportId}:${status}`);
    try {
      await updateContentReportStatus(reportId, status);
      setFeedback({
        type: "success",
        title: status === "RESOLVED" ? "Denuncia resolvida" : "Denuncia dispensada",
        message: status === "RESOLVED"
          ? "A denuncia foi marcada como analisada. A decisao sobre o anuncio fica na fila de moderacao."
          : "A denuncia foi arquivada sem acao adicional sobre o anuncio."
      });
      await refreshPrivateData();
      await refreshAdminData();
    } finally {
      setBusyAction(null);
    }
  }

  const filteredContentEntries = (content?.entries ?? []).filter((entry) => {
    const query = contentQuery.trim().toLowerCase();
    if (!query) {
      return true;
    }
    return [entry.key, entry.value, entry.defaultValue, entry.status, entry.type].join(" ").toLowerCase().includes(query);
  });

  return (
    <div className="dashboard-page admin-page">
      <div className="dashboard-heading">
        <div><h1>Admin</h1><p>Moderacao, denuncias, conteudo, catalogo operacional, monetizacao e cache.</p></div>
        <div className="inline-actions">
          <Button variant="outline" onClick={refreshAdminData} disabled={isAdminDataLoading || Boolean(busyAction)}><RefreshCw size={16} /> {isAdminDataLoading ? "Atualizando..." : "Atualizar"}</Button>
          <Button onClick={clearCache} disabled={Boolean(busyAction)}>{busyAction === "cache:clear" ? "Limpando..." : "Limpar cache"}</Button>
        </div>
      </div>
      {!adminModeration && isLoadingPrivate ? (
        <div className="section-loading" role="status">Carregando dados administrativos...</div>
      ) : !adminModeration ? (
        <EmptyState title="Admin indisponivel" description="O backend libera esta area apenas para e-mails configurados como administradores." />
      ) : (
        <div className="admin-grid">
          <section className="dashboard-section">
            <h2>Fila de moderacao</h2>
            {adminModeration.pendingInterests?.length ? adminModeration.pendingInterests.map((interest) => (
              <article className="manage-card" key={interest.id}>
                <div><span className="pill">{interest.status}</span><h3>{interest.title}</h3><p>{interest.description}</p></div>
                <div className="inline-actions">
                  <Button size="sm" disabled={Boolean(busyAction)} onClick={() => decision(interest.id, "APPROVED")}>Aprovar</Button>
                  <Button size="sm" disabled={Boolean(busyAction)} variant="outline" onClick={() => decision(interest.id, "HIDDEN")}>Ocultar</Button>
                  <Button size="sm" disabled={Boolean(busyAction)} variant="danger" onClick={() => decision(interest.id, "REJECTED")}>Recusar</Button>
                </div>
              </article>
            )) : <EmptyState title="Fila vazia" description="Nada pendente de revisao manual agora." />}
          </section>

          <section className="dashboard-section">
            <h2>Flags operacionais</h2>
            {catalog ? (
              <div className="settings-list">
                <label className="checkbox-row"><input type="checkbox" checked={Boolean(catalog.monetizationSettings?.creditPurchasesEnabled)} onChange={(event) => updateCatalogFlag("monetizationSettings", "creditPurchasesEnabled", event.target.checked)} /><span>Ativar compra de creditos</span></label>
                <label className="checkbox-row"><input type="checkbox" checked={Boolean(catalog.monetizationSettings?.boostPurchasesEnabled)} onChange={(event) => updateCatalogFlag("monetizationSettings", "boostPurchasesEnabled", event.target.checked)} /><span>Ativar boost de procuras</span></label>
                <label className="checkbox-row"><input type="checkbox" checked={Boolean(catalog.moderationSettings?.userBlockListEnabled)} onChange={(event) => updateCatalogFlag("moderationSettings", "userBlockListEnabled", event.target.checked)} /><span>Ativar block list de usuarios</span></label>
                <Button onClick={() => saveCatalogSettings()} disabled={busyAction === "catalog:save"}><Save size={16} /> {busyAction === "catalog:save" ? "Salvando..." : "Salvar flags"}</Button>
              </div>
            ) : <EmptyState title="Catalogo indisponivel" description="Nao foi possivel carregar as flags operacionais." />}
          </section>

          <section className="dashboard-section">
            <h2>Regras locais</h2>
            <form className="stack-form" onSubmit={submitRule}>
              <label>Termo<input value={ruleForm.term} onChange={(event) => setRuleForm((current) => ({ ...current, term: event.target.value }))} required maxLength={80} /></label>
              <label>Risco<select value={ruleForm.riskLevel} onChange={(event) => setRuleForm((current) => ({ ...current, riskLevel: event.target.value }))}><option value="HIGH">Alto risco</option><option value="MEDIUM">Medio risco</option><option value="LOW">Baixo risco</option></select></label>
              <label className="checkbox-row"><input type="checkbox" checked={ruleForm.active} onChange={(event) => setRuleForm((current) => ({ ...current, active: event.target.checked }))} /><span>Regra ativa</span></label>
              <Button type="submit" disabled={busyAction === "rule:save"}><ShieldCheck size={16} /> {busyAction === "rule:save" ? "Salvando..." : ruleForm.id ? "Atualizar regra" : "Salvar regra"}</Button>
              {ruleForm.id ? <Button type="button" variant="outline" onClick={() => setRuleForm({ id: null, term: "", riskLevel: "HIGH", active: true })}>Cancelar edicao</Button> : null}
            </form>
            <div className="rule-list">
              {(adminModeration.rules ?? []).map((rule) => (
                <article key={String(rule.id ?? rule.term)} className="rule-row">
                  <div>
                    <strong>{String(rule.term ?? "")}</strong>
                    <span>{String(rule.riskLevel ?? "")} - {rule.active === false ? "inativa" : "ativa"}</span>
                  </div>
                  <div className="inline-actions">
                    <Button type="button" size="sm" variant="outline" disabled={Boolean(busyAction)} onClick={() => editRule(rule)}><Pencil size={15} /> Editar</Button>
                    <Button type="button" size="sm" variant="danger" disabled={Boolean(busyAction)} onClick={() => removeRule(String(rule.id ?? ""))}><Trash2 size={15} /> {busyAction === `rule:delete:${String(rule.id ?? "")}` ? "Removendo..." : "Remover"}</Button>
                  </div>
                </article>
              ))}
            </div>
          </section>

          <section className="dashboard-section">
            <h2>Produtos e categorias</h2>
            {catalog ? (
              <div className="admin-edit-list">
                <article><strong>{catalog.products?.length ?? 0} produtos</strong><span>Credito, assinatura e boost</span></article>
                <article><strong>{catalog.categories?.length ?? 0} categorias</strong><span>Rotas SEO e filtros publicos</span></article>
                <h3>Produtos</h3>
                {(catalog.products ?? []).map((product, index) => (
                  <article key={String(product.code ?? product.name ?? index)} className="admin-edit-row">
                    <label>Nome<input value={String(product.name ?? "")} onChange={(event) => updateCatalogArrayItem("products", index, "name", event.target.value)} /></label>
                    <label>Codigo<input value={String(product.code ?? "")} onChange={(event) => updateCatalogArrayItem("products", index, "code", event.target.value)} /></label>
                    <label>Tipo<input value={String(product.type ?? "")} onChange={(event) => updateCatalogArrayItem("products", index, "type", event.target.value)} /></label>
                    <label>Preco<input type="number" min="0" step="0.01" value={Number(product.price ?? 0)} onChange={(event) => updateCatalogArrayItem("products", index, "price", Number(event.target.value))} /></label>
                    <label className="checkbox-row"><input type="checkbox" checked={product.enabled !== false} onChange={(event) => updateCatalogArrayItem("products", index, "enabled", event.target.checked)} /><span>Ativo</span></label>
                  </article>
                ))}
                <h3>Categorias</h3>
                {(catalog.categories ?? []).map((category, index) => (
                  <article key={String(category.value ?? category.label ?? index)} className="admin-edit-row">
                    <label>Valor<input value={String(category.value ?? "")} onChange={(event) => updateCatalogArrayItem("categories", index, "value", event.target.value)} /></label>
                    <label>Label<input value={String(category.label ?? "")} onChange={(event) => updateCatalogArrayItem("categories", index, "label", event.target.value)} /></label>
                    <label>Ordem<input type="number" value={Number(category.sortOrder ?? index)} onChange={(event) => updateCatalogArrayItem("categories", index, "sortOrder", Number(event.target.value))} /></label>
                    <label className="checkbox-row"><input type="checkbox" checked={category.active !== false} onChange={(event) => updateCatalogArrayItem("categories", index, "active", event.target.checked)} /><span>Ativa</span></label>
                  </article>
                ))}
                <Button onClick={() => saveCatalogSettings()} disabled={busyAction === "catalog:save"}><Save size={16} /> {busyAction === "catalog:save" ? "Salvando..." : "Salvar produtos e categorias"}</Button>
              </div>
            ) : <EmptyState title="Sem catalogo" description="Carregue o catalogo para visualizar produtos e categorias." />}
          </section>

          <section className="dashboard-section">
            <h2>Denuncias abertas</h2>
            <p className="admin-help-text">Resolver marca a denuncia como analisada. Dispensar arquiva a denuncia quando nao houver acao necessaria; a decisao sobre o anuncio denunciado continua na fila de moderacao.</p>
            {(adminModeration.openReports ?? []).length ? (adminModeration.openReports ?? []).map((report) => (
              <article className="manage-card" key={String(report.id)}>
                <div><span className="status-pill status-pill--warning">{String(report.contentStatus ?? "REPORT")}</span><h3>{String(report.contentTitle ?? report.reason ?? "Denuncia")}</h3><p>{String(report.message ?? "Sem mensagem adicional.")}</p><small>{String(report.reason ?? "Sem motivo informado.")}</small></div>
                <div className="inline-actions">
                  <Button size="sm" disabled={Boolean(busyAction)} onClick={() => updateReportStatus(String(report.id), "RESOLVED")}>{busyAction === `report:${String(report.id)}:RESOLVED` ? "Resolvendo..." : "Resolver"}</Button>
                  <Button size="sm" disabled={Boolean(busyAction)} variant="outline" onClick={() => updateReportStatus(String(report.id), "DISMISSED")}>{busyAction === `report:${String(report.id)}:DISMISSED` ? "Dispensando..." : "Dispensar"}</Button>
                </div>
              </article>
            )) : <EmptyState title="Nenhuma denuncia aberta" description="As denuncias dos usuarios aparecerao aqui." />}
          </section>

          <section className="dashboard-section">
            <div className="section-heading">
              <h2>Ouvidoria</h2>
              <label className="compact-filter">Status<select value={ombudsmanStatusFilter} onChange={(event) => setOmbudsmanStatusFilter(event.target.value)}><option value="OPEN">Novas/pendentes</option><option value="IN_REVIEW">Em analise</option><option value="ANSWERED">Respondidas</option><option value="CLOSED">Fechadas</option><option value="ALL">Todos</option></select></label>
            </div>
            <span className="admin-help-text">{ombudsman.length} registros no filtro atual</span>
            {ombudsman.length ? (
              <div className="ombudsman-list">
                {ombudsman.map((request) => {
                  const requestId = String(request.id ?? request.protocol ?? "");
                  return (
                    <article key={requestId} className="ombudsman-card">
                      <div className="ombudsman-card__content">
                        <div className="ombudsman-card__header">
                          <span className="status-pill status-pill--warning">{statusLabel(request.status)}</span>
                          <small>{formatDateTime(request.createdAt)}</small>
                        </div>
                        <h3>{request.subject || "Manifestacao"}</h3>
                        <p>{request.message || "Sem mensagem adicional."}</p>
                        <dl className="ombudsman-meta">
                          <div><dt>Protocolo</dt><dd>{request.protocol || "Sem protocolo"}</dd></div>
                          <div><dt>Tipo</dt><dd>{request.type || "Ouvidoria"}</dd></div>
                          <div><dt>Contato</dt><dd>{request.email || request.name || "Nao informado"}</dd></div>
                        </dl>
                      </div>
                      <div className="ombudsman-actions">
                        <Button size="sm" variant="outline" disabled={Boolean(busyAction)} onClick={() => updateOmbudsmanStatus(requestId, "IN_REVIEW")}>Em analise</Button>
                        <Button size="sm" disabled={Boolean(busyAction)} onClick={() => updateOmbudsmanStatus(requestId, "ANSWERED")}>Marcar respondida</Button>
                        <Button size="sm" variant="danger" disabled={Boolean(busyAction)} onClick={() => updateOmbudsmanStatus(requestId, "CLOSED")}>Fechar</Button>
                      </div>
                    </article>
                  );
                })}
              </div>
            ) : (
              <EmptyState title={isAdminDataLoading ? "Carregando ouvidoria" : "Nenhuma manifestacao"} description={isAdminDataLoading ? "Buscando entradas da ouvidoria..." : "As solicitacoes de ouvidoria aparecerao aqui."} />
            )}
          </section>

          <section className="dashboard-section admin-collapsible-section">
            <button
              type="button"
              className="admin-section-toggle"
              onClick={() => {
                const nextOpen = !isContentOpen;
                setIsContentOpen(nextOpen);
                if (nextOpen) {
                  loadContentData().catch(() => {});
                }
              }}
              aria-expanded={isContentOpen}
            >
              <span>
                <strong>Conteudo do CRM</strong>
                <small>{content?.entries?.length ? `${content.entries.length} entradas carregadas` : "Clique para carregar e pesquisar"}</small>
              </span>
              <ChevronDown size={18} />
            </button>
            {isContentOpen ? (
              <>
              <label className="admin-search-field">
                Buscar por chave ou valor
                <input value={contentQuery} onChange={(event) => setContentQuery(event.target.value)} placeholder="Ex: address.lookup ou CEP" />
              </label>
              {filteredContentEntries.length ? (
              <div className="admin-edit-list">
                {filteredContentEntries.map((entry) => {
                  const index = content?.entries?.indexOf(entry) ?? 0;
                  return (
                  <article key={String(entry.id ?? entry.key ?? index)} className="admin-edit-row admin-edit-row--content">
                    <label>Chave<input value={String(entry.key ?? "")} onChange={(event) => updateContentEntry(index, "key", event.target.value)} /></label>
                    <label>Status<input value={String(entry.status ?? "")} onChange={(event) => updateContentEntry(index, "status", event.target.value)} /></label>
                    <label>Valor<textarea rows={4} value={String(entry.value ?? entry.defaultValue ?? "")} onChange={(event) => updateContentEntry(index, "value", event.target.value)} /></label>
                    <div className="inline-actions">
                      <Button size="sm" disabled={Boolean(busyAction)} onClick={() => saveContent(index)}>{busyAction === `content:save:${String(entry.key ?? index)}` ? "Salvando..." : "Salvar"}</Button>
                      {entry.id ? <Button size="sm" disabled={Boolean(busyAction)} variant="outline" onClick={() => publishContent(index)}>{busyAction === `content:publish:${String(entry.key ?? index)}` ? "Publicando..." : "Publicar"}</Button> : null}
                      {entry.id ? <Button size="sm" disabled={Boolean(busyAction)} variant="outline" onClick={() => archiveContent(index)}>{busyAction === `content:archive:${String(entry.key ?? index)}` ? "Arquivando..." : "Arquivar"}</Button> : null}
                    </div>
                  </article>
                  );
                })}
              </div>
              ) : <EmptyState title={isAdminDataLoading ? "Carregando conteudo" : "Nenhum conteudo encontrado"} description={isAdminDataLoading ? "Buscando entradas do CRM..." : "Ajuste a busca ou atualize os dados."} />}
              </>
            ) : null}
          </section>
        </div>
      )}
    </div>
  );
}
