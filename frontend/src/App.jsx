import { useDeferredValue, useEffect, useMemo, useRef, useState } from "react";

import {
  cancelSubscription,
  clearSession,
  boostInterest,
  closeInterest,
  connectChatSocket,
  createInterest,
  createOffer,
  createSellerItem,
  decideInterestModeration,
  deactivateSellerItem,
  deleteModerationRule,
  deleteInterest,
  fetchAdminModeration,
  fetchCategories,
  fetchInterest,
  fetchDashboard,
  fetchInterests,
  fetchMe,
  fetchMonetizationAccount,
  fetchOfferConversation,
  fetchOffers,
  fetchSellerItems,
  forgotPassword,
  getStoredSession,
  login,
  logout,
  purchaseProduct,
  register,
  reportInterest,
  renewInterest,
  resetPassword,
  saveModerationRule,
  sendOfferMessage,
  shareSellerItemOffer,
  syncPayment,
  updateInterest,
  updateSellerItem,
  storeSession,
  verifyEmail
} from "./api";
import logo from "./assets/eu-procuro-logo.png";
import mercadoPagoLogo from "./assets/mercado-pago.svg";
import AuthModal from "./components/AuthModal";
import EmptyState from "./components/EmptyState";
import FeedbackModal from "./components/FeedbackModal";
import Header from "./components/Header";
import InterestCard from "./components/InterestCard";
import NotificationModal from "./components/NotificationModal";
import OfferConversationModal from "./components/OfferConversationModal";
import StatCard from "./components/StatCard";

const initialInterestForm = {
  title: "",
  description: "",
  referenceImageUrl: "",
  category: "SERVICOS",
  budgetMin: "",
  budgetMax: "",
  city: "",
  state: "",
  neighborhood: "",
  desiredRadiusKm: "30",
  acceptsNationwideOffers: true,
  allowsWhatsappContact: false,
  whatsappContact: "",
  boostEnabled: false,
  preferredCondition: "",
  preferredContactMode: "Chat",
  tags: ""
};

const initialOfferForm = {
  offeredPrice: "",
  sellerPhone: "",
  message: "",
  includesDelivery: false,
  highlights: ""
};

const initialSellerItemForm = {
  title: "",
  description: "",
  referenceImageUrl: "",
  category: "SERVICOS",
  desiredPrice: "",
  city: "",
  state: "",
  neighborhood: "",
  tags: ""
};

const initialSellerItemShareForm = {
  sellerPhone: "",
  message: "",
  includesDelivery: false
};

const initialReportForm = {
  reason: "",
  message: ""
};

const initialModerationRuleForm = {
  id: "",
  term: "",
  riskLevel: "HIGH",
  active: true
};

const initialLoginForm = {
  email: "",
  password: ""
};

const initialRegisterForm = {
  name: "",
  email: "",
  documentNumber: "",
  password: "",
  city: "",
  state: "",
  bio: ""
};

const initialForgotForm = {
  email: ""
};

const loggedSections = {
  EXPLORE: "EXPLORE",
  MY_INTERESTS: "MY_INTERESTS",
  SENT_OFFERS: "SENT_OFFERS",
  RECEIVED_OFFERS: "RECEIVED_OFFERS",
  SELLER_ITEMS: "SELLER_ITEMS",
  CREDITS: "CREDITS",
  ADMIN: "ADMIN",
  NEW_INTEREST: "NEW_INTEREST"
};

const MESSAGE_SEEN_STORAGE_KEY = "eu-procuro-message-seen";
const MAX_REFERENCE_IMAGE_SIZE = 1200;
const REFERENCE_IMAGE_QUALITY = 0.78;
const HOME_PAGE_SIZE = 10;
const TITLE_MAX_LENGTH = 80;
const DESCRIPTION_MAX_LENGTH = 120;
const LISTING_EXPIRATION_DAYS = Number(import.meta.env.VITE_LISTING_EXPIRATION_DAYS ?? 30);
const FALLBACK_CATEGORIES = [
  { value: "AUTOMOVEIS", label: "Automóveis" },
  { value: "IMOVEIS", label: "Imóveis" },
  { value: "SERVICOS", label: "Serviços" },
  { value: "ELETRONICOS", label: "Eletrônicos" },
  { value: "INSTRUMENTOS", label: "Instrumentos" },
  { value: "OUTROS", label: "Outros" }
];

function currency(value) {
  if (value === null || value === undefined || value === "") {
    return "A combinar";
  }

  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
  }).format(Number(value));
}

function formatTimestamp(value) {
  if (!value) {
    return "Agora";
  }

  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}

function paymentStatusLabel(status) {
  const labels = {
    APPROVED: "Aprovado",
    PENDING: "Pendente",
    CREATED: "Pendente",
    REJECTED: "Reprovado",
    CANCELLED: "Reprovado"
  };
  return labels[status] ?? "Pendente";
}

function paymentStatusTone(status) {
  if (status === "APPROVED") {
    return "approved";
  }
  if (status === "REJECTED" || status === "CANCELLED") {
    return "rejected";
  }
  return "pending";
}

function paymentMethodLabel(method) {
  const normalized = String(method ?? "").toUpperCase();
  const labels = {
    PIX: "Pix",
    CREDIT_CARD: "Cartão de crédito",
    DEBIT_CARD: "Cartão de débito",
    ACCOUNT_MONEY: "Saldo Mercado Pago",
    TICKET: "Boleto"
  };
  return labels[normalized] ?? (method || "Mercado Pago");
}

function listingExpiresAt(listing) {
  if (listing?.expiresAt) {
    return new Date(listing.expiresAt);
  }

  if (!listing?.createdAt) {
    return null;
  }

  const createdAt = new Date(listing.createdAt);
  if (Number.isNaN(createdAt.getTime())) {
    return null;
  }

  createdAt.setDate(createdAt.getDate() + LISTING_EXPIRATION_DAYS);
  return createdAt;
}

function formatRemainingListingTime(listing) {
  const expiresAt = listingExpiresAt(listing);
  if (!expiresAt) {
    return `Ativo por até ${LISTING_EXPIRATION_DAYS} dias`;
  }

  const diff = expiresAt.getTime() - Date.now();
  if (diff <= 0) {
    return "Expirado";
  }

  const days = Math.ceil(diff / (1000 * 60 * 60 * 24));
  if (days > 1) {
    return `Expira em ${days} dias`;
  }

  const hours = Math.max(1, Math.ceil(diff / (1000 * 60 * 60)));
  return hours > 1 ? `Expira em ${hours} horas` : "Expira em 1 hora";
}

function remainingListingDays(listing) {
  const expiresAt = listingExpiresAt(listing);
  if (!expiresAt) {
    return null;
  }

  return Math.ceil((expiresAt.getTime() - Date.now()) / (1000 * 60 * 60 * 24));
}

function isListingExpiringSoon(listing) {
  const days = remainingListingDays(listing);
  return days !== null && days > 0 && days < 10;
}

function expiryPillClass(listing) {
  return `expiry-pill ${isListingExpiringSoon(listing) ? "expiry-pill--warning" : ""}`;
}

function moderationStatusLabel(status) {
  const labels = {
    PENDING: "Em análise",
    APPROVED: "Aprovado",
    REVIEW_REQUIRED: "Em revisão",
    REJECTED: "Rejeitado",
    HIDDEN: "Oculto",
    REPORTED: "Denunciado",
    OPEN: "Publicado",
    CLOSED: "Desativado"
  };
  return labels[status] ?? "Em análise";
}

function moderationStatusTone(status) {
  const tones = {
    APPROVED: "approved",
    OPEN: "approved",
    PENDING: "pending",
    REVIEW_REQUIRED: "warning",
    REPORTED: "warning",
    REJECTED: "rejected",
    HIDDEN: "rejected",
    CLOSED: "neutral"
  };
  return tones[status] ?? "pending";
}

function limitText(value, maxLength) {
  return String(value ?? "").slice(0, maxLength);
}

function hasLink(value) {
  return /(https?:\/\/|www\.|\b[a-z0-9][a-z0-9-]*(?:\.[a-z0-9][a-z0-9-]*)+\b)/i.test(String(value ?? ""));
}

function firstName(value) {
  return value?.trim().split(/\s+/)[0] ?? "usuário";
}

function createResetStateFromLocation() {
  const params = new URLSearchParams(window.location.search);
  return {
    mode: params.get("mode") === "reset" ? "reset" : "login",
    token: params.get("token") ?? ""
  };
}

function createInitialSharedInterestId() {
  const params = new URLSearchParams(window.location.search);
  return params.get("interest") ?? "";
}

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    if (!file.type.startsWith("image/")) {
      reject(new Error("Selecione uma imagem valida."));
      return;
    }

    const image = new Image();
    const objectUrl = URL.createObjectURL(file);

    image.onload = () => {
      const scale = Math.min(1, MAX_REFERENCE_IMAGE_SIZE / Math.max(image.width, image.height));
      const canvas = document.createElement("canvas");
      canvas.width = Math.max(1, Math.round(image.width * scale));
      canvas.height = Math.max(1, Math.round(image.height * scale));

      const context = canvas.getContext("2d");
      context.drawImage(image, 0, 0, canvas.width, canvas.height);
      URL.revokeObjectURL(objectUrl);
      resolve(canvas.toDataURL("image/jpeg", REFERENCE_IMAGE_QUALITY));
    };

    image.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      reject(new Error("Nao foi possivel ler a imagem."));
    };

    image.src = objectUrl;
  });
}

function byNewest(left, right) {
  return new Date(right.createdAt ?? 0).getTime() - new Date(left.createdAt ?? 0).getTime();
}

function mapInterestToForm(interest) {
  return {
    title: interest?.title ?? "",
    description: interest?.description ?? "",
    referenceImageUrl: interest?.referenceImageUrl ?? "",
    category: interest?.category ?? "SERVICOS",
    budgetMin: interest?.budgetMin ?? "",
    budgetMax: interest?.budgetMax ?? "",
    city: interest?.location?.city ?? "",
    state: interest?.location?.state ?? "",
    neighborhood: interest?.location?.neighborhood ?? "",
    desiredRadiusKm: interest?.desiredRadiusKm ?? "30",
    acceptsNationwideOffers: Boolean(interest?.acceptsNationwideOffers),
    allowsWhatsappContact: Boolean(interest?.allowsWhatsappContact),
    whatsappContact: interest?.whatsappContact ?? "",
    boostEnabled: Boolean(interest?.boostEnabled),
    preferredCondition: interest?.preferredCondition ?? "",
    preferredContactMode: interest?.preferredContactMode ?? "Chat",
    tags: interest?.tags?.join(", ") ?? ""
  };
}

function buildInterestPayload(interestForm) {
  return {
    title: interestForm.title,
    description: interestForm.description,
    referenceImageUrl: interestForm.referenceImageUrl || null,
    category: interestForm.category,
    budgetMin: interestForm.budgetMin || 0,
    budgetMax: interestForm.budgetMax,
    city: interestForm.city,
    state: interestForm.state,
    neighborhood: interestForm.neighborhood,
    desiredRadiusKm: Number(interestForm.desiredRadiusKm || 0),
    acceptsNationwideOffers: interestForm.acceptsNationwideOffers,
    allowsWhatsappContact: interestForm.allowsWhatsappContact,
    whatsappContact: interestForm.allowsWhatsappContact ? interestForm.whatsappContact : null,
    boostEnabled: interestForm.boostEnabled,
    preferredCondition: interestForm.preferredCondition,
    preferredContactMode: interestForm.preferredContactMode,
    tags: interestForm.tags
      .split(",")
      .map((tag) => tag.trim())
      .filter(Boolean)
  };
}

function buildSellerItemPayload(itemForm) {
  return {
    title: itemForm.title,
    description: itemForm.description,
    referenceImageUrl: itemForm.referenceImageUrl || null,
    category: itemForm.category,
    desiredPrice: itemForm.desiredPrice || null,
    city: itemForm.city,
    state: itemForm.state,
    neighborhood: itemForm.neighborhood,
    tags: itemForm.tags
      .split(",")
      .map((tag) => tag.trim())
      .filter(Boolean)
  };
}

function mapSellerItemToForm(groupOrItem) {
  const item = groupOrItem?.item ?? groupOrItem;
  return {
    title: item?.title ?? "",
    description: item?.description ?? "",
    referenceImageUrl: item?.referenceImageUrl ?? "",
    category: item?.category ?? "SERVICOS",
    desiredPrice: item?.desiredPrice ?? "",
    city: item?.location?.city ?? "",
    state: item?.location?.state ?? "",
    neighborhood: item?.location?.neighborhood ?? "",
    tags: item?.tags?.join(", ") ?? ""
  };
}

function isBoostActive(interest) {
  return Boolean(
    interest?.boostEnabled
    && interest?.boostedUntil
    && new Date(interest.boostedUntil).getTime() > Date.now()
  );
}

function BoostRocket() {
  return <span className="boost-rocket" aria-label="Interesse impulsionado" title="Interesse impulsionado">🚀</span>;
}

function WhatsAppIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        d="M7.1 19.4 3.6 20.4 4.6 17A8.7 8.7 0 1 1 7.1 19.4Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <path
        d="M8.6 8.1c.2-.4.4-.4.7-.4h.5c.2 0 .4.1.5.4l.8 1.8c.1.3.1.5-.1.7l-.5.6c.8 1.4 1.9 2.5 3.4 3.2l.6-.7c.2-.2.4-.3.7-.2l1.8.8c.3.1.4.3.4.6v.5c0 .3-.1.6-.4.8-.5.4-1.2.6-1.9.5-3.5-.5-6.3-3.2-6.8-6.7-.1-.7.1-1.4.3-1.9Z"
        fill="currentColor"
      />
    </svg>
  );
}

function XIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        d="M4.8 4.5h4.5l3.5 4.8 4.1-4.8h2.3l-5.3 6.2 5.8 8.8h-4.5l-3.9-5.6-4.8 5.6H4.2l6-7L4.8 4.5Zm3.4 1.8 7.9 11.4h1.3L9.5 6.3H8.2Z"
        fill="currentColor"
      />
    </svg>
  );
}

function LinkIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        d="M9.4 14.6 14.6 9.4"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path
        d="M10.6 7.2 12 5.8a4 4 0 0 1 5.7 5.7l-1.9 1.9a4 4 0 0 1-5.7 0"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path
        d="M13.4 16.8 12 18.2a4 4 0 0 1-5.7-5.7l1.9-1.9a4 4 0 0 1 5.7 0"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  );
}

function readSeenMessages(userId) {
  if (!userId) {
    return {};
  }

  try {
    const rawValue = window.localStorage.getItem(`${MESSAGE_SEEN_STORAGE_KEY}:${userId}`);
    return rawValue ? JSON.parse(rawValue) : {};
  } catch (error) {
    return {};
  }
}

function writeSeenMessages(userId, seenMap) {
  if (!userId) {
    return;
  }

  window.localStorage.setItem(`${MESSAGE_SEEN_STORAGE_KEY}:${userId}`, JSON.stringify(seenMap));
}

function adminReportNotificationId(report) {
  return `admin-report:${report?.id ?? report?.contentId ?? "unknown"}`;
}

function latestIncomingMessageTimestamp(conversation, currentUserId) {
  return (conversation?.messages ?? [])
    .filter((message) => message.senderId !== currentUserId)
    .reduce((latest, message) => {
      const nextValue = new Date(message.createdAt ?? 0).getTime();
      return nextValue > latest ? nextValue : latest;
    }, 0);
}

function useDebouncedValue(value, delayMs) {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => setDebouncedValue(value), delayMs);
    return () => window.clearTimeout(timeoutId);
  }, [value, delayMs]);

  return debouncedValue;
}

function FieldCounter({ value, max }) {
  return (
    <small className="field-counter">
      {String(value ?? "").length}/{max}
    </small>
  );
}

