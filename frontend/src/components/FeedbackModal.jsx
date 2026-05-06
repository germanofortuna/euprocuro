import { useContentText } from "../content/ContentContext";

export default function FeedbackModal({ modal, onClose }) {
  const { t } = useContentText();

  if (!modal) {
    return null;
  }

  return (
    <div className="modal-overlay" role="presentation" onClick={onClose}>
      <div
        className={`feedback-modal feedback-modal--${modal.type ?? "info"}`}
        role="dialog"
        aria-modal="true"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="feedback-modal__header">
          <strong>{modal.title}</strong>
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
