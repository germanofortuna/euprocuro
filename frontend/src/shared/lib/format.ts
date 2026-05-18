import type { Category, Interest, User } from "@/shared/api/types";

export const FALLBACK_CATEGORIES: Category[] = [
  { value: "AUTOMOVEIS", label: "Automóveis", active: true, sortOrder: 10 },
  { value: "IMOVEIS", label: "Imóveis", active: true, sortOrder: 20 },
  { value: "SERVICOS", label: "Serviços", active: true, sortOrder: 30 },
  { value: "ELETRONICOS", label: "Eletrônicos", active: true, sortOrder: 40 },
  { value: "INSTRUMENTOS", label: "Instrumentos", active: true, sortOrder: 50 },
  { value: "OUTROS", label: "Outros", active: true, sortOrder: 60 }
];

export const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL"
});

export const dateFormatter = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short"
});

export function currency(value?: number | string | null) {
  if (value === null || value === undefined || value === "") {
    return "A combinar";
  }
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? currencyFormatter.format(numberValue) : "A combinar";
}

export function budgetLabel(interest: Pick<Interest, "budgetMin" | "budgetMax">) {
  if (interest.budgetMin && interest.budgetMax) {
    return `${currency(interest.budgetMin)} até ${currency(interest.budgetMax)}`;
  }
  if (interest.budgetMax) {
    return `Até ${currency(interest.budgetMax)}`;
  }
  if (interest.budgetMin) {
    return `A partir de ${currency(interest.budgetMin)}`;
  }
  return "A combinar";
}

export function formatDateTime(value?: string | null) {
  if (!value) {
    return "Agora";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Agora" : dateFormatter.format(date);
}

export function categoryLabel(categories: Category[], value?: string | null) {
  if (!value) {
    return "Categoria";
  }
  return categories.find((category) => category.value === value)?.label ?? value;
}

export function locationLabel(interest: Pick<Interest, "location">) {
  if (interest.location?.remote) {
    return "Online";
  }
  const city = interest.location?.city;
  const state = interest.location?.state;
  if (city && state) {
    return `${city}, ${state}`;
  }
  return city ?? state ?? "Brasil";
}

export function firstName(value?: string | null) {
  return value?.trim().split(/\s+/)[0] || "usuário";
}

export function slugifyCategory(value: string) {
  return value
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/_/g, "-")
    .replace(/[^a-z0-9-]/g, "");
}

export function categoryFromSlug(categories: Category[], slug?: string) {
  if (!slug) {
    return null;
  }
  return categories.find((category) => slugifyCategory(category.value) === slug || slugifyCategory(category.label) === slug) ?? null;
}

export function limitText(value: string, maxLength: number) {
  return value.length > maxLength ? value.slice(0, maxLength) : value;
}

export function hasLink(value: string) {
  return /(https?:\/\/|www\.|[a-z0-9-]+\.[a-z]{2,})/i.test(value);
}

export function formatCep(value: string) {
  const digits = value.replace(/\D/g, "").slice(0, 8);
  if (digits.length <= 5) {
    return digits;
  }
  return `${digits.slice(0, 5)}-${digits.slice(5)}`;
}

export function formatCpfCnpj(value: string) {
  const digits = value.replace(/\D/g, "").slice(0, 14);
  if (digits.length <= 11) {
    return digits
      .replace(/^(\d{3})(\d)/, "$1.$2")
      .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
      .replace(/\.(\d{3})(\d)/, ".$1-$2");
  }

  return digits
    .replace(/^(\d{2})(\d)/, "$1.$2")
    .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1/$2")
    .replace(/(\d{4})(\d)/, "$1-$2");
}

export function isBoostActive(interest: Pick<Interest, "boostedUntil">) {
  return Boolean(interest.boostedUntil && new Date(interest.boostedUntil).getTime() > Date.now());
}

