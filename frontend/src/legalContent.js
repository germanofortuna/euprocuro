import legalContent from "./content/legal-pages.json";

export const TERMS_VERSION = legalContent.termsVersion;
export const LEGAL_SLUGS = legalContent.slugs;
export const legalPages = legalContent.pages;

export const legalNavigation = Object.entries(legalPages).map(([slug, page]) => ({
  slug,
  label: page.label,
  title: page.title
}));
