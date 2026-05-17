"use client";

import Link from "next/link";
import { Button } from "@/shared/ui/button";
import { LEGAL_SLUGS } from "@/features/legal/legal-content";
import { useCookieConsent } from "./cookie-consent-context";

export function CookieConsentBanner() {
  const { acceptAll, rejectOptional, openPreferences, isBannerVisible } = useCookieConsent();

  if (!isBannerVisible) {
    return null;
  }

  return (
    <section className="cookie-banner" aria-label="Consentimento de cookies">
      <div className="cookie-banner__content">
        <p>
          Usamos cookies necessários para o funcionamento do site e, com sua autorização, cookies de análise e marketing para melhorar sua experiência.
        </p>
        <div className="cookie-banner__links" aria-label="Documentos legais">
          <Link href={`/legal/${LEGAL_SLUGS.privacy}`}>Política de Privacidade</Link>
          <Link href={`/legal/${LEGAL_SLUGS.terms}`}>Termos de Uso</Link>
        </div>
      </div>
      <div className="cookie-banner__actions">
        <Button type="button" variant="primary" size="sm" onClick={acceptAll}>Aceitar todos</Button>
        <Button type="button" variant="outline" size="sm" onClick={rejectOptional}>Recusar opcionais</Button>
        <Button type="button" variant="ghost" size="sm" onClick={openPreferences}>Configurar</Button>
      </div>
    </section>
  );
}