export function activeCategories(categories: Category[]) {
  return [...(categories.length ? categories : FALLBACK_CATEGORIES)]
    .filter((category) => category.active !== false)
    .sort((left, right) => (left.sortOrder ?? 0) - (right.sortOrder ?? 0));
}

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Pendente",
  OPEN: "Aberta",
  APPROVED: "Aprovada",
  REVIEW_REQUIRED: "Revisao necessaria",
  REJECTED: "Rejeitada",
  REPORTED: "Denunciada",
  HIDDEN: "Oculta",
  CLOSED: "Desativada",
  SENT: "Enviada",
  ACCEPTED: "Aceita",
  DECLINED: "Recusada",
  CANCELLED: "Cancelada",
  CANCELED: "Cancelada",
  FAILED: "Falhou",
  ERROR: "Erro",
  RESOLVED: "Resolvida",
  DISMISSED: "Dispensada",
  ANSWERED: "Respondida",
  IN_REVIEW: "Em analise"
};

export function statusLabel(status?: string | null) {
  const normalized = String(status ?? "PENDING").toUpperCase();
  return STATUS_LABELS[normalized] ?? normalized.replace(/_/g, " ").toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase());
}

export function statusTone(status?: string | null) {
  const normalized = String(status ?? "").toUpperCase();
  if (["REJECTED", "HIDDEN", "DECLINED", "CANCELLED", "CANCELED", "FAILED", "ERROR"].includes(normalized)) {
    return "danger";
  }
  if (["PENDING", "REVIEW_REQUIRED", "REPORTED", "IN_REVIEW"].includes(normalized)) {
    return "warning";
  }
  if (["APPROVED", "OPEN", "ACCEPTED", "SENT", "RESOLVED", "ANSWERED"].includes(normalized)) {
    return "success";
  }
  return "neutral";
}

export function listingExpirationLabel(interest: Pick<Interest, "createdAt" | "expiresAt">) {
  const expiresAt = listingExpiresAt(interest);

  if (!expiresAt) {
    return "Prazo de expiracao indisponivel";
  }

  const diffDays = Math.ceil((expiresAt.getTime() - Date.now()) / (24 * 60 * 60 * 1000));
  if (diffDays < 0) {
    return `Expirou em ${formatDateTime(expiresAt.toISOString())}`;
  }
  if (diffDays === 0) {
    return "Expira hoje";
  }
  return `Expira em ${diffDays} ${diffDays === 1 ? "dia" : "dias"}`;
}

export function listingExpiresAt(interest: Pick<Interest, "createdAt" | "expiresAt">) {
  const explicitDate = interest.expiresAt ? new Date(interest.expiresAt) : null;
  const baseDate = explicitDate && !Number.isNaN(explicitDate.getTime()) ? explicitDate : null;
  const createdDate = interest.createdAt ? new Date(interest.createdAt) : null;
  const days = Number(process.env.NEXT_PUBLIC_LISTING_EXPIRATION_DAYS ?? process.env.VITE_LISTING_EXPIRATION_DAYS ?? 30);
  return baseDate ?? (createdDate && !Number.isNaN(createdDate.getTime()) ? new Date(createdDate.getTime() + days * 24 * 60 * 60 * 1000) : null);
}

export function isListingExpired(interest: Pick<Interest, "createdAt" | "expiresAt">) {
  const expiresAt = listingExpiresAt(interest);
  return Boolean(expiresAt && expiresAt.getTime() <= Date.now());
}

export function isAdminUser(user?: User | null, hasAdminAccess = false) {
  const markers = [
    user?.role,
    ...(user?.roles ?? []),
    ...(user?.authorities ?? [])
  ].map((item) => String(item ?? "").toUpperCase());

  return Boolean(user?.admin || user?.isAdmin || hasAdminAccess || markers.includes("ADMIN") || markers.includes("ROLE_ADMIN"));
}

export function categorySearchPlaceholder(category?: Category | null) {
  const normalized = String(category?.value ?? category?.label ?? "").toUpperCase();
  if (normalized.includes("INSTRUMENT")) {
    return "Ex: guitarra stratocaster";
  }
  if (normalized.includes("AUTOM")) {
    return "Ex: Corsa Wind";
  }
  if (normalized.includes("IMOV")) {
    return "Ex: apartamento 2 quartos";
  }
  if (normalized.includes("SERV")) {
    return "Ex: eletricista residencial";
  }
  if (normalized.includes("ELETR")) {
    return "Ex: notebook gamer";
  }
  return "Ex: bicicleta aro 29";
}
