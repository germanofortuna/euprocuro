import legalContent from "@/content/legal-pages.json";
import type { PublicContentEntry } from "@/shared/api/types";

export const LEGAL_SLUGS = legalContent.slugs;
export const TERMS_VERSION = legalContent.termsVersion;

export type LegalPage = {
  title: string;
  label: string;
  updatedAt?: string;
  effectiveAt?: string;
  summary?: string;
  sections?: Array<{
    title: string;
    paragraphs?: string[];
    items?: string[];
  }>;
};

const LEGAL_ENTRY_KEYS = {
  [LEGAL_SLUGS.terms]: "legal.page.termos-de-uso",
  [LEGAL_SLUGS.privacy]: "legal.page.politica-de-privacidade",
  [LEGAL_SLUGS.prohibitedContent]: "legal.page.politica-de-conteudo-proibido",
  [LEGAL_SLUGS.reports]: "legal.page.politica-de-denuncia-e-remocao"
};

function parseLegalEntry(entry: PublicContentEntry | null | undefined, fallback: LegalPage): LegalPage {
  if (!entry?.value) {
    return fallback;
  }
  try {
    return { ...fallback, ...JSON.parse(entry.value) };
  } catch {
    return fallback;
  }
}

export function buildLegalPages(entries: Record<string, PublicContentEntry> = {}) {
  return Object.entries(legalContent.pages).reduce<Record<string, LegalPage>>((pages, [slug, fallbackPage]) => {
    pages[slug] = parseLegalEntry(entries[LEGAL_ENTRY_KEYS[slug] as string], fallbackPage as LegalPage);
    return pages;
  }, {});
}

export const fallbackLegalPages = legalContent.pages as Record<string, LegalPage>;

export function legalNavigation(pages: Record<string, LegalPage>) {
  return Object.entries(pages).map(([slug, page]) => ({
    slug,
    href: `/legal/${slug}`,
    label: page.label || page.title
  }));
}
