function formatTimestamp(value) {
  if (!value) {
    return "Agora";
  }

  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}

export default function NotificationModal({
  visible,
  notifications,
  anchorStyle,
  onClose,
  onSelect,
  onMarkAllRead
}) {
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
            <span className="eyebrow">Notificações</span>
            <h2>Novidades</h2>
          </div>
          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            aria-label="Fechar modal"
          >
            ×
          </button>
        </div>

        {notifications.length ? (
          <>
            <button type="button" className="text-button notification-read-all" onClick={onMarkAllRead}>
              Marcar todas como lida
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
                  <span>{formatTimestamp(notification.createdAt)}</span>
                </button>
              ))}
            </div>
          </>
        ) : (
          <div className="empty-state empty-state--compact">
            <h3>Nada novo por aqui</h3>
            <p>Quando chegar uma mensagem ou um interesse compatível, ele vai aparecer neste painel.</p>
          </div>
        )}
      </div>
    </div>
  );
}
