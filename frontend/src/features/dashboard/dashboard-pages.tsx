"use client";

import Image from "next/image";
import Link from "next/link";
import { CreditCard, Eye, MessageSquare, Package, Plus, Search, Sparkles, Trash2, Upload, type LucideIcon } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import type { ComponentProps } from "react";
import mercadoPagoLogo from "@/assets/mercado-pago.svg";
import { usePlatform } from "@/features/platform/platform-context";
import type { Interest, Offer, OfferConversation } from "@/shared/api/types";
import { fetchOfferConversation, sendOfferMessage } from "@/shared/api/client";
import { budgetLabel, categoryLabel, formatDateTime, listingExpirationLabel, locationLabel, statusLabel, statusTone } from "@/shared/lib/format";
import { referenceImageSrc } from "@/shared/lib/images";
import { readImageFile } from "@/shared/lib/image-upload";
import { rememberInterestListHref } from "@/shared/lib/interest-list-navigation";
import { Button } from "@/shared/ui/button";
import { EmptyState } from "@/shared/ui/empty-state";

type FormSubmitHandler = NonNullable<ComponentProps<"form">["onSubmit"]>;

function StatCard({ title, value, detail, icon: Icon }: { title: string; value: number | string; detail: string; icon: LucideIcon }) {
  return (
    <article className="stat-card">
      <div><h2>{title}</h2><strong>{value}</strong><p>{detail}</p></div>
      <Icon size={20} />
    </article>
  );
}

function ManageInterestCard({ interest }: { interest: Interest }) {
  const { categories, deleteOwnInterest } = usePlatform();
  const imageSrc = referenceImageSrc(interest);

  return (
    <article className={`manage-card${imageSrc ? " manage-card--with-image" : ""}`}>
      
      {imageSrc ? (
        <Link className="manage-card__image" href={`/interesses/${interest.id}`} aria-label={`Ver detalhes de ${interest.title}`}>
          <img src={imageSrc} alt={`Imagem de referencia da procura ${interest.title}`} loading="lazy" />
        </Link>
      ) : null}
      <div>
        <span className={`status-pill status-pill--${statusTone(interest.status)}`}>{statusLabel(interest.status ?? "OPEN")}</span>
        <h3>{interest.title}</h3>
        <p>{interest.description}</p>
        <div className="interest-meta">
          <span>{categoryLabel(categories, interest.category)}</span>
          <span>{locationLabel(interest)}</span>
          <span>{budgetLabel(interest)}</span>
          <span>{listingExpirationLabel(interest)}</span>
        </div>
      </div>
      <div className="inline-actions">
        <Link className="button button--outline button--sm" href={`/interesses/${interest.id}`}>Detalhes</Link>
        <Button size="icon" variant="danger" onClick={() => window.confirm("Deseja excluir esta procura definitivamente?") && deleteOwnInterest(interest.id)} aria-label="Excluir procura"><Trash2 size={16} /><span className="responsive-action-label">Excluir</span></Button>
      </div>
    </article>
  );
}

export function MyInterestsPage() {
  const { dashboard, currentUser, monetization, isLoadingPrivate } = usePlatform();
  const interests = dashboard?.myInterests ?? [];
  const receivedOffers = dashboard?.receivedOffers ?? [];
  const sentOffers = dashboard?.sentOffers ?? [];
  const creditPurchasesEnabled = Boolean(monetization?.settings?.creditPurchasesEnabled);
  useEffect(() => {
    rememberInterestListHref("/meus-interesses");
  }, []);

  return (
    <div className="dashboard-page">
      <div className="dashboard-heading">
        <div>
          <h1>Olá, {currentUser?.name?.split(" ")[0] ?? "usuário"}</h1>
          <p>Bem-vindo de volta ao seu painel operacional.</p>
        </div>
        <Link className="button button--primary" href="/cadastrar-interesse"><Plus size={16} /> Publicar nova procura</Link>
      </div>
      <div className="stat-grid">
        <StatCard title="Minhas Procuras Ativas" value={interests.length} detail={`${interests.filter((item) => item.boostedUntil).length} com boost ativo`} icon={Search} />
        <StatCard title="Propostas Recebidas" value={receivedOffers.length} detail="Acompanhe respostas das suas procuras" icon={MessageSquare} />
        <StatCard title="Propostas Enviadas" value={sentOffers.length} detail="Negociações que você iniciou" icon={Package} />
      </div>
      <section className="dashboard-section">
        <div className="section-heading"><h2>Minhas procuras</h2>{creditPurchasesEnabled ? <span>{monetization?.sellerCredits ?? currentUser?.credits ?? 0} créditos</span> : null}</div>
        {isLoadingPrivate && !dashboard ? (
          <div className="section-loading" role="status">Carregando suas procuras...</div>
        ) : interests.length ? (
          <div className="manage-list">{interests.map((interest) => <ManageInterestCard key={interest.id} interest={interest} />)}</div>
        ) : (
          <EmptyState title="Nenhuma procura cadastrada" description="Publique uma necessidade para receber propostas de quem pode atender." />
        )}
      </section>
    </div>
  );
}

