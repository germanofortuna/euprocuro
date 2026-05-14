export const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://euprocuro.com";

export function canonical(path = "/") {
  return new URL(path, SITE_URL).toString();
}

export function truncateDescription(value?: string | null, max = 155) {
  const normalized = String(value ?? "").replace(/\s+/g, " ").trim();
  if (!normalized) {
    return "Eu Procuro é um marketplace reverso para publicar o que você procura e receber propostas de quem pode atender.";
  }
  return normalized.length > max ? `${normalized.slice(0, max - 1)}…` : normalized;
}
