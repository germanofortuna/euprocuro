import { memo } from "react";

import { useContentText } from "../content/ContentContext";
import EmptyState from "./EmptyState";

const timestampFormatter = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short"
});

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL"
});

function formatTimestamp(value, t) {
  if (!value) {
    return t("global.time.now");
  }

  return timestampFormatter.format(new Date(value));
}

function currency(value, t) {
  if (value === null || value === undefined || value === "") {
    return t("global.currency.negotiable");
  }

  return currencyFormatter.format(Number(value));
}

const ConversationThread = memo(function ConversationThread({ messages, currentUserId, t }) {
  if (messages.length === 0) {
    return (
      <EmptyState
        title={t("conversation.empty.title")}
        description={t("conversation.empty.description")}
      />
    );
  }

  return messages.map((message) => {
    const isMine = message.senderId === currentUserId;
    return (
      <article
        key={message.id}
        className={`conversation-bubble ${isMine ? "conversation-bubble--mine" : ""}`}
      >
        <strong>{message.senderName}</strong>
        <p>{message.content}</p>
        <span>{formatTimestamp(message.createdAt, t)}</span>
      </article>
    );
  });
});

export default function OfferConversationModal({ modal, currentUserId, onClose, onDraftChange, onSubmit }) {
  const { t } = useContentText();

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
  const conversationMessages = modal.data?.messages ?? [];

  return (
    <div className="modal-overlay" role="presentation" onClick={onClose}>
      <div className="conversation-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
        <div className="feedback-modal__header">
          <div>
            <span className="eyebrow">{t("conversation.eyebrow")}</span>
            <h2>{modal.data?.interestTitle ?? t("conversation.fallbackTitle")}</h2>
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

        {modal.isLoading ? (
          <div className="loading-card">{t("conversation.loading")}</div>
        ) : (
          <>
            <div className="conversation-meta">
              <div className="hero-card">
                <strong>{t("conversation.contact.title")}</strong>
                <p>{counterpartyName ?? t("conversation.participantFallback")}</p>
                {counterpartyEmail ? <p>{counterpartyEmail}</p> : null}
                {counterpartyPhone ? <p>{counterpartyPhone}</p> : null}
                <div className="contact-actions">
                  {mailtoLink ? (
                    <a className="ghost-button" href={mailtoLink} target="_blank" rel="noreferrer">
                      {t("conversation.contact.email")}
                    </a>
                  ) : null}
                  {whatsappLink ? (
                    <a className="ghost-button" href={whatsappLink} target="_blank" rel="noreferrer">
                      WhatsApp
                    </a>
                  ) : null}
                  {!mailtoLink && !whatsappLink ? (
                    <span className="muted-inline">
                      {t("conversation.contact.chatOnly")}
                    </span>
                  ) : null}
                </div>
              </div>
              <div className="hero-card">
                <strong>{t("conversation.offer.title")}</strong>
                {modal.data?.offerImageUrl ? (
                  <img
                    className="offer-card__image"
                    src={modal.data.offerImageUrl}
                    alt={t("conversation.offer.imageAlt")}
                    loading="lazy"
                    decoding="async"
                  />
                ) : null}
                <p>{currency(modal.data?.offeredPrice, t)}</p>
                <p>{t("conversation.offer.buyer", { name: modal.data?.buyerName ?? "" })}</p>
              </div>
            </div>

            <div className="conversation-thread">
              <ConversationThread messages={conversationMessages} currentUserId={currentUserId} t={t} />
            </div>

            <form className="conversation-form" onSubmit={onSubmit}>
              <textarea
                rows="3"
                placeholder={t("conversation.form.placeholder")}
                value={modal.draftMessage}
                onChange={(event) => onDraftChange(event.target.value)}
                required
              />
              <button type="submit" className="primary-button" disabled={modal.isSending}>
                {modal.isSending ? t("common.actions.sending") : t("conversation.form.submit")}
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
