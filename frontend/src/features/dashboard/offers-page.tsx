"use client";

import { useState } from "react";
import { ImagePlus, X } from "lucide-react";
import type { ChangeEvent, ComponentProps } from "react";
import { fetchOfferConversation, sendOfferMessage } from "@/shared/api/client";
import type { Offer, OfferConversation } from "@/shared/api/types";
import { formatDateTime, statusLabel, statusTone } from "@/shared/lib/format";
import { readImageFile } from "@/shared/lib/image-upload";
import { Button } from "@/shared/ui/button";
import { EmptyState } from "@/shared/ui/empty-state";
import { usePlatform } from "@/features/platform/platform-context";

type FormSubmitHandler = NonNullable<ComponentProps<"form">["onSubmit"]>;
const CHAT_IMAGE_OPTIONS = { maxBytes: 3 * 1024 * 1024, maxSide: 960, quality: 0.76, maxOutputLength: 1_000_000 } as const;

export function OffersPage({ type }: { type: "sent" | "received" }) {
  const { dashboard, currentUser, refreshPrivateData, setFeedback } = usePlatform();
  const offers = type === "sent" ? dashboard?.sentOffers ?? [] : dashboard?.receivedOffers ?? [];
  const [conversation, setConversation] = useState<{ visible: boolean; offer: Offer | null; data: OfferConversation | null; message: string; imageUrl: string; loading: boolean }>({
    visible: false,
    offer: null,
    data: null,
    message: "",
    imageUrl: "",
    loading: false
  });

  async function openConversation(offer: Offer) {
    setConversation({ visible: true, offer, data: null, message: "", imageUrl: "", loading: true });
    try {
      const data = await fetchOfferConversation(offer.id);
      setConversation({ visible: true, offer, data, message: "", imageUrl: "", loading: false });
    } catch (error) {
      setConversation({ visible: true, offer, data: { ...offer, messages: [] }, message: "", imageUrl: "", loading: false });
      setFeedback({ type: "error", title: "Conversa indisponivel", message: error instanceof Error ? error.message : "Nao foi possivel carregar a conversa." });
    }
  }

  const submitConversationMessage: FormSubmitHandler = async (event) => {
    event.preventDefault();
    if (!conversation.offer?.id || (!conversation.message.trim() && !conversation.imageUrl)) {
      return;
    }
    setConversation((current) => ({ ...current, loading: true }));
    try {
      await sendOfferMessage(conversation.offer.id, { content: conversation.message.trim(), imageUrl: conversation.imageUrl || null });
      const data = await fetchOfferConversation(conversation.offer.id);
      setConversation((current) => ({ ...current, data, message: "", imageUrl: "", loading: false }));
      await refreshPrivateData();
    } catch (error) {
      setConversation((current) => ({ ...current, loading: false }));
      setFeedback({ type: "error", title: "Mensagem nao enviada", message: error instanceof Error ? error.message : "Tente novamente em instantes." });
    }
  };

  async function handleConversationImageChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) {
      return;
    }
    try {
      const imageUrl = await readImageFile(file, CHAT_IMAGE_OPTIONS);
      setConversation((current) => ({ ...current, imageUrl }));
    } catch (error) {
      setFeedback({ type: "error", title: "Imagem invalida", message: error instanceof Error ? error.message : "Selecione outra imagem." });
    }
  }

  function closeConversation() {
    setConversation({ visible: false, offer: null, data: null, message: "", imageUrl: "", loading: false });
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
                    {message.content ? <p>{message.content}</p> : null}
                    {message.imageUrl ? <a href={message.imageUrl} target="_blank" rel="noreferrer"><img className="chat-message__image" src={message.imageUrl} alt="Imagem enviada no chat" /></a> : null}
                    <small>{formatDateTime(message.createdAt)}</small>
                  </article>
                ))
              ) : !conversation.loading ? (
                <p className="muted-text">Ainda nao ha mensagens nesta conversa.</p>
              ) : null}
            </div>
            <form className="conversation-form" onSubmit={submitConversationMessage}>
              <textarea rows={3} value={conversation.message} onChange={(event) => setConversation((current) => ({ ...current, message: event.target.value }))} placeholder="Digite sua mensagem" />
              {conversation.imageUrl ? (
                <div className="conversation-image-preview">
                  <img src={conversation.imageUrl} alt="Previa da imagem do chat" />
                  <button type="button" className="icon-button" onClick={() => setConversation((current) => ({ ...current, imageUrl: "" }))} aria-label="Remover imagem"><X size={16} /></button>
                </div>
              ) : null}
              <div className="modal-actions">
                <label className="button button--outline conversation-file-button">
                  <ImagePlus size={16} />
                  Anexar imagem
                  <input type="file" accept="image/jpeg,image/png,image/webp" onChange={handleConversationImageChange} />
                </label>
                <Button type="button" variant="outline" onClick={closeConversation}>Fechar</Button>
                <Button type="submit" disabled={conversation.loading || (!conversation.message.trim() && !conversation.imageUrl)}>Enviar mensagem</Button>
              </div>
            </form>
          </section>
        </div>
      ) : null}
    </div>
  );
}
