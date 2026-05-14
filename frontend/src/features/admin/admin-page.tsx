"use client";

import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { ChevronDown, Pencil, RefreshCw, Save, ShieldCheck, Trash2 } from "lucide-react";
import { usePlatform } from "@/features/platform/platform-context";
import {
  decideInterestModeration,
  deleteModerationRule,
  fetchAdminCatalog,
  fetchAdminContent,
  invalidatePublicCache,
  archiveContentEntry,
  publishContentEntry,
  saveAdminCatalog,
  saveContentEntry,
  saveModerationRule,
  updateContentReportStatus
} from "@/shared/api/client";
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

export function AdminPage() {
  const { adminModeration, refreshPrivateData, setFeedback } = usePlatform();
  const [ruleForm, setRuleForm] = useState<{ id: string | null; term: string; riskLevel: string; active: boolean }>({ id: null, term: "", riskLevel: "HIGH", active: true });
  const [catalog, setCatalog] = useState<AdminCatalog | null>(null);
  const [content, setContent] = useState<AdminContent | null>(null);
  const [isAdminDataLoading, setIsAdminDataLoading] = useState(false);
  const [isContentOpen, setIsContentOpen] = useState(false);
  const [contentQuery, setContentQuery] = useState("");

  async function refreshAdminData() {
    setIsAdminDataLoading(true);
    try {
      const catalogPayload = await fetchAdminCatalog().catch(() => null);
      setCatalog(catalogPayload as AdminCatalog | null);
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
      setContent(contentPayload as AdminContent | null);
    } finally {
      setIsAdminDataLoading(false);
    }
  }

  useEffect(() => {
    if (adminModeration) {
      refreshAdminData().catch(() => {});
    }
  }, [adminModeration]);

  async function decision(interestId: string, status: "APPROVED" | "REJECTED" | "HIDDEN") {
    await decideInterestModeration(interestId, { status });
    setFeedback({ type: "success", title: "Decisao aplicada", message: `Procura marcada como ${status}.` });
    await refreshAdminData();
  }

  async function submitRule(event: FormEvent) {
    event.preventDefault();
    await saveModerationRule(ruleForm.id, { term: ruleForm.term, riskLevel: ruleForm.riskLevel, active: ruleForm.active });
    setRuleForm({ id: null, term: "", riskLevel: "HIGH", active: true });
    setFeedback({ type: "success", title: "Regra salva", message: "A regra de moderacao foi atualizada." });
    await refreshPrivateData();
  }

  function editRule(rule: Record<string, unknown>) {
    setRuleForm({
      id: rule.id ? String(rule.id) : null,
      term: String(rule.term ?? ""),
      riskLevel: String(rule.riskLevel ?? "HIGH"),
      active: rule.active !== false
    });
  }

  async function clearCache() {
    await invalidatePublicCache("all");
    setFeedback({ type: "success", title: "Cache limpo", message: "Os caches publicos serao reconstruidos nas proximas leituras." });
  }

  async function saveCatalogSettings(nextCatalog = catalog) {
    if (!nextCatalog) {
      return;
    }
    const payload = {
      monetizationSettings: nextCatalog.monetizationSettings ?? {},
      moderationSettings: nextCatalog.moderationSettings ?? {},
      categories: nextCatalog.categories ?? [],
      products: nextCatalog.products ?? []
    };
    const saved = await saveAdminCatalog(payload);
    setCatalog(saved as AdminCatalog);
    setFeedback({ type: "success", title: "Catalogo salvo", message: "Flags e catalogo operacional foram atualizados." });
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
    await saveContentEntry(entry.id ? String(entry.id) : null, entry);
    setFeedback({ type: "success", title: "Conteudo salvo", message: "A entrada do CRM foi atualizada." });
    await loadContentData(true);
  }

  const filteredContentEntries = (content?.entries ?? []).filter((entry) => {
    const query = contentQuery.trim().toLowerCase();
    if (!query) {
      return true;
    }
    return [entry.key, entry.value, entry.status, entry.type].join(" ").toLowerCase().includes(query);
  });

  return (
    <div className="dashboard-page admin-page">
      <div className="dashboard-heading">
        <div><h1>Admin</h1><p>Moderacao, denuncias, conteudo, catalogo operacional, monetizacao e cache.</p></div>
        <div className="inline-actions">
          <Button variant="outline" onClick={refreshAdminData} disabled={isAdminDataLoading}><RefreshCw size={16} /> Atualizar</Button>
          <Button onClick={clearCache}>Limpar cache</Button>
        </div>
      </div>
      {!adminModeration ? (
        <EmptyState title="Admin indisponivel" description="O backend libera esta area apenas para e-mails configurados como administradores." />
      ) : (
        <div className="admin-grid">
          <section className="dashboard-section">
            <h2>Fila de moderacao</h2>
            {adminModeration.pendingInterests?.length ? adminModeration.pendingInterests.map((interest) => (
              <article className="manage-card" key={interest.id}>
                <div><span className="pill">{interest.status}</span><h3>{interest.title}</h3><p>{interest.description}</p></div>
                <div className="inline-actions">
                  <Button size="sm" onClick={() => decision(interest.id, "APPROVED")}>Aprovar</Button>
                  <Button size="sm" variant="outline" onClick={() => decision(interest.id, "HIDDEN")}>Ocultar</Button>
                  <Button size="sm" variant="danger" onClick={() => decision(interest.id, "REJECTED")}>Recusar</Button>
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
                <Button onClick={() => saveCatalogSettings()}><Save size={16} /> Salvar flags</Button>
              </div>
            ) : <EmptyState title="Catalogo indisponivel" description="Nao foi possivel carregar as flags operacionais." />}
          </section>

          <section className="dashboard-section">
            <h2>Regras locais</h2>
            <form className="stack-form" onSubmit={submitRule}>
              <label>Termo<input value={ruleForm.term} onChange={(event) => setRuleForm((current) => ({ ...current, term: event.target.value }))} required maxLength={80} /></label>
              <label>Risco<select value={ruleForm.riskLevel} onChange={(event) => setRuleForm((current) => ({ ...current, riskLevel: event.target.value }))}><option value="HIGH">Alto risco</option><option value="MEDIUM">Medio risco</option><option value="LOW">Baixo risco</option></select></label>
              <label className="checkbox-row"><input type="checkbox" checked={ruleForm.active} onChange={(event) => setRuleForm((current) => ({ ...current, active: event.target.checked }))} /><span>Regra ativa</span></label>
              <Button type="submit"><ShieldCheck size={16} /> {ruleForm.id ? "Atualizar regra" : "Salvar regra"}</Button>
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
                    <Button type="button" size="sm" variant="outline" onClick={() => editRule(rule)}><Pencil size={15} /> Editar</Button>
                    <Button type="button" size="sm" variant="danger" onClick={() => window.confirm("Deseja remover esta regra de moderacao?") && deleteModerationRule(String(rule.id)).then(refreshPrivateData)}><Trash2 size={15} /> Remover</Button>
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
                <Button onClick={() => saveCatalogSettings()}><Save size={16} /> Salvar produtos e categorias</Button>
              </div>
            ) : <EmptyState title="Sem catalogo" description="Carregue o catalogo para visualizar produtos e categorias." />}
          </section>

          <section className="dashboard-section">
            <h2>Denuncias abertas</h2>
            {(adminModeration.openReports ?? []).length ? (adminModeration.openReports ?? []).map((report) => (
              <article className="manage-card" key={String(report.id)}>
                <div><h3>{String(report.reason ?? "Denuncia")}</h3><p>{String(report.message ?? "Sem mensagem adicional.")}</p></div>
                <div className="inline-actions">
                  <Button size="sm" onClick={() => updateContentReportStatus(String(report.id), "RESOLVED").then(refreshAdminData)}>Resolver</Button>
                  <Button size="sm" variant="outline" onClick={() => updateContentReportStatus(String(report.id), "DISMISSED").then(refreshAdminData)}>Dispensar</Button>
                </div>
              </article>
            )) : <EmptyState title="Nenhuma denuncia aberta" description="As denuncias dos usuarios aparecerao aqui." />}
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
                    <label>Valor<textarea rows={4} value={String(entry.value ?? "")} onChange={(event) => updateContentEntry(index, "value", event.target.value)} /></label>
                    <div className="inline-actions">
                      <Button size="sm" onClick={() => saveContent(index)}>Salvar</Button>
                      {entry.id ? <Button size="sm" variant="outline" onClick={() => publishContentEntry(String(entry.id)).then(() => loadContentData(true))}>Publicar</Button> : null}
                      {entry.id ? <Button size="sm" variant="outline" onClick={() => archiveContentEntry(String(entry.id)).then(() => loadContentData(true))}>Arquivar</Button> : null}
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
