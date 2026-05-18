"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { fetchPublicContent } from "@/shared/api/client";
import defaultContent from "@/content/default-content.json";
import type { PublicContentCatalog, PublicContentEntry } from "@/shared/api/types";

const PUBLIC_CONTENT_CACHE_KEY = "eu-procuro-public-content";

type ContentContextValue = {
  contentVersion: string;
  entries: Record<string, PublicContentEntry>;
  getEntry: (key: string) => PublicContentEntry | null;
  t: (key: string, variables?: Record<string, string | number | null | undefined>) => string;
};

const ContentContext = createContext<ContentContextValue | null>(null);

function interpolate(value: string, variables: Record<string, string | number | null | undefined> = {}) {
  return Object.entries(variables).reduce(
    (text, [key, replacement]) => text.replaceAll(`{{${key}}}`, String(replacement ?? "")),
    value
  );
}

function normalizeEntries(payloadEntries: PublicContentCatalog["entries"]): Record<string, PublicContentEntry> {
  if (!payloadEntries || typeof payloadEntries !== "object") {
    return {};
  }

  return Object.entries(payloadEntries).reduce<Record<string, PublicContentEntry>>((entries, [key, entry]) => {
    if (entry && typeof entry === "object" && "value" in entry) {
      entries[key] = entry as PublicContentEntry;
      return entries;
    }
    entries[key] = {
      key,
      type: "TEXT",
      version: 0,
      value: String(entry ?? "")
    };
    return entries;
  }, {});
}

function readCachedPublicContent() {
  if (typeof window === "undefined") {
    return null;
  }
  try {
    const cached = window.localStorage.getItem(PUBLIC_CONTENT_CACHE_KEY);
    return cached ? (JSON.parse(cached) as PublicContentCatalog) : null;
  } catch {
    return null;
  }
}

function writeCachedPublicContent(payload: PublicContentCatalog) {
  try {
    window.localStorage.setItem(PUBLIC_CONTENT_CACHE_KEY, JSON.stringify(payload));
  } catch {
    // Public content cache is a progressive enhancement.
  }
}

const fallbackEntries = normalizeEntries(defaultContent.entries);

export function ContentProvider({ children }: { children: React.ReactNode }) {
  const cachedContent = typeof window !== "undefined" ? readCachedPublicContent() : null;
  const [remoteEntries, setRemoteEntries] = useState(() => normalizeEntries(cachedContent?.entries));
  const [contentVersion, setContentVersion] = useState(cachedContent?.version ?? defaultContent.version);

  useEffect(() => {
    let isCancelled = false;
    fetchPublicContent()
      .then((payload) => {
        if (isCancelled) {
          return;
        }
        const normalized = normalizeEntries(payload?.entries);
        setRemoteEntries(normalized);
        setContentVersion(payload?.version ?? defaultContent.version);
        writeCachedPublicContent(payload);
      })
      .catch(() => {
        // Runtime copy must never block the product.
      });

    return () => {
      isCancelled = true;
    };
  }, []);

  const entries = useMemo(() => ({ ...fallbackEntries, ...remoteEntries }), [remoteEntries]);
  const getEntry = useCallback((key: string) => entries[key] ?? null, [entries]);
  const t = useCallback(
    (key: string, variables?: Record<string, string | number | null | undefined>) =>
      interpolate(getEntry(key)?.value ?? key, variables),
    [getEntry]
  );

  const value = useMemo(
    () => ({
      contentVersion,
      entries,
      getEntry,
      t
    }),
    [contentVersion, entries, getEntry, t]
  );

  return <ContentContext.Provider value={value}>{children}</ContentContext.Provider>;
}

export function useContentText() {
  const context = useContext(ContentContext);
  if (!context) {
    throw new Error("useContentText must be used within ContentProvider.");
  }
  return context;
}
