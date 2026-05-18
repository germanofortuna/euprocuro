"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowRight, ChevronDown, Filter, Plus, Search, Sparkles, Trash2, Trophy, Users, Zap } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import type { ComponentProps } from "react";
import { trackEvent } from "@/features/analytics/analytics";
import { usePlatform } from "@/features/platform/platform-context";
import { fetchInterest, fetchInterests, lookupAddressByPostalCode } from "@/shared/api/client";
import type { Interest } from "@/shared/api/types";
import { formatCep, limitText } from "@/shared/lib/format";
import { AuthIntentLink } from "@/shared/ui/auth-intent-link";
import { BackButton } from "@/shared/ui/back-button";
import { Button } from "@/shared/ui/button";
import { EmptyState } from "@/shared/ui/empty-state";
import { SPECIAL_STICKER_SELECTIONS, STICKERS_CATEGORY, stickerFlagImageForOption, stickerFlagImageForSelection, stickerGroupForSelection, stickerGroups, stickerSelectionLabel, normalizeStickerNumbers, normalizeStickerPlayers } from "./stickers-data";

type FormSubmitHandler = NonNullable<ComponentProps<"form">["onSubmit"]>;
type StickerPublishEntry = { id: string; selection: string; numbers: string; players: string };

const STICKERS_PAGE_SIZE = 24;
const emptyStickerFilters = { stickerType: "", stickerGroup: "", stickerSelection: "", stickerNumber: "", stickerPlayer: "", state: "", city: "", neighborhood: "" };

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

function StickerSelectionDisplay({ value, fallback = "Copa 2026" }: { value?: string | null; fallback?: string }) {
  const label = stickerSelectionLabel(value) || fallback;
  const flagSrc = stickerFlagImageForSelection(value);
  return (
    <span className="sticker-selection-display">
      {flagSrc ? <img src={flagSrc} alt="" loading="lazy" /> : null}
      <span>{label}</span>
    </span>
  );
}

