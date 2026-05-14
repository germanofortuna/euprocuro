"use client";

import { useMemo } from "react";
import { buildLegalPages, legalNavigation, TERMS_VERSION } from "./legal-content";
import { useContentText } from "@/features/content/content-context";

export function useLegalContent() {
  const { entries } = useContentText();
  const pages = useMemo(() => buildLegalPages(entries), [entries]);
  const termsEntry = entries["legal.page.termos-de-uso"];
  const termsVersion = termsEntry?.version ? String(termsEntry.version) : TERMS_VERSION;

  return {
    pages,
    navigation: legalNavigation(pages),
    termsVersion
  };
}
