"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import {
  clearCookieConsent,
  getCookieConsent,
  hasCookieConsent as hasStoredCookieConsent,
  saveCookieConsent,
  shouldShowCookieBanner,
  subscribeCookieConsentChanges,
  type CookieConsentCategory,
  type CookieConsentPreferences
} from "./cookie-consent";

type CookieConsentContextValue = {
  consent: CookieConsentPreferences | null;
  hasHydrated: boolean;
  isBannerVisible: boolean;
  isPreferencesOpen: boolean;
  acceptAll: () => void;
  rejectOptional: () => void;
  savePreferences: (preferences: Pick<CookieConsentPreferences, "analytics" | "marketing">) => void;
  hasCookieConsent: (category: CookieConsentCategory) => boolean;
  openPreferences: () => void;
  closePreferences: () => void;
  clearPreferences: () => void;
};

const CookieConsentContext = createContext<CookieConsentContextValue | null>(null);

export function CookieConsentProvider({ children }: { children: React.ReactNode }) {
  const [consent, setConsent] = useState<CookieConsentPreferences | null>(null);
  const [hasHydrated, setHasHydrated] = useState(false);
  const [isBannerVisible, setIsBannerVisible] = useState(false);
  const [isPreferencesOpen, setIsPreferencesOpen] = useState(false);

  const syncConsent = useCallback(() => {
    const nextConsent = getCookieConsent();
    setConsent(nextConsent);
    setIsBannerVisible(shouldShowCookieBanner());
  }, []);

  useEffect(() => {
    syncConsent();
    setHasHydrated(true);
    return subscribeCookieConsentChanges(syncConsent);
  }, [syncConsent]);

  const persistPreferences = useCallback((preferences: Pick<CookieConsentPreferences, "analytics" | "marketing">) => {
    const nextConsent = saveCookieConsent(preferences);
    setConsent(nextConsent);
    setIsBannerVisible(false);
    setIsPreferencesOpen(false);
  }, []);

  const value = useMemo<CookieConsentContextValue>(
    () => ({
      consent,
      hasHydrated,
      isBannerVisible: hasHydrated && isBannerVisible,
      isPreferencesOpen,
      acceptAll: () => persistPreferences({ analytics: true, marketing: true }),
      rejectOptional: () => persistPreferences({ analytics: false, marketing: false }),
      savePreferences: persistPreferences,
      hasCookieConsent: (category) => hasStoredCookieConsent(category),
      openPreferences: () => setIsPreferencesOpen(true),
      closePreferences: () => setIsPreferencesOpen(false),
      clearPreferences: () => {
        clearCookieConsent();
        setConsent(null);
        setIsBannerVisible(true);
      }
    }),
    [consent, hasHydrated, isBannerVisible, isPreferencesOpen, persistPreferences]
  );

  return <CookieConsentContext.Provider value={value}>{children}</CookieConsentContext.Provider>;
}

export function useCookieConsent() {
  const context = useContext(CookieConsentContext);
  if (!context) {
    throw new Error("useCookieConsent must be used within CookieConsentProvider.");
  }
  return context;
}
