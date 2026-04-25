import EmptyState from "./EmptyState";

function formatTimestamp(value) {
  if (!value) {
    return "Agora";
  }

  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}

function currency(value) {
  if (value === null || value === undefined || value === "") {
    return "A combinar";
  }

  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
  }).format(Number(value));
}

export default function OfferConversationModal({ modal, currentUserId, onClose, onDraftChange, onSubmit }) {
  if (!modal?.visible) {
    return null;
  }

  const isSeller = modal.data?.sellerId === currentUserId;
  const counterpartyName = isSeller ? modal.data?.buyerName : modal.data?.sellerName;
  const counterpartyEmail = isSeller ? null : modal.data?.sellerEmail;
  const counterpartyPhone = isSeller ? null : modal.data?.sellerPhone;
  const whatsappLink = counterpartyPhone
    ? `https://wa.me/${counterpartyPhone.replace(/\D/g, "")}`
    : null;
  const mailtoLink = counterpartyEmail ? `mailto:${counterpartyEmail}` : null;

  return (
    <div className="modal-overlay" role="presentation" onClick={onClose}>
      <div className="conversation-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
        <div className="feedback-modal__header">
          <div>
            <span className="eyebrow">Conversa</span>
            <h2>{modal.data?.interestTitle ?? "Oferta recebida"}</h2>
          </div>
          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            aria-label="Fechar modal"
          >
            X
          </button>
        </div>

        {modal.isLoading ? (
          <div className="loading-card">Carregando conversa...</div>
        ) : (
          <>
            <div className="conversation-meta">
              <div className="hero-card">
                <strong>Contato</strong>
                <p>{counterpartyName ?? "Participante da conversa"}</p>
                {counterpartyEmail ? <p>{counterpartyEmail}</p> : null}
                {counterpartyPhone ? <p>{counterpartyPhone}</p> : null}
                <div className="contact-actions">
                  {mailtoLink ? (
                    <a className="ghost-button" href={mailtoLink} target="_blank" rel="noreferrer">
                      Enviar e-mail
                    </a>
                  ) : null}
                  {whatsappLink ? (
                    <a className="ghost-button" href={whatsappLink} target="_blank" rel="noreferrer">
                      WhatsApp
                    </a>
                  ) : null}
                  {!mailtoLink && !whatsappLink ? (
                    <span className="muted-inline">
                      Use o chat abaixo para alinhar os detalhes.
                    </span>
                  ) : null}
                </div>
              </div>
              <div className="hero-card">
                <strong>Oferta</strong>
                <p>{currency(modal.data?.offeredPrice)}</p>
                <p>Comprador: {modal.data?.buyerName}</p>
              </div>
            </div>

            <div className="conversation-thread">
              {(modal.data?.messages ?? []).length === 0 ? (
                <EmptyState
                  title="Ainda sem mensagens"
                  description="Use o chat abaixo para alinhar detalhes da negociação dentro da plataforma."
                />
              ) : (
                modal.data.messages.map((message) => {
                  const isMine = message.senderId === currentUserId;
                  return (
                    <article
                      key={message.id}
                      className={`conversation-bubble ${isMine ? "conversation-bubble--mine" : ""}`}
                    >
                      <strong>{message.senderName}</strong>
                      <p>{message.content}</p>
                      <span>{formatTimestamp(message.createdAt)}</span>
                    </article>
                  );
                })
              )}
            </div>

            <form className="conversation-form" onSubmit={onSubmit}>
              <textarea
                rows="3"
                placeholder="Escreva sua mensagem para continuar a negociação"
                value={modal.draftMessage}
                onChange={(event) => onDraftChange(event.target.value)}
                required
              />
              <button type="submit" className="primary-button" disabled={modal.isSending}>
                {modal.isSending ? "Enviando..." : "Enviar mensagem"}
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
