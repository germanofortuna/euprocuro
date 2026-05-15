"use client";

import Image from "next/image";
import Link from "next/link";
import { ArrowLeft, Copy, CreditCard, Flag, MapPin, MessageSquare, Pencil, RefreshCw, Send, Share2, Sparkles, Tag } from "lucide-react";
import { useEffect, useState } from "react";
import type { ComponentProps } from "react";
import mercadoPagoLogo from "@/assets/mercado-pago.svg";
import { usePlatform } from "@/features/platform/platform-context";
import { fetchInterest } from "@/shared/api/client";
import type { Interest, MonetizationProduct } from "@/shared/api/types";
import { budgetLabel, categoryLabel, listingExpirationLabel, locationLabel, statusLabel, statusTone } from "@/shared/lib/format";
import { referenceImageSrc } from "@/shared/lib/images";
import { readInterestListHref } from "@/shared/lib/interest-list-navigation";
import { Button } from "@/shared/ui/button";
import { FieldCounter } from "@/shared/ui/field-counter";
import { trackEvent } from "@/features/analytics/analytics";

type FormSubmitHandler = NonNullable<ComponentProps<"form">["onSubmit"]>;
const REPORT_REASON_MAX_LENGTH = 120;
const REPORT_MESSAGE_MAX_LENGTH = 600;

function moneyLabel(value?: number | null) {
  return Number(value ?? 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function boostSort(left: MonetizationProduct, right: MonetizationProduct) {
  return (left.sortOrder ?? left.durationDays ?? 0) - (right.sortOrder ?? right.durationDays ?? 0);
}

function WhatsAppIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="brand-svg">
      <path d="M12.04 2a9.86 9.86 0 0 0-8.5 14.86L2.5 22l5.28-1.02A9.93 9.93 0 1 0 12.04 2Zm5.78 14.16c-.24.68-1.38 1.3-1.93 1.34-.5.04-1.13.06-3.66-.98-3.07-1.27-5.05-4.37-5.2-4.57-.15-.2-1.24-1.65-1.24-3.16s.78-2.25 1.06-2.56c.28-.31.61-.39.82-.39h.59c.19.01.44-.07.69.52.26.63.88 2.15.96 2.31.08.16.13.35.03.55-.1.2-.15.32-.31.5-.16.18-.33.4-.47.54-.15.15-.31.32-.13.63.18.31.81 1.33 1.73 2.15 1.19 1.06 2.2 1.39 2.51 1.54.31.16.49.13.67-.08.18-.21.78-.91.99-1.22.21-.31.42-.26.7-.16.29.1 1.82.86 2.13 1.02.31.16.52.23.6.36.08.13.08.75-.16 1.43Z" />
    </svg>
  );
}

function XBrandIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="brand-svg">
      <path d="M14.38 10.23 22.26 1h-1.87l-6.84 8.01L8.09 1H1.8l8.26 12.12L1.8 22.8h1.87l7.22-8.46 5.77 8.46h6.29l-8.57-12.57Zm-2.56 3-0.84-1.21L4.33 2.42h2.86l5.38 7.76.84 1.21 6.99 10.09h-2.86l-5.72-8.25Z" />
    </svg>
  );
}

function FacebookIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="brand-svg">
      <path d="M14.2 8.1V6.55c0-.74.5-.91.85-.91h2.16V2.12L14.24 2.1c-3.3 0-4.05 2.47-4.05 4.05V8.1H7.6v3.63h2.59V22h4.01V11.73h2.96l.39-3.63H14.2Z" />
    </svg>
  );
}