export function OffersPage({ type }: { type: "sent" | "received" }) {
  const { dashboard, currentUser, isLoadingPrivate, refreshPrivateData, setFeedback } = usePlatform();
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
        <div><h1>{type === "sent" ? "Propostas enviadas" : "Propostas recebidas"}</h1><p>Acompanhe negociações e mensagens pelo chat da plataforma.</p></div>
      </div>
      {isLoadingPrivate && !dashboard ? (
        <div className="section-loading" role="status">Carregando propostas...</div>
      ) : offers.length ? (
        <div className="manage-list">
          {offers.map((offer) => (
            <article className="manage-card" key={offer.id}>
              <div><span className="pill">{offer.status ?? "PENDING"}</span><h3>{offer.interestTitle ?? "Procura"}</h3><p>{offer.message ?? "Sem mensagem adicional."}</p><span>{formatDateTime(offer.createdAt)}</span></div>
              <Button variant="outline" onClick={() => openConversation(offer)}>Abrir conversa</Button>
            </article>
          ))}
        </div>
      ) : <EmptyState title="Nenhuma proposta por aqui" description="Quando houver propostas, elas aparecerão nesta tela." />}
    </div>
  );
}

export function SellerItemsPage() {
  const { sellerItems, saveSellerItem, categories, isLoadingPrivate, setFeedback } = usePlatform();
  const [form, setForm] = useState({ title: "", description: "", category: "", desiredPrice: "", tags: "", referenceImageUrl: "" });
  const [isSavingItem, setIsSavingItem] = useState(false);
  const [selectedSellerItemId, setSelectedSellerItemId] = useState<string | null>(null);
  const matchesRef = useRef<HTMLDivElement | null>(null);
  const selectedSellerItemGroup = useMemo(
    () => sellerItems.find((group) => group.item?.id === selectedSellerItemId) ?? sellerItems[0] ?? null,
    [sellerItems, selectedSellerItemId]
  );
  const selectedItem = selectedSellerItemGroup?.item ?? null;
  const selectedMatches = useMemo(() => {
    const itemCity = String(selectedItem?.location?.city ?? "").trim().toLowerCase();
    const itemState = String(selectedItem?.location?.state ?? "").trim().toLowerCase();
    return Array.from(new Map((selectedSellerItemGroup?.matchingInterests ?? []).map((interest) => [interest.id, interest])).values())
      .sort((left, right) => {
        const leftCity = String(left.location?.city ?? "").trim().toLowerCase();
        const rightCity = String(right.location?.city ?? "").trim().toLowerCase();
        const leftState = String(left.location?.state ?? "").trim().toLowerCase();
        const rightState = String(right.location?.state ?? "").trim().toLowerCase();
        const leftScore = (itemCity && leftCity === itemCity ? 2 : 0) + (itemState && leftState === itemState ? 1 : 0);
        const rightScore = (itemCity && rightCity === itemCity ? 2 : 0) + (itemState && rightState === itemState ? 1 : 0);
        if (rightScore !== leftScore) {
          return rightScore - leftScore;
        }
        return String(left.title ?? "").localeCompare(String(right.title ?? ""), "pt-BR");
      });
  }, [selectedItem?.location?.city, selectedItem?.location?.state, selectedSellerItemGroup]);

  function selectItem(itemId?: string | null, scrollToMatches = false) {
    if (!itemId) {
      return;
    }
    setSelectedSellerItemId(itemId);
    if (scrollToMatches) {
      window.requestAnimationFrame(() => matchesRef.current?.scrollIntoView({ behavior: "smooth", block: "start" }));
    }
  }

  const submit: FormSubmitHandler = async (event) => {
    event.preventDefault();
    setIsSavingItem(true);
    try {
      await saveSellerItem({
        title: form.title,
        description: form.description,
        category: form.category,
        desiredPrice: form.desiredPrice ? Number(form.desiredPrice) : null,
        referenceImageUrl: form.referenceImageUrl || null,
        tags: form.tags.split(",").map((tag) => tag.trim()).filter(Boolean)
      });
      setForm({ title: "", description: "", category: "", desiredPrice: "", tags: "", referenceImageUrl: "" });
    } finally {
      setIsSavingItem(false);
    }
  };

  async function handleItemImageChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    try {
      const referenceImageUrl = await readImageFile(file);
      setForm((current) => ({ ...current, referenceImageUrl }));
    } catch (error) {
      setFeedback({ type: "error", title: "Imagem invalida", message: error instanceof Error ? error.message : "Selecione outra imagem." });
    }
  }

  return (
    <div className="dashboard-page">
      <div className="dashboard-heading"><div><h1>Meus Itens e Compatibilidades</h1><p>Cadastre produtos e serviços para encontrarmos procuras compatíveis automaticamente.</p></div></div>
      <div className="split-dashboard">
        <form className="dashboard-section stack-form" onSubmit={submit}>
          <h2>Cadastrar item disponível</h2>
          <label>Título<input value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} required /></label>
          <label>Categoria<select value={form.category} onChange={(event) => setForm((current) => ({ ...current, category: event.target.value }))} required><option value="">Selecione...</option>{categories.map((category) => <option key={category.value} value={category.value}>{category.label}</option>)}</select></label>
          <label>Descrição<textarea rows={4} value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} required /></label>
          <label>Valor desejado<input type="number" min="0" value={form.desiredPrice} onChange={(event) => setForm((current) => ({ ...current, desiredPrice: event.target.value }))} /></label>
          <label>Tags<input value={form.tags} onChange={(event) => setForm((current) => ({ ...current, tags: event.target.value }))} /></label>
          <label className="upload-box">
            {form.referenceImageUrl ? <img className="upload-preview" src={form.referenceImageUrl} alt="Previa da imagem do item" /> : <Upload size={28} />}
            <strong>{form.referenceImageUrl ? "Trocar imagem do item" : "Imagem do item"}</strong>
            <p>Use uma foto real do produto ou uma referencia visual do servico.</p>
            <span className="button button--outline button--sm">Selecionar imagem</span>
            <input type="file" accept="image/png,image/jpeg,image/webp" onChange={handleItemImageChange} />
          </label>
          <Button type="submit" disabled={isSavingItem}><Plus size={16} /> {isSavingItem ? "Cadastrando..." : "Cadastrar Novo Item"}</Button>
        </form>
        <section className="dashboard-section">
          <h2>Itens cadastrados</h2>
          {isLoadingPrivate && !sellerItems.length ? (
            <div className="section-loading" role="status">Carregando itens cadastrados...</div>
          ) : sellerItems.length ? (
            <div className="seller-item-list">
              {sellerItems.map((group) => {
                const item = group.item;
                const active = item?.id === selectedItem?.id;
                const matchCount = group.matchCount ?? group.matchingInterests?.length ?? 0;
                return (
                  <button type="button" className={`seller-item-button${active ? " is-active" : ""}`} key={item?.id ?? item?.title} onClick={() => selectItem(item?.id)}>
                    <span className="seller-item-thumb">{item?.referenceImageUrl ? <img src={item.referenceImageUrl} alt="" /> : (item?.title ?? item?.name ?? "I").charAt(0)}</span>
                    <span>
                      <strong>{item?.title ?? item?.name ?? "Item cadastrado"}</strong>
                      <small>{categoryLabel(categories, item?.category)} - {item?.active === false ? "Pausado" : "Ativo"}</small>
                    </span>
                    <span className="seller-match-count" onClick={(event) => { event.stopPropagation(); selectItem(item?.id, true); }}>
                      <strong>{matchCount}</strong>
                      <small>possiveis interessados</small>
                    </span>
                  </button>
                );
              })}
            </div>
          ) : <EmptyState title="Nenhum item cadastrado" description="Cadastre o que você tem para negociar." />}
        </section>
      </div>
      <section className="dashboard-section seller-item-detail-panel">
        <div className="section-heading">
          <h2>Detalhes do item</h2>
          {selectedItem ? <span>{selectedMatches.length} possiveis interessados</span> : null}
        </div>
        {selectedItem ? (
          <div className="seller-item-detail">
            <div className="seller-item-media">
              {selectedItem.referenceImageUrl ? <img src={selectedItem.referenceImageUrl} alt={selectedItem.title ?? selectedItem.name ?? "Item"} /> : <Package size={28} />}
            </div>
            <div>
              <span className={`status-pill status-pill--${selectedItem.active === false ? "neutral" : "success"}`}>{selectedItem.active === false ? "Pausado" : "Ativo"}</span>
              <h3>{selectedItem.title ?? selectedItem.name}</h3>
              <p>{selectedItem.description ?? "Sem descricao cadastrada."}</p>
              <div className="interest-meta">
                <span>{categoryLabel(categories, selectedItem.category)}</span>
                <span>{locationLabel({ location: selectedItem.location })}</span>
                <span>{selectedItem.desiredPrice ? selectedItem.desiredPrice.toLocaleString("pt-BR", { style: "currency", currency: "BRL" }) : "Valor a combinar"}</span>
              </div>
              {selectedItem.tags?.length ? <div className="tag-list">{selectedItem.tags.map((tag) => <span key={tag}>{tag}</span>)}</div> : null}
            </div>
          </div>
        ) : <EmptyState title="Selecione um item" description="Clique em um item cadastrado para ver detalhes e interessados compatíveis." />}
      </section>
      <section className="dashboard-section" ref={matchesRef}>
        <div className="section-heading"><h2>Possiveis interessados</h2>{selectedItem ? <span>{selectedItem.title ?? selectedItem.name}</span> : null}</div>
        {selectedMatches.length ? (
          <div className="seller-match-list">
            {selectedMatches.map((interest) => (
              <Link className="seller-match-card" key={interest.id} href={`/interesses/${interest.id}`}>
                {referenceImageSrc(interest) ? <img src={referenceImageSrc(interest)} alt="" /> : <span className="seller-item-thumb"><Search size={18} /></span>}
                <div>
                  <strong>{interest.title}</strong>
                  <p>{interest.description}</p>
                  <div className="interest-meta">
                    <span>{budgetLabel(interest)}</span>
                    <span>{locationLabel(interest)}</span>
                  </div>
                </div>
                <span className="button button--primary button--sm"><Eye size={16} /> Ver procura</span>
              </Link>
            ))}
          </div>
        ) : <EmptyState title="Nenhum interessado compatível ainda" description="Quando alguem procurar algo parecido com este item, a procura aparecerá aqui." />}
      </section>
    </div>
  );
}