function StickerSelectionSelect({
  value,
  onChange,
  groups,
  placeholder,
  includeSpecial = false,
  specialOptions = []
}: {
  value: string;
  onChange: (value: string) => void;
  groups: ReturnType<typeof selectionOptions>;
  placeholder: string;
  includeSpecial?: boolean;
  specialOptions?: string[];
}) {
  const [isOpen, setIsOpen] = useState(false);
  const selectedLabel = value ? stickerSelectionLabel(value) : "";
  const selectedFlagSrc = stickerFlagImageForSelection(value);

  function choose(nextValue: string) {
    onChange(nextValue);
    setIsOpen(false);
  }

  return (
    <div className="sticker-selection-select" onBlur={(event) => {
      if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
        setIsOpen(false);
      }
    }}>
      <button type="button" className="sticker-selection-select__button" onClick={() => setIsOpen((current) => !current)} aria-expanded={isOpen}>
        <span className={selectedLabel ? "sticker-selection-select__value" : "sticker-selection-select__placeholder"}>
          {selectedFlagSrc ? <img src={selectedFlagSrc} alt="" loading="lazy" /> : null}
          <span>{selectedLabel || placeholder}</span>
        </span>
        <ChevronDown size={18} aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className="sticker-selection-select__menu" role="listbox">
          <button type="button" className="sticker-selection-select__option" onClick={() => choose("")}>
            {placeholder}
          </button>
          {groups.map(([group, selections]) => (
            <div className="sticker-selection-select__group" key={group}>
              <strong>Grupo {group}</strong>
              {selections.map((selection) => {
                const flagSrc = stickerFlagImageForOption(selection);
                return (
                  <button type="button" className="sticker-selection-select__option" key={selection.name} onClick={() => choose(selection.name)} role="option" aria-selected={value === selection.name}>
                    {flagSrc ? <img src={flagSrc} alt="" loading="lazy" /> : null}
                    <span>{selection.name}</span>
                  </button>
                );
              })}
            </div>
          ))}
          {includeSpecial ? (
            <div className="sticker-selection-select__group">
              <strong>Especiais</strong>
              {specialOptions.map((item) => (
                <button type="button" className="sticker-selection-select__option" key={item} onClick={() => choose(item)} role="option" aria-selected={value === item}>
                  <span>{item}</span>
                </button>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

function createStickerPublishEntry(): StickerPublishEntry {
  return { id: `stickers-${Date.now()}-${Math.random().toString(36).slice(2)}`, selection: "", numbers: "", players: "" };
}

function stickerIdentifiers(numbers: string[] = [], players: string[] = [], limit = 8) {
  return [...numbers, ...players].slice(0, limit).join(", ");
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
  const identifiers = stickerIdentifiers(item.stickerDetails?.numbers ?? [], item.stickerDetails?.players ?? []);
  return { action, separator, selection, identifiers };
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
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [nextOffset, setNextOffset] = useState(0);
  const [hasMoreStickers, setHasMoreStickers] = useState(false);
  const searchSequenceRef = useRef(0);
  const groups = useMemo(selectionOptions, []);
  const visibleSelectionGroups = useMemo(() => selectionsForGroup(filters.stickerGroup, groups), [filters.stickerGroup, groups]);
  const myStickerIds = useMemo(
    () => new Set((dashboard?.myInterests ?? []).filter((item) => item.category === STICKERS_CATEGORY).map((item) => item.id)),
    [dashboard?.myInterests]
  );
  const stickersEnabled = operationalSettings.featureFlags?.stickersPageEnabled !== false;

  async function loadStickers(nextFilters = filters, offset = 0, append = false) {
    const searchSequence = searchSequenceRef.current + 1;
    searchSequenceRef.current = searchSequence;
    if (!stickersEnabled) {
      setStickers([]);
      setNextOffset(0);
      setHasMoreStickers(false);
      return;
    }
    if (append) {
      setIsLoadingMore(true);
    } else {
      setIsLoading(true);
    }
    try {
      const payload = await fetchInterests({
        category: STICKERS_CATEGORY,
        limit: STICKERS_PAGE_SIZE,
        offset,
        includeOwn: "true",
        ...nextFilters
      });
      if (searchSequenceRef.current === searchSequence) {
        const filteredPayload = payload.filter((item) => matchesLocationFilters(item, nextFilters));
        setStickers((current) => {
          const merged = append ? [...current, ...filteredPayload] : filteredPayload;
          const unique = new Map(merged.map((item) => [item.id, item]));
          return Array.from(unique.values());
        });
        setNextOffset(offset + payload.length);
        setHasMoreStickers(payload.length === STICKERS_PAGE_SIZE);
      }
    } finally {
      if (searchSequenceRef.current === searchSequence) {
        setIsLoading(false);
        setIsLoadingMore(false);
      }
    }
  }

  useEffect(() => {
    if (!stickersEnabled) {
      setStickers([]);
      setNextOffset(0);
      setHasMoreStickers(false);
      return;
    }
    const timer = window.setTimeout(() => {
      loadStickers(filters, 0, false).catch(() => {
        setStickers([]);
        setNextOffset(0);
        setHasMoreStickers(false);
      });
    }, 380);
    return () => window.clearTimeout(timer);
  }, [filters, stickersEnabled]);

  const applyFilters: FormSubmitHandler = (event) => {
    event.preventDefault();
  };

  function updateFilters(nextFilters: typeof filters) {
    setStickers([]);
    setNextOffset(0);
    setHasMoreStickers(false);
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

  function loadMoreStickers() {
    if (isLoading || isLoadingMore || !hasMoreStickers) {
      return;
    }
    trackEvent("stickers_load_more_clicked", { offset: nextOffset });
    loadStickers(filters, nextOffset, true).catch(() => undefined);
  }

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
          <h2><Search size={24} /> Figurinhas</h2>
          <AuthIntentLink className="button button--primary button--sm stickers-section-publish" href="/figurinhas/publicar">Publicar</AuthIntentLink>
        </div>
        <div className="stickers-grid-layout">
          <aside className="filter-panel stickers-filter-panel">
            <h2><Filter size={19} /> Filtros</h2>
            <form className="stack-form" onSubmit={applyFilters}>
              <label>Tipo<select value={filters.stickerType} onChange={(event) => updateFilters({ ...filters, stickerType: event.target.value })}><option value="">Todos</option><option value="MISSING">Faltantes</option><option value="AVAILABLE">Repetidas</option></select></label>
              <label>Grupo<select value={filters.stickerGroup} onChange={(event) => updateFilters({ ...filters, stickerGroup: event.target.value, stickerSelection: "" })}><option value="">Todos os grupos</option>{groups.map(([group]) => <option key={group} value={group}>Grupo {group}</option>)}<option value="SPECIAL">Especiais</option></select></label>
              <label>Seleção<StickerSelectionSelect value={filters.stickerSelection} onChange={(value) => updateFilters({ ...filters, stickerSelection: value })} groups={visibleSelectionGroups} placeholder="Todas" includeSpecial={filters.stickerGroup === "" || filters.stickerGroup === "SPECIAL"} specialOptions={SPECIAL_STICKER_SELECTIONS} /></label>
              <label>Número<input value={filters.stickerNumber} onChange={(event) => updateFilters({ ...filters, stickerNumber: event.target.value })} placeholder="Ex: 12 ou FW26" /></label>
              <label>Jogador<input value={filters.stickerPlayer} onChange={(event) => updateFilters({ ...filters, stickerPlayer: event.target.value })} placeholder="Ex: Lionel Messi" /></label>
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
                    <span className="sticker-card__selection"><StickerSelectionDisplay value={titleParts.selection} /></span>
                    {titleParts.identifiers ? <span>: {titleParts.identifiers}</span> : null}
                  </h3>
                  <p>{item.description}</p>
                  <div className="sticker-card__meta-grid">
                    <span><small>Seleção</small><StickerSelectionDisplay value={item.stickerDetails?.selection} /></span>
                    <span><small>Grupo</small>{item.stickerDetails?.group === "SPECIAL" ? "Especiais" : `Grupo ${item.stickerDetails?.group ?? "-"}`}</span>
                    {stickerLocationParts(item).map((part) => <span key={part}><small>{part.split(":")[0]}</small>{part.split(":").slice(1).join(":").trim()}</span>)}
                  </div>
                  <div className="sticker-number-list">
                    {(item.stickerDetails?.numbers ?? []).slice(0, 10).map((number) => <span key={number}>{number}</span>)}
                    {(item.stickerDetails?.players ?? []).slice(0, 6).map((player) => <span key={player}>{player}</span>)}
                  </div>
                  <strong>Ver detalhes e fazer proposta <ArrowRight size={16} /></strong>
                </Link>
              );
            }) : null}
            {!isLoading && !stickers.length ? <EmptyState title="Nenhuma figurinha encontrada" description="Ajuste os filtros ou publique a primeira procura de figurinhas." /> : null}
            {!isLoading && stickers.length && hasMoreStickers ? (
              <div className="stickers-load-more">
                <Button type="button" variant="outline" onClick={loadMoreStickers} disabled={isLoadingMore}>
                  {isLoadingMore ? "Carregando..." : "Ver mais figurinhas"}
                </Button>
              </div>
            ) : null}
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

function buildTitle(type: "MISSING" | "AVAILABLE", selection: string, numbers: string[], players: string[] = []) {
  const verb = type === "MISSING" ? "Procuro figurinhas" : "Tenho repetidas";
  const target = selection ? `${selection}` : "Copa 2026";
  const identifiers = stickerIdentifiers(numbers, players);
  return limitText(`${verb} - ${target}: ${identifiers}`, 80);
}

function buildDescription(type: "MISSING" | "AVAILABLE", selection: string, numbers: string[], players: string[], extra: string) {
  const action = type === "MISSING" ? "Figurinhas faltantes" : "Figurinhas disponíveis para troca ou venda";
  const lines = [
    `${action}:`,
    numbers.length ? `${numbers.join(", ")}${selection ? ` - ${selection}` : ""}.` : "",
    players.length ? `Jogadores: ${players.join(", ")}${selection ? ` - ${selection}` : ""}.` : "",
    extra
  ].filter(Boolean);
  const base = lines.join("\n").trim();
  return limitText(base, 120);
}

function extractStickerExtraDescription(interest: Interest) {
  const selection = interest.stickerDetails?.selection ?? "";
  const numbers = interest.stickerDetails?.numbers ?? [];
  const players = interest.stickerDetails?.players ?? [];
  const generatedLines = new Set([
    "figurinhas faltantes:",
    "figurinhas disponiveis para troca ou venda:",
    "figurinhas disponíveis para troca ou venda:",
    numbers.length ? `${numbers.join(", ")}${selection ? ` - ${selection}` : ""}.`.toLowerCase() : "",
    players.length ? `jogadores: ${players.join(", ")}${selection ? ` - ${selection}` : ""}.`.toLowerCase() : ""
  ].filter(Boolean));

  return String(interest.description ?? "")
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line && !generatedLines.has(line.toLowerCase()))
    .join("\n");
}

export function PublishStickersPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const editingInterestId = searchParams.get("editar") ?? searchParams.get("edit");
  const isEditing = Boolean(editingInterestId);
  const { currentUser, dashboard, openAuthModal, saveInterest, setFeedback, operationalSettings } = usePlatform();
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
  const [entries, setEntries] = useState<StickerPublishEntry[]>([{ id: "stickers-1", selection: "", numbers: "", players: "" }]);
  const [isSaving, setIsSaving] = useState(false);
  const [isLoadingInterest, setIsLoadingInterest] = useState(false);
  const loadedEditingInterestRef = useRef<string | null>(null);
  const [publishLookupState, setPublishLookupState] = useState<{ loading: boolean; message: string; tone: "muted" | "success" | "error" }>({ loading: false, message: "", tone: "muted" });
  const groups = useMemo(selectionOptions, []);
  const previewEntries = entries
    .map((entry) => ({ ...entry, numbers: normalizeStickerNumbers(entry.numbers), players: normalizeStickerPlayers(entry.players), group: stickerGroupForSelection(entry.selection) }))
    .filter((entry) => entry.selection || entry.numbers.length || entry.players.length);
  const stickersEnabled = operationalSettings.featureFlags?.stickersPageEnabled !== false;

  function applyEditingInterest(interest: Interest) {
    if (interest.category !== STICKERS_CATEGORY) {
      setFeedback({ type: "error", title: "Procura incompatível", message: "Esta edição deve ser feita pelo formulário comum." });
      router.replace(`/cadastrar-interesse?editar=${interest.id}`);
      return;
    }
    setForm((current) => ({
      ...current,
      type: interest.stickerDetails?.type === "AVAILABLE" ? "AVAILABLE" : "MISSING",
      postalCode: formatCep(String(interest.location?.postalCode ?? "")),
      city: String(interest.location?.city ?? ""),
      state: String(interest.location?.state ?? "").toUpperCase().slice(0, 2),
      neighborhood: String(interest.location?.neighborhood ?? ""),
      country: String(interest.location?.country ?? "Brasil"),
      description: extractStickerExtraDescription(interest),
      tags: (interest.tags ?? [])
        .filter((tag) => !["copa-2026", "figurinhas", "faltantes", "repetidas", interest.stickerDetails?.selection].includes(tag))
        .join(", ")
    }));
    setEntries([{
      id: `stickers-edit-${interest.id}`,
      selection: String(interest.stickerDetails?.selection ?? ""),
      numbers: (interest.stickerDetails?.numbers ?? []).join(", "),
      players: (interest.stickerDetails?.players ?? []).join(", ")
    }]);
    loadedEditingInterestRef.current = interest.id;
  }

  useEffect(() => {
    if (!editingInterestId) {
      loadedEditingInterestRef.current = null;
      return;
    }
    if (loadedEditingInterestRef.current === editingInterestId) {
      return;
    }
    const cachedInterest = dashboard?.myInterests?.find((interest) => interest.id === editingInterestId);
    if (cachedInterest) {
      applyEditingInterest(cachedInterest);
      return;
    }
    setIsLoadingInterest(true);
    fetchInterest(editingInterestId)
      .then(applyEditingInterest)
      .catch((error) => setFeedback({ type: "error", title: "Procura indisponível", message: error instanceof Error ? error.message : "Não foi possível carregar esta procura para edição." }))
      .finally(() => setIsLoadingInterest(false));
  }, [dashboard?.myInterests, editingInterestId]);

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
      openAuthModal("login", editingInterestId ? `/figurinhas/publicar?editar=${editingInterestId}` : "/figurinhas/publicar");
      return;
    }
    const publishEntries = entries
      .map((entry) => ({
        selection: entry.selection,
        numbers: normalizeStickerNumbers(entry.numbers),
        players: normalizeStickerPlayers(entry.players),
        group: stickerGroupForSelection(entry.selection)
      }))
      .filter((entry) => entry.selection || entry.numbers.length || entry.players.length);
    if (!publishEntries.length || publishEntries.some((entry) => !entry.selection || (!entry.numbers.length && !entry.players.length))) {
      setFeedback({ type: "error", title: "Informe as figurinhas", message: "Digite pelo menos um número, código de figurinha ou nome de jogador." });
      return;
    }
    setIsSaving(true);
    try {
      trackEvent("stickers_publish_started", { type: form.type, totalSelections: publishEntries.length });
      const savedItems = [];
      for (const entry of publishEntries) {
        const saved = await saveInterest({
          title: buildTitle(form.type, entry.selection, entry.numbers, entry.players),
          description: buildDescription(form.type, entry.selection, entry.numbers, entry.players, form.description),
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
          tags: ["copa-2026", "figurinhas", form.type === "MISSING" ? "faltantes" : "repetidas", entry.selection, ...entry.players, ...form.tags.split(",")].map((tag) => tag.trim()).filter(Boolean),
          stickerDetails: {
            type: form.type,
            group: entry.group || "SPECIAL",
            selection: entry.selection || null,
            numbers: entry.numbers,
            players: entry.players
          }
        }, editingInterestId);
        savedItems.push(saved);
        if (editingInterestId) {
          break;
        }
      }
      trackEvent("stickers_publish_completed", { type: form.type, totalSelections: savedItems.length, interestId: savedItems[0]?.id });
      if (editingInterestId && savedItems[0]?.id) {
        router.push(`/interesses/${savedItems[0].id}`);
      } else if (savedItems.length > 1) {
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
      <BackButton />
      <div className="form-heading">
        <span className="pill">Copa 2026</span>
        <h1>{isEditing ? "Editar Figurinhas" : "Publicar Figurinhas"}</h1>
        <p>{isEditing ? "Ajuste seleção, números ou jogadores e envie para nova validação." : "Informe suas faltantes ou repetidas para receber propostas de outros colecionadores."}</p>
      </div>
      {isLoadingInterest ? <div className="section-loading" role="status">Carregando procura para edição...</div> : null}
      <form className="feature-form stickers-form" onSubmit={submit}>
        <section className="form-section">
          <h2>Informações das figurinhas</h2>
          <div className="segmented-control">
            <button type="button" className={form.type === "MISSING" ? "is-active" : ""} onClick={() => setForm((current) => ({ ...current, type: "MISSING" }))}>Procuro faltantes</button>
            <button type="button" className={form.type === "AVAILABLE" ? "is-active" : ""} onClick={() => setForm((current) => ({ ...current, type: "AVAILABLE" }))}>Tenho repetidas</button>
          </div>
          <div className="sticker-entry-list">
            {entries.map((entry, index) => (
              <div className={isEditing ? "sticker-entry-row sticker-entry-row--editing" : "sticker-entry-row"} key={entry.id}>
                <span className="sticker-entry-index">Seleção {index + 1}</span>
                <label>Seleção ou especial<StickerSelectionSelect value={entry.selection} onChange={(value) => updateEntry(entry.id, { selection: value })} groups={groups} placeholder="Escolha a seleção/especiais" includeSpecial specialOptions={SPECIAL_STICKER_SELECTIONS} /></label>
                <label>Números ou códigos<input value={entry.numbers} onChange={(event) => updateEntry(entry.id, { numbers: event.target.value })} placeholder="Ex: 12, 45, 78, FW26" /></label>
                <label>Jogadores<input value={entry.players} onChange={(event) => updateEntry(entry.id, { players: event.target.value })} placeholder="Ex: Lionel Messi, Neymar, Cristiano Ronaldo" /></label>
                {!isEditing ? <Button type="button" variant="outline" className="sticker-entry-remove" onClick={() => removeEntry(entry.id)} disabled={entries.length === 1} title={entries.length === 1 ? "Mantenha pelo menos uma seleção" : "Remover seleção"} aria-label={entries.length === 1 ? "Mantenha pelo menos uma seleção" : "Remover seleção"}><Trash2 size={17} /></Button> : null}
              </div>
            ))}
          </div>
          {!isEditing ? <div className="sticker-entry-add-row">
            <Button type="button" variant="outline" className="sticker-entry-add" onClick={addEntry}><Plus size={16} /> Adicionar outra seleção</Button>
            <small>A cada seleção adicionada, outro card de interesse será criado.</small>
          </div> : null}
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
                  <p>{buildTitle(form.type, entry.selection, entry.numbers, entry.players)}</p>
                  <div className="sticker-number-list">
                    {entry.numbers.slice(0, 12).map((number) => <span key={number}>{number}</span>)}
                    {entry.players.slice(0, 8).map((player) => <span key={player}>{player}</span>)}
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </section>
        <div className="form-actions">
          <Link className="button button--outline" href={editingInterestId ? `/interesses/${editingInterestId}` : "/figurinhas"}>Cancelar</Link>
          <Button type="submit" disabled={isSaving || isLoadingInterest}>{isSaving ? (isEditing ? "Salvando..." : "Publicando...") : (isEditing ? "Salvar Alterações" : "Publicar Procura - Grátis")}</Button>
        </div>
      </form>
    </section>
  );
}
