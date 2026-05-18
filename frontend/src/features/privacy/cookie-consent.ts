"use client";

export const CONSENT_VERSION = "1.0";
const COOKIE_CONSENT_STORAGE_KEY = "eu-procuro-cookie-consent";
const COOKIE_CONSENT_EVENT = "eu-procuro-cookie-consent-changed";

export type CookieConsentCategory = "necessary" | "analytics" | "marketing";

export type CookieConsentPreferences = {
  necessary: true;
  analytics: boolean;
  marketing: boolean;
  version: string;
  updatedAt: string;
};

type CookieConsentInput = Partial<Pick<CookieConsentPreferences, "analytics" | "marketing">>;

function browserStorage() {
  return typeof window === "undefined" ? null : window.localStorage;
}

function notifyConsentChanged() {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(COOKIE_CONSENT_EVENT));
  }
}

function normalizeConsent(value: unknown): CookieConsentPreferences | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const record = value as Partial<CookieConsentPreferences>;
  if (record.version !== CONSENT_VERSION) {
    return null;
  }

  return {
    necessary: true,
    analytics: Boolean(record.analytics),
    marketing: Boolean(record.marketing),
    version: CONSENT_VERSION,
    updatedAt: typeof record.updatedAt === "string" ? record.updatedAt : new Date().toISOString()
  };
}

export function getCookieConsent(): CookieConsentPreferences | null {
  const storage = browserStorage();
  if (!storage) {
    return null;
  }

  try {
    const rawValue = storage.getItem(COOKIE_CONSENT_STORAGE_KEY);
    return rawValue ? normalizeConsent(JSON.parse(rawValue)) : null;
  } catch {
    storage.removeItem(COOKIE_CONSENT_STORAGE_KEY);
    return null;
  }
}

export function saveCookieConsent(preferences: CookieConsentInput): CookieConsentPreferences {
  const nextPreferences: CookieConsentPreferences = {
    necessary: true,
    analytics: Boolean(preferences.analytics),
    marketing: Boolean(preferences.marketing),
    version: CONSENT_VERSION,
    updatedAt: new Date().toISOString()
  };

  browserStorage()?.setItem(COOKIE_CONSENT_STORAGE_KEY, JSON.stringify(nextPreferences));
  notifyConsentChanged();
  return nextPreferences;
}

export function hasCookieConsent(category: CookieConsentCategory) {
  if (category === "necessary") {
    return true;
  }
  return Boolean(getCookieConsent()?.[category]);
}

export function shouldShowCookieBanner() {
  return !getCookieConsent();
}

export function clearCookieConsent() {
  browserStorage()?.removeItem(COOKIE_CONSENT_STORAGE_KEY);
  notifyConsentChanged();
}

export function subscribeCookieConsentChanges(listener: () => void) {
  if (typeof window === "undefined") {
    return () => {};
  }

  window.addEventListener(COOKIE_CONSENT_EVENT, listener);
  window.addEventListener("storage", listener);
  return () => {
    window.removeEventListener(COOKIE_CONSENT_EVENT, listener);
    window.removeEventListener("storage", listener);
  };
}
