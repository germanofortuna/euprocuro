import { useMemo } from "react";

import { LEGAL_SLUGS, TERMS_VERSION, legalPages as fallbackLegalPages } from "../legalContent";
import { useContentText } from "./ContentContext";

const LEGAL_ENTRY_KEYS = {
  [LEGAL_SLUGS.terms]: "legal.page.termos-de-uso",
  [LEGAL_SLUGS.privacy]: "legal.page.politica-de-privacidade",
  [LEGAL_SLUGS.prohibitedContent]: "legal.page.politica-de-conteudo-proibido",
  [LEGAL_SLUGS.reports]: "legal.page.politica-de-denuncia-e-remocao"
};

function parseLegalPage(entry, fallbackPage) {
  if (!entry?.value) {
    return fallbackPage;
  }

  try {
    const parsed = typeof entry.value === "string" ? JSON.parse(entry.value) : entry.value;
    if (!parsed?.title || !Array.isArray(parsed.sections)) {
      return fallbackPage;
    }
    return parsed;
  } catch {
    return fallbackPage;
  }
}

export function useLegalContent() {
  const { getEntry, contentVersion } = useContentText();

  return useMemo(() => {
    const pages = Object.entries(fallbackLegalPages).reduce((accumulator, [slug, fallbackPage]) => {
      accumulator[slug] = parseLegalPage(getEntry(LEGAL_ENTRY_KEYS[slug]), fallbackPage);
      return accumulator;
    }, {});
    const termsEntry = getEntry(LEGAL_ENTRY_KEYS[LEGAL_SLUGS.terms]);

    return {
      pages,
      termsVersion: termsEntry?.version ? String(termsEntry.version) : TERMS_VERSION
    };
  }, [contentVersion, getEntry]);
}
