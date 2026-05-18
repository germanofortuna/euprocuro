"use client";

import { Button } from "@/shared/ui/button";
import { useCookieConsent } from "./cookie-consent-context";

export function CookiePreferencesButton() {
  const { openPreferences } = useCookieConsent();

  return (
    <div className="cookie-preferences-entrypoint">
      <Button type="button" variant="outline" onClick={openPreferences}>Gerenciar preferências de cookies</Button>
    </div>
  );
}
