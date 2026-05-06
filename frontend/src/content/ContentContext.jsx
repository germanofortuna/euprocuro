import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";

import { fetchPublicContent } from "../api";
import defaultContent from "./default-content.json";

const PUBLIC_CONTENT_CACHE_KEY = "eu-procuro-public-content";
const ContentContext = createContext(null);

function interpolate(value, variables = {}) {
  if (typeof value !== "string") {
    return value ?? "";
  }

  return Object.entries(variables).reduce(
    (text, [key, replacement]) => text.replaceAll(`{{${key}}}`, replacement ?? ""),
    value
  );
}

function normalizeEntries(payloadEntries) {
  if (!payloadEntries || typeof payloadEntries !== "object") {
    return {};
  }

  return Object.entries(payloadEntries).reduce((entries, [key, entry]) => {
    if (entry && typeof entry === "object" && "value" in entry) {
      entries[key] = entry;
      return entries;
    }

    entries[key] = {
      key,
      type: "TEXT",
      version: 0,
      value: entry
    };
    return entries;
  }, {});
}

function readCachedPublicContent() {
  try {
    const cached = window.localStorage.getItem(PUBLIC_CONTENT_CACHE_KEY);
    return cached ? JSON.parse(cached) : null;
  } catch {
    return null;
  }
}

function writeCachedPublicContent(payload) {
  try {
    window.localStorage.setItem(PUBLIC_CONTENT_CACHE_KEY, JSON.stringify(payload));
  } catch {
    // Public copy cache is a progressive enhancement.
  }
}

const fallbackEntries = Object.entries(defaultContent.entries).reduce((entries, [key, value]) => ({
  ...entries,
  [key]: {
    key,
    type: "TEXT",
    version: 0,
    value
  }
}), {});

export function ContentProvider({ children }) {
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
        // Keep local fallback. Public content must never block the product.
      });

    return () => {
      isCancelled = true;
    };
  }, []);

  const entries = useMemo(() => ({
    ...fallbackEntries,
    ...remoteEntries
  }), [remoteEntries]);

  const getEntry = useCallback((key) => entries[key] ?? null, [entries]);

  const t = useCallback((key, variables) => {
    const entry = getEntry(key);
    return interpolate(entry?.value ?? key, variables);
  }, [getEntry]);

  const value = useMemo(() => ({
    contentVersion,
    entries,
    getEntry,
    t
  }), [contentVersion, entries, getEntry, t]);

  return (
    <ContentContext.Provider value={value}>
      {children}
    </ContentContext.Provider>
  );
}

export function useContentText() {
  const context = useContext(ContentContext);

  if (!context) {
    throw new Error("useContentText must be used within ContentProvider.");
  }

  return context;
}
