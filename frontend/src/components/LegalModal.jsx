import { useEffect } from "react";

import { useContentText } from "../content/ContentContext";
import { useLegalContent } from "../content/useLegalContent";
import { LEGAL_SLUGS } from "../legalContent";

export default function LegalModal({ isOpen, onClose }) {
  const { t } = useContentText();
  const { pages, termsVersion } = useLegalContent();
  const page = pages[LEGAL_SLUGS.terms];

  useEffect(() => {
    if (!isOpen) {
      return undefined;
    }

    function handleKeyDown(event) {
      if (event.key === "Escape") {
        onClose();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !page) {
    return null;
  }

  return (
    <div
      className="modal-overlay modal-overlay--legal"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <section
        className="legal-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="terms-modal-title"
      >
        <div className="legal-modal__header">
          <div>
            <span className="eyebrow">{t("legal.version", { version: termsVersion })}</span>
            <h3 id="terms-modal-title">{page.title}</h3>
          </div>
          <button type="button" className="icon-button" aria-label={t("common.actions.closeTerms")} onClick={onClose}>
            X
          </button>
        </div>

        <div className="legal-modal__body">
          {page.sections.map((section) => (
            <section key={section.title}>
              <h4>{section.title}</h4>
              {section.paragraphs?.map((paragraph) => (
                <p key={paragraph}>{paragraph}</p>
              ))}
              {section.items?.length ? (
                <ul>
                  {section.items.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              ) : null}
            </section>
          ))}
        </div>

        <div className="legal-modal__footer">
          <a href={`#${LEGAL_SLUGS.terms}`} onClick={onClose}>
            {t("legal.fullPage")}
          </a>
          <button type="button" className="primary-button secondary" onClick={onClose}>
            {t("legal.understood")}
          </button>
        </div>
      </section>
    </div>
  );
}
