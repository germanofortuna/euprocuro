"use client";

import { Analytics } from "@/features/analytics/analytics";
import { useCookieConsent } from "./cookie-consent-context";

export function OptionalScripts() {
  const { hasCookieConsent, hasHydrated } = useCookieConsent();

  if (!hasHydrated || !hasCookieConsent("analytics")) {
    return null;
  }

  return <Analytics />;
}