export function InterestDetailPage({ interestId, initialInterest }: { interestId: string; initialInterest?: Interest | null }) {
  const { categories, dashboard, isLoadingPrivate, currentUser, monetization, submitOffer, submitReport, openAuthModal, setFeedback, closeOwnInterest, activateOwnInterest, renewOwnInterest, boostOwnInterest } = usePlatform();
  const [routeInterest, setRouteInterest] = useState<Interest | null>(null);
  const [isLoadingRouteInterest, setIsLoadingRouteInterest] = useState(false);
  const [isImageOpen, setIsImageOpen] = useState(false);
  const [interestListHref, setInterestListHref] = useState("/categorias");
  const ownDashboardInterest = currentUser?.id ? dashboard?.myInterests?.find((item) => item.id === interestId) ?? null : null;
  const routeInitialInterest = initialInterest?.id === interestId ? initialInterest : null;
  const routeFetchedInterest = routeInterest?.id === interestId ? routeInterest : null;
  const publicRouteInterest = routeInitialInterest ?? routeFetchedInterest;
  const detailInterest = ownDashboardInterest ?? publicRouteInterest;
  const isMine = Boolean(ownDashboardInterest);
  const [offerForm, setOfferForm] = useState({ offeredPrice: "", sellerPhone: "", message: "", highlights: "" });
  const [reportForm, setReportForm] = useState({ reason: "", message: "" });
  const [isReportOpen, setIsReportOpen] = useState(false);
  const [isOfferSubmitting, setIsOfferSubmitting] = useState(false);
  const [isReportSubmitting, setIsReportSubmitting] = useState(false);
  const [boostingKey, setBoostingKey] = useState<string | null>(null);
  const [selectedBoostCode, setSelectedBoostCode] = useState("");
  const [selectedBoostPayment, setSelectedBoostPayment] = useState<"CREDITS" | "MERCADO_PAGO">("MERCADO_PAGO");
  const [shareUrl, setShareUrl] = useState("");

  useEffect(() => {
    setRouteInterest(null);
    setIsLoadingRouteInterest(false);
  }, [interestId]);

  useEffect(() => {
    if (!interestId || ownDashboardInterest || routeInitialInterest || routeFetchedInterest) {
      return;
    }
    let isCurrent = true;
    setIsLoadingRouteInterest(true);
    fetchInterest(interestId)
      .then((detail) => {
        if (isCurrent && detail.id === interestId) {
          setRouteInterest(detail);
        }
      })
      .catch(() => {
        if (isCurrent) {
          setRouteInterest(null);
        }
      })
      .finally(() => {
        if (isCurrent) {
          setIsLoadingRouteInterest(false);
        }
      });
    return () => {
      isCurrent = false;
    };
  }, [interestId, ownDashboardInterest, routeInitialInterest, routeFetchedInterest]);

  useEffect(() => {
    setShareUrl(window.location.href);
    setInterestListHref(readInterestListHref());
  }, []);

  if (!detailInterest && (isLoadingRouteInterest || isLoadingPrivate)) {
    return (
      <section className="route-shell centered-route">
        <div className="section-loading" role="status">Carregando detalhes da procura...</div>
      </section>
    );
  }

  if (!detailInterest) {
    return (
      <section className="route-shell centered-route">
        <div className="auth-card">
          <h1>Procura não encontrada</h1>
          <p>Essa procura pode não existir mais ou ainda não estar pública.</p>
          <Link className="button button--primary" href={interestListHref}>Voltar para as procuras</Link>
        </div>
      </section>
    );
  }

  const resolvedInterest = detailInterest;
  const isRejected = resolvedInterest.status === "REJECTED";
  const detailImageSrc = referenceImageSrc(resolvedInterest);
  const whatsappUrl = `https://wa.me/?text=${encodeURIComponent(`Olha esta procura no Eu Procuro: ${resolvedInterest.title} - ${shareUrl}`)}`;
  const xUrl = `https://twitter.com/intent/tweet?text=${encodeURIComponent(`Olha esta procura no Eu Procuro: ${resolvedInterest.title}`)}&url=${encodeURIComponent(shareUrl)}`;
  const facebookUrl = `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(shareUrl)}`;
  const creditPurchasesEnabled = Boolean(monetization?.settings?.creditPurchasesEnabled);
  const boostPurchasesEnabled = Boolean(monetization?.settings?.boostPurchasesEnabled);
  const sellerCredits = monetization?.sellerCredits ?? currentUser?.sellerCredits ?? currentUser?.credits ?? 0;
  const boostProducts = (monetization?.products ?? [])
    .filter((product) => String(product.type).toUpperCase() === "BOOST" && product.enabled !== false)
    .sort(boostSort);
  const selectedBoostProduct = boostProducts.find((product) => product.code === selectedBoostCode) ?? boostProducts[0] ?? null;
  const selectedBoostCreditCost = Number(selectedBoostProduct?.credits ?? 0);
  const selectedBoostCanUseCredits = creditPurchasesEnabled && selectedBoostCreditCost > 0;
  const selectedBoostHasEnoughCredits = selectedBoostCanUseCredits && sellerCredits >= selectedBoostCreditCost;
  const selectedBoostPaymentMethod = selectedBoostCanUseCredits ? selectedBoostPayment : "MERCADO_PAGO";
  const boostedUntil = resolvedInterest.boostedUntil ? new Date(resolvedInterest.boostedUntil) : null;
  const isBoostActive = Boolean(boostedUntil && boostedUntil.getTime() > Date.now());
  const interestStatus = String(resolvedInterest.status ?? "OPEN").toUpperCase();
  const isPublishedInterest = ["OPEN", "APPROVED"].includes(interestStatus);
  const shouldShowBoostWaitMessage = isMine && ["PENDING", "REVIEW_REQUIRED", "IN_REVIEW"].includes(interestStatus);

  const submitOfferForm: FormSubmitHandler = async (event) => {
    event.preventDefault();
    setIsOfferSubmitting(true);
    try {
      await submitOffer(resolvedInterest.id, {
        offeredPrice: offerForm.offeredPrice ? Number(offerForm.offeredPrice) : null,
        sellerPhone: offerForm.sellerPhone,
        message: offerForm.message,
        highlights: offerForm.highlights.split(",").map((item) => item.trim()).filter(Boolean)
      });
      setOfferForm({ offeredPrice: "", sellerPhone: "", message: "", highlights: "" });
    } finally {
      setIsOfferSubmitting(false);
    }
  };

  async function copyLink() {
    await navigator.clipboard?.writeText(shareUrl);
    trackEvent("share_interest", { method: "copy", interest_id: resolvedInterest.id });
    setFeedback({ type: "success", title: "Link copiado", message: "Agora você pode compartilhar esta procura." });
  }

  const submitReportForm: FormSubmitHandler = async (event) => {
    event.preventDefault();
    setIsReportSubmitting(true);
    try {
      await submitReport(resolvedInterest.id, reportForm);
      setReportForm({ reason: "", message: "" });
      setIsReportOpen(false);
    } finally {
      setIsReportSubmitting(false);
    }
  };

  async function handleBoost(product: MonetizationProduct, paymentMethod: "CREDITS" | "MERCADO_PAGO") {
    const key = `${product.code}:${paymentMethod}`;
    setBoostingKey(key);
    try {
      await boostOwnInterest(resolvedInterest.id, product.code, paymentMethod);
    } catch (error) {
      setFeedback({ type: "error", title: "Boost indisponivel", message: error instanceof Error ? error.message : "Nao foi possivel ativar o boost agora." });
    } finally {
      setBoostingKey(null);
    }
  }

  async function handleSelectedBoost() {
    if (!selectedBoostProduct) {
      return;
    }
    await handleBoost(selectedBoostProduct, selectedBoostPaymentMethod);
  }

  return (
    <section className="route-shell detail-route">
      <Link href={interestListHref} className="back-link"><ArrowLeft size={16} /> Voltar para as procuras</Link>
      <div className="detail-grid">
        <article className="detail-main">
          <span className={`pill pill--${statusTone(resolvedInterest.status)}`}>{statusLabel(resolvedInterest.status ?? "OPEN")}</span>
          {detailImageSrc ? (
            <button type="button" className="detail-hero-image" onClick={() => setIsImageOpen(true)} aria-label="Ampliar imagem da procura">
              <img src={detailImageSrc} alt={`Imagem de referencia da procura ${resolvedInterest.title}`} />
            </button>
          ) : null}
          <h1>{resolvedInterest.title}</h1>
          <div className="interest-meta interest-meta--large">
            <span><Tag size={16} /> {categoryLabel(categories, resolvedInterest.category)}</span>
            <span><MapPin size={16} /> {locationLabel(resolvedInterest)}</span>
          </div>
          <section className="document-section">
            <h2>Descrição</h2>
            <p>{resolvedInterest.description}</p>
          </section>
          <section className="document-section">
            <h2>Tags</h2>
            <div className="tag-list">{(resolvedInterest.tags ?? []).map((tag) => <span key={tag}>{tag}</span>)}</div>
          </section>
        </article>
        <aside className="action-panel">
          <div>
            <span>Orçamento estimado</span>
            <strong>{budgetLabel(resolvedInterest)}</strong>
          </div>
          {isMine ? (
            <div className="notice-box">
              <strong>{isRejected ? "Sua procura foi rejeitada pela nossa moderação!" : "Esta procura é sua"}</strong>
              <p>{isRejected ? "Você pode editá-la para que seja reavaliada." : "Gerencie ajustes, renovacao e disponibilidade desta procura por aqui."}</p>
              {!isRejected ? <span className="detail-expiration">{listingExpirationLabel(resolvedInterest)}</span> : null}
              {shouldShowBoostWaitMessage ? (
                <div className="boost-wait-message">
                  <strong>Aguarde a publicação</strong>
                  <p>O boost ficará disponível depois que sua procura for aprovada e publicada pela moderação.</p>
                </div>
              ) : null}
              {isPublishedInterest && boostPurchasesEnabled && boostProducts.length ? (
                <div className="boost-panel">
                  <div className="boost-panel__header">
                    <strong><Sparkles size={16} /> Impulsionar procura</strong>
                    {isBoostActive && boostedUntil ? <span>Boost ativo ate {boostedUntil.toLocaleDateString("pt-BR")}</span> : null}
                  </div>
                  <label className="boost-select-label">Escolha o boost
                    <select value={selectedBoostProduct?.code ?? ""} onChange={(event) => setSelectedBoostCode(event.target.value)}>
                      {boostProducts.map((product) => (
                        <option key={product.code} value={product.code}>
                          {product.name} - {product.durationDays ? `${product.durationDays} dias` : "duracao configuravel"} - {moneyLabel(product.price)}
                        </option>
                      ))}
                    </select>
                  </label>
                  {selectedBoostProduct ? (
                    <div className="boost-selected-summary">
                      <strong>{selectedBoostProduct.name}</strong>
                      <span>{selectedBoostProduct.durationDays ? `${selectedBoostProduct.durationDays} dias de destaque` : "Duração configurável"} · {moneyLabel(selectedBoostProduct.price)}</span>
                      {selectedBoostCanUseCredits ? <small>{selectedBoostCreditCost} créditos para ativar pelo saldo</small> : creditPurchasesEnabled ? <small>Custo em créditos ainda não configurado no CRM.</small> : null}
                    </div>
                  ) : null}
                  <div className="boost-payment-choice" role="radiogroup" aria-label="Forma de pagamento do boost">
                    {selectedBoostCanUseCredits ? (
                      <label>
                        <input type="radio" name="boost-payment" checked={selectedBoostPayment === "CREDITS"} onChange={() => setSelectedBoostPayment("CREDITS")} />
                        <span><CreditCard size={15} /> Usar {selectedBoostCreditCost} créditos</span>
                      </label>
                    ) : null}
                    <label>
                      <input type="radio" name="boost-payment" checked={selectedBoostPaymentMethod === "MERCADO_PAGO"} onChange={() => setSelectedBoostPayment("MERCADO_PAGO")} />
                      <span><Image className="payment-gateway-icon payment-gateway-icon--inline" src={mercadoPagoLogo} alt="" width={42} height={26} aria-hidden="true" /> Mercado Pago</span>
                    </label>
                  </div>
                  {selectedBoostPaymentMethod === "CREDITS" && selectedBoostCanUseCredits && !selectedBoostHasEnoughCredits ? (
                    <Link className="button button--outline" href="/comprar-creditos"><CreditCard size={15} /> Adicionar créditos</Link>
                  ) : (
                    <Button
                      type="button"
                      className="payment-button"
                      disabled={!selectedBoostProduct || Boolean(boostingKey)}
                      onClick={handleSelectedBoost}
                    >
                      {selectedBoostPaymentMethod === "MERCADO_PAGO" ? <Image className="payment-gateway-icon" src={mercadoPagoLogo} alt="" width={54} height={36} aria-hidden="true" /> : <CreditCard size={17} />}
                      <span>{boostingKey ? "Ativando..." : selectedBoostPaymentMethod === "CREDITS" ? `Impulsionar com ${selectedBoostCreditCost} créditos` : "Pagar boost com Mercado Pago"}</span>
                    </Button>
                  )}
                </div>
              ) : null}
              <div className="owner-action-grid">
                <Link className="button button--primary" href={`/cadastrar-interesse?editar=${resolvedInterest.id}`}><Pencil size={16} /> Editar</Link>
                {!isRejected ? <Button type="button" variant="outline" onClick={() => renewOwnInterest(resolvedInterest.id)} title="Usa 1 credito para adicionar mais 30 dias a procura"><RefreshCw size={16} /> Renovar por 1 credito</Button> : null}
                {resolvedInterest.status === "CLOSED" ? (
                  <Button type="button" variant="outline" onClick={() => activateOwnInterest(resolvedInterest.id)}>Ativar</Button>
                ) : (
                  <Button type="button" variant="outline" onClick={() => closeOwnInterest(resolvedInterest.id)}>Desativar</Button>
                )}
                <Link className="button button--outline" href="/meus-interesses">Ir para minhas procuras</Link>
              </div>
            </div>
          ) : currentUser?.id ? (
            <form className="stack-form" onSubmit={submitOfferForm}>
              <label>Valor da proposta<input type="number" min="0" value={offerForm.offeredPrice} onChange={(event) => setOfferForm((current) => ({ ...current, offeredPrice: event.target.value }))} /></label>
              <label>Telefone ou WhatsApp<input value={offerForm.sellerPhone} onChange={(event) => setOfferForm((current) => ({ ...current, sellerPhone: event.target.value }))} /></label>
              <label>Mensagem<textarea rows={4} value={offerForm.message} onChange={(event) => setOfferForm((current) => ({ ...current, message: event.target.value }))} required /></label>
              <label>Destaques<input value={offerForm.highlights} onChange={(event) => setOfferForm((current) => ({ ...current, highlights: event.target.value }))} placeholder="Entrega, garantia, disponibilidade" /></label>
              <Button type="submit" disabled={isOfferSubmitting}><Send size={16} /> {isOfferSubmitting ? "Enviando..." : "Enviar proposta"}</Button>
            </form>
          ) : (
            <div className="notice-box">
              <strong>Tem algo para atender esta procura?</strong>
              <p>Entre na plataforma para enviar uma proposta ou publicar sua própria procura.</p>
              <Button type="button" onClick={() => openAuthModal("login")}>Entrar para enviar proposta</Button>
            </div>
          )}
          <div className="share-panel">
            <h2><Share2 size={17} /> Compartilhar</h2>
            <div className="share-grid">
              <a className="button button--outline button--sm share-icon-button" href={whatsappUrl} target="_blank" rel="noreferrer" aria-label="Compartilhar no WhatsApp" title="WhatsApp" onClick={() => trackEvent("share_interest", { method: "whatsapp", interest_id: resolvedInterest.id })}><span className="brand-icon brand-icon--whatsapp"><WhatsAppIcon /></span></a>
              <a className="button button--outline button--sm share-icon-button" href={xUrl} target="_blank" rel="noreferrer" aria-label="Compartilhar no X" title="X" onClick={() => trackEvent("share_interest", { method: "x", interest_id: resolvedInterest.id })}><span className="brand-icon brand-icon--x"><XBrandIcon /></span></a>
              <a className="button button--outline button--sm share-icon-button" href={facebookUrl} target="_blank" rel="noreferrer" aria-label="Compartilhar no Facebook" title="Facebook" onClick={() => trackEvent("share_interest", { method: "facebook", interest_id: resolvedInterest.id })}><span className="brand-icon brand-icon--facebook"><FacebookIcon /></span></a>
              <button className="button button--outline button--sm" type="button" onClick={copyLink}><Copy size={15} /> Copiar Link</button>
            </div>
          </div>
          <button type="button" className="text-button danger-text" onClick={() => currentUser?.id ? setIsReportOpen(true) : openAuthModal("login")}><Flag size={15} /> Denunciar esta procura</button>
        </aside>
      </div>
      {isReportOpen ? (
        <div className="modal-overlay" role="presentation" onClick={() => setIsReportOpen(false)}>
          <form className="modal-card report-modal stack-form" role="dialog" aria-modal="true" onSubmit={submitReportForm} onClick={(event) => event.stopPropagation()}>
            <h2>Denunciar procura</h2>
            <label>Motivo<input value={reportForm.reason} maxLength={REPORT_REASON_MAX_LENGTH} onChange={(event) => setReportForm((current) => ({ ...current, reason: event.target.value }))} required /><FieldCounter value={reportForm.reason} max={REPORT_REASON_MAX_LENGTH} /></label>
            <label>Mensagem<textarea rows={4} value={reportForm.message} maxLength={REPORT_MESSAGE_MAX_LENGTH} onChange={(event) => setReportForm((current) => ({ ...current, message: event.target.value }))} required /><FieldCounter value={reportForm.message} max={REPORT_MESSAGE_MAX_LENGTH} /></label>
            <div className="modal-actions">
              <Button type="button" variant="outline" onClick={() => setIsReportOpen(false)}>Cancelar</Button>
              <Button type="submit" disabled={isReportSubmitting}><MessageSquare size={16} /> {isReportSubmitting ? "Enviando..." : "Enviar denuncia"}</Button>
            </div>
          </form>
        </div>
      ) : null}
      {isImageOpen && detailImageSrc ? (
        <div className="modal-overlay image-lightbox" role="presentation" onClick={() => setIsImageOpen(false)}>
          <div className="image-lightbox__content" role="dialog" aria-modal="true" aria-label="Imagem ampliada da procura" onClick={(event) => event.stopPropagation()}>
            <button type="button" className="icon-button image-lightbox__close" onClick={() => setIsImageOpen(false)} aria-label="Fechar imagem ampliada">×</button>
            <img src={detailImageSrc} alt={`Imagem de referencia da procura ${resolvedInterest.title}`} />
          </div>
        </div>
      ) : null}
    </section>
  );
}
