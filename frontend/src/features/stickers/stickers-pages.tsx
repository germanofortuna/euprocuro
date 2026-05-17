"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, ArrowRight, Filter, Plus, Search, Sparkles, Trash2, Trophy, Users, Zap } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import type { ComponentProps } from "react";
import { trackEvent } from "@/features/analytics/analytics";
import { usePlatform } from "@/features/platform/platform-context";
import { fetchInterests, lookupAddressByPostalCode } from "@/shared/api/client";
import type { Interest } from "@/shared/api/types";
import { formatCep, limitText } from "@/shared/lib/format";
import { AuthIntentLink } from "@/shared/ui/auth-intent-link";
import { Button } from "@/shared/ui/button";
import { EmptyState } from "@/shared/ui/empty-state";
import { SPECIAL_STICKER_SELECTIONS, STICKERS_CATEGORY, stickerGroupForSelection, stickerGroups, normalizeStickerNumbers } from "./stickers-data";

type FormSubmitHandler = NonNullable<ComponentProps<"form">["onSubmit"]>;
type StickerPublishEntry = { id: string; selection: string; numbers: string };

const emptyStickerFilters = { stickerType: "", stickerGroup: "", stickerSelection: "", stickerNumber: "", state: "", city: "", neighborhood: "" };

const benefits = [
  { icon: Users, title: "Encontre colecionadores", description: "Veja quem procura ou tem repetidas da Copa 2026 na sua região." },
  { icon: Zap, title: "Trocas inteligentes", description: "Filtre por grupo, seleção e números para achar exatamente o que falta." },
  { icon: Trophy, title: "Complete o álbum", description: "Receba propostas no fluxo seguro de mensagens do Eu Procuro." }
];

function stickerTypeLabel(type?: string | null) {
  return type === "AVAILABLE" ? "Tenho repetidas" : "Procuro faltantes";
}

function selectionOptions() {
  return Object.entries(stickerGroups());
}

function createStickerPublishEntry(): StickerPublishEntry {
  return { id: `stickers-${Date.now()}-${Math.random().toString(36).slice(2)}`, selection: "", numbers: "" };
}

function selectionsForGroup(group: string, groups: ReturnType<typeof selectionOptions>) {
  if (!group) {
    return groups;
  }
  if (group === "SPECIAL") {
    return [];
  }
  return groups.filter(([currentGroup]) => currentGroup === group);
}

function stickerLocationParts(item: Interest) {
  return [
    item.location?.state ? `UF: ${item.location.state}` : "",
    item.location?.city ? `Cidade: ${item.location.city}` : "",
    item.location?.neighborhood ? `Bairro: ${item.location.neighborhood}` : ""
  ].filter(Boolean);
}

function stickerTitleParts(item: Interest) {
  const type = item.stickerDetails?.type;
  const action = type === "AVAILABLE" ? "Tenho repetidas" : "Procuro figurinhas";
  const separator = type === "AVAILABLE" ? " - " : ": ";
  const selection = item.stickerDetails?.selection ?? "Copa 2026";
  const numbers = (item.stickerDetails?.numbers ?? []).slice(0, 8).join(", ");
  return { action, separator, selection, numbers };
}

