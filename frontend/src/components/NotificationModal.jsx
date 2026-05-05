import { useContentText } from "../content/ContentContext";

const timestampFormatter = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short"
});

function formatTimestamp(value, t) {
  if (!value) {
    return t("global.time.now");
  }

  return timestampFormatter.format(new Date(value));
}

export default function NotificationModal({
  visible,
  notifications,
  anchorStyle,
  onClose,
  onSelect,
  onMarkAllRead
}) {
  const { t } = useContentText();

  if (!visible) {
    return null;
  }

  return (
    <div className="modal-overlay modal-overlay--topbar modal-overlay--plain" role="presentation" onClick={onClose}>
      <div
        className="notification-modal"
        style={anchorStyle}
        role="dialog"
        aria-modal="true"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="feedback-modal__header">
          <div>
            <span className="eyebrow">{t("notifications.eyebrow")}</span>
            <h2>{t("notifications.title")}</h2>
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

        {notifications.length ? (
          <>
            <button type="button" className="text-button notification-read-all" onClick={onMarkAllRead}>
              {t("notifications.markAllRead")}
            </button>
            <div className="notification-list">
              {notifications.map((notification) => (
                <button
                  key={notification.id ?? notification.offerId}
                  type="button"
                  className="notification-item"
                  onClick={() => onSelect(notification)}
                >
                  <strong>{notification.title}</strong>
                  <p>{notification.message}</p>
                  <span>{formatTimestamp(notification.createdAt, t)}</span>
                </button>
              ))}
            </div>
          </>
        ) : (
          <div className="empty-state empty-state--compact">
            <h3>{t("notifications.empty.title")}</h3>
            <p>{t("notifications.empty.description")}</p>
          </div>
        )}
      </div>
    </div>
  );
}
