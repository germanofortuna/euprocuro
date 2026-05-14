"use client";

import { X } from "lucide-react";
import { useLegalContent } from "./use-legal-content";
import { LEGAL_SLUGS } from "./legal-content";
import { Button } from "@/shared/ui/button";

export function LegalModal({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) {
  const { pages, termsVersion } = useLegalContent();
  const page = pages[LEGAL_SLUGS.terms];

  if (!isOpen) {
    return null;
  }

  return (
    <div className="modal-overlay modal-overlay--legal" role="presentation" onClick={onClose}>
      <div className="modal-card legal-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div>
            <span className="pill">Versão {termsVersion}</span>
            <h2>{page.title}</h2>
          </div>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Fechar modal">
            <X size={18} />
          </button>
        </div>
        <div className="legal-document legal-document--modal">
          {page.summary ? <p className="legal-summary">{page.summary}</p> : null}
          {(page.sections ?? []).map((section) => (
            <section key={section.title}>
              <h3>{section.title}</h3>
              {(section.paragraphs ?? []).map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
              {section.items?.length ? (
                <ul>{section.items.map((item) => <li key={item}>{item}</li>)}</ul>
              ) : null}
            </section>
          ))}
        </div>
        <div className="modal-actions">
          <a className="text-button" href={`/legal/${LEGAL_SLUGS.terms}`} onClick={onClose}>Abrir página completa</a>
          <Button type="button" onClick={onClose}>Entendi</Button>
        </div>
      </div>
    </div>
  );
}