export default function App() {
  const initialResetState = useMemo(() => createResetStateFromLocation(), []);
  const initialSharedInterestId = useMemo(() => createInitialSharedInterestId(), []);
  const sharedInterestIdRef = useRef(initialSharedInterestId);
  const paymentReturnHandledRef = useRef(false);
  const emailVerificationHandledRef = useRef(false);
  const notificationButtonRef = useRef(null);
  const publicRequestSeq = useRef(0);
  const detailRequestSeq = useRef(0);
  const realtimeHandlerRef = useRef(null);
  const myInterestsSectionRef = useRef(null);
  const sentOffersSectionRef = useRef(null);
  const receivedOffersSectionRef = useRef(null);
  const sellerItemsSectionRef = useRef(null);
  const newInterestSectionRef = useRef(null);
  const [session, setSession] = useState(() => getStoredSession());
  const [dashboard, setDashboard] = useState(null);
  const [monetizationAccount, setMonetizationAccount] = useState(null);
  const [categories, setCategories] = useState([]);
  const [interests, setInterests] = useState([]);
  const [selectedInterest, setSelectedInterest] = useState(null);
  const [offers, setOffers] = useState([]);
  const [sellerItems, setSellerItems] = useState([]);
  const [selectedSellerItemId, setSelectedSellerItemId] = useState(null);
  const [showInactiveSellerItems, setShowInactiveSellerItems] = useState(false);
  const [authMode, setAuthMode] = useState(initialResetState.mode);
  const [isAuthModalVisible, setIsAuthModalVisible] = useState(initialResetState.mode === "reset");
  const [loginForm, setLoginForm] = useState(initialLoginForm);
  const [loginInlineError, setLoginInlineError] = useState("");
  const [registerForm, setRegisterForm] = useState(initialRegisterForm);
  const [forgotForm, setForgotForm] = useState(initialForgotForm);
  const [resetForm, setResetForm] = useState({
    token: initialResetState.token,
    newPassword: "",
    confirmPassword: ""
  });
  const [interestForm, setInterestForm] = useState(initialInterestForm);
  const [editingInterestId, setEditingInterestId] = useState(null);
  const [isInterestModalVisible, setIsInterestModalVisible] = useState(false);
  const [offerForm, setOfferForm] = useState(initialOfferForm);
  const [sellerItemForm, setSellerItemForm] = useState(initialSellerItemForm);
  const [editingSellerItemId, setEditingSellerItemId] = useState(null);
  const [isSellerItemModalVisible, setIsSellerItemModalVisible] = useState(false);
  const [sellerItemShareForm, setSellerItemShareForm] = useState(initialSellerItemShareForm);
  const [reportModal, setReportModal] = useState({ visible: false, interest: null, form: initialReportForm, isSubmitting: false });
  const [adminModeration, setAdminModeration] = useState(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [moderationRuleForm, setModerationRuleForm] = useState(initialModerationRuleForm);
  const [isSubmittingModerationRule, setIsSubmittingModerationRule] = useState(false);
  const [isModerationActionLoading, setIsModerationActionLoading] = useState(false);
  const [expandedInterests, setExpandedInterests] = useState({});
  const [expandedOffers, setExpandedOffers] = useState({});
  const [filters, setFilters] = useState({
    query: "",
    category: "",
    city: "",
    maxBudget: ""
  });
  const [homeMatchFilter, setHomeMatchFilter] = useState(null);
  const [homeOffset, setHomeOffset] = useState(0);
  const [hasMoreInterests, setHasMoreInterests] = useState(false);
  const [loggedSection, setLoggedSection] = useState(loggedSections.EXPLORE);
  const [passwordRecoveryPreview, setPasswordRecoveryPreview] = useState(null);
  const [isLoadingPublic, setIsLoadingPublic] = useState(true);
  const [isLoadingMorePublic, setIsLoadingMorePublic] = useState(false);
  const [isLoadingInterestDetail, setIsLoadingInterestDetail] = useState(false);
  const [isLoadingPrivate, setIsLoadingPrivate] = useState(Boolean(getStoredSession()));
  const [isSubmittingAuth, setIsSubmittingAuth] = useState(false);
  const [isSubmittingInterest, setIsSubmittingInterest] = useState(false);
  const [isSubmittingOffer, setIsSubmittingOffer] = useState(false);
  const [isSubmittingSellerItem, setIsSubmittingSellerItem] = useState(false);
  const [sharingSellerItemInterestId, setSharingSellerItemInterestId] = useState(null);
  const [isProcessingPurchase, setIsProcessingPurchase] = useState(false);
  const [isPaymentReturnLoading, setIsPaymentReturnLoading] = useState(false);
  const [paymentStatus, setPaymentStatus] = useState(null);
  const [selectedPurchaseProductCode, setSelectedPurchaseProductCode] = useState(null);
  const [feedbackModal, setFeedbackModal] = useState(null);
  const [hasUnreadMessages, setHasUnreadMessages] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [isNotificationModalVisible, setIsNotificationModalVisible] = useState(false);
  const [notificationAnchorStyle, setNotificationAnchorStyle] = useState(null);
  const [messageSyncKey, setMessageSyncKey] = useState(0);
  const [conversationModal, setConversationModal] = useState({
    visible: false,
    isLoading: false,
    isSending: false,
    draftMessage: "",
    data: null
  });

  const debouncedQuery = useDebouncedValue(filters.query, 350);
  const deferredQuery = useDeferredValue(debouncedQuery);
  const currentUser = session?.user ?? null;
  const myInterests = useMemo(
    () => (dashboard?.myInterests ?? [])
      .filter((interest) => !["CLOSED", "HIDDEN"].includes(interest.status))
      .slice()
      .sort(byNewest),
    [dashboard?.myInterests]
  );
  const sentOffers = useMemo(() => (dashboard?.offersSent ?? []).slice().sort(byNewest), [dashboard?.offersSent]);
  const receivedOffers = useMemo(() => (dashboard?.offersReceived ?? []).slice().sort(byNewest), [dashboard?.offersReceived]);
  const creditProducts = useMemo(
    () => (monetizationAccount?.products ?? []).filter((product) => product.type === "CREDIT_PACK"),
    [monetizationAccount?.products]
  );
  const subscriptionProducts = useMemo(
    () => (monetizationAccount?.products ?? []).filter((product) => product.type === "SUBSCRIPTION"),
    [monetizationAccount?.products]
  );
  const purchaseProducts = useMemo(
    () => [...creditProducts, ...subscriptionProducts],
    [creditProducts, subscriptionProducts]
  );
  const selectedPurchaseProduct = useMemo(
    () => purchaseProducts.find((product) => product.code === selectedPurchaseProductCode) ?? purchaseProducts[0] ?? null,
    [purchaseProducts, selectedPurchaseProductCode]
  );
  const boostProducts = useMemo(
    () => (monetizationAccount?.products ?? []).filter((product) => product.type === "BOOST"),
    [monetizationAccount?.products]
  );
  const visibleHomeInterests = useMemo(
    () => {
      const source = homeMatchFilter?.matchingInterests ?? interests;
      return source.filter((interest) => !currentUser?.id || interest.ownerId !== currentUser.id);
    },
    [homeMatchFilter, interests, currentUser?.id]
  );
  const selectedSellerItemGroup = useMemo(
    () => sellerItems.find((group) => group.item?.id === selectedSellerItemId) ?? sellerItems[0] ?? null,
    [sellerItems, selectedSellerItemId]
  );
  const isSelectedInterestMine = selectedInterest?.ownerId === currentUser?.id;
  const sentOfferForSelectedInterest = useMemo(
    () => sentOffers.find((offer) => offer.interestPostId === selectedInterest?.id) ?? null,
    [sentOffers, selectedInterest?.id]
  );
  const canSendOffer = Boolean(monetizationAccount?.subscriptionActive || (monetizationAccount?.sellerCredits ?? 0) > 0);
  const noCreditsTooltip = "Você não possui créditos ativos para enviar ofertas.";
  const unreadAdminReportCount = useMemo(() => {
    if (!isAdmin || !currentUser?.id) {
      return 0;
    }

    const seenMap = readSeenMessages(currentUser.id);
    return (adminModeration?.openReports ?? []).filter((report) => {
      const createdAt = new Date(report.createdAt ?? 0).getTime();
      return createdAt > Number(seenMap[adminReportNotificationId(report)] ?? 0);
    }).length;
  }, [isAdmin, currentUser?.id, adminModeration?.openReports, messageSyncKey]);

  function openFeedback(type, title, message) {
    setFeedbackModal({ type, title, message });
  }

  function markAdminReportsSeen() {
    if (!currentUser?.id) {
      return;
    }

    const openReports = adminModeration?.openReports ?? [];
    if (!openReports.length) {
      return;
    }

    const seenMap = readSeenMessages(currentUser.id);
    const nextSeenMap = openReports.reduce((accumulator, report) => ({
      ...accumulator,
      [adminReportNotificationId(report)]: new Date(report.createdAt ?? Date.now()).getTime()
    }), seenMap);

    writeSeenMessages(currentUser.id, nextSeenMap);
    setMessageSyncKey((current) => current + 1);
  }

  function updateInterestUrl(interestId, replace = false) {
    const url = new URL(window.location.href);

    if (interestId) {
      url.searchParams.delete("mode");
      url.searchParams.delete("token");
      url.searchParams.set("interest", interestId);
    } else {
      url.searchParams.delete("interest");
    }

    sharedInterestIdRef.current = interestId ?? "";
    const nextUrl = `${url.pathname}${url.search}${url.hash}`;
    window.history[replace ? "replaceState" : "pushState"]({}, "", nextUrl);
  }

  async function loadInterestDetail(interestId, options = {}) {
    if (!interestId) {
      setSelectedInterest(null);
      return;
    }

    const requestId = detailRequestSeq.current + 1;
    detailRequestSeq.current = requestId;
    const shouldUpdateUrl = options.updateUrl !== false;
    const preservedScrollY = options.preserveScroll ? window.scrollY : null;
    const restoreScroll = () => {
      if (preservedScrollY === null) {
        return;
      }
      window.requestAnimationFrame(() => window.scrollTo({ top: preservedScrollY, left: 0, behavior: "auto" }));
    };

    if (options.summary) {
      setSelectedInterest(options.summary);
      restoreScroll();
    }

    if (shouldUpdateUrl) {
      updateInterestUrl(interestId, Boolean(options.replace));
    }

    setIsLoadingInterestDetail(true);

    try {
      const interest = await fetchInterest(interestId);
      if (requestId !== detailRequestSeq.current) {
        return;
      }

      setSelectedInterest(interest);
      setInterests((current) => (
        current.some((item) => item.id === interest.id)
          ? current.map((item) => (item.id === interest.id ? { ...item, ...interest } : item))
          : [interest, ...current]
      ));
      restoreScroll();
    } catch (requestError) {
      if (requestId === detailRequestSeq.current) {
        setSelectedInterest(null);
        openFeedback("error", "Falha ao abrir anúncio", requestError.message || "Tente novamente.");
      }
    } finally {
      if (requestId === detailRequestSeq.current) {
        setIsLoadingInterestDetail(false);
      }
    }
  }

  function selectPublicInterest(interest, options = {}) {
    setSelectedInterest(interest);
    setLoggedSection(loggedSections.EXPLORE);
    loadInterestDetail(interest.id, {
      summary: interest,
      replace: Boolean(options.replace),
      preserveScroll: true
    }).catch(() => {});
  }

  function buildInterestShareUrl(interest) {
    const url = new URL(`${window.location.origin}${window.location.pathname}`);
    url.searchParams.set("interest", interest.id);
    return url.toString();
  }

  function buildInterestShareText(interest) {
    return `Olha esse interesse no Eu Procuro: ${interest.title} - ${buildInterestShareUrl(interest)}`;
  }

  function buildWhatsAppShareUrl(interest) {
    return `https://wa.me/?text=${encodeURIComponent(buildInterestShareText(interest))}`;
  }

  function buildXShareUrl(interest) {
    const url = new URL("https://twitter.com/intent/tweet");
    url.searchParams.set("text", `Olha esse interesse no Eu Procuro: ${interest.title}`);
    url.searchParams.set("url", buildInterestShareUrl(interest));
    return url.toString();
  }

  async function copyInterestLinkToClipboard(interest) {
    const url = buildInterestShareUrl(interest);

    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(url);
      return;
    }

    window.prompt("Copie o link do interesse:", url);
  }

  async function handleCopyInterestLink(interest) {
    try {
      await copyInterestLinkToClipboard(interest);
      openFeedback("success", "Link copiado", "Agora você pode compartilhar esse interesse.");
    } catch (error) {
      window.prompt("Copie o link do interesse:", buildInterestShareUrl(interest));
    }
  }

  function renderInterestShareActions(interest) {
    if (!interest?.id) {
      return null;
    }

    if (!["APPROVED", "OPEN"].includes(selectedInterest?.status)) {
      return null;
    }

    return (
      <div className="share-card">
        <div>
          <span className="eyebrow">Compartilhar</span>
          <strong>Envie este interesse para alguém</strong>
        </div>
        <div className="share-card__actions">
          <a
            className="share-button share-button--whatsapp"
            href={buildWhatsAppShareUrl(interest)}
            target="_blank"
            rel="noreferrer"
            aria-label="Compartilhar no WhatsApp"
          >
            <WhatsAppIcon />
          </a>
          <a
            className="share-button share-button--x"
            href={buildXShareUrl(interest)}
            target="_blank"
            rel="noreferrer"
            aria-label="Compartilhar no X"
          >
            <XIcon />
          </a>
          <button
            type="button"
            className="share-button share-button--link"
            onClick={() => handleCopyInterestLink(interest)}
          >
            <LinkIcon />
            Copiar link
          </button>
        </div>
      </div>
    );
  }

  function openAuthModal(mode) {
    setAuthMode(mode);
    setIsAuthModalVisible(true);
  }

  function updateHomeFilters(updater) {
    setHomeMatchFilter(null);
    updateInterestUrl(null, true);
    setHomeOffset(0);
    setHasMoreInterests(false);
    setFilters(updater);
  }

  function clearHomeMatchFilter() {
    setHomeMatchFilter(null);
    updateInterestUrl(null, true);
    const nextVisibleInterests = interests.filter((interest) => !currentUser?.id || interest.ownerId !== currentUser.id);
    setSelectedInterest((current) =>
      nextVisibleInterests.find((interest) => interest.id === current?.id) ?? null
    );
  }

  function openSellerItemMatches(group) {
    const item = group?.item;
    if (!item?.id) {
      return;
    }

    const matchingInterests = (group.matchingInterests ?? [])
      .filter((interest) => !currentUser?.id || interest.ownerId !== currentUser.id);

    setSelectedSellerItemId(item.id);
    setHomeMatchFilter({
      sellerItemId: item.id,
      sellerItemTitle: item.title ?? "item cadastrado",
      matchingInterests
    });
    setFilters({
      query: "",
      category: "",
      city: "",
      maxBudget: ""
    });
    setSelectedInterest(matchingInterests[0] ?? null);
    navigateTo(loggedSections.EXPLORE);
  }

  function navigateTo(section) {
    setLoggedSection(section);

    if (section === loggedSections.EXPLORE) {
      setSelectedInterest((current) =>
        visibleHomeInterests.find((interest) => interest.id === current?.id) ?? current ?? null
      );
    }

    if (section === loggedSections.MY_INTERESTS) {
      setSelectedInterest((current) => myInterests.find((interest) => interest.id === current?.id) ?? myInterests[0] ?? null);
    }

    if (section === loggedSections.NEW_INTEREST) {
      window.requestAnimationFrame(() => {
        newInterestSectionRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "start"
        });
      });
    }

    if (section === loggedSections.MY_INTERESTS) {
      window.requestAnimationFrame(() => {
        myInterestsSectionRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "start"
        });
      });
    }

    if (section === loggedSections.SENT_OFFERS) {
      window.requestAnimationFrame(() => {
        sentOffersSectionRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "start"
        });
      });
    }

    if (section === loggedSections.RECEIVED_OFFERS) {
      window.requestAnimationFrame(() => {
        receivedOffersSectionRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "start"
        });
      });
    }

    if (section === loggedSections.SELLER_ITEMS) {
      window.requestAnimationFrame(() => {
        sellerItemsSectionRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "start"
        });
      });
    }
  }

  function openNewInterestForm() {
    setEditingInterestId(null);
    setInterestForm(initialInterestForm);
    setIsInterestModalVisible(true);
  }

  function toggleInterestExpansion(interest) {
    setSelectedInterest(interest);
    setExpandedInterests((current) => ({
      ...current,
      [interest.id]: !current[interest.id]
    }));
  }

  function toggleOfferExpansion(offerId) {
    setExpandedOffers((current) => ({
      ...current,
      [offerId]: !current[offerId]
    }));
  }

  function startEditingInterest(interest) {
    setEditingInterestId(interest.id);
    setInterestForm(mapInterestToForm(interest));
    setIsInterestModalVisible(true);
  }

  function cancelInterestEditing() {
    setEditingInterestId(null);
    setInterestForm(initialInterestForm);
    setIsInterestModalVisible(false);
  }

  function startEditingSellerItem(group) {
    const item = group?.item ?? group;
    if (!item?.id) {
      return;
    }

    setEditingSellerItemId(item.id);
    setSellerItemForm(mapSellerItemToForm(item));
    setSelectedSellerItemId(item.id);
    setIsSellerItemModalVisible(true);
  }

  function cancelSellerItemEditing() {
    setEditingSellerItemId(null);
    setSellerItemForm(initialSellerItemForm);
    setIsSellerItemModalVisible(false);
  }

  function closeAuthModal() {
    setIsAuthModalVisible(false);
    setPasswordRecoveryPreview(null);
    if (authMode === "reset") {
      setAuthMode("login");
    }
  }

  async function refreshPublicData(nextFilters = filters, preserveSelection = true, options = {}) {
    const requestId = publicRequestSeq.current + 1;
    publicRequestSeq.current = requestId;
    const append = Boolean(options.append);
    const offset = append ? homeOffset : 0;

    if (append) {
      setIsLoadingMorePublic(true);
    } else {
      setIsLoadingPublic(true);
    }

    try {
      const interestData = await fetchInterests({
        query: nextFilters.query,
        category: nextFilters.category,
        city: nextFilters.city,
        maxBudget: nextFilters.maxBudget || undefined,
        offset,
        limit: HOME_PAGE_SIZE + 1
      });

      if (requestId !== publicRequestSeq.current) {
        return;
      }

      const pageInterests = interestData.slice(0, HOME_PAGE_SIZE);
      setHasMoreInterests(interestData.length > HOME_PAGE_SIZE);
      setHomeOffset(offset + pageInterests.length);
      setInterests((current) => {
        if (!append) {
          return pageInterests;
        }

        const currentIds = new Set(current.map((interest) => interest.id));
        return [
          ...current,
          ...pageInterests.filter((interest) => !currentIds.has(interest.id))
        ];
      });

      if (!append && !sharedInterestIdRef.current && pageInterests[0]?.id) {
        loadInterestDetail(pageInterests[0].id, {
          summary: pageInterests[0],
          updateUrl: false
        }).catch(() => {});
      }

      if (append) {
        return;
      }

      setSelectedInterest((currentSelected) => {
        if (preserveSelection && currentSelected) {
          const refreshedSelection = pageInterests.find((interest) => interest.id === currentSelected.id);
          if (refreshedSelection) {
            return refreshedSelection;
          }

          if (sharedInterestIdRef.current === currentSelected.id) {
            return currentSelected;
          }

          return null;
        }

        return sharedInterestIdRef.current
          ? pageInterests.find((interest) => interest.id === sharedInterestIdRef.current) ?? currentSelected ?? null
          : null;
      });
    } catch (requestError) {
      if (requestId === publicRequestSeq.current) {
        openFeedback("error", "Falha ao carregar", requestError.message || "Não foi possível carregar a plataforma.");
      }
    } finally {
      if (requestId === publicRequestSeq.current) {
        setIsLoadingPublic(false);
        setIsLoadingMorePublic(false);
      }
    }
  }

  function handleLoadMoreInterests() {
    refreshPublicData({
      query: deferredQuery,
      category: filters.category,
      city: filters.city,
      maxBudget: filters.maxBudget
    }, true, { append: true }).catch(() => {});
  }

  async function refreshCategories() {
    try {
      const loadedCategories = await fetchCategories();
      setCategories(loadedCategories?.length ? loadedCategories : FALLBACK_CATEGORIES);
    } catch (requestError) {
      setCategories(FALLBACK_CATEGORIES);
      openFeedback("error", "Falha ao carregar categorias", requestError.message || "Tente novamente.");
    }
  }

  async function refreshPrivateData(options = {}) {
    if (!session) {
      return;
    }

    if (!session.token && !session.user?.id) {
      clearSession();
      setSession(null);
      setIsLoadingPrivate(false);
      return;
    }

    const silent = Boolean(options.silent);
    if (!silent) {
      setIsLoadingPrivate(true);
    }

    try {
      const [me, dashboardData, monetizationData, sellerItemData] = await Promise.all([
        fetchMe(),
        fetchDashboard(),
        fetchMonetizationAccount(),
        fetchSellerItems({ includeInactive: showInactiveSellerItems })
      ]);
      const nextSession = buildSessionFromMeResponse(me, session);

      setSession(nextSession);
      storeSession(nextSession);
      setDashboard(dashboardData);
      setMonetizationAccount(monetizationData);
      setSellerItems(sellerItemData);
      setSelectedSellerItemId((current) =>
        sellerItemData.some((group) => group.item?.id === current) ? current : sellerItemData[0]?.item?.id ?? null
      );
      try {
        const moderationData = await fetchAdminModeration();
        setAdminModeration(moderationData);
        setIsAdmin(true);
      } catch {
        setAdminModeration(null);
        setIsAdmin(false);
      }
    } catch (requestError) {
      clearSession();
      setSession(null);
      setDashboard(null);
      setMonetizationAccount(null);
      setSellerItems([]);
      setAdminModeration(null);
      setIsAdmin(false);
      setLoggedSection(loggedSections.EXPLORE);
      openFeedback("error", "Sessão encerrada", requestError.message || "Entre novamente para continuar.");
    } finally {
      if (!silent) {
        setIsLoadingPrivate(false);
      }
    }
  }

  function handleRealtimeEvent(envelope) {
    if (envelope?.type === "offer.created") {
      refreshPrivateData({ silent: true }).then(() => {
        setMessageSyncKey((current) => current + 1);
      }).catch(() => {});
      return;
    }

    if (envelope?.type === "interest.moderation.updated") {
      refreshPrivateData({ silent: true }).then(() => {
        setMessageSyncKey((current) => current + 1);
      }).catch(() => {});

      if (envelope.payload?.status === "REJECTED") {
        openFeedback(
          "error",
          "Anúncio recusado pela moderação",
          envelope.payload?.reason || "Seu anúncio foi recusado. Você pode editar e enviar novamente para análise."
        );
      }
      return;
    }

    if (envelope?.type !== "conversation-message.created" || !envelope.payload) {
      return;
    }

    const message = envelope.payload;
    const isOpenConversation = conversationModal.visible && conversationModal.data?.offerId === message.offerId;
    if (isOpenConversation && currentUser?.id && message.senderId !== currentUser.id) {
      const seenMap = readSeenMessages(currentUser.id);
      writeSeenMessages(currentUser.id, {
        ...seenMap,
        [message.offerId]: new Date(message.createdAt ?? Date.now()).getTime(),
        [`offer:${message.offerId}`]: new Date(message.createdAt ?? Date.now()).getTime()
      });
    }

    setConversationModal((current) => {
      if (!current.visible || current.data?.offerId !== message.offerId) {
        return current;
      }

      const currentMessages = current.data?.messages ?? [];
      if (currentMessages.some((currentMessage) => currentMessage.id === message.id)) {
        return current;
      }

      return {
        ...current,
        data: {
          ...current.data,
          messages: [...currentMessages, message]
        }
      };
    });

    setMessageSyncKey((current) => current + 1);
    refreshPrivateData({ silent: true }).catch(() => {});
  }

  realtimeHandlerRef.current = handleRealtimeEvent;

  useEffect(() => {
    refreshCategories();
  }, []);

  useEffect(() => {
    if (session) {
      return;
    }

    let isCancelled = false;

    fetchMe()
      .then((me) => {
        if (isCancelled) {
          return;
        }

        const nextSession = buildSessionFromMeResponse(me, getStoredSession());

        storeSession(nextSession);
        setSession(nextSession);
      })
      .catch(() => {
        if (!isCancelled) {
          setIsLoadingPrivate(false);
        }
      });

    return () => {
      isCancelled = true;
    };
  }, []);

  useEffect(() => {
    refreshPublicData({
      query: deferredQuery,
      category: filters.category,
      city: filters.city,
      maxBudget: filters.maxBudget
    }).catch(() => {});
  }, [deferredQuery, filters.category, filters.city, filters.maxBudget]);

  useEffect(() => {
    if (!sharedInterestIdRef.current) {
      return undefined;
    }

    setHomeMatchFilter(null);
    setLoggedSection(loggedSections.EXPLORE);
    loadInterestDetail(sharedInterestIdRef.current, { replace: true }).catch(() => {});
    return undefined;
  }, []);

  useEffect(() => {
    if (emailVerificationHandledRef.current) {
      return;
    }

    const url = new URL(window.location.href);
    const mode = url.searchParams.get("mode");
    const token = url.searchParams.get("token");

    if (mode !== "verify-email" || !token) {
      return;
    }

    emailVerificationHandledRef.current = true;

    verifyEmail(token)
      .then((response) => {
        openFeedback(
          "success",
          "E-mail verificado",
          response?.message ?? "Seu e-mail foi verificado com sucesso."
        );

        if (!session) {
          return null;
        }

        return fetchMe()
          .then((me) => {
            const nextSession = buildSessionFromMeResponse(me, session);
            storeSession(nextSession);
            setSession(nextSession);
            return null;
          });
      })
      .catch((requestError) => {
        openFeedback(
          "error",
          "Não foi possível verificar",
          requestError.message || "O link de verificação pode ter expirado."
        );
      })
      .finally(() => {
        url.searchParams.delete("mode");
        url.searchParams.delete("token");
        window.history.replaceState({}, "", `${url.pathname}${url.search}${url.hash}`);
      });
  }, [session?.token]);

  useEffect(() => {
    if (!session) {
      setIsLoadingPrivate(false);
      setDashboard(null);
      setSellerItems([]);
      return;
    }

    refreshPrivateData();
  }, [session?.user?.id, showInactiveSellerItems]);

  useEffect(() => {
    if (!session || paymentReturnHandledRef.current) {
      return;
    }

    const url = new URL(window.location.href);
    const paymentId = url.searchParams.get("payment_id") || url.searchParams.get("collection_id");
    const paymentResult = url.searchParams.get("payment");

    if (!paymentId) {
      return;
    }

    paymentReturnHandledRef.current = true;
    setIsPaymentReturnLoading(true);
    navigateTo(loggedSections.CREDITS);
    setPaymentStatus((current) => ({
      ...(current ?? {}),
      step: paymentResult === "failure" ? "FAILED" : "PAYMENT",
      message: "Sincronizando pagamento com o Mercado Pago..."
    }));

    syncPayment({ paymentId })
      .then(() => refreshPrivateData({ silent: true }))
      .then(() => {
        setPaymentStatus((current) => ({
          ...(current ?? {}),
          step: paymentResult === "failure" ? "FAILED" : "COMPLETED",
          message: paymentResult === "failure"
            ? "O Mercado Pago retornou uma tentativa recusada."
            : "Pagamento sincronizado. Se aprovado, seus créditos já foram liberados."
        }));
        if (paymentResult === "failure") {
          openFeedback("error", "Pagamento recusado", "O Mercado Pago retornou uma tentativa recusada.");
        } else {
          openFeedback("success", "Pagamento sincronizado", "Atualizamos seu saldo com o status retornado pelo Mercado Pago.");
        }
      })
      .catch((requestError) => {
        setPaymentStatus((current) => ({
          ...(current ?? {}),
          step: "PAYMENT",
          message: requestError.message || "Pagamento recebido, mas ainda pendente de confirmação."
        }));
        openFeedback("error", "Pagamento pendente", requestError.message || "Ainda não foi possível confirmar o pagamento.");
      })
      .finally(() => {
        [
          "payment",
          "payment_id",
          "collection_id",
          "collection_status",
          "status",
          "external_reference",
          "payment_type",
          "merchant_order_id",
          "preference_id",
          "site_id",
          "processing_mode",
          "merchant_account_id"
        ].forEach((param) => {
          url.searchParams.delete(param);
        });
        window.history.replaceState({}, "", `${url.pathname}${url.search}${url.hash}`);
        setIsPaymentReturnLoading(false);
      });
  }, [session?.token]);

  useEffect(() => {
    if (!currentUser?.id) {
      return undefined;
    }

    let socket = null;
    let reconnectTimeoutId = null;
    let closedByEffect = false;

    const connect = () => {
      socket = connectChatSocket({
        token: session.token,
        onMessage: (envelope) => realtimeHandlerRef.current?.(envelope),
        onClose: () => {
          if (!closedByEffect) {
            reconnectTimeoutId = window.setTimeout(connect, 3000);
          }
        }
      });
    };

    connect();

    return () => {
      closedByEffect = true;
      if (reconnectTimeoutId) {
        window.clearTimeout(reconnectTimeoutId);
      }
      socket?.close();
    };
  }, [session?.token, currentUser?.id]);

  useEffect(() => {
    if (!session || !selectedInterest || !isSelectedInterestMine) {
      setOffers([]);
      return;
    }

    fetchOffers(selectedInterest.id)
      .then(setOffers)
      .catch((requestError) => {
        setOffers([]);
        openFeedback("error", "Não foi possível carregar ofertas", requestError.message || "Tente novamente.");
      });
  }, [session, selectedInterest?.id, isSelectedInterestMine]);

  useEffect(() => {
    if (loggedSection !== loggedSections.MY_INTERESTS || myInterests.length === 0) {
      return;
    }

    const currentSelectedId = selectedInterest?.id;
    const hasCurrentSelection = myInterests.some((interest) => interest.id === currentSelectedId);
    if (!hasCurrentSelection) {
      setSelectedInterest(myInterests[0]);
    }
  }, [loggedSection, myInterests, selectedInterest?.id]);

  useEffect(() => {
    if (sellerItems.length === 0) {
      setSelectedSellerItemId(null);
      return;
    }

    setSelectedSellerItemId((current) =>
      sellerItems.some((group) => group.item?.id === current) ? current : sellerItems[0].item?.id ?? null
    );
  }, [sellerItems]);

  useEffect(() => {
    if (!selectedInterest?.id) {
      return;
    }

    setExpandedInterests((current) => ({
      ...current,
      [selectedInterest.id]: true
    }));
  }, [selectedInterest?.id]);

  useEffect(() => {
    if (loggedSection !== loggedSections.EXPLORE) {
      return;
    }

    setSelectedInterest((current) => {
      if (current && visibleHomeInterests.some((interest) => interest.id === current.id)) {
        return current;
      }

      if (current && sharedInterestIdRef.current === current.id) {
        return current;
      }

      return null;
    });
  }, [loggedSection, visibleHomeInterests]);

  useEffect(() => {
    if (!session || !currentUser?.id) {
      setHasUnreadMessages(false);
      setNotifications([]);
      return;
    }

    const receivedIds = new Set(receivedOffers.map((offer) => offer.id));
    const seenMap = readSeenMessages(currentUser.id);
    const newOfferEntries = receivedOffers
      .map((offer) => {
        const createdAt = new Date(offer.createdAt ?? 0).getTime();
        const notificationId = `new-offer:${offer.id}`;
        const lastSeen = Number(seenMap[notificationId] ?? 0);
        if (!createdAt || createdAt <= lastSeen) {
          return null;
        }

        return {
          id: notificationId,
          type: "new-offer",
          offerId: offer.id,
          section: loggedSections.RECEIVED_OFFERS,
          title: offer.interestTitle ?? "Nova oferta recebida",
          message: `${offer.sellerName ?? "Um vendedor"} enviou uma proposta: ${offer.message ?? "sem descrição."}`,
          createdAt: offer.createdAt
        };
      })
      .filter(Boolean);

    const unreadMessageEntries = [...receivedOffers, ...sentOffers]
      .map((offer) => {
        if (!offer.latestMessageAt || offer.latestMessageSenderId === currentUser.id) {
          return null;
        }

        const latestIncoming = new Date(offer.latestMessageAt).getTime();
        const notificationId = `offer:${offer.id}`;
        const lastSeen = Number(seenMap[notificationId] ?? seenMap[offer.id] ?? 0);
        if (latestIncoming <= lastSeen) {
          return null;
        }

        return {
          id: notificationId,
          type: "message",
          offerId: offer.id,
          section: receivedIds.has(offer.id) ? loggedSections.RECEIVED_OFFERS : loggedSections.SENT_OFFERS,
          title: offer.interestTitle ?? "Nova mensagem",
          message: offer.latestMessage ?? "Você recebeu uma nova mensagem.",
          createdAt: offer.latestMessageAt
        };
      })
      .filter(Boolean);

    const sellerItemEntries = sellerItems
      .filter((group) => group.item?.active)
      .map((group) => {
        const matches = group.matchingInterests ?? [];
        if (!group.item?.id || matches.length === 0) {
          return null;
        }

        const latestMatchTime = matches.reduce((latest, interest) => {
          const nextTime = new Date(interest.createdAt ?? 0).getTime();
          return nextTime > latest ? nextTime : latest;
        }, 0);
        const notificationId = `seller-item:${group.item.id}:${group.matchCount}:${latestMatchTime}`;
        if (seenMap[notificationId]) {
          return null;
        }

        return {
          id: notificationId,
          type: "seller-item-match",
          sellerItemId: group.item.id,
          section: loggedSections.SELLER_ITEMS,
          title: group.item.title ?? "Item parecido",
          message: "Existem pessoas procurando um item parecido com o seu.",
          createdAt: latestMatchTime ? new Date(latestMatchTime).toISOString() : new Date().toISOString()
        };
      })
      .filter(Boolean);

    const expiringInterestEntries = myInterests
      .filter((interest) => ["OPEN", "APPROVED"].includes(interest.status) && isListingExpiringSoon(interest))
      .map((interest) => {
        const expiresAt = listingExpiresAt(interest);
        const notificationId = `interest-expiring:${interest.id}:${expiresAt?.toISOString() ?? "unknown"}`;
        if (seenMap[notificationId]) {
          return null;
        }

        return {
          id: notificationId,
          type: "interest-expiring",
          interestId: interest.id,
          section: loggedSections.MY_INTERESTS,
          title: interest.title ?? "Anúncio perto de expirar",
          message: `Seu anúncio expira em breve. ${formatRemainingListingTime(interest)}.`,
          createdAt: expiresAt?.toISOString() ?? new Date().toISOString()
        };
      })
      .filter(Boolean);

    const moderationEntries = myInterests
      .filter((interest) => ["REJECTED", "REVIEW_REQUIRED", "REPORTED"].includes(interest.status))
      .map((interest) => {
        const notificationId = `interest-moderation:${interest.id}:${interest.status}:${interest.updatedAt ?? ""}`;
        if (seenMap[notificationId]) {
          return null;
        }

        const rejected = interest.status === "REJECTED";
        return {
          id: notificationId,
          type: "interest-moderation",
          interestId: interest.id,
          section: loggedSections.MY_INTERESTS,
          title: rejected ? "Anúncio rejeitado" : "Anúncio em análise",
          message: rejected
            ? "Seu anúncio foi rejeitado. Você pode editar e enviar novamente para análise ou excluir."
            : (interest.moderation?.reason ?? "Seu anúncio está aguardando revisão."),
          createdAt: interest.updatedAt ?? new Date().toISOString()
        };
      })
      .filter(Boolean);

    const adminReportEntries = isAdmin
      ? (adminModeration?.openReports ?? [])
        .map((report) => {
          const notificationId = adminReportNotificationId(report);
          const createdAt = new Date(report.createdAt ?? 0).getTime();
          if (!createdAt || createdAt <= Number(seenMap[notificationId] ?? 0)) {
            return null;
          }

          return {
            id: notificationId,
            type: "admin-report",
            reportId: report.id,
            section: loggedSections.ADMIN,
            title: "Nova denúncia recebida",
            message: report.reason ?? "Um usuário denunciou um anúncio para revisão.",
            createdAt: report.createdAt
          };
        })
        .filter(Boolean)
      : [];

    const unreadEntries = [
      ...adminReportEntries,
      ...moderationEntries,
      ...expiringInterestEntries,
      ...newOfferEntries,
      ...unreadMessageEntries,
      ...sellerItemEntries
    ]
      .sort((left, right) => new Date(right.createdAt ?? 0).getTime() - new Date(left.createdAt ?? 0).getTime());

    setNotifications(unreadEntries);
    setHasUnreadMessages(unreadEntries.length > 0);
  }, [session, currentUser?.id, receivedOffers, sentOffers, sellerItems, myInterests, isAdmin, adminModeration?.openReports, messageSyncKey]);

  useEffect(() => {
    if (!session) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      refreshPrivateData().catch(() => {});
    }, 45000);

    return () => window.clearInterval(intervalId);
  }, [session?.user?.id]);

  async function handleLoginSubmit(event) {
    event.preventDefault();
    setIsSubmittingAuth(true);
    setLoginInlineError("");

    try {
      const authResponse = await login(loginForm);
      const nextSession = {
        expiresAt: authResponse.expiresAt,
        token: authResponse.token ?? null,
        user: authResponse.user
      };

      storeSession(nextSession);
      setSession(nextSession);
      setPasswordRecoveryPreview(null);
      setLoginForm(initialLoginForm);
      closeAuthModal();
      openFeedback("success", "Login realizado", "Você entrou com sucesso na plataforma.");
    } catch (requestError) {
      const message = requestError.message || "Confira seu e-mail e senha.";
      if (message.toLowerCase().includes("confirme seu e-mail")) {
        setLoginInlineError(message);
      } else {
        openFeedback("error", "Não foi possível entrar", message);
      }
    } finally {
      setIsSubmittingAuth(false);
    }
  }

  async function handleRegisterSubmit(event) {
    event.preventDefault();
    setIsSubmittingAuth(true);

    try {
      const response = await register(registerForm);
      setRegisterForm(initialRegisterForm);
      setHomeMatchFilter(null);
      setLoginForm((current) => ({ ...current, email: registerForm.email, password: "" }));
      setAuthMode("login");
      openFeedback(
        "success",
        "Confirme seu e-mail",
        response?.message ?? "Sua conta foi criada. Confirme seu e-mail para entrar."
      );
    } catch (requestError) {
      openFeedback("error", "Não foi possível criar a conta", requestError.message || "Revise os dados e tente novamente.");
    } finally {
      setIsSubmittingAuth(false);
    }
  }

  async function handleForgotPasswordSubmit(event) {
    event.preventDefault();
    setIsSubmittingAuth(true);

    try {
      const response = await forgotPassword(forgotForm);
      setPasswordRecoveryPreview(response);
      setForgotForm(initialForgotForm);
      openFeedback("success", "Solicitação enviada", response.message);
    } catch (requestError) {
      openFeedback("error", "Falha ao solicitar redefinição", requestError.message || "Tente novamente.");
    } finally {
      setIsSubmittingAuth(false);
    }
  }

  async function handleResetPasswordSubmit(event) {
    event.preventDefault();
    setIsSubmittingAuth(true);

    try {
      await resetPassword(resetForm);
      window.history.replaceState({}, "", window.location.pathname);
      setResetForm({ token: "", newPassword: "", confirmPassword: "" });
      setAuthMode("login");
      openFeedback("success", "Senha redefinida", "Agora você pode entrar com a nova senha.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível redefinir", requestError.message || "Confira o token e tente novamente.");
    } finally {
      setIsSubmittingAuth(false);
    }
  }

  async function handleLogout() {
    try {
      await logout();
    } catch (requestError) {
      // limpeza local mesmo em falha remota
    } finally {
      clearSession();
      setSession(null);
      setDashboard(null);
      setMonetizationAccount(null);
      setSellerItems([]);
      setAdminModeration(null);
      setIsAdmin(false);
      setLoggedSection(loggedSections.EXPLORE);
      setOffers([]);
      setConversationModal((current) => ({ ...current, visible: false, data: null, draftMessage: "" }));
      openFeedback("success", "Sessão encerrada", "Você saiu da área logada.");
    }
  }

  async function handleInterestImageChange(event) {
    const [file] = event.target.files ?? [];
    if (!file) {
      setInterestForm((current) => ({ ...current, referenceImageUrl: "" }));
      return;
    }

    try {
      const dataUrl = await fileToDataUrl(file);
      setInterestForm((current) => ({ ...current, referenceImageUrl: dataUrl }));
    } catch (requestError) {
      openFeedback("error", "Imagem inválida", requestError.message || "Não foi possível usar a imagem.");
    }
  }

  async function handleInterestSubmit(event) {
    event.preventDefault();

    if (!session) {
      openAuthModal("register");
      return;
    }

    if (hasLink(interestForm.description)) {
      openFeedback(
        "error",
        "Link não permitido",
        "Remova links da descrição do anúncio antes de enviar para moderação."
      );
      return;
    }

    setIsSubmittingInterest(true);

    try {
      if (editingInterestId) {
        await updateInterest(editingInterestId, buildInterestPayload(interestForm));
      } else {
        await createInterest(buildInterestPayload(interestForm));
      }

      setEditingInterestId(null);
      setInterestForm(initialInterestForm);
      setIsInterestModalVisible(false);
      await Promise.all([refreshPrivateData(), refreshPublicData()]);
      navigateTo(loggedSections.MY_INTERESTS);
      openFeedback(
        "success",
        editingInterestId ? "Alteração recebida" : "Interesse recebido",
        editingInterestId
          ? "Vamos validar a alteração agora. Se houver recusa, você receberá um aviso para editar novamente."
          : "Vamos validar o anúncio agora. Se houver recusa, você receberá um aviso para ajustar."
      );
    } catch (requestError) {
      openFeedback(
        "error",
        editingInterestId ? "Não foi possível atualizar" : "Não foi possível publicar",
        requestError.message || "Revise os dados e tente novamente."
      );
    } finally {
      setIsSubmittingInterest(false);
    }
  }

  async function handleOfferSubmit(event) {
    event.preventDefault();

    if (!session) {
      openAuthModal("login");
      return;
    }

    if (!selectedInterest) {
      return;
    }

    if (!canSendOffer) {
      openFeedback("error", "Créditos insuficientes", noCreditsTooltip);
      return;
    }

    setIsSubmittingOffer(true);

    try {
      await createOffer(selectedInterest.id, {
        offeredPrice: offerForm.offeredPrice,
        sellerPhone: offerForm.sellerPhone,
        message: offerForm.message,
        includesDelivery: offerForm.includesDelivery,
        highlights: offerForm.highlights
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean)
      });

      setOfferForm(initialOfferForm);
      await refreshPrivateData();
      navigateTo(loggedSections.SENT_OFFERS);
      openFeedback("success", "Oferta enviada", "Sua oferta foi enviada para o anunciante.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível enviar", requestError.message || "Tente novamente.");
    } finally {
      setIsSubmittingOffer(false);
    }
  }

  async function handleCloseInterest(interestId) {
    if (!interestId) {
      return;
    }

    try {
      await closeInterest(interestId);
      await Promise.all([refreshPrivateData(), refreshPublicData()]);
      setSelectedInterest(null);
      openFeedback("success", "Anúncio desativado", "Seu anúncio não aparecerá mais para outros usuários.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível desativar", requestError.message || "Tente novamente.");
    }
  }

  async function handleDeleteInterest(interestId) {
    if (!interestId || !window.confirm("Deseja excluir este anúncio definitivamente?")) {
      return;
    }

    try {
      await deleteInterest(interestId);
      await Promise.all([refreshPrivateData(), refreshPublicData()]);
      setSelectedInterest(null);
      openFeedback("success", "Anúncio excluído", "O anúncio foi removido da plataforma.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível excluir", requestError.message || "Tente novamente.");
    }
  }

  function buildSessionFromMeResponse(me, previousSession = null) {
    if (!me) {
      return previousSession;
    }

    const user = me.user ?? (
        me.id
            ? {
              id: me.id,
              name: me.name,
              email: me.email,
              city: me.city,
              state: me.state,
              sellerCredits: me.credits,
              credits: me.credits
            }
            : null
    );

    return {
      expiresAt: me.expiresAt ?? previousSession?.expiresAt ?? null,
      token: me.token ?? previousSession?.token ?? null,
      user
    };
  }

  function openReportModal(interest) {
    setReportModal({
      visible: true,
      interest,
      form: initialReportForm,
      isSubmitting: false
    });
  }

  function closeReportModal() {
    setReportModal({ visible: false, interest: null, form: initialReportForm, isSubmitting: false });
  }

  async function handleReportSubmit(event) {
    event.preventDefault();
    if (!reportModal.interest?.id) {
      return;
    }

    setReportModal((current) => ({ ...current, isSubmitting: true }));
    try {
      await reportInterest(reportModal.interest.id, reportModal.form);
      closeReportModal();
      await refreshPublicData();
      openFeedback("success", "Denúncia enviada", "Obrigado. O conteúdo será analisado pela moderação.");
    } catch (requestError) {
      setReportModal((current) => ({ ...current, isSubmitting: false }));
      openFeedback("error", "Não foi possível denunciar", requestError.message || "Tente novamente.");
    }
  }

  async function refreshAdminModerationData() {
    try {
      const data = await fetchAdminModeration();
      setAdminModeration(data);
      setIsAdmin(true);
    } catch (requestError) {
      setAdminModeration(null);
      setIsAdmin(false);
      throw requestError;
    }
  }

  function startEditingModerationRule(rule) {
    setModerationRuleForm({
      id: rule.id,
      term: rule.term ?? "",
      riskLevel: rule.riskLevel ?? "HIGH",
      active: Boolean(rule.active)
    });
  }

  async function handleModerationRuleSubmit(event) {
    event.preventDefault();
    setIsSubmittingModerationRule(true);
    try {
      await saveModerationRule(moderationRuleForm.id || null, {
        term: moderationRuleForm.term,
        riskLevel: moderationRuleForm.riskLevel,
        active: moderationRuleForm.active
      });
      setModerationRuleForm(initialModerationRuleForm);
      await refreshAdminModerationData();
      openFeedback("success", "Regra salva", "A regra de moderação foi atualizada.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível salvar a regra", requestError.message || "Tente novamente.");
    } finally {
      setIsSubmittingModerationRule(false);
    }
  }

  async function handleDeleteModerationRule(ruleId) {
    if (!ruleId || !window.confirm("Deseja remover esta regra de moderação?")) {
      return;
    }

    setIsModerationActionLoading(true);
    try {
      await deleteModerationRule(ruleId);
      await refreshAdminModerationData();
      openFeedback("success", "Regra removida", "A regra não será mais usada na moderação local.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível remover", requestError.message || "Tente novamente.");
    } finally {
      setIsModerationActionLoading(false);
    }
  }

  async function handleModerationDecision(interestId, status) {
    if (!interestId) {
      return;
    }

    setIsModerationActionLoading(true);
    try {
      await decideInterestModeration(interestId, { status });
      await Promise.all([refreshAdminModerationData(), refreshPrivateData({ silent: true }), refreshPublicData()]);
      openFeedback("success", "Decisão aplicada", `Anúncio marcado como ${moderationStatusLabel(status).toLowerCase()}.`);
    } catch (requestError) {
      openFeedback("error", "Não foi possível aplicar decisão", requestError.message || "Tente novamente.");
    } finally {
      setIsModerationActionLoading(false);
    }
  }

  async function handlePurchaseProduct(productCode, paymentMethod = "MERCADO_PAGO") {
    if (!session) {
      openAuthModal("login");
      return;
    }

    const product = (monetizationAccount?.products ?? []).find((item) => item.code === productCode);
    setPaymentStatus({
      productCode,
      productName: product?.name ?? "Produto",
      paymentMethod,
      provider: "LOCAL_MOCK",
      step: "PAYMENT",
      message: "Pedido criado. Aguardando confirmação do pagamento."
    });
    navigateTo(loggedSections.CREDITS);
    setIsProcessingPurchase(true);
    try {
      const checkout = await purchaseProduct({ productCode, paymentMethod });
      const checkoutUrl = checkout.checkoutUrl ?? "";
      const isExternalCheckout = checkoutUrl && !checkoutUrl.startsWith("local://");

      if (isExternalCheckout) {
        setPaymentStatus({
          productCode,
          productName: product?.name ?? productCode,
          paymentMethod: checkout.paymentMethod ?? paymentMethod,
          provider: checkout.provider ?? "MERCADO_PAGO_CHECKOUT_PRO",
          checkoutUrl,
          step: "PAYMENT",
          message: checkout.message || "Finalize o pagamento no Mercado Pago. Os creditos serao liberados apos a confirmacao."
        });
        openFeedback("success", "Checkout criado", "Voce sera direcionado para concluir o pagamento com seguranca.");
        window.location.assign(checkoutUrl);
        return;
      }
      await refreshPrivateData();
      setPaymentStatus({
        productCode,
        productName: product?.name ?? productCode,
        paymentMethod: checkout.paymentMethod ?? paymentMethod,
        provider: checkout.provider ?? "LOCAL_MOCK",
        step: "COMPLETED",
        checkoutUrl,
        message: checkout.message || "Pagamento aprovado e créditos liberados."
      });
      openFeedback("success", "Pagamento aprovado", checkout.message || "Seu saldo foi atualizado.");
    } catch (requestError) {
      setPaymentStatus((current) => ({
        ...(current ?? {
          productCode,
          productName: product?.name ?? productCode,
          paymentMethod
        }),
        step: "FAILED",
        message: requestError.message || "Tente novamente."
      }));
      openFeedback("error", "Compra não concluída", requestError.message || "Tente novamente.");
    } finally {
      setIsProcessingPurchase(false);
    }
  }

  async function handleCancelSubscription() {
    if (!window.confirm("Deseja cancelar seu Plano Pro? O benefício será encerrado imediatamente.")) {
      return;
    }

    setIsProcessingPurchase(true);
    try {
      const account = await cancelSubscription();
      setMonetizationAccount(account);
      await refreshPrivateData({ silent: true });
      openFeedback("success", "Plano cancelado", "Seu Plano Pro foi cancelado e não há cobrança recorrente ativa neste MVP.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível cancelar", requestError.message || "Tente novamente.");
    } finally {
      setIsProcessingPurchase(false);
    }
  }

  async function handleSellerItemImageChange(event) {
    const [file] = event.target.files ?? [];
    if (!file) {
      setSellerItemForm((current) => ({ ...current, referenceImageUrl: "" }));
      return;
    }

    try {
      const dataUrl = await fileToDataUrl(file);
      setSellerItemForm((current) => ({ ...current, referenceImageUrl: dataUrl }));
    } catch (requestError) {
      openFeedback("error", "Imagem inválida", requestError.message || "Não foi possível usar a imagem.");
    }
  }

  async function handleSellerItemSubmit(event) {
    event.preventDefault();

    if (!session) {
      openAuthModal("login");
      return;
    }

    setIsSubmittingSellerItem(true);
    try {
      const item = editingSellerItemId
        ? await updateSellerItem(editingSellerItemId, buildSellerItemPayload(sellerItemForm))
        : await createSellerItem(buildSellerItemPayload(sellerItemForm));
      setSellerItemForm(initialSellerItemForm);
      setEditingSellerItemId(null);
      setIsSellerItemModalVisible(false);
      await refreshPrivateData();
      setSelectedSellerItemId(item.id);
      openFeedback(
        "success",
        editingSellerItemId ? "Item atualizado" : "Item cadastrado",
        editingSellerItemId
          ? "Seu item foi atualizado com sucesso."
          : "Agora vamos monitorar interesses compatíveis com ele."
      );
    } catch (requestError) {
      openFeedback(
        "error",
        editingSellerItemId ? "Não foi possível atualizar" : "Não foi possível cadastrar",
        requestError.message || "Revise os dados e tente novamente."
      );
    } finally {
      setIsSubmittingSellerItem(false);
    }
  }

  async function handleDeactivateSellerItem(itemId) {
    if (!itemId) {
      return;
    }

    try {
      await deactivateSellerItem(itemId);
      await refreshPrivateData();
      openFeedback("success", "Item desativado", "Você pode cadastrar outro item quando quiser.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível desativar", requestError.message || "Tente novamente.");
    }
  }

  async function handleShareSellerItem(itemId, interest) {
    if (!itemId || !interest?.id) {
      return;
    }

    if (!canSendOffer) {
      openFeedback("error", "Créditos insuficientes", noCreditsTooltip);
      return;
    }

    setSharingSellerItemInterestId(interest.id);
    try {
      await shareSellerItemOffer(itemId, interest.id, {
        offeredPrice: selectedSellerItemGroup?.item?.desiredPrice,
        sellerPhone: sellerItemShareForm.sellerPhone,
        message: sellerItemShareForm.message,
        includesDelivery: sellerItemShareForm.includesDelivery
      });
      await refreshPrivateData();
      setSellerItemShareForm(initialSellerItemShareForm);
      navigateTo(loggedSections.SENT_OFFERS);
      openFeedback("success", "Item compartilhado", "Sua oferta foi enviada usando o item cadastrado.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível compartilhar", requestError.message || "Tente novamente.");
    } finally {
      setSharingSellerItemInterestId(null);
    }
  }

  async function handleBoostInterest(boostCode, interestId = selectedInterest?.id, paymentMethod = "MERCADO_PAGO") {
    if (!interestId) {
      return;
    }

    setIsProcessingPurchase(true);
    try {
      await boostInterest(interestId, { boostCode, paymentMethod });
      await Promise.all([refreshPrivateData(), refreshPublicData()]);
      openFeedback("success", "Boost ativado", "Seu interesse foi impulsionado com sucesso.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível impulsionar", requestError.message || "Tente novamente.");
    } finally {
      setIsProcessingPurchase(false);
    }
  }

  async function handleRenewInterest(interestId = selectedInterest?.id) {
    if (!interestId) {
      return;
    }

    const availableCredits = monetizationAccount?.sellerCredits ?? currentUser?.sellerCredits ?? 0;
    if (availableCredits <= 0) {
      openFeedback(
        "error",
        "Créditos insuficientes",
        "Você precisa de 1 crédito para renovar o anúncio. Abra a página de créditos para comprar."
      );
      navigateTo(loggedSections.CREDITS);
      return;
    }

    try {
      await renewInterest(interestId);
      await Promise.all([refreshPrivateData(), refreshPublicData(), loadInterestDetail(interestId, { updateUrl: false })]);
      openFeedback("success", "Anúncio renovado", `Seu anúncio ganhou mais ${LISTING_EXPIRATION_DAYS} dias.`);
    } catch (requestError) {
      openFeedback("error", "Não foi possível renovar", requestError.message || "Tente novamente.");
    }
  }

  async function openConversation(offerId) {
    setConversationModal({
      visible: true,
      isLoading: true,
      isSending: false,
      draftMessage: "",
      data: null
    });

    try {
      const data = await fetchOfferConversation(offerId);
      const latestIncoming = latestIncomingMessageTimestamp(data, currentUser?.id);
      if (latestIncoming && currentUser?.id) {
        const seenMap = readSeenMessages(currentUser.id);
        writeSeenMessages(currentUser.id, {
          ...seenMap,
          [offerId]: latestIncoming
        });
        setMessageSyncKey((current) => current + 1);
      }

      setConversationModal({
        visible: true,
        isLoading: false,
        isSending: false,
        draftMessage: "",
        data
      });
    } catch (requestError) {
      setConversationModal((current) => ({ ...current, visible: false, isLoading: false }));
      openFeedback("error", "Não foi possível abrir a conversa", requestError.message || "Tente novamente.");
    }
  }

  async function handleConversationSubmit(event) {
    event.preventDefault();

    if (!conversationModal.data?.offerId || !conversationModal.draftMessage.trim()) {
      return;
    }

    setConversationModal((current) => ({ ...current, isSending: true }));

    try {
      const message = await sendOfferMessage(conversationModal.data.offerId, {
        content: conversationModal.draftMessage
      });

      setConversationModal((current) => ({
        ...current,
        isSending: false,
        draftMessage: "",
        data: {
          ...current.data,
          messages: [...(current.data?.messages ?? []), message]
        }
      }));
    } catch (requestError) {
      setConversationModal((current) => ({ ...current, isSending: false }));
      openFeedback("error", "Não foi possível enviar a mensagem", requestError.message || "Tente novamente.");
    }
  }

  function closeConversationModal() {
    setConversationModal({
      visible: false,
      isLoading: false,
      isSending: false,
      draftMessage: "",
      data: null
    });
  }

  async function handleNotificationSelect(notification) {
    setIsNotificationModalVisible(false);
    if (currentUser?.id && notification.id) {
      const seenMap = readSeenMessages(currentUser.id);
      writeSeenMessages(currentUser.id, {
        ...seenMap,
        [notification.id]: new Date(notification.createdAt ?? 0).getTime()
      });
      setMessageSyncKey((current) => current + 1);
    }

    if (notification.type === "seller-item-match") {
      if (currentUser?.id && notification.id) {
        const seenMap = readSeenMessages(currentUser.id);
        writeSeenMessages(currentUser.id, {
          ...seenMap,
          [notification.id]: new Date(notification.createdAt ?? 0).getTime()
        });
        setMessageSyncKey((current) => current + 1);
      }
      if (notification.sellerItemId) {
        setSelectedSellerItemId(notification.sellerItemId);
      }
      const matchedGroup = sellerItems.find((group) => group.item?.id === notification.sellerItemId);
      const matchingInterests = (matchedGroup?.matchingInterests ?? [])
        .filter((interest) => !currentUser?.id || interest.ownerId !== currentUser.id);
      setHomeMatchFilter({
        sellerItemId: notification.sellerItemId,
        sellerItemTitle: matchedGroup?.item?.title ?? notification.title ?? "item parecido",
        matchingInterests
      });
      setFilters({
        query: "",
        category: "",
        city: "",
        maxBudget: ""
      });
      setSelectedInterest(matchingInterests[0] ?? null);
      navigateTo(loggedSections.EXPLORE);
      return;
    }

    if (notification.type === "interest-expiring") {
      const interest = myInterests.find((item) => item.id === notification.interestId);
      if (interest) {
        setSelectedInterest(interest);
        setExpandedInterests((current) => ({ ...current, [interest.id]: true }));
      }
      navigateTo(loggedSections.MY_INTERESTS);
      return;
    }

    if (notification.type === "interest-moderation") {
      const interest = myInterests.find((item) => item.id === notification.interestId);
      if (interest) {
        setSelectedInterest(interest);
        setExpandedInterests((current) => ({ ...current, [interest.id]: true }));
      }
      navigateTo(loggedSections.MY_INTERESTS);
      return;
    }

    if (notification.type === "admin-report") {
      markAdminReportsSeen();
      navigateTo(loggedSections.ADMIN);
      return;
    }

    navigateTo(notification.section ?? loggedSections.RECEIVED_OFFERS);
    if (notification.offerId) {
      await openConversation(notification.offerId);
    }
  }

  function openNotificationModal() {
    const button = notificationButtonRef.current;
    if (button) {
      const rect = button.getBoundingClientRect();
      const modalWidth = Math.min(320, window.innerWidth - 32);
      const left = Math.max(16, rect.right - modalWidth);
      const top = rect.bottom + 12;
      setNotificationAnchorStyle({
        position: "fixed",
        top: `${top}px`,
        left: `${left}px`
      });
    } else {
      setNotificationAnchorStyle(null);
    }

    setIsNotificationModalVisible(true);
  }

  function handleMarkAllNotificationsRead() {
    if (!currentUser?.id || notifications.length === 0) {
      setIsNotificationModalVisible(false);
      return;
    }

    const seenMap = readSeenMessages(currentUser.id);
    const nextSeenMap = notifications.reduce((accumulator, notification) => ({
      ...accumulator,
      [notification.id ?? notification.offerId]: new Date(notification.createdAt ?? 0).getTime()
    }), seenMap);

    writeSeenMessages(currentUser.id, nextSeenMap);
    setNotifications([]);
    setHasUnreadMessages(false);
    setMessageSyncKey((current) => current + 1);
  }

  function renderPublicHome(showHero = true) {
    return (
      <>
        {showHero ? (
          <section className="hero hero--public">
            <div className="hero__copy">
              <h1>Encontre oportunidades publicadas e negocie direto com quem está procurando.</h1>
              <p>
                A home mostra os interesses cadastrados na plataforma. Quem quiser publicar um novo
                interesse ou responder com oferta pode entrar e seguir para a área correta.
              </p>
              <div className="hero__actions">
                <button type="button" className="primary-button" onClick={() => openAuthModal("register")}>
                  Quero cadastrar um interesse
                </button>
                <button type="button" className="ghost-button" onClick={() => openAuthModal("login")}>
                  Entrar na conta
                </button>
              </div>
            </div>

            <div className="hero__aside">
              <div className="hero-card">
                <strong>O que você encontra aqui</strong>
                <p>Demandas reais de compra, filtros por categoria e uma forma clara de responder.</p>
              </div>
              <div className="hero-card">
                <strong>Sem conta?</strong>
                <p>Você pode explorar a home normalmente e criar sua conta apenas quando quiser agir.</p>
              </div>
            </div>
          </section>
        ) : null}

        <section className="workspace-grid">
          <article className="panel panel--wide">
              <div className="panel__header">
                <div>
                  <span className="eyebrow">Home</span>
                  <h2>Os usuários estão interessados em...</h2>
                </div>
                <div className="panel__header-note">Use os filtros para buscar serviços, produtos ou regiões.</div>
              </div>

            <div className="filters">
              <input
                value={filters.query}
                onChange={(event) =>
                  updateHomeFilters((current) => ({ ...current, query: event.target.value }))
                }
                placeholder="Buscar por produto, serviço ou palavra-chave"
              />

              <select
                value={filters.category}
                onChange={(event) =>
                  updateHomeFilters((current) => ({ ...current, category: event.target.value }))
                }
              >
                <option value="">Todas as categorias</option>
                {categories.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>

              <input
                value={filters.city}
                onChange={(event) =>
                  updateHomeFilters((current) => ({ ...current, city: event.target.value }))
                }
                placeholder="Cidade"
              />

              <input
                type="number"
                min="0"
                value={filters.maxBudget}
                onChange={(event) =>
                  updateHomeFilters((current) => ({ ...current, maxBudget: event.target.value }))
                }
                placeholder="Orçamento máximo"
              />
            </div>

            {homeMatchFilter ? (
              <div className="context-filter-card">
                <div>
                  <span className="eyebrow">Filtro por item</span>
                  <strong>Mostrando interesses parecidos com: {homeMatchFilter.sellerItemTitle}</strong>
                </div>
                <button type="button" className="text-button" onClick={clearHomeMatchFilter}>
                  Limpar filtro
                </button>
              </div>
            ) : null}

            {isLoadingPublic && !homeMatchFilter ? (
              <div className="loading-card">Carregando interesses publicados...</div>
            ) : visibleHomeInterests.length === 0 ? (
              <EmptyState
                title={homeMatchFilter ? "Nenhum interesse parecido encontrado" : (session ? "Nenhum interesse de outros usuários" : "Nada publicado ainda")}
                description={
                  homeMatchFilter
                    ? "Quando aparecer um novo interesse parecido com esse item, ele ficará listado aqui."
                    : session
                    ? "Quando outros usuários publicarem interesses, eles aparecerão aqui."
                    : "Quando os primeiros interesses forem cadastrados, eles aparecerão aqui."
                }
              />
            ) : (
              <>
                <div className="interest-list">
                  {visibleHomeInterests.map((interest) => (
                    <InterestCard
                      key={interest.id}
                      interest={interest}
                      selected={interest.id === selectedInterest?.id}
                      onClick={selectPublicInterest}
                    />
                  ))}
                </div>
                {!homeMatchFilter && hasMoreInterests ? (
                  <button
                    type="button"
                    className="ghost-button load-more-button"
                    disabled={isLoadingMorePublic}
                    onClick={handleLoadMoreInterests}
                  >
                    {isLoadingMorePublic ? "Carregando..." : "Ver mais"}
                  </button>
                ) : null}
              </>
            )}
          </article>

          <aside className="panel panel--sticky">
            <div className="panel__header">
              <div className="panel-title-stack">
                <span className="eyebrow detail-owner-line">
                  {selectedInterest ? (
                    <>
                      <span>O usuário</span>
                      <strong>{firstName(selectedInterest.ownerName)}</strong>
                      <span>procura por um(a)</span>
                    </>
                  ) : "Selecione um interesse"}
                </span>
                <h2 className="title-with-badge">
                  {selectedInterest?.title ?? "Selecione um interesse"}
                  {isBoostActive(selectedInterest) ? <BoostRocket /> : null}
                </h2>
              </div>
            </div>

            {isLoadingInterestDetail ? (
              <div className="loading-card">Carregando anúncio...</div>
            ) : selectedInterest ? (
              <>
                {selectedInterest.referenceImageUrl ? (
                  <img
                    className="detail-image"
                    src={selectedInterest.referenceImageUrl}
                    alt={selectedInterest.title}
                    decoding="async"
                  />
                ) : null}

                <p className="detail-description">{selectedInterest.description}</p>

                <div className="detail-block">
                  <div className="detail-row">
                    <span>Categoria</span>
                    <strong>{selectedInterest.category}</strong>
                  </div>
                  <div className="detail-row">
                    <span>Localidade</span>
                    <strong>
                      {selectedInterest.location?.city}/{selectedInterest.location?.state}
                    </strong>
                  </div>
                  <div className="detail-row">
                    <span>Faixa de valor</span>
                    <strong>
                      {currency(selectedInterest.budgetMin)} até {currency(selectedInterest.budgetMax)}
                    </strong>
                  </div>
                  <div className="detail-row">
                    <span>Publicado por</span>
                    <strong>{selectedInterest.ownerName}</strong>
                  </div>
                  {isSelectedInterestMine ? (
                    <div className="detail-row">
                      <span>Status</span>
                      <strong className={`moderation-badge moderation-badge--${moderationStatusTone(selectedInterest.status)}`}>
                        {moderationStatusLabel(selectedInterest.status)}
                      </strong>
                    </div>
                  ) : null}
                  {isSelectedInterestMine ? (
                    <div className="detail-row">
                      <span>Tempo restante</span>
                      <strong className={expiryPillClass(selectedInterest)}>
                        {isListingExpiringSoon(selectedInterest) ? "⚠ " : ""}
                        {formatRemainingListingTime(selectedInterest)}
                      </strong>
                    </div>
                  ) : null}
                  {selectedInterest.allowsWhatsappContact && selectedInterest.whatsappContact ? (
                    <div className="detail-row">
                      <span>WhatsApp</span>
                      <a
                        href={`https://wa.me/${selectedInterest.whatsappContact.replace(/\D/g, "")}`}
                        target="_blank"
                        rel="noreferrer"
                      >
                        {selectedInterest.whatsappContact}
                      </a>
                    </div>
                  ) : null}
                </div>

                <div className="tag-cluster">
                  {selectedInterest.tags?.map((tag) => (
                    <span key={tag}>{tag}</span>
                  ))}
                </div>

                {renderInterestShareActions(selectedInterest)}

                {!isSelectedInterestMine ? (
                  <button
                    type="button"
                    className="report-button"
                    onClick={() => {
                      if (!session) {
                        openFeedback("info", "Entre para denunciar", "Faça login para informar a moderação sobre este interesse.");
                        openAuthModal("login");
                        return;
                      }
                      openReportModal(selectedInterest);
                    }}
                  >
                    <span aria-hidden="true">⚠</span>
                    <span>Denunciar este interesse</span>
                  </button>
                ) : null}

                {session ? (
                  isSelectedInterestMine ? (
                    <div className="cta-card">
                      <strong>Este interesse é seu</strong>
                      <p>Use a página de interesses ativos para acompanhar respostas recebidas.</p>
                      <button
                        type="button"
                        className="primary-button"
                        onClick={() => navigateTo(loggedSections.MY_INTERESTS)}
                      >
                        Ir para interesses ativos
                      </button>
                    </div>
                  ) : sentOfferForSelectedInterest ? (
                    renderSentOfferSummary(sentOfferForSelectedInterest)
                  ) : (
                    <form className="stacked-form" onSubmit={handleOfferSubmit}>
                      <div className="form-heading">
                        <span className="eyebrow">Responder</span>
                        <h3>Enviar oferta</h3>
                      </div>
                      <input
                        type="number"
                        min="0"
                        placeholder="Valor ofertado"
                        value={offerForm.offeredPrice}
                        onChange={(event) =>
                          setOfferForm((current) => ({
                            ...current,
                            offeredPrice: event.target.value
                          }))
                        }
                        required
                      />
                      <input
                        placeholder="Telefone ou WhatsApp"
                        value={offerForm.sellerPhone}
                        onChange={(event) =>
                          setOfferForm((current) => ({
                            ...current,
                            sellerPhone: event.target.value
                          }))
                        }
                        required
                      />
                      <textarea
                        rows="4"
                        placeholder="Explique como sua oferta atende ao interesse"
                        value={offerForm.message}
                        onChange={(event) =>
                          setOfferForm((current) => ({
                            ...current,
                            message: limitText(event.target.value, DESCRIPTION_MAX_LENGTH)
                          }))
                        }
                        maxLength={DESCRIPTION_MAX_LENGTH}
                        required
                      />
                      <FieldCounter value={offerForm.message} max={DESCRIPTION_MAX_LENGTH} />
                      <input
                        placeholder="Destaques separados por vírgula"
                        value={offerForm.highlights}
                        onChange={(event) =>
                          setOfferForm((current) => ({
                            ...current,
                            highlights: event.target.value
                          }))
                        }
                      />
                      <label className="checkbox-row">
                        <input
                          type="checkbox"
                          checked={offerForm.includesDelivery}
                          onChange={(event) =>
                            setOfferForm((current) => ({
                              ...current,
                              includesDelivery: event.target.checked
                            }))
                          }
                        />
                        <span>Inclui entrega ou deslocamento</span>
                      </label>
                      <button
                        type="submit"
                        className="primary-button"
                        disabled={isSubmittingOffer || !canSendOffer}
                        title={!canSendOffer ? noCreditsTooltip : undefined}
                      >
                        {isSubmittingOffer ? "Enviando..." : "Enviar oferta"}
                      </button>
                      {!canSendOffer ? <p className="form-note">{noCreditsTooltip}</p> : null}
                    </form>
                  )
                ) : (
                  <div className="cta-card">
                    <strong>Gostou desta oportunidade?</strong>
                    <p>Entre na plataforma para enviar uma oferta ou publicar seu próprio interesse.</p>
                    <div className="cta-card__actions">
                      <button type="button" className="primary-button" onClick={() => openAuthModal("login")}>
                        Entrar
                      </button>
                      <button type="button" className="ghost-button" onClick={() => openAuthModal("register")}>
                        Criar conta
                      </button>
                    </div>
                  </div>
                )}
              </>
            ) : (
              <EmptyState
                title="Nada selecionado"
                description="Escolha um interesse para ver mais detalhes."
              />
            )}
          </aside>
        </section>
      </>
    );
  }

  function renderInterestListItem(interest) {
    const isExpanded = Boolean(expandedInterests[interest.id]);
    const isSelected = interest.id === selectedInterest?.id;

    return (
      <article
        key={interest.id}
        className={`accordion-card ${isSelected ? "accordion-card--selected" : ""}`}
      >
        <button
          type="button"
          className="accordion-card__summary"
          onClick={() => toggleInterestExpansion(interest)}
        >
          <div className="accordion-card__leading">
            {interest.referenceImageUrl ? (
              <img
                className="accordion-card__thumb"
                src={interest.referenceImageUrl}
                alt={interest.title}
                loading="lazy"
                decoding="async"
              />
            ) : (
              <div className="accordion-card__thumb accordion-card__thumb--placeholder">
                {interest.title?.charAt(0) ?? "I"}
              </div>
            )}

            <div className="accordion-card__summary-main">
              <div className="accordion-card__copy">
                <strong className="title-with-badge">
                  {interest.title}
                  {isBoostActive(interest) ? <BoostRocket /> : null}
                </strong>
                <span className={`moderation-badge moderation-badge--${moderationStatusTone(interest.status)}`}>
                  {moderationStatusLabel(interest.status)}
                </span>
                <span>{interest.location?.city ? `${interest.location.city}/${interest.location?.state}` : "Sem local informado"}</span>
              </div>
            </div>
          </div>

          <span className="accordion-card__toggle">{isExpanded ? "−" : "+"}</span>
        </button>

        {isExpanded ? (
          <div className="accordion-card__content">
            <p>{interest.description}</p>
            <div className="accordion-card__meta">
              <span>{currency(interest.budgetMax)}</span>
              <span>{interest.tags?.slice(0, 3).join(" • ") || "Sem tags"}</span>
              <span className={`${expiryPillClass(interest)} expiry-pill--inline`}>
                {isListingExpiringSoon(interest) ? "⚠ " : ""}
                {formatRemainingListingTime(interest)}
              </span>
            </div>
          </div>
        ) : null}
      </article>
    );
  }

  function renderOfferListItem(offer, side = "left", interestImageUrl = null) {
    const isExpanded = Boolean(expandedOffers[offer.id]);
    const resolvedImageUrl = offer.offerImageUrl ?? interestImageUrl ?? offer.referenceImageUrl ?? null;
    const isIncomingOffer = side === "right" || side === "received";
    const primaryLabel = isIncomingOffer ? offer.sellerName : offer.interestTitle;
    const receivedSecondaryLabel = `${currency(offer.offeredPrice)} - ${offer.interestTitle ?? "Interesse"}`;
    const secondaryLabel = isIncomingOffer
      ? `${currency(offer.offeredPrice)} • ${formatTimestamp(offer.createdAt)}`
      : `${currency(offer.offeredPrice)} • ${offer.sellerName ?? "Sem anunciante"}`;

    return (
      <article key={offer.id} className="accordion-card">
        <button
          type="button"
          className="accordion-card__summary"
          onClick={() => toggleOfferExpansion(offer.id)}
        >
          <div className="accordion-card__leading">
            {resolvedImageUrl ? (
              <img
                className="accordion-card__thumb"
                src={resolvedImageUrl}
                alt={offer.interestTitle}
                loading="lazy"
                decoding="async"
              />
            ) : (
              <div className="accordion-card__thumb accordion-card__thumb--placeholder">
                {(primaryLabel ?? "O").charAt(0)}
              </div>
            )}

            <div className="accordion-card__summary-main">
              <div className="accordion-card__copy">
                <strong>{primaryLabel}</strong>
                <span>{side === "received" ? receivedSecondaryLabel : secondaryLabel}</span>
              </div>
            </div>
          </div>

          <span className="accordion-card__toggle">{isExpanded ? "−" : "+"}</span>
        </button>

        {isExpanded ? (
          <div className="accordion-card__content">
            {offer.offerImageUrl ? (
              <img
                className="offer-card__image"
                src={offer.offerImageUrl}
                alt={`Foto enviada por ${offer.sellerName ?? "vendedor"}`}
                loading="lazy"
                decoding="async"
              />
            ) : null}
            <p>{offer.message || "Sem mensagem informada."}</p>
            <div className="accordion-card__meta">
              {isIncomingOffer ? <span>{offer.sellerEmail || "Sem e-mail"}</span> : <span>{offer.interestTitle}</span>}
              <span>{offer.sellerPhone || formatTimestamp(offer.createdAt)}</span>
            </div>
            {offer.highlights?.length ? (
              <div className="tag-cluster tag-cluster--compact">
                {offer.highlights.map((highlight) => (
                  <span key={highlight}>{highlight}</span>
                ))}
              </div>
            ) : null}
            <button
              type="button"
              className="ghost-button"
              onClick={() => openConversation(offer.id)}
            >
              Abrir conversa
            </button>
          </div>
        ) : null}
      </article>
    );
  }

  function renderSentOfferSummary(offer) {
    return (
      <div className="sent-offer-summary">
        <div className="form-heading">
          <span className="eyebrow">Oferta enviada</span>
          <h3>Você já respondeu este interesse</h3>
        </div>

        {offer.offerImageUrl ? (
          <img
            className="offer-card__image"
            src={offer.offerImageUrl}
            alt={`Foto enviada por ${offer.sellerName ?? "você"}`}
            loading="lazy"
            decoding="async"
          />
        ) : null}

        <div className="sent-offer-summary__grid">
          <div>
            <span>Valor ofertado</span>
            <strong>{currency(offer.offeredPrice)}</strong>
          </div>
          <div>
            <span>Contato informado</span>
            <strong>{offer.sellerPhone || "Não informado"}</strong>
          </div>
          <div>
            <span>Enviada em</span>
            <strong>{formatTimestamp(offer.createdAt)}</strong>
          </div>
          <div>
            <span>Entrega/deslocamento</span>
            <strong>{offer.includesDelivery ? "Inclui" : "Não informado"}</strong>
          </div>
        </div>

        <p>{offer.message || "Sem mensagem informada."}</p>

        {offer.highlights?.length ? (
          <div className="tag-cluster tag-cluster--compact">
            {offer.highlights.map((highlight) => (
              <span key={highlight}>{highlight}</span>
            ))}
          </div>
        ) : null}

        <div className="cta-card__actions">
          <button
            type="button"
            className="primary-button"
            onClick={() => openConversation(offer.id)}
          >
            Abrir conversa
          </button>
          <button
            type="button"
            className="ghost-button"
            onClick={() => navigateTo(loggedSections.SENT_OFFERS)}
          >
            Ver ofertas enviadas
          </button>
        </div>
      </div>
    );
  }

  function renderPaymentTracker() {
    if (!paymentStatus) {
      return (
        <div className="payment-tracker payment-tracker--empty">
          <strong>Nenhum pagamento em andamento</strong>
          <p>Quando você comprar créditos, o acompanhamento do pedido aparecerá aqui.</p>
        </div>
      );
    }

    const steps = [
      { key: "ORDER", label: "Pedido criado" },
      { key: "PAYMENT", label: "Pagamento" },
      { key: "COMPLETED", label: "Créditos liberados" }
    ];
    const currentIndex = paymentStatus.step === "FAILED"
      ? 1
      : steps.findIndex((step) => step.key === paymentStatus.step);

    return (
      <div className={`payment-tracker ${paymentStatus.step === "FAILED" ? "payment-tracker--failed" : ""}`}>
        <div className="payment-tracker__header">
          <div>
            <span className="eyebrow">Status do pagamento</span>
            <h3>{paymentStatus.productName}</h3>
          </div>
          <span>Mercado Pago</span>
        </div>

        <div className="payment-steps">
          {steps.map((step, index) => {
            const isDone = paymentStatus.step !== "FAILED" && index <= currentIndex;
            const isActive = index === currentIndex;
            return (
              <div
                key={step.key}
                className={`payment-step ${isDone ? "done" : ""} ${isActive ? "active" : ""}`}
              >
                <span>{index + 1}</span>
                <strong>{step.label}</strong>
              </div>
            );
          })}
        </div>

        <p>{paymentStatus.message}</p>
        {paymentStatus.checkoutUrl && !paymentStatus.checkoutUrl.startsWith("local://") && (
          <a className="primary-button primary-button--compact" href={paymentStatus.checkoutUrl}>
            Abrir checkout
          </a>
        )}
      </div>
    );
  }

  function renderPaymentHistory() {
    const history = monetizationAccount?.paymentHistory ?? [];

    return (
      <div className="payment-history">
        <div className="payment-history__header">
          <div>
            <span className="eyebrow">Histórico</span>
            <h3>Últimos pagamentos</h3>
          </div>
          <small>{history.length} registros</small>
        </div>

        {history.length === 0 ? (
          <div className="payment-history__empty">
            <strong>Nenhuma compra registrada</strong>
            <p>Quando você comprar créditos ou um plano, os pagamentos aparecerão aqui.</p>
          </div>
        ) : (
          <div className="payment-history__list">
            {history.map((payment) => {
              const tone = paymentStatusTone(payment.status);
              return (
                <article key={payment.id} className="payment-history__item">
                  <div>
                    <strong>{payment.productName ?? payment.productCode ?? "Compra de créditos"}</strong>
                    <span>{formatTimestamp(payment.createdAt)}</span>
                  </div>
                  <div>
                    <span>{paymentMethodLabel(payment.paymentMethod)}</span>
                    <small>
                      {payment.provider === "MERCADO_PAGO_CHECKOUT_PRO"
                        ? "Mercado Pago"
                        : payment.provider || "Mercado Pago"}
                    </small>
                  </div>
                  <strong>{currency(payment.amount)}</strong>
                  <span className={`payment-status-pill payment-status-pill--${tone}`}>
                    {paymentStatusLabel(payment.status)}
                  </span>
                </article>
              );
            })}
          </div>
        )}
      </div>
    );
  }

  function renderCreditsPage() {
    const sellerCredits = monetizationAccount?.sellerCredits ?? 0;
    const purchasedCreditsTotal = monetizationAccount?.purchasedCreditsTotal ?? 0;
    const hasPurchasedCredits = purchasedCreditsTotal > 0;
    const hasNoCredits = !monetizationAccount?.subscriptionActive && sellerCredits <= 0;

    return (
      <section className="panel panel--spaced credits-page">
        <div className="panel__header">
          <div>
            <span className="eyebrow">Página</span>
            <h2>Comprar créditos</h2>
          </div>
          <button type="button" className="ghost-button" onClick={() => navigateTo(loggedSections.EXPLORE)}>
            Voltar para home
          </button>
        </div>

        <div className={`credits-summary ${hasNoCredits ? "credits-summary--empty" : ""}`}>
          <div>
            <span>Saldo atual</span>
            <strong>{monetizationAccount?.subscriptionActive ? "Plano Pro ativo" : `${sellerCredits} créditos`}</strong>
          </div>
          <p>
            {hasPurchasedCredits
              ? `Você já comprou ${purchasedCreditsTotal} créditos. Seu saldo atual considera créditos usados e disponíveis.`
              : `Você ainda não comprou créditos. Créditos grátis restantes: ${sellerCredits}.`}
          </p>
        </div>

        {renderPaymentTracker()}
        {renderPaymentHistory()}

        {monetizationAccount?.subscriptionActive ? (
          <div className="plan-active-card">
            <span className="eyebrow">Plano ativo</span>
            <h3>Você já possui o Plano Pro</h3>
            <p>Enquanto o plano estiver ativo, você pode enviar propostas sem consumir créditos.</p>
            <button
              type="button"
              className="text-button plan-active-card__cancel"
              disabled={isProcessingPurchase}
              onClick={handleCancelSubscription}
            >
              Cancelar plano
            </button>
          </div>
        ) : (
          <div className="purchase-flow">
            <article className="purchase-column">
              <span className="eyebrow">Escolha uma opção</span>
              <h3>Créditos ou plano para enviar propostas</h3>
              <div className="purchase-options">
                {purchaseProducts.map((product) => {
                  const isSelected = selectedPurchaseProduct?.code === product.code;
                  const description = product.type === "SUBSCRIPTION"
                    ? `Plano ativo por ${product.durationDays} dias para vendedores frequentes.`
                    : `${product.credits} propostas para responder interesses de compradores.`;

                  return (
                    <button
                      key={product.code}
                      type="button"
                      className={`purchase-option ${isSelected ? "purchase-option--selected" : ""}`}
                      onClick={() => setSelectedPurchaseProductCode(product.code)}
                      aria-pressed={isSelected}
                    >
                      <span className="purchase-option__radio" aria-hidden="true" />
                      <span className="purchase-option__content">
                        <strong>{product.name}</strong>
                        <small>{description}</small>
                      </span>
                      <span className="purchase-option__price">{currency(product.price)}</span>
                    </button>
                  );
                })}
              </div>
            </article>

            <article className="purchase-column purchase-payment">
              <span className="eyebrow">Pagamento</span>
              <h3>{selectedPurchaseProduct ? selectedPurchaseProduct.name : "Selecione uma opção"}</h3>
              <p>
                {selectedPurchaseProduct
                  ? `Total selecionado: ${currency(selectedPurchaseProduct.price)}`
                  : "Escolha um pacote ou plano para continuar."}
              </p>
              <div className="product-chip__actions product-chip__actions--payment">
                <button
                  type="button"
                  className="primary-button primary-button--compact mercado-pago-button"
                  disabled={isProcessingPurchase || !selectedPurchaseProduct}
                  onClick={() => selectedPurchaseProduct && handlePurchaseProduct(selectedPurchaseProduct.code)}
                >
                  <span className="mercado-pago-button__icon" aria-hidden="true">
                    <img className="mercado-pago-button__logo" src={mercadoPagoLogo} alt="" />
                  </span>
                  <span>Pague com Mercado Pago</span>
                </button>
              </div>
            </article>
          </div>
        )}
      </section>
    );
  }

  function renderSellerItemForm() {
    return (
      <form className="stacked-form seller-item-form" onSubmit={handleSellerItemSubmit}>
        <div className="form-heading">
          <span className="eyebrow">{editingSellerItemId ? "Editar item" : "Cadastrar item"}</span>
          <h3>{editingSellerItemId ? "Atualize o item cadastrado" : "Algo que você aceitaria negociar"}</h3>
        </div>
        <input
          placeholder="Título do item ou serviço"
          value={sellerItemForm.title}
          onChange={(event) =>
            setSellerItemForm((current) => ({
              ...current,
              title: limitText(event.target.value, TITLE_MAX_LENGTH)
            }))
          }
          maxLength={TITLE_MAX_LENGTH}
          required
        />
        <FieldCounter value={sellerItemForm.title} max={TITLE_MAX_LENGTH} />
        <textarea
          rows="4"
          placeholder="Descreva o que você tem, estado de conservação ou detalhes do serviço"
          value={sellerItemForm.description}
          onChange={(event) =>
            setSellerItemForm((current) => ({
              ...current,
              description: limitText(event.target.value, DESCRIPTION_MAX_LENGTH)
            }))
          }
          maxLength={DESCRIPTION_MAX_LENGTH}
          required
        />
        <FieldCounter value={sellerItemForm.description} max={DESCRIPTION_MAX_LENGTH} />

        <div className="two-columns">
          <select
            value={sellerItemForm.category}
            onChange={(event) => setSellerItemForm((current) => ({ ...current, category: event.target.value }))}
          >
            {categories.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <input
            type="number"
            min="0"
            placeholder="Valor que você analisaria"
            value={sellerItemForm.desiredPrice}
            onChange={(event) => setSellerItemForm((current) => ({ ...current, desiredPrice: event.target.value }))}
          />
        </div>

        <div className="three-columns">
          <input
            placeholder="Cidade"
            value={sellerItemForm.city}
            onChange={(event) => setSellerItemForm((current) => ({ ...current, city: event.target.value }))}
          />
          <input
            placeholder="Estado"
            value={sellerItemForm.state}
            onChange={(event) => setSellerItemForm((current) => ({ ...current, state: event.target.value }))}
          />
          <input
            placeholder="Bairro"
            value={sellerItemForm.neighborhood}
            onChange={(event) => setSellerItemForm((current) => ({ ...current, neighborhood: event.target.value }))}
          />
        </div>

        <input
          placeholder="Tags separadas por vírgula"
          value={sellerItemForm.tags}
          onChange={(event) => setSellerItemForm((current) => ({ ...current, tags: event.target.value }))}
        />

        <div className="media-field">
          <label htmlFor="seller-item-image">Foto do item</label>
          <input id="seller-item-image" type="file" accept="image/*" onChange={handleSellerItemImageChange} />
          {sellerItemForm.referenceImageUrl ? (
            <img
              className="interest-upload-preview"
              src={sellerItemForm.referenceImageUrl}
              alt="Prévia do item"
              decoding="async"
            />
          ) : null}
        </div>

        <div className="form-actions">
          {editingSellerItemId ? (
            <button type="button" className="ghost-button" onClick={cancelSellerItemEditing}>
              Cancelar edição
            </button>
          ) : null}
          <button type="submit" className="primary-button" disabled={isSubmittingSellerItem}>
            {isSubmittingSellerItem
              ? (editingSellerItemId ? "Salvando..." : "Cadastrando...")
              : (editingSellerItemId ? "Salvar alterações" : "Cadastrar item")}
          </button>
        </div>
      </form>
    );
  }

  function renderSellerItemsPage() {
    const selectedItem = selectedSellerItemGroup?.item ?? null;
    const matchingInterests = selectedSellerItemGroup?.matchingInterests ?? [];
    const shareDisabled = !canSendOffer || !sellerItemShareForm.sellerPhone.trim();

    return (
      <section ref={sellerItemsSectionRef} className="workspace-grid workspace-grid--wide">
        <article className="panel">
          <div className="panel__header">
            <div>
              <span className="eyebrow">Página</span>
              <h2>Meus itens</h2>
            </div>
          </div>

          <div className="cta-card seller-item-create-card">
            <strong>Cadastre itens que você aceitaria negociar</strong>
            <p>Ao cadastrar um item, a plataforma mostra interesses parecidos de outros usuários.</p>
            <div className="cta-card__actions">
              <button
                type="button"
                className="primary-button"
                onClick={() => {
                  setEditingSellerItemId(null);
                  setSellerItemForm(initialSellerItemForm);
                  setIsSellerItemModalVisible(true);
                }}
              >
                Cadastrar item
              </button>
            </div>
          </div>
        </article>

        <aside className="panel panel--sticky">
          <div className="panel__header">
            <div>
              <span className="eyebrow">Meus Itens</span>
              <h2>{selectedItem?.title ?? "Cadastre um item"}</h2>
            </div>
          </div>
          <label className="seller-items-toggle">
            <input
              type="checkbox"
              checked={showInactiveSellerItems}
              onChange={(event) => setShowInactiveSellerItems(event.target.checked)}
            />
            <span>Mostrar itens desativados</span>
          </label>

          {sellerItems.length ? (
            <>
              <div className="seller-item-tabs">
                {sellerItems.map((group) => (
                  <button
                    type="button"
                    key={group.item.id}
                    className={[
                      group.item.id === selectedItem?.id ? "active" : "",
                      !group.item.active ? "seller-item-tab--inactive" : ""
                    ].filter(Boolean).join(" ")}
                    onClick={() => setSelectedSellerItemId(group.item.id)}
                  >
                    {group.item.referenceImageUrl ? (
                      <img src={group.item.referenceImageUrl} alt={group.item.title} loading="lazy" decoding="async" />
                    ) : (
                      <span className="seller-item-tab__placeholder">{group.item.title?.charAt(0) ?? "I"}</span>
                    )}
                    <span className="seller-item-tab__title">
                      <strong>{group.item.title}</strong>
                      {!group.item.active ? <em>Desativado</em> : null}
                    </span>
                    <small
                      className="seller-match-count"
                      onClick={(event) => {
                        event.stopPropagation();
                        openSellerItemMatches(group);
                      }}
                    >
                      <strong>{group.matchCount}</strong>
                      <span>possíveis<br />interessados</span>
                    </small>
                  </button>
                ))}
              </div>

              {selectedItem ? (
                <div className="seller-item-summary">
                  <div className="seller-item-summary__content">
                    <strong>{currency(selectedItem.desiredPrice)}</strong>
                    {!selectedItem.active ? <span className="seller-item-status-badge">Desativado</span> : null}
                    {selectedItem.description ? (
                      <p title={selectedItem.description}>{selectedItem.description}</p>
                    ) : null}
                    {selectedItem.tags?.length ? (
                      <div className="seller-item-summary__tags" aria-label="Tags do item">
                        {selectedItem.tags.slice(0, 3).map((tag) => (
                          <span key={tag}>{tag}</span>
                        ))}
                      </div>
                    ) : null}
                  </div>
                  <div className="inline-actions inline-actions--seller-summary">
                    <button
                      type="button"
                      className="ghost-button ghost-button--small"
                      onClick={() => startEditingSellerItem(selectedItem)}
                    >
                      Editar item
                    </button>
                    {selectedItem.active ? (
                      <button
                        type="button"
                        className="ghost-button ghost-button--small"
                        onClick={() => handleDeactivateSellerItem(selectedItem.id)}
                      >
                        Desativar item
                      </button>
                    ) : null}
                  </div>
                </div>
              ) : null}

              <div className="stacked-form seller-share-form">
                <input
                  placeholder="Telefone ou WhatsApp para propostas"
                  value={sellerItemShareForm.sellerPhone}
                  onChange={(event) =>
                    setSellerItemShareForm((current) => ({ ...current, sellerPhone: event.target.value }))
                  }
                  required
                />
                <textarea
                  rows="3"
                  placeholder="Mensagem opcional ao compartilhar este item"
                  value={sellerItemShareForm.message}
                  onChange={(event) =>
                    setSellerItemShareForm((current) => ({
                      ...current,
                      message: limitText(event.target.value, DESCRIPTION_MAX_LENGTH)
                    }))
                  }
                  maxLength={DESCRIPTION_MAX_LENGTH}
                />
                <FieldCounter value={sellerItemShareForm.message} max={DESCRIPTION_MAX_LENGTH} />
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={sellerItemShareForm.includesDelivery}
                    onChange={(event) =>
                      setSellerItemShareForm((current) => ({ ...current, includesDelivery: event.target.checked }))
                    }
                  />
                  <span>Inclui entrega ou deslocamento</span>
                </label>
                {!canSendOffer ? <p className="form-note">{noCreditsTooltip}</p> : null}
              </div>

              {matchingInterests.length ? (
                <div className="seller-match-list">
                  {matchingInterests.map((interest) => (
                    <article key={interest.id} className="seller-match-card">
                      {interest.referenceImageUrl ? (
                        <img src={interest.referenceImageUrl} alt={interest.title} loading="lazy" decoding="async" />
                      ) : null}
                      <div>
                        <strong className="title-with-badge">
                          {interest.title}
                          {isBoostActive(interest) ? <BoostRocket /> : null}
                        </strong>
                        <p>{interest.description}</p>
                        <span>{currency(interest.budgetMax)}</span>
                      </div>
                      <button
                        type="button"
                        className="primary-button primary-button--compact"
                        disabled={shareDisabled || sharingSellerItemInterestId === interest.id}
                        title={!canSendOffer ? noCreditsTooltip : !sellerItemShareForm.sellerPhone.trim() ? "Informe um telefone para enviar a oferta." : undefined}
                        onClick={() => handleShareSellerItem(selectedItem.id, interest)}
                      >
                        {sharingSellerItemInterestId === interest.id ? "Enviando..." : "Compartilhar item"}
                      </button>
                    </article>
                  ))}
                </div>
              ) : (
                <EmptyState
                  title="Nenhum interesse compatível ainda"
                  description="Quando alguém procurar algo parecido com este item, ele aparecerá aqui."
                />
              )}
            </>
          ) : (
            <EmptyState
              title="Nenhum item cadastrado"
              description="Cadastre um item ou serviço para descobrir usuários interessados em algo parecido."
            />
          )}
        </aside>

        {isSellerItemModalVisible ? (
          <div className="modal-overlay" role="presentation" onClick={cancelSellerItemEditing}>
            <section
              className="form-modal panel panel--form"
              role="dialog"
              aria-modal="true"
              aria-labelledby="seller-item-form-title"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="feedback-modal__header">
                <div>
                  <span className="eyebrow">Meus itens</span>
                  <h2 id="seller-item-form-title">{editingSellerItemId ? "Editar item" : "Cadastrar item"}</h2>
                </div>
                <button
                  type="button"
                  className="modal-close-button"
                  onClick={cancelSellerItemEditing}
                  aria-label="Fechar modal"
                >
                  X
                </button>
              </div>
              {renderSellerItemForm()}
            </section>
          </div>
        ) : null}
      </section>
    );
  }

  function renderAdminModerationPage() {
    const pendingInterests = adminModeration?.pendingInterests ?? [];
    const rules = adminModeration?.rules ?? [];
    const openReports = adminModeration?.openReports ?? [];

    return (
      <section className="admin-moderation panel panel--spaced">
        <div className="panel__header">
          <div>
            <span className="eyebrow">Admin</span>
            <h2>Moderação de conteúdo</h2>
          </div>
          <button
            type="button"
            className="ghost-button ghost-button--small"
            onClick={() => refreshAdminModerationData().catch((requestError) => {
              openFeedback("error", "Não foi possível atualizar", requestError.message || "Tente novamente.");
            })}
          >
            Atualizar
          </button>
        </div>

        <article className="admin-card admin-card--priority">
          <div className="form-heading">
            <span className="eyebrow">Fila manual</span>
            <h3>{pendingInterests.length} anúncio(s) para revisar</h3>
          </div>
          <div className="admin-list">
            {pendingInterests.length ? pendingInterests.map((interest) => (
              <article key={interest.id} className="admin-list-item admin-list-item--stacked">
                <div>
                  <strong>{interest.title}</strong>
                  <span>{moderationStatusLabel(interest.status)} · {interest.category}</span>
                  <p>{interest.description}</p>
                  {interest.moderation?.reason ? <small>{interest.moderation.reason}</small> : null}
                </div>
                <div className="inline-actions admin-decision-actions">
                  <button
                    type="button"
                    className="primary-button moderation-action-button"
                    disabled={isModerationActionLoading}
                    onClick={() => handleModerationDecision(interest.id, "APPROVED")}
                  >
                    Aprovar anúncio
                  </button>
                  <button
                    type="button"
                    className="ghost-button moderation-action-button"
                    disabled={isModerationActionLoading}
                    onClick={() => handleModerationDecision(interest.id, "HIDDEN")}
                  >
                    Ocultar anúncio
                  </button>
                  <button
                    type="button"
                    className="danger-button moderation-action-button"
                    disabled={isModerationActionLoading}
                    onClick={() => handleModerationDecision(interest.id, "REJECTED")}
                  >
                    Recusar anúncio
                  </button>
                </div>
              </article>
            )) : (
              <EmptyState title="Fila vazia" description="Nada pendente de revisão manual agora." />
            )}
          </div>
        </article>

        <div className="admin-grid">
          <article className="admin-card">
            <div className="form-heading">
              <span className="eyebrow">Regras locais</span>
              <h3>{moderationRuleForm.id ? "Editar regra" : "Nova regra"}</h3>
            </div>
            <form className="stacked-form" onSubmit={handleModerationRuleSubmit}>
              <input
                placeholder="Palavra ou expressão"
                value={moderationRuleForm.term}
                onChange={(event) =>
                  setModerationRuleForm((current) => ({
                    ...current,
                    term: limitText(event.target.value, TITLE_MAX_LENGTH)
                  }))
                }
                maxLength={TITLE_MAX_LENGTH}
                required
              />
              <FieldCounter value={moderationRuleForm.term} max={TITLE_MAX_LENGTH} />
              <div className="two-columns">
                <select
                  value={moderationRuleForm.riskLevel}
                  onChange={(event) =>
                    setModerationRuleForm((current) => ({ ...current, riskLevel: event.target.value }))
                  }
                >
                  <option value="HIGH">Alto risco</option>
                  <option value="MEDIUM">Médio risco</option>
                  <option value="LOW">Baixo risco</option>
                </select>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={moderationRuleForm.active}
                    onChange={(event) =>
                      setModerationRuleForm((current) => ({ ...current, active: event.target.checked }))
                    }
                  />
                  <span>Regra ativa</span>
                </label>
              </div>
              <div className="inline-actions">
                {moderationRuleForm.id ? (
                  <button type="button" className="ghost-button ghost-button--small" onClick={() => setModerationRuleForm(initialModerationRuleForm)}>
                    Cancelar
                  </button>
                ) : null}
                <button type="submit" className="primary-button primary-button--compact" disabled={isSubmittingModerationRule}>
                  {isSubmittingModerationRule ? "Salvando..." : "Salvar regra"}
                </button>
              </div>
            </form>

            <div className="admin-list">
              {rules.length ? rules.map((rule) => (
                <article key={rule.id} className="admin-list-item">
                  <div>
                    <strong>{rule.term}</strong>
                    <span>{rule.riskLevel} · {rule.active ? "ativa" : "inativa"}</span>
                  </div>
                  <div className="inline-actions">
                    <button type="button" className="ghost-button ghost-button--small" onClick={() => startEditingModerationRule(rule)}>
                      Editar
                    </button>
                    <button type="button" className="danger-button action-button--compact" disabled={isModerationActionLoading} onClick={() => handleDeleteModerationRule(rule.id)}>
                      Remover
                    </button>
                  </div>
                </article>
              )) : (
                <EmptyState title="Nenhuma regra cadastrada" description="Cadastre termos locais para bloqueio ou revisão manual." />
              )}
            </div>
          </article>

          <article className="admin-card">
            <div className="form-heading">
              <span className="eyebrow">Denúncias</span>
              <h3>{openReports.length} denúncia(s) abertas</h3>
            </div>
            <div className="admin-list">
              {openReports.length ? openReports.map((report) => (
                <article key={report.id} className="admin-list-item">
                  <div>
                    <strong>{report.reason}</strong>
                    <span>Conteúdo: {report.contentId}</span>
                    {report.message ? <p>{report.message}</p> : null}
                  </div>
                </article>
              )) : (
                <EmptyState title="Nenhuma denúncia aberta" description="As denúncias dos usuários aparecerão aqui." />
              )}
            </div>
          </article>
        </div>
      </section>
    );
  }

  function renderLoggedArea() {
    const statCards = [
      {
        key: loggedSections.MY_INTERESTS,
        label: "Meus Interesses",
        value: dashboard?.totalActiveInterests ?? "...",
        accent: loggedSection === loggedSections.MY_INTERESTS
      },
      {
        key: loggedSections.SENT_OFFERS,
        label: "Ofertas Enviadas",
        value: dashboard?.totalOffersSent ?? "...",
        accent: loggedSection === loggedSections.SENT_OFFERS
      },
      {
        key: loggedSections.RECEIVED_OFFERS,
        label: "Ofertas Recebidas",
        value: dashboard?.totalOffersReceived ?? "...",
        accent: loggedSection === loggedSections.RECEIVED_OFFERS
      },
    ];

    return (
      <>
        <section className="hero hero--private">
          <div className="hero__stats hero__stats--actions">
            {statCards.map((card) => (
              <StatCard
                key={card.key}
                label={card.label}
                value={card.value}
                accent={card.accent}
                clickable
                onClick={() => navigateTo(card.key)}
              />
            ))}
          </div>
        </section>

        <section className="section-nav">
          <button
            type="button"
            className={loggedSection === loggedSections.EXPLORE ? "active" : ""}
            onClick={() => navigateTo(loggedSections.EXPLORE)}
          >
            Home
          </button>
          <button
              type="button"
              className={loggedSection === loggedSections.NEW_INTEREST ? "active" : ""}
              onClick={openNewInterestForm}
          >
            Cadastrar interesse
          </button>
          <button
            type="button"
            className={loggedSection === loggedSections.MY_INTERESTS ? "active" : ""}
            onClick={() => navigateTo(loggedSections.MY_INTERESTS)}
          >
            Meus Interesses
          </button>
          <button
            type="button"
            className={loggedSection === loggedSections.SENT_OFFERS ? "active" : ""}
            onClick={() => navigateTo(loggedSections.SENT_OFFERS)}
          >
            Ofertas enviadas
          </button>
          <button
            type="button"
            className={loggedSection === loggedSections.RECEIVED_OFFERS ? "active" : ""}
            onClick={() => navigateTo(loggedSections.RECEIVED_OFFERS)}
          >
            Ofertas recebidas
          </button>
          <button
            type="button"
            className={loggedSection === loggedSections.SELLER_ITEMS ? "active" : ""}
            onClick={() => navigateTo(loggedSections.SELLER_ITEMS)}
          >
            Meus itens
          </button>
          <button
            type="button"
            className={loggedSection === loggedSections.CREDITS ? "active" : ""}
            onClick={() => navigateTo(loggedSections.CREDITS)}
          >
            Comprar créditos
          </button>
          {isAdmin ? (
            <button
              type="button"
              className={`admin-nav-button ${loggedSection === loggedSections.ADMIN ? "active" : ""}`}
              onClick={() => {
                markAdminReportsSeen();
                navigateTo(loggedSections.ADMIN);
              }}
            >
              <span>Moderação</span>
              {unreadAdminReportCount > 0 ? (
                <strong className="admin-nav-badge">{unreadAdminReportCount}</strong>
              ) : null}
            </button>
          ) : null}
        </section>

        {loggedSection === loggedSections.EXPLORE ? renderPublicHome(false) : null}

        {loggedSection === loggedSections.CREDITS ? renderCreditsPage() : null}

        {loggedSection === loggedSections.MY_INTERESTS ? (
          <section ref={myInterestsSectionRef} className="workspace-grid">
            <article className="panel">
              <div className="panel__header">
                <div>
                  <span className="eyebrow">Página</span>
                  <h2>Interesses ativos</h2>
                </div>
              </div>

              {myInterests.length ? (
                <div className="accordion-list">{myInterests.map(renderInterestListItem)}</div>
              ) : (
                <EmptyState
                  title="Nenhum interesse ativo"
                  description="Cadastre um novo interesse para começar a receber ofertas."
                />
              )}
            </article>

            <aside className="panel panel--sticky">
              <div className="panel__header">
                <div>
                  <span className="eyebrow">Respostas</span>
                  <h2>{selectedInterest?.title ?? "Escolha um interesse"}</h2>
                </div>
              </div>

              {selectedInterest && isSelectedInterestMine ? (
                <>
                  {selectedInterest.referenceImageUrl ? (
                    <img
                      className="detail-image"
                      src={selectedInterest.referenceImageUrl}
                      alt={selectedInterest.title}
                      decoding="async"
                    />
                  ) : null}

                  <p className="detail-description">{selectedInterest.description}</p>
                  {["PENDING", "REVIEW_REQUIRED", "REJECTED", "REPORTED"].includes(selectedInterest.status) ? (
                    <div className={`moderation-callout moderation-callout--${moderationStatusTone(selectedInterest.status)}`}>
                      <strong>{moderationStatusLabel(selectedInterest.status)}</strong>
                      <p>
                        {selectedInterest.status === "REJECTED"
                          ? "Seu anúncio foi rejeitado. Edite para enviar novamente para análise ou exclua se preferir."
                          : (selectedInterest.moderation?.reason ?? "Seu anúncio ainda não está disponível publicamente.")}
                      </p>
                    </div>
                  ) : null}
                  <div className="expiry-renewal-row">
                    <span className={`${expiryPillClass(selectedInterest)} expiry-pill--inline`}>
                      {isListingExpiringSoon(selectedInterest) ? "⚠ " : ""}
                      {formatRemainingListingTime(selectedInterest)}
                    </span>
                    {isListingExpiringSoon(selectedInterest) ? (
                      <button
                        type="button"
                        className="renewal-button"
                        onClick={() => handleRenewInterest(selectedInterest.id)}
                        title="Usa 1 crédito para adicionar mais 30 dias ao anúncio"
                      >
                        <span className="renewal-button__icon" aria-hidden="true">↻</span>
                        <span>Renovar por 1 crédito</span>
                      </button>
                    ) : null}
                  </div>

                  {renderInterestShareActions(selectedInterest)}

                  <div className="inline-actions inline-actions--interest-actions">
                    <button
                      type="button"
                      className="ghost-button action-button--compact"
                      onClick={() => startEditingInterest(selectedInterest)}
                    >
                      Editar anúncio
                    </button>
                    <button
                      type="button"
                      className="ghost-button action-button--compact"
                      onClick={() => handleCloseInterest(selectedInterest.id)}
                    >
                      Desativar anúncio
                    </button>
                    <button
                      type="button"
                      className="danger-button action-button--compact"
                      onClick={() => handleDeleteInterest(selectedInterest.id)}
                    >
                      Excluir anúncio
                    </button>
                  </div>
                  {["APPROVED", "OPEN"].includes(selectedInterest?.status) && (
                      <>
                        <div className="boost-box">
                          <div>
                            <strong>Impulsionar interesse</strong>
                            <p>
                              {selectedInterest.boostedUntil
                                  ? `Destaque ativo até ${formatTimestamp(selectedInterest.boostedUntil)}`
                                  : "Apareça com prioridade na busca e na home."}
                            </p>
                          </div>

                          <div className="boost-box__actions">
                            {boostProducts.map((product) => (
                                <article key={product.code} className="product-chip product-chip--boost">
                                  <div>
                                    <strong>{product.name}</strong>
                                    <span>{currency(product.price)}</span>
                                  </div>

                                  <div className="product-chip__actions">
                                    <button
                                        type="button"
                                        className="text-button"
                                        disabled={isProcessingPurchase}
                                        onClick={() =>
                                            handleBoostInterest(product.code, selectedInterest.id, "MERCADO_PAGO")
                                        }
                                    >
                                      Pague com Mercado Pago
                                    </button>
                                  </div>
                                </article>
                            ))}
                          </div>
                        </div>

                        <div className="offers">
                          <div className="offers__header">
                            <span className="eyebrow">Ofertas recebidas</span>
                            <strong>{offers.length}</strong>
                          </div>

                          {offers.length === 0 ? (
                              <EmptyState
                                  title="Ainda sem ofertas"
                                  description="Quando alguém responder ao seu interesse, as mensagens aparecerão aqui."
                              />
                          ) : (
                              <div className="accordion-list">
                                {offers.map((offer) =>
                                    renderOfferListItem(offer, "right", selectedInterest.referenceImageUrl)
                                )}
                              </div>
                          )}
                        </div>
                      </>
                  )}
                </>
              ) : (
                <EmptyState
                  title="Selecione um interesse seu"
                  description="Clique em um interesse ativo para acompanhar as respostas."
                />
              )}
            </aside>
          </section>
        ) : null}

        {loggedSection === loggedSections.SENT_OFFERS ? (
          <section ref={sentOffersSectionRef} className="panel panel--spaced">
            <div className="panel__header">
              <div>
                <span className="eyebrow">Página</span>
                <h2>Ofertas enviadas</h2>
              </div>
            </div>

            {sentOffers.length ? (
              <div className="accordion-list">
                {sentOffers.map((offer) => renderOfferListItem(offer, "left"))}
              </div>
            ) : (
              <EmptyState
                title="Nenhuma oferta enviada"
                description="As ofertas que você enviar para outros interesses aparecerão aqui."
              />
            )}
          </section>
        ) : null}

        {loggedSection === loggedSections.RECEIVED_OFFERS ? (
          <section ref={receivedOffersSectionRef} className="panel panel--spaced">
            <div className="panel__header">
              <div>
                <span className="eyebrow">Página</span>
                <h2>Ofertas recebidas</h2>
              </div>
            </div>

            {receivedOffers.length ? (
              <div className="accordion-list">
                {receivedOffers.map((offer) => renderOfferListItem(offer, "received"))}
              </div>
            ) : (
              <EmptyState
                title="Nenhuma oferta recebida"
                description="As respostas aos seus interesses ficarão listadas aqui."
              />
            )}
          </section>
        ) : null}

        {loggedSection === loggedSections.SELLER_ITEMS ? renderSellerItemsPage() : null}

        {loggedSection === loggedSections.ADMIN && isAdmin ? renderAdminModerationPage() : null}

        {isInterestModalVisible ? (
          <div className="modal-overlay" role="presentation" onClick={cancelInterestEditing}>
            <section
              ref={newInterestSectionRef}
              className="form-modal panel panel--form"
              role="dialog"
              aria-modal="true"
              aria-labelledby="interest-form-title"
              onClick={(event) => event.stopPropagation()}
            >
            <div className="feedback-modal__header">
              <div>
                <span className="eyebrow">Página</span>
                <h2 id="interest-form-title">{editingInterestId ? "Editar anúncio" : "Cadastrar interesse"}</h2>
              </div>
              <button
                type="button"
                className="modal-close-button"
                onClick={cancelInterestEditing}
                aria-label="Fechar modal"
              >
                X
              </button>
            </div>

            <form className="stacked-form" onSubmit={handleInterestSubmit}>
              <div className="expiry-note">
                Este anúncio ficará ativo por até {LISTING_EXPIRATION_DAYS} dias e depois será removido automaticamente.
              </div>

              {editingInterestId ? (
                <div className="cta-card">
                  <strong>Você está editando um anúncio</strong>
                  <p>Ajuste os dados abaixo e salve para atualizar o anúncio publicado.</p>
                </div>
              ) : null}

              <input
                placeholder="Título do interesse"
                value={interestForm.title}
                onChange={(event) =>
                  setInterestForm((current) => ({
                    ...current,
                    title: limitText(event.target.value, TITLE_MAX_LENGTH)
                  }))
                }
                maxLength={TITLE_MAX_LENGTH}
                required
              />
              <FieldCounter value={interestForm.title} max={TITLE_MAX_LENGTH} />
              <textarea
                rows="4"
                placeholder="Descreva o item ou serviço que você procura"
                value={interestForm.description}
                onChange={(event) =>
                  setInterestForm((current) => ({
                    ...current,
                    description: limitText(event.target.value, DESCRIPTION_MAX_LENGTH)
                  }))
                }
                maxLength={DESCRIPTION_MAX_LENGTH}
                required
              />
              <FieldCounter value={interestForm.description} max={DESCRIPTION_MAX_LENGTH} />
              {hasLink(interestForm.description) ? (
                <p className="form-note form-note--compact">Links não são permitidos no anúncio.</p>
              ) : null}

              <div className="two-columns">
                <select
                  value={interestForm.category}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, category: event.target.value }))
                  }
                >
                  {categories.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
                <input
                  placeholder="Tags separadas por vírgula"
                  value={interestForm.tags}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, tags: event.target.value }))
                  }
                />
              </div>

              <div className="three-columns">
                <input
                  type="number"
                  min="0"
                  placeholder="Orçamento mínimo"
                  value={interestForm.budgetMin}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, budgetMin: event.target.value }))
                  }
                />
                <input
                  type="number"
                  min="0"
                  placeholder="Orçamento máximo"
                  value={interestForm.budgetMax}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, budgetMax: event.target.value }))
                  }
                  required
                />
                <input
                  type="number"
                  min="0"
                  placeholder="Raio em km"
                  value={interestForm.desiredRadiusKm}
                  onChange={(event) =>
                    setInterestForm((current) => ({
                      ...current,
                      desiredRadiusKm: event.target.value
                    }))
                  }
                />
              </div>

              <div className="three-columns">
                <input
                  placeholder="Cidade"
                  value={interestForm.city}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, city: event.target.value }))
                  }
                  required
                />
                <input
                  placeholder="Estado"
                  value={interestForm.state}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, state: event.target.value }))
                  }
                  required
                />
                <input
                  placeholder="Bairro"
                  value={interestForm.neighborhood}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, neighborhood: event.target.value }))
                  }
                />
              </div>

              <div className="two-columns">
                <input
                  placeholder="Condição desejada"
                  value={interestForm.preferredCondition}
                  onChange={(event) =>
                    setInterestForm((current) => ({
                      ...current,
                      preferredCondition: event.target.value
                    }))
                  }
                />
                <input
                  placeholder="Canal preferido de contato"
                  value={interestForm.preferredContactMode}
                  onChange={(event) =>
                    setInterestForm((current) => ({
                      ...current,
                      preferredContactMode: event.target.value
                    }))
                  }
                />
              </div>

              <div className="media-field">
                <label htmlFor="interest-image">Foto de referência</label>
                <input id="interest-image" type="file" accept="image/*" onChange={handleInterestImageChange} />
                {interestForm.referenceImageUrl ? (
                  <img
                    className="interest-upload-preview"
                    src={interestForm.referenceImageUrl}
                    alt="Prévia do interesse"
                    decoding="async"
                  />
                ) : null}
              </div>

              <div className="two-columns">
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={interestForm.acceptsNationwideOffers}
                    onChange={(event) =>
                      setInterestForm((current) => ({
                        ...current,
                        acceptsNationwideOffers: event.target.checked
                      }))
                    }
                  />
                  <span>Aceita propostas de todo o Brasil</span>
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={interestForm.allowsWhatsappContact}
                    onChange={(event) =>
                      setInterestForm((current) => ({
                        ...current,
                        allowsWhatsappContact: event.target.checked,
                        whatsappContact: event.target.checked ? current.whatsappContact : ""
                      }))
                    }
                  />
                  <span>Permitir contato via WhatsApp</span>
                </label>
              </div>

              {interestForm.allowsWhatsappContact ? (
                <input
                  placeholder="WhatsApp para contato"
                  value={interestForm.whatsappContact}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, whatsappContact: event.target.value }))
                  }
                  required
                />
              ) : null}

              <div className="two-columns">
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={interestForm.boostEnabled}
                    onChange={(event) =>
                      setInterestForm((current) => ({
                        ...current,
                        boostEnabled: event.target.checked
                      }))
                    }
                  />
                  <span>Destacar interesse</span>
                </label>
              </div>

              <div className="form-actions">
                {editingInterestId ? (
                  <button type="button" className="ghost-button" onClick={cancelInterestEditing}>
                    Cancelar edição
                  </button>
                ) : null}
                <button type="submit" className="primary-button" disabled={isSubmittingInterest}>
                  {isSubmittingInterest
                    ? (editingInterestId ? "Salvando..." : "Publicando...")
                    : (editingInterestId ? "Salvar alterações" : "Publicar interesse")}
                </button>
              </div>
            </form>
            </section>
          </div>
        ) : null}
      </>
    );
  }

  return (
    <div className="app-shell">
      <div className="background-grid" />

      <main className="page">
        <Header
          user={currentUser}
          currentSection={loggedSection}
          hasNotifications={hasUnreadMessages}
          sellerCredits={monetizationAccount?.sellerCredits}
          subscriptionActive={monetizationAccount?.subscriptionActive}
          isLoggedIn={Boolean(session)}
          notificationButtonRef={notificationButtonRef}
          onLoginClick={() => openAuthModal("login")}
          onRegisterClick={() => openAuthModal("register")}
          onCreditsClick={() => navigateTo(loggedSections.CREDITS)}
          onNotificationClick={openNotificationModal}
          onLogout={handleLogout}
          onNavigate={(section) => {
            if (section === loggedSections.NEW_INTEREST) {
              openNewInterestForm();
              return;
            }

            navigateTo(section);
          }}
        />

        {isLoadingPrivate && session ? (
          <section className="loading-card loading-card--full">Carregando sua área logada...</section>
        ) : null}

        {!session ? renderPublicHome(true) : renderLoggedArea()}
      </main>

      <AuthModal
        visible={isAuthModalVisible}
        mode={authMode}
        isSubmitting={isSubmittingAuth}
        loginForm={loginForm}
        registerForm={registerForm}
        forgotForm={forgotForm}
        resetForm={resetForm}
        loginInlineError={loginInlineError}
        passwordRecoveryPreview={passwordRecoveryPreview}
        onClose={closeAuthModal}
        onModeChange={setAuthMode}
        onLoginChange={(updater) => {
          setLoginInlineError("");
          setLoginForm(updater);
        }}
        onRegisterChange={setRegisterForm}
        onForgotChange={setForgotForm}
        onResetChange={setResetForm}
        onLoginSubmit={handleLoginSubmit}
        onRegisterSubmit={handleRegisterSubmit}
        onForgotSubmit={handleForgotPasswordSubmit}
        onResetSubmit={handleResetPasswordSubmit}
      />

      <OfferConversationModal
        modal={conversationModal}
        currentUserId={currentUser?.id}
        onClose={closeConversationModal}
        onDraftChange={(value) =>
          setConversationModal((current) => ({ ...current, draftMessage: value }))
        }
        onSubmit={handleConversationSubmit}
      />

      <NotificationModal
        visible={isNotificationModalVisible}
        notifications={notifications}
        anchorStyle={notificationAnchorStyle}
        onClose={() => setIsNotificationModalVisible(false)}
        onMarkAllRead={handleMarkAllNotificationsRead}
        onSelect={handleNotificationSelect}
      />

      {reportModal.visible ? (
        <div className="modal-overlay" role="presentation" onClick={closeReportModal}>
          <section
            className="feedback-modal panel"
            role="dialog"
            aria-modal="true"
            aria-labelledby="report-modal-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="feedback-modal__header">
              <div>
                <span className="eyebrow">Denúncia</span>
                <h2 id="report-modal-title">Denunciar interesse</h2>
              </div>
              <button type="button" className="modal-close-button" onClick={closeReportModal} aria-label="Fechar modal">
                X
              </button>
            </div>
            <form className="stacked-form" onSubmit={handleReportSubmit}>
              <input
                placeholder="Motivo da denúncia"
                value={reportModal.form.reason}
                onChange={(event) =>
                  setReportModal((current) => ({
                    ...current,
                    form: { ...current.form, reason: limitText(event.target.value, TITLE_MAX_LENGTH) }
                  }))
                }
                maxLength={TITLE_MAX_LENGTH}
                required
              />
              <FieldCounter value={reportModal.form.reason} max={TITLE_MAX_LENGTH} />
              <textarea
                rows="3"
                placeholder="Conte um pouco mais, se quiser"
                value={reportModal.form.message}
                onChange={(event) =>
                  setReportModal((current) => ({
                    ...current,
                    form: { ...current.form, message: limitText(event.target.value, DESCRIPTION_MAX_LENGTH) }
                  }))
                }
                maxLength={DESCRIPTION_MAX_LENGTH}
              />
              <FieldCounter value={reportModal.form.message} max={DESCRIPTION_MAX_LENGTH} />
              <p className="form-note">O conteúdo será analisado pela equipe de moderação.</p>
              <button type="submit" className="primary-button" disabled={reportModal.isSubmitting}>
                {reportModal.isSubmitting ? "Enviando..." : "Enviar denúncia"}
              </button>
            </form>
          </section>
        </div>
      ) : null}

      {isPaymentReturnLoading ? (
        <div className="modal-overlay">
          <div className="payment-return-loading" role="status" aria-live="polite">
            <span className="payment-return-loading__spinner" aria-hidden="true" />
            <div>
              <strong>Confirmando pagamento</strong>
              <p>Estamos sincronizando seu retorno do Mercado Pago.</p>
            </div>
          </div>
        </div>
      ) : null}

      <FeedbackModal modal={feedbackModal} onClose={() => setFeedbackModal(null)} />

      <footer className="app-footer">
        <small>Todos os direitos reservados para Eu Procuro Corp.</small>
      </footer>
    </div>
  );
}
