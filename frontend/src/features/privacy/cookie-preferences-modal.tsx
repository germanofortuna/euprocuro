"use client";

import { useEffect, useState } from "react";
import { Button } from "@/shared/ui/button";
import { useCookieConsent } from "./cookie-consent-context";

type PreferenceRowProps = {
  title: string;
  description: string;
  checked: boolean;
  disabled?: boolean;
  onChange?: (checked: boolean) => void;
};

function PreferenceRow({ title, description, checked, disabled = false, onChange }: PreferenceRowProps) {
  return (
    <label className={`cookie-preference-row${disabled ? " is-disabled" : ""}`}>
      <span>
        <strong>{title}</strong>
        <small>{description}</small>
      </span>
      <input type="checkbox" checked={checked} disabled={disabled} onChange={(event) => onChange?.(event.target.checked)} />
    </label>
  );
}

export function CookiePreferencesModal() {
  const { consent, acceptAll, closePreferences, isPreferencesOpen, rejectOptional, savePreferences } = useCookieConsent();
  const [analytics, setAnalytics] = useState(false);
  const [marketing, setMarketing] = useState(false);

  useEffect(() => {
    if (!isPreferencesOpen) {
      return;
    }
    setAnalytics(Boolean(consent?.analytics));
    setMarketing(Boolean(consent?.marketing));
  }, [consent, isPreferencesOpen]);

  if (!isPreferencesOpen) {
    return null;
  }

  return (
    <div className="modal-overlay modal-overlay--plain cookie-modal-overlay" role="presentation">
      <section className="modal-card cookie-preferences-modal" role="dialog" aria-modal="true" aria-labelledby="cookie-preferences-title">
        <div className="modal-header">
          <div>
            <span className="pill">LGPD e cookies</span>
            <h2 id="cookie-preferences-title">Preferências de cookies</h2>
          </div>
          <button type="button" className="text-button" onClick={closePreferences}>Fechar</button>
        </div>

        <p className="cookie-preferences-modal__intro">
          Você pode permitir ou bloquear cookies opcionais. Os necessários continuam ativos para manter segurança, sessão e preferências essenciais.
        </p>

        <div className="cookie-preferences-list">
          <PreferenceRow
            title="Necessários"
            description="Mantêm autenticação, sessão, segurança e preferências essenciais da aplicação."
            checked
            disabled
          />
          <PreferenceRow
            title="Analytics"
            description="Ajuda a entender navegação e uso do site. Fica desligado até sua autorização."
            checked={analytics}
            onChange={setAnalytics}
          />
          <PreferenceRow
            title="Marketing"
            description="Pode ser usado no futuro para campanhas, pixels e remarketing. Fica desligado por padrão."
            checked={marketing}
            onChange={setMarketing}
          />
        </div>

        <div className="modal-actions cookie-preferences-actions">
          <Button type="button" variant="primary" onClick={() => savePreferences({ analytics, marketing })}>Salvar preferências</Button>
          <Button type="button" variant="outline" onClick={rejectOptional}>Recusar opcionais</Button>
          <Button type="button" variant="ghost" onClick={acceptAll}>Aceitar todos</Button>
        </div>
      </section>
    </div>
  );
}
