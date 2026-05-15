"use client";

import { useState } from "react";
import type { ComponentProps } from "react";
import { fetchOfferConversation, sendOfferMessage } from "@/shared/api/client";
import type { Offer, OfferConversation } from "@/shared/api/types";
import { formatDateTime, statusLabel, statusTone } from "@/shared/lib/format";
import { Button } from "@/shared/ui/button";
import { EmptyState } from "@/shared/ui/empty-state";
import { usePlatform } from "@/features/platform/platform-context";

type FormSubmitHandler = NonNullable<ComponentProps<"form">["onSubmit"]>;

export function OffersPage({ type }: { type: "sent" | "received" }) {
  const { dashboard, currentUser, refreshPrivateData, setFeedback } = usePlatform();
  const offers = type === "sent" ? dashboard?.sentOffers ?? [] : dashboard?.receivedOffers ?? [];
  const [conversation, setConversation] = useState<{ visible: boolean; offer: Offer | null; data: OfferConversation | null; message: string; loading: boolean }>({
    visible: false,
    offer: null,
    data: null,
    message: "",
    loading: false
  });

  async function openConversation(offer: Offer) {
    setConversation({ visible: true, offer, data: null, message: "", loading: true });
    try {
      const data = await fetchOfferConversation(offer.id);
      setConversation({ visible: true, offer, data, message: "", loading: false });
    } catch (error) {
      setConversation({ visible: true, offer, data: { ...offer, messages: [] }, message: "", loading: false });
      setFeedback({ type: "error", title: "Conversa indisponivel", message: error instanceof Error ? error.message : "Nao foi possivel carregar a conversa." });
    }
  }

  const submitConversationMessage: FormSubmitHandler = async (event) => {
    event.preventDefault();
    if (!conversation.offer?.id || !conversation.message.trim()) {
      return;
    }
    setConversation((current) => ({ ...current, loading: true }));
    try {
      await sendOfferMessage(conversation.offer.id, { content: conversation.message.trim() });
      const data = await fetchOfferConversation(conversation.offer.id);
      setConversation((current) => ({ ...current, data, message: "", loading: false }));
      await refreshPrivateData();
    } catch (error) {
      setConversation((current) => ({ ...current, loading: false }));
      setFeedback({ type: "error", title: "Mensagem nao enviada", message: error instanceof Error ? error.message : "Tente novamente em instantes." });
    }
  };

  function closeConversation() {
    setConversation({ visible: false, offer: null, data: null, message: "", loading: false });
  }

  return (
    <div className="dashboard-page">
      <div className="dashboard-heading">
        <div><h1>{type === "sent" ? "Propostas enviadas" : "Propostas recebidas"}</h1><p>Acompanhe negociacoes e mensagens pelo chat da plataforma.</p></div>
      </div>
      {offers.length ? (
        <div className="manage-list">
          {offers.map((offer) => (
            <article className="manage-card" key={offer.id}>
              <div><span className={`status-pill status-pill--${statusTone(offer.status)}`}>{statusLabel(offer.status)}</span><h3>{offer.interestTitle ?? "Procura"}</h3><p>{offer.message ?? "Sem mensagem adicional."}</p><span>{formatDateTime(offer.createdAt)}</span></div>
              <Button variant="outline" onClick={() => openConversation(offer)}>Abrir conversa</Button>
            </article>
          ))}
        </div>
      ) : <EmptyState title="Nenhuma proposta por aqui" description="Quando houver propostas, elas aparecerao nesta tela." />}
      {conversation.visible ? (
        <div className="modal-overlay" role="presentation" onClick={closeConversation}>
          <section className="modal-card conversation-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
            <div className="modal-header">
              <div>
                <span className={`status-pill status-pill--${statusTone(conversation.offer?.status)}`}>{statusLabel(conversation.offer?.status)}</span>
                <h2>{conversation.offer?.interestTitle ?? "Conversa"}</h2>
              </div>
              <button type="button" className="icon-button" onClick={closeConversation} aria-label="Fechar conversa">x</button>
            </div>
            <div className="conversation-list" aria-live="polite">
              {conversation.loading && !conversation.data ? <p className="muted-text">Carregando conversa...</p> : null}
              {(conversation.data?.messages ?? []).length ? (
                conversation.data?.messages?.map((message) => (
                  <article className={message.senderId === currentUser?.id ? "chat-message chat-message--mine" : "chat-message"} key={message.id}>
                    <strong>{message.senderName ?? (message.senderId === currentUser?.id ? "Voce" : "Contato")}</strong>
                    <p>{message.content}</p>
                    <small>{formatDateTime(message.createdAt)}</small>
                  </article>
                ))
              ) : !conversation.loading ? (
                <p className="muted-text">Ainda nao ha mensagens nesta conversa.</p>
              ) : null}
            </div>
            <form className="conversation-form" onSubmit={submitConversationMessage}>
              <textarea rows={3} value={conversation.message} onChange={(event) => setConversation((current) => ({ ...current, message: event.target.value }))} placeholder="Digite sua mensagem" />
              <div className="modal-actions">
                <Button type="button" variant="outline" onClick={closeConversation}>Fechar</Button>
                <Button type="submit" disabled={conversation.loading || !conversation.message.trim()}>Enviar mensagem</Button>
              </div>
            </form>
          </section>
        </div>
      ) : null}
    </div>
  );
}