function normalizeFilterText(value?: string | null) {
  return String(value ?? "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}

function matchesLocationFilters(item: Interest, filters: typeof emptyStickerFilters) {
  const state = normalizeFilterText(filters.state);
  const city = normalizeFilterText(filters.city);
  const neighborhood = normalizeFilterText(filters.neighborhood);
  const itemState = normalizeFilterText(item.location?.state);
  const itemCity = normalizeFilterText(item.location?.city);
  const itemNeighborhood = normalizeFilterText(item.location?.neighborhood);

  return (!state || itemState === state)
    && (!city || itemCity.includes(city))
    && (!neighborhood || itemNeighborhood.includes(neighborhood));
}

export function StickersLandingPage() {
  const { dashboard, operationalSettings } = usePlatform();
  const [filters, setFilters] = useState(emptyStickerFilters);
  const [stickers, setStickers] = useState<Interest[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const searchSequenceRef = useRef(0);
  const groups = useMemo(selectionOptions, []);
  const visibleSelectionGroups = useMemo(() => selectionsForGroup(filters.stickerGroup, groups), [filters.stickerGroup, groups]);
  const myStickerIds = useMemo(
    () => new Set((dashboard?.myInterests ?? []).filter((item) => item.category === STICKERS_CATEGORY).map((item) => item.id)),
    [dashboard?.myInterests]
  );
  const stickersEnabled = operationalSettings.featureFlags?.stickersPageEnabled !== false;

  async function loadStickers(nextFilters = filters) {
    const searchSequence = searchSequenceRef.current + 1;
    searchSequenceRef.current = searchSequence;
    if (!stickersEnabled) {
      setStickers([]);
      return;
    }
    setIsLoading(true);
    try {
      const payload = await fetchInterests({
        category: STICKERS_CATEGORY,
        limit: 24,
        includeOwn: "true",
        ...nextFilters
      });
      if (searchSequenceRef.current === searchSequence) {
        setStickers(payload.filter((item) => matchesLocationFilters(item, nextFilters)));
      }
    } finally {
      if (searchSequenceRef.current === searchSequence) {
        setIsLoading(false);
      }
    }
  }

  useEffect(() => {
    if (!stickersEnabled) {
      setStickers([]);
      return;
    }
    const timer = window.setTimeout(() => {
      loadStickers(filters).catch(() => setStickers([]));
    }, 380);
    return () => window.clearTimeout(timer);
  }, [filters, stickersEnabled]);

  const applyFilters: FormSubmitHandler = (event) => {
    event.preventDefault();
  };

  function updateFilters(nextFilters: typeof filters) {
    setStickers([]);
    setFilters(nextFilters);
    const { city, state, neighborhood, ...analyticsFilters } = nextFilters;
    trackEvent("stickers_filter_applied", {
      ...analyticsFilters,
      hasCity: Boolean(city),
      hasState: Boolean(state),
      hasNeighborhood: Boolean(neighborhood)
    });
  }

  function clearFilters() {
    updateFilters(emptyStickerFilters);
  };

  if (!stickersEnabled) {
    return <section className="route-shell"><EmptyState title="Figurinhas indisponível" description="Esta página está temporariamente desabilitada." /></section>;
  }

  return (
    <div className="stickers-page">
      <section className="stickers-hero">
        <div className="stickers-hero__pattern" aria-hidden="true" />
        <div className="stickers-hero__content">
          <span className="stickers-badge"><Sparkles size={18} /> Copa 2026 - Complete seu álbum! <Sparkles size={18} /></span>
          <h1>Troque Figurinhas <span>da Copa 2026</span></h1>
          <p>Publique suas <strong>figurinhas faltantes</strong> ou <strong>repetidas</strong> e receba propostas de outros colecionadores.</p>
          <div className="stickers-hero__actions">
            <AuthIntentLink className="button button--secondary button--lg" href="/figurinhas/publicar" onClick={() => trackEvent("stickers_publish_cta_clicked")}>
              <Sparkles size={19} /> Publicar Minhas Figurinhas
            </AuthIntentLink>
            <a className="button button--outline button--lg stickers-hero__dark-button" href="#figurinhas-disponiveis" onClick={() => trackEvent("stickers_view_available_clicked")}>Ver Figurinhas Disponíveis</a>
          </div>
        </div>
      </section>

      <section id="figurinhas-disponiveis" className="marketplace-section stickers-market">
        <div className="section-heading">
          <h2><Search size={24} /> Figurinhas publicadas</h2>
          <AuthIntentLink className="button button--primary button--sm stickers-section-publish" href="/figurinhas/publicar">Publicar</AuthIntentLink>
        </div>
        <div className="stickers-grid-layout">
          <aside className="filter-panel stickers-filter-panel">
            <h2><Filter size={19} /> Filtros</h2>
            <form className="stack-form" onSubmit={applyFilters}>
              <label>Tipo<select value={filters.stickerType} onChange={(event) => updateFilters({ ...filters, stickerType: event.target.value })}><option value="">Todos</option><option value="MISSING">Faltantes</option><option value="AVAILABLE">Repetidas</option></select></label>
              <label>Grupo<select value={filters.stickerGroup} onChange={(event) => updateFilters({ ...filters, stickerGroup: event.target.value, stickerSelection: "" })}><option value="">Todos os grupos</option>{groups.map(([group]) => <option key={group} value={group}>Grupo {group}</option>)}<option value="SPECIAL">Especiais</option></select></label>
              <label>Seleção<select value={filters.stickerSelection} onChange={(event) => updateFilters({ ...filters, stickerSelection: event.target.value })}><option value="">Todas</option>{visibleSelectionGroups.map(([group, selections]) => <optgroup key={group} label={`Grupo ${group}`}>{selections.map((selection) => <option key={selection.name} value={selection.name}>{selection.emblem} {selection.name}</option>)}</optgroup>)}{filters.stickerGroup === "" || filters.stickerGroup === "SPECIAL" ? <optgroup label="Especiais">{SPECIAL_STICKER_SELECTIONS.map((item) => <option key={item} value={item}>{item}</option>)}</optgroup> : null}</select></label>
              <label>Número<input value={filters.stickerNumber} onChange={(event) => updateFilters({ ...filters, stickerNumber: event.target.value })} placeholder="Ex: 12 ou FW26" /></label>
              <div className="form-grid form-grid--3 stickers-location-grid">
                <label>UF<input value={filters.state} onChange={(event) => updateFilters({ ...filters, state: event.target.value.toUpperCase().slice(0, 2) })} placeholder="SP" /></label>
                <label>Cidade<input value={filters.city} onChange={(event) => updateFilters({ ...filters, city: event.target.value })} placeholder="Cidade" /></label>
                <label>Bairro<input value={filters.neighborhood} onChange={(event) => updateFilters({ ...filters, neighborhood: event.target.value })} placeholder="Bairro" /></label>
              </div>
              <Button type="button" variant="outline" onClick={clearFilters}>Limpar Filtros</Button>
            </form>
          </aside>
          <div className="stickers-list">
            {isLoading ? <div className="section-loading" role="status">Carregando figurinhas...</div> : null}
            {!isLoading && stickers.length ? stickers.map((item) => {
              const titleParts = stickerTitleParts(item);
              return (
                <Link key={item.id} href={`/interesses/${item.id}`} className="sticker-card">
                  {item.boostedUntil ? <span className="sticker-card__boost"><Zap size={13} /> Destaque</span> : null}
                  <div className="sticker-card__badges">
                    <span className={`sticker-card__type sticker-card__type--${item.stickerDetails?.type === "AVAILABLE" ? "available" : "missing"}`}>{stickerTypeLabel(item.stickerDetails?.type)}</span>
                    {myStickerIds.has(item.id) ? <span className="sticker-card__own">Seu registro</span> : null}
                  </div>
                  <h3 className="sticker-card__title">
                    <span>{titleParts.action}{titleParts.separator}</span>
                    <span className="sticker-card__selection">{titleParts.selection}</span>
                    {titleParts.numbers ? <span>: {titleParts.numbers}</span> : null}
                  </h3>
                  <p>{item.description}</p>
                  <div className="sticker-card__meta-grid">
                    <span><small>Seleção</small>{item.stickerDetails?.selection ?? "Copa 2026"}</span>
                    <span><small>Grupo</small>{item.stickerDetails?.group === "SPECIAL" ? "Especiais" : `Grupo ${item.stickerDetails?.group ?? "-"}`}</span>
                    {stickerLocationParts(item).map((part) => <span key={part}><small>{part.split(":")[0]}</small>{part.split(":").slice(1).join(":").trim()}</span>)}
                  </div>
                  <div className="sticker-number-list">{(item.stickerDetails?.numbers ?? []).slice(0, 10).map((number) => <span key={number}>{number}</span>)}</div>
                  <strong>Ver detalhes e fazer proposta <ArrowRight size={16} /></strong>
                </Link>
              );
            }) : null}
            {!isLoading && !stickers.length ? <EmptyState title="Nenhuma figurinha encontrada" description="Ajuste os filtros ou publique a primeira procura de figurinhas." /> : null}
          </div>
        </div>
      </section>

      <section className="stickers-benefits">
        <div className="benefit-grid">
          {benefits.map((benefit) => {
            const Icon = benefit.icon;
            return <article key={benefit.title}><Icon /><h3>{benefit.title}</h3><p>{benefit.description}</p></article>;
          })}
        </div>
      </section>
    </div>
  );
}

function buildTitle(type: "MISSING" | "AVAILABLE", selection: string, numbers: string[]) {
  const verb = type === "MISSING" ? "Procuro figurinhas" : "Tenho repetidas";
  const target = selection ? `${selection}` : "Copa 2026";
  return limitText(`${verb} - ${target}: ${numbers.slice(0, 8).join(", ")}`, 80);
}

function buildDescription(type: "MISSING" | "AVAILABLE", selection: string, numbers: string[], extra: string) {
  const action = type === "MISSING" ? "Figurinhas faltantes" : "Figurinhas disponíveis para troca ou venda";
  const base = `${action}:\n${numbers.join(", ")}${selection ? ` - ${selection}` : ""}.${extra ? `\n${extra}` : ""}`.trim();
  return limitText(base, 120);
}

export function PublishStickersPage() {
  const router = useRouter();
  const { currentUser, openAuthModal, saveInterest, setFeedback, operationalSettings } = usePlatform();
  const [form, setForm] = useState({
    type: "MISSING" as "MISSING" | "AVAILABLE",
    postalCode: "",
    city: currentUser?.city ?? "",
    state: currentUser?.state ?? "",
    neighborhood: currentUser?.neighborhood ?? "",
    country: currentUser?.country ?? "Brasil",
    description: "",
    tags: ""
  });
  const [entries, setEntries] = useState<StickerPublishEntry[]>([{ id: "stickers-1", selection: "", numbers: "" }]);
  const [isSaving, setIsSaving] = useState(false);
  const [publishLookupState, setPublishLookupState] = useState<{ loading: boolean; message: string; tone: "muted" | "success" | "error" }>({ loading: false, message: "", tone: "muted" });
  const groups = useMemo(selectionOptions, []);
  const previewEntries = entries
    .map((entry) => ({ ...entry, numbers: normalizeStickerNumbers(entry.numbers), group: stickerGroupForSelection(entry.selection) }))
    .filter((entry) => entry.selection || entry.numbers.length);
  const stickersEnabled = operationalSettings.featureFlags?.stickersPageEnabled !== false;

  useEffect(() => {
    setForm((current) => ({
      ...current,
      postalCode: current.postalCode || currentUser?.postalCode || "",
      city: current.city || currentUser?.city || "",
      state: current.state || currentUser?.state || "",
      neighborhood: current.neighborhood || currentUser?.neighborhood || "",
      country: current.country || currentUser?.country || "Brasil"
    }));
  }, [currentUser?.city, currentUser?.country, currentUser?.neighborhood, currentUser?.postalCode, currentUser?.state]);

  async function lookupPublishPostalCode(postalCode = form.postalCode) {
    const normalizedPostalCode = String(postalCode).replace(/\D/g, "");
    if (!normalizedPostalCode) {
      setPublishLookupState({ loading: false, message: "", tone: "muted" });
      return;
    }
    if (normalizedPostalCode.length !== 8) {
      setPublishLookupState({ loading: false, message: "Digite um CEP com 8 números.", tone: "error" });
      return;
    }
    setPublishLookupState({ loading: true, message: "Buscando endereço pelo CEP...", tone: "muted" });
    try {
      const address = await lookupAddressByPostalCode(normalizedPostalCode);
      setForm((current) => ({
        ...current,
        postalCode: formatCep(String(address.postalCode ?? current.postalCode)),
        city: String(address.city ?? current.city ?? ""),
        state: String(address.state ?? current.state ?? "").toUpperCase().slice(0, 2),
        neighborhood: String(address.neighborhood ?? current.neighborhood ?? ""),
        country: String(address.country ?? current.country ?? "Brasil")
      }));
      setPublishLookupState({ loading: false, message: "Endereço preenchido pelo CEP.", tone: "success" });
    } catch (error) {
      setPublishLookupState({ loading: false, message: error instanceof Error ? error.message : "Não encontramos esse CEP. Preencha cidade, UF e bairro manualmente.", tone: "error" });
    }
  }

  useEffect(() => {
    const normalizedPostalCode = form.postalCode.replace(/\D/g, "");
    if (normalizedPostalCode.length !== 8) {
      return;
    }
    const timer = window.setTimeout(() => {
      lookupPublishPostalCode(normalizedPostalCode);
    }, 380);
    return () => window.clearTimeout(timer);
  }, [form.postalCode]);

  const submit: FormSubmitHandler = async (event) => {
    event.preventDefault();
    if (!currentUser?.id) {
      openAuthModal("login", "/figurinhas/publicar");
      return;
    }
    const publishEntries = entries
      .map((entry) => ({
        selection: entry.selection,
        numbers: normalizeStickerNumbers(entry.numbers),
        group: stickerGroupForSelection(entry.selection)
      }))
      .filter((entry) => entry.selection || entry.numbers.length);
    if (!publishEntries.length || publishEntries.some((entry) => !entry.selection || !entry.numbers.length)) {
      setFeedback({ type: "error", title: "Informe as figurinhas", message: "Digite pelo menos um número ou código de figurinha." });
      return;
    }
    setIsSaving(true);
    try {
      trackEvent("stickers_publish_started", { type: form.type, totalSelections: publishEntries.length });
      const savedItems = [];
      for (const entry of publishEntries) {
        const saved = await saveInterest({
          title: buildTitle(form.type, entry.selection, entry.numbers),
          description: buildDescription(form.type, entry.selection, entry.numbers, form.description),
          category: STICKERS_CATEGORY,
          budgetMin: null,
          budgetMax: 0,
          postalCode: form.postalCode,
          city: form.city,
          state: form.state.toUpperCase().slice(0, 2),
          neighborhood: form.neighborhood,
          country: form.country || "Brasil",
          desiredRadiusKm: 25,
          preferredCondition: form.type === "MISSING" ? "Faltantes" : "Repetidas",
          preferredContactMode: "CHAT",
          tags: ["copa-2026", "figurinhas", form.type === "MISSING" ? "faltantes" : "repetidas", entry.selection, ...form.tags.split(",")].map((tag) => tag.trim()).filter(Boolean),
          stickerDetails: {
            type: form.type,
            group: entry.group || "SPECIAL",
            selection: entry.selection || null,
            numbers: entry.numbers
          }
        });
        savedItems.push(saved);
      }
      trackEvent("stickers_publish_completed", { type: form.type, totalSelections: savedItems.length, interestId: savedItems[0]?.id });
      if (savedItems.length > 1) {
        router.push("/meus-interesses");
      } else if (savedItems[0]?.id) {
        const saved = savedItems[0];
        router.push(`/interesses/${saved.id}`);
      }
    } catch (error) {
      setFeedback({ type: "error", title: "Não foi possível publicar", message: error instanceof Error ? error.message : "Revise os dados e tente novamente." });
    } finally {
      setIsSaving(false);
    }
  };

  function updateEntry(id: string, patch: Partial<StickerPublishEntry>) {
    setEntries((current) => current.map((entry) => entry.id === id ? { ...entry, ...patch } : entry));
  }

  function addEntry() {
    setEntries((current) => [...current, createStickerPublishEntry()]);
  }

  function removeEntry(id: string) {
    setEntries((current) => current.length > 1 ? current.filter((entry) => entry.id !== id) : current);
  }

  if (!stickersEnabled) {
    return <section className="route-shell"><EmptyState title="Figurinhas indisponível" description="Esta página está temporariamente desabilitada." /></section>;
  }

  return (
    <section className="route-shell stickers-publish">
      <Link href="/figurinhas" className="back-link"><ArrowLeft size={16} /> Voltar para Figurinhas</Link>
      <div className="form-heading">
        <span className="pill">Copa 2026</span>
        <h1>Publicar Figurinhas</h1>
        <p>Informe suas faltantes ou repetidas para receber propostas de outros colecionadores.</p>
      </div>
      <form className="feature-form stickers-form" onSubmit={submit}>
        <section className="form-section">
          <h2>Informações das figurinhas</h2>
          <div className="segmented-control">
            <button type="button" className={form.type === "MISSING" ? "is-active" : ""} onClick={() => setForm((current) => ({ ...current, type: "MISSING" }))}>Procuro faltantes</button>
            <button type="button" className={form.type === "AVAILABLE" ? "is-active" : ""} onClick={() => setForm((current) => ({ ...current, type: "AVAILABLE" }))}>Tenho repetidas</button>
          </div>
          <div className="sticker-entry-list">
            {entries.map((entry, index) => (
              <div className="sticker-entry-row" key={entry.id}>
                <span className="sticker-entry-index">Seleção {index + 1}</span>
                <label>Seleção ou especial<select value={entry.selection} onChange={(event) => updateEntry(entry.id, { selection: event.target.value })} required><option value="">Selecione a seleção ou tipo especial...</option>{groups.map(([group, selections]) => <optgroup key={group} label={`Grupo ${group}`}>{selections.map((selection) => <option key={selection.name} value={selection.name}>{selection.emblem} {selection.name}</option>)}</optgroup>)}<optgroup label="Especiais">{SPECIAL_STICKER_SELECTIONS.map((item) => <option key={item} value={item}>{item}</option>)}</optgroup></select></label>
                <label>Números das figurinhas<input value={entry.numbers} onChange={(event) => updateEntry(entry.id, { numbers: event.target.value })} placeholder="Ex: 12, 45, 78, FW26" required /></label>
                <Button type="button" variant="outline" className="sticker-entry-remove" onClick={() => removeEntry(entry.id)} disabled={entries.length === 1} title={entries.length === 1 ? "Mantenha pelo menos uma seleção" : "Remover seleção"}><Trash2 size={16} /><span>Remover</span></Button>
              </div>
            ))}
          </div>
          <div className="sticker-entry-add-row">
            <Button type="button" variant="outline" className="sticker-entry-add" onClick={addEntry}><Plus size={16} /> Adicionar outra seleção</Button>
            <small>A cada seleção adicionada, outro card de interesse será criado.</small>
          </div>
          <label>Descrição adicional<textarea rows={4} value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: limitText(event.target.value, 80) }))} placeholder="Ex: aceito troca presencial, envio por correio ou compra em lote." /></label>
          <label>Tags<input value={form.tags} onChange={(event) => setForm((current) => ({ ...current, tags: event.target.value }))} placeholder="Ex: troca presencial, urgente, lote" /></label>
        </section>
        <section className="form-section">
          <h2>Localização</h2>
          <div className="form-grid form-grid--3">
            <label>CEP<input value={form.postalCode} onChange={(event) => setForm((current) => ({ ...current, postalCode: formatCep(event.target.value) }))} onBlur={() => lookupPublishPostalCode()} placeholder="00000-000" /></label>
            <label>Cidade<input value={form.city} onChange={(event) => setForm((current) => ({ ...current, city: event.target.value }))} required /></label>
            <label>UF<input value={form.state} onChange={(event) => setForm((current) => ({ ...current, state: event.target.value.toUpperCase().slice(0, 2) }))} required /></label>
          </div>
          {publishLookupState.message ? <span className={`address-lookup-note address-lookup-note--${publishLookupState.tone}`} role="status" aria-live="polite" aria-busy={publishLookupState.loading}>{publishLookupState.message}</span> : null}
          <span className="address-lookup-note">Usamos os dados do seu cadastro como sugestão. Ajuste cidade, UF e bairro se a troca for em outro local.</span>
          <div className="form-grid">
            <label>Bairro<input value={form.neighborhood} onChange={(event) => setForm((current) => ({ ...current, neighborhood: event.target.value }))} placeholder="Bairro" /></label>
            <label>País<input value={form.country} onChange={(event) => setForm((current) => ({ ...current, country: event.target.value }))} /></label>
          </div>
          {previewEntries.length ? (
            <div className="sticker-preview">
              <strong>Prévia do anúncio</strong>
              {previewEntries.slice(0, 4).map((entry) => (
                <div className="sticker-preview-entry" key={entry.id}>
                  <p>{buildTitle(form.type, entry.selection, entry.numbers)}</p>
                  <div className="sticker-number-list">{entry.numbers.slice(0, 12).map((number) => <span key={number}>{number}</span>)}</div>
                </div>
              ))}
            </div>
          ) : null}
        </section>
        <div className="form-actions">
          <Link className="button button--outline" href="/figurinhas">Cancelar</Link>
          <Button type="submit" disabled={isSaving}>{isSaving ? "Publicando..." : "Publicar Procura - Grátis"}</Button>
        </div>
      </form>
    </section>
  );
}