export function CreditsPage() {
  const { monetization, buyProduct, cancelPlan, setFeedback, isLoadingPrivate } = usePlatform();
  const products = (monetization?.products ?? []).filter((product) => String(product.type).toUpperCase() !== "BOOST");
  const creditPurchasesEnabled = Boolean(monetization?.settings?.creditPurchasesEnabled);
  const [buyingProductCode, setBuyingProductCode] = useState<string | null>(null);
  async function purchase(productCode: string) {
    setBuyingProductCode(productCode);
    try {
      await buyProduct(productCode);
    } catch (error) {
      setFeedback({ type: "error", title: "Compra indisponível", message: error instanceof Error ? error.message : "Nao foi possivel iniciar o checkout agora." });
    } finally {
      setBuyingProductCode(null);
    }
  }
  if (isLoadingPrivate && !monetization) {
    return <div className="dashboard-page"><div className="section-loading" role="status">Carregando produtos...</div></div>;
  }
  if (!creditPurchasesEnabled) {
    return (
      <div className="dashboard-page">
        <div className="dashboard-heading"><div><h1>Creditos e Plano</h1><p>Compra de creditos e planos esta desativada no CRM operacional.</p></div></div>
        <section className="dashboard-section">
          <EmptyState title="Compra de creditos indisponivel" description="O CRM operacional desativou a compra de creditos e planos no momento." />
        </section>
      </div>
    );
  }
  return (
    <div className="dashboard-page">
      <div className="dashboard-heading"><div><h1>Créditos e Plano</h1><p>Gerencie saldo, Plano Pro e histórico de pagamentos.</p></div></div>
      <div className="stat-grid">
        <StatCard title="Saldo atual" value={monetization?.sellerCredits ?? 0} detail="Créditos disponíveis para propostas" icon={CreditCard} />
        <StatCard title="Plano Pro" value={monetization?.proSubscriptionActive ? "Ativo" : "Inativo"} detail="Envios liberados enquanto ativo" icon={Sparkles} />
      </div>
      <section className="dashboard-section">
        <h2>Produtos disponíveis</h2>
        <div className="product-grid">
          {products.length ? products.map((product) => (
            <article className="product-card" key={product.code}>
              {product.promotional ? <span className="pill">{product.promotionLabel ?? "Promoção"}</span> : null}
              <h3>{product.name}</h3>
              <p>{product.description}</p>
              <strong>{product.price?.toLocaleString("pt-BR", { style: "currency", currency: "BRL" })}</strong>
              <Button className="payment-button" onClick={() => purchase(product.code)} disabled={buyingProductCode === product.code}>
                <Image className="payment-gateway-icon" src={mercadoPagoLogo} alt="" width={54} height={36} aria-hidden="true" />
                <span>{buyingProductCode === product.code ? "Abrindo checkout..." : "Comprar com Mercado Pago"}</span>
              </Button>
            </article>
          )) : <EmptyState title="Compras indisponíveis" description="O CRM operacional ainda não liberou produtos para compra." />}
        </div>
        {monetization?.proSubscriptionActive ? <Button variant="outline" onClick={() => window.confirm("Deseja cancelar seu Plano Pro? O benefício será encerrado imediatamente.") && cancelPlan()}>Cancelar Plano Pro</Button> : null}
      </section>
    </div>
  );
}
