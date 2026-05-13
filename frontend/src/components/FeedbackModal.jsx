import { useContentText } from "../content/ContentContext";

function StatusIcon() {
  return (
    <svg
      className="feedback-modal__status-icon"
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" />
      <path d="M12 7.5v5.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M12 16.5h.01" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

export default function FeedbackModal({ modal, onClose }) {
  const { t } = useContentText();

  if (!modal) {
    return null;
  }

  const hasStatusIcon = ["error", "warning"].includes(modal.type);

  return (
    <div className="modal-overlay" role="presentation" onClick={onClose}>
      <div
        className={`feedback-modal feedback-modal--${modal.type ?? "info"}`}
        role="dialog"
        aria-modal="true"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="feedback-modal__header">
          <div className="feedback-modal__title">
            {hasStatusIcon ? <StatusIcon /> : null}
            <strong>{modal.title}</strong>
          </div>
          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            aria-label={t("common.actions.closeModal")}
          >
            X
          </button>
        </div>
        <p>{modal.message}</p>
        <button type="button" className="primary-button primary-button--compact" onClick={onClose}>
          {t("legal.understood")}
        </button>
      </div>
    </div>
  );
}
