import { useDeferredValue, useEffect, useMemo, useRef, useState } from "react";

import {
  activateInterest,
  activateSellerItem,
  cancelSubscription,
  clearSession,
  boostInterest,
  closeInterest,
  connectChatSocket,
  createInterest,
  createOmbudsmanRequest,
  createOffer,
  createSellerItem,
  decideInterestModeration,
  deactivateSellerItem,
  deleteModerationRule,
  deleteInterest,
  fetchAdminModeration,
  fetchAdminOmbudsman,
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
  isAuthError,
  login,
  lookupAddressByPostalCode,
  logout,
  purchaseProduct,
  register,
  reportInterest,
  renewInterest,
  respondAdminOmbudsmanRequest,
  resetPassword,
  saveModerationRule,
  sendOfferMessage,
  shareSellerItemOffer,
  syncPayment,
  updateInterest,
  updateAdminOmbudsmanStatus,
  updateContentReportStatus,
  updateSellerItem,
  storeSession,
  verifyEmail
} from "./api";
import { trackEvent, trackPageView } from "./analytics";
import mercadoPagoLogo from "./assets/mercado-pago.svg";
import AuthModal from "./components/AuthModal";
import BoostRocket from "./components/BoostRocket";
import ContentAdminPanel from "./components/ContentAdminPanel";
import DashboardNavigation from "./components/DashboardNavigation";
import EmptyState from "./components/EmptyState";
import FeedbackModal from "./components/FeedbackModal";
import Footer from "./components/Footer";
import Header from "./components/Header";
import InterestCard from "./components/InterestCard";
import LegalPage from "./components/LegalPage";
import NotificationModal from "./components/NotificationModal";
import OfferConversationModal from "./components/OfferConversationModal";
import OperationalCatalogAdminPanel from "./components/OperationalCatalogAdminPanel";
import StatCard from "./components/StatCard";
import { useContentText } from "./content/ContentContext";
import { useLegalContent } from "./content/useLegalContent";
import { legalPages } from "./legalContent";

const initialInterestForm = {
  title: "",
  description: "",
  referenceImageUrl: "",
  category: "SERVICOS",
  budgetMin: "",
  budgetMax: "",
  postalCode: "",
  city: "",
  state: "",
  neighborhood: "",
  country: "Brasil",
  desiredRadiusKm: "30",
  allowsWhatsappContact: false,
  whatsappContact: "",
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
  postalCode: "",
  city: "",
  state: "",
  neighborhood: "",
  country: "Brasil",
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

const initialOmbudsmanForm = {
  name: "",
  email: "",
  type: "Reclamacao",
  subject: "",
  message: "",
  relatedEntityType: "",
  relatedEntityId: "",
  truthDeclarationAccepted: false
};

const OMBUDSMAN_TYPES = [
  "Reclamação",
  "Denúncia sobre atendimento",
  "Problema com pagamento",
  "Contestação de moderação",
  "Sugestão",
  "Outro"
];

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
  postalCode: "",
  city: "",
  state: "",
  neighborhood: "",
  country: "Brasil",
  termsOpened: false,
  termsAccepted: false
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

const sectionRoutes = {
  [loggedSections.EXPLORE]: "/",
  [loggedSections.NEW_INTEREST]: "/cadastrar-interesse",
  [loggedSections.MY_INTERESTS]: "/meus-interesses",
  [loggedSections.SENT_OFFERS]: "/ofertas-enviadas",
  [loggedSections.RECEIVED_OFFERS]: "/ofertas-recebidas",
  [loggedSections.SELLER_ITEMS]: "/meus-itens",
  [loggedSections.CREDITS]: "/comprar-creditos",
  [loggedSections.ADMIN]: "/admin"
};

const routeSections = Object.fromEntries(
  Object.entries(sectionRoutes).map(([section, route]) => [route, section])
);

const MESSAGE_SEEN_STORAGE_KEY = "eu-procuro-message-seen";
const MAX_REFERENCE_IMAGE_SIZE = 1200;
const REFERENCE_IMAGE_QUALITY = 0.78;
const HOME_PAGE_SIZE = 10;
const TITLE_MAX_LENGTH = 80;
const DESCRIPTION_MAX_LENGTH = 250;
const LISTING_EXPIRATION_DAYS = Number(import.meta.env.VITE_LISTING_EXPIRATION_DAYS ?? 30);
const FALLBACK_CATEGORIES = [
  { value: "AUTOMOVEIS", labelKey: "categories.automoveis" },
  { value: "IMOVEIS", labelKey: "categories.imoveis" },
  { value: "SERVICOS", labelKey: "categories.servicos" },
  { value: "ELETRONICOS", labelKey: "categories.eletronicos" },
  { value: "INSTRUMENTOS", labelKey: "categories.instrumentos" },
  { value: "OUTROS", labelKey: "categories.outros" }
];
const AUTH_SESSION_MODE = import.meta.env.VITE_AUTH_SESSION_MODE ?? "bearer";
const SHOULD_RECOVER_SESSION_FROM_COOKIE = AUTH_SESSION_MODE === "cookie";

function currency(value, t) {
  if (value === null || value === undefined || value === "") {
    return t ? t("global.currency.negotiable") : "";
  }

  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
  }).format(Number(value));
}

function ProductPrice({ product }) {
  const hasPromotion = Boolean(product?.promotional && product.originalPrice);

  return (
    <span className="product-price">
      {hasPromotion ? <s>{currency(product.originalPrice)}</s> : null}
      <strong>{currency(product?.price)}</strong>
      {hasPromotion && product.promotionLabel ? <em>{product.promotionLabel}</em> : null}
    </span>
  );
}

function formatTimestamp(value, t) {
  if (!value) {
    return t ? t("global.time.now") : "";
  }

  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}

function paymentStatusLabel(status, t) {
  return t ? t(`payment.status.${status || "PENDING"}`) : (status || "PENDING");
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

function paymentMethodLabel(method, t) {
  const normalized = String(method ?? "").toUpperCase();
  return t && normalized ? t(`payment.method.${normalized}`) : (method || "Mercado Pago");
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

function formatRemainingListingTime(listing, t) {
  const expiresAt = listingExpiresAt(listing);
  if (!expiresAt) {
    return t ? t("listing.activeDays", { count: LISTING_EXPIRATION_DAYS }) : "";
  }

  const diff = expiresAt.getTime() - Date.now();
  if (diff <= 0) {
    return t ? t("listing.expired") : "";
  }

  const days = Math.ceil(diff / (1000 * 60 * 60 * 24));
  if (days > 1) {
    return t ? t("listing.expires.days", { count: days }) : "";
  }

  const hours = Math.max(1, Math.ceil(diff / (1000 * 60 * 60)));
  return hours > 1 ? t("listing.expires.hours", { count: hours }) : t("listing.expires.oneHour");
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

function moderationStatusLabel(status, t) {
  return t ? t(`moderation.status.${status || "PENDING"}`) : (status || "PENDING");
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

function contentReportStatusLabel(status, t) {
  return t ? t(`admin.moderation.reports.status.${status || "OPEN"}`) : (status || "OPEN");
}

function limitText(value, maxLength) {
  return String(value ?? "").slice(0, maxLength);
}

function hasLink(value) {
  return /(https?:\/\/|www\.|\b[a-z0-9][a-z0-9-]*(?:\.[a-z0-9][a-z0-9-]*)+\b)/i.test(String(value ?? ""));
}

function firstName(value, t) {
  return value?.trim().split(/\s+/)[0] ?? (t ? t("global.user.fallback") : "");
}

function fallbackCategories(t) {
  return FALLBACK_CATEGORIES.map((category) => ({
    value: category.value,
    label: t(category.labelKey)
  }));
}

function createResetStateFromLocation() {
  const params = new URLSearchParams(window.location.search);
  return {
    mode: params.get("mode") === "reset" ? "reset" : "login",
    token: params.get("token") ?? ""
  };
}

function createInitialSharedInterestId() {
  const interestIdFromPath = window.location.pathname.match(/^\/interesses\/([^/]+)\/?$/)?.[1];
  if (interestIdFromPath) {
    return decodeURIComponent(interestIdFromPath);
  }

  const params = new URLSearchParams(window.location.search);
  return params.get("interest") ?? "";
}

function getActiveLegalPageSlug() {
  const slugFromPath = window.location.pathname.match(/^\/legal\/([^/]+)\/?$/)?.[1];
  if (slugFromPath && legalPages[slugFromPath]) {
    return slugFromPath;
  }

  const slug = window.location.hash.replace("#", "");
  return legalPages[slug] ? slug : "";
}

function isOmbudsmanRoute() {
  return window.location.pathname.replace(/\/+$/, "") === "/ouvidoria";
}

function getStoredTheme() {
  const storedTheme = window.localStorage.getItem("euProcuroTheme");
  return storedTheme === "light" ? "light" : "dark";
}

function getSectionFromPath() {
  const normalizedPath = window.location.pathname.replace(/\/+$/, "") || "/";
  if (normalizedPath === "/ouvidoria") {
    return loggedSections.EXPLORE;
  }
  return routeSections[normalizedPath] ?? loggedSections.EXPLORE;
}

function updateMetaTag(selector, attribute, value) {
  const element = document.head.querySelector(selector);
  if (element) {
    element.setAttribute(attribute, value);
  }
}

function upsertCanonical(url) {
  let canonical = document.head.querySelector("link[rel='canonical']");
  if (!canonical) {
    canonical = document.createElement("link");
    canonical.rel = "canonical";
    document.head.appendChild(canonical);
  }
  canonical.href = url;
}

function applyPageMeta({ title, description, url, robots = "index,follow" }) {
  document.title = title;
  updateMetaTag("meta[name='description']", "content", description);
  updateMetaTag("meta[name='robots']", "content", robots);
  updateMetaTag("meta[property='og:title']", "content", title);
  updateMetaTag("meta[property='og:description']", "content", description);
  updateMetaTag("meta[property='og:url']", "content", url);
  updateMetaTag("meta[name='twitter:title']", "content", title);
  updateMetaTag("meta[name='twitter:description']", "content", description);
  upsertCanonical(url);
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
    postalCode: interest?.location?.postalCode ?? "",
    city: interest?.location?.city ?? "",
    state: interest?.location?.state ?? "",
    neighborhood: interest?.location?.neighborhood ?? "",
    country: interest?.location?.country ?? "Brasil",
    desiredRadiusKm: interest?.desiredRadiusKm ?? "30",
    allowsWhatsappContact: Boolean(interest?.allowsWhatsappContact),
    whatsappContact: interest?.whatsappContact ?? "",
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
    postalCode: interestForm.postalCode,
    city: interestForm.city,
    state: interestForm.state,
    neighborhood: interestForm.neighborhood,
    country: interestForm.country,
    desiredRadiusKm: Number(interestForm.desiredRadiusKm || 0),
    allowsWhatsappContact: interestForm.allowsWhatsappContact,
    whatsappContact: interestForm.allowsWhatsappContact ? interestForm.whatsappContact : null,
    preferredCondition: interestForm.preferredCondition,
    preferredContactMode: interestForm.preferredContactMode,
    tags: interestForm.tags
      .split(",")
      .map((tag) => tag.trim())
      .filter(Boolean)
  };
}

function hasInvalidBudgetRange(interestForm) {
  const min = Number(interestForm.budgetMin || 0);
  const max = Number(interestForm.budgetMax || 0);
  return Number.isFinite(min) && Number.isFinite(max) && min > max;
}

function buildSellerItemPayload(itemForm) {
  return {
    title: itemForm.title,
    description: itemForm.description,
    referenceImageUrl: itemForm.referenceImageUrl || null,
    category: itemForm.category,
    desiredPrice: itemForm.desiredPrice || null,
    postalCode: itemForm.postalCode,
    city: itemForm.city,
    state: itemForm.state,
    neighborhood: itemForm.neighborhood,
    country: itemForm.country,
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
    postalCode: item?.location?.postalCode ?? "",
    city: item?.location?.city ?? "",
    state: item?.location?.state ?? "",
    neighborhood: item?.location?.neighborhood ?? "",
    country: item?.location?.country ?? "Brasil",
    tags: item?.tags?.join(", ") ?? ""
  };
}

function isBoostActive(interest) {
  return Boolean(
    interest?.boostedUntil
    && new Date(interest.boostedUntil).getTime() > Date.now()
  );
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

function FacebookIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        d="M14.2 8.4h2.3V5.1c-.4-.1-1.7-.2-3.2-.2-3.2 0-5.3 1.9-5.3 5.5v3.1H4.5v3.7H8v6.7h4.2v-6.7h3.5l.6-3.7h-4.1v-2.7c0-1.1.3-2.4 2-2.4Z"
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

function seenTimestamp(value) {
  const timestamp = new Date(value ?? Date.now()).getTime();
  return Number.isFinite(timestamp) && timestamp > 0 ? timestamp : Date.now();
}

function notificationSeenPatch(notification, timestampOverride) {
  const timestamp = timestampOverride ?? seenTimestamp(notification?.createdAt);
  const patch = {};

  if (notification?.id) {
    patch[notification.id] = timestamp;
  }

  if (notification?.offerId) {
    patch[notification.offerId] = timestamp;
    patch[`offer:${notification.offerId}`] = timestamp;
    patch[`new-offer:${notification.offerId}`] = timestamp;
  }

  return patch;
}

function adminReportNotificationId(report) {
  return `admin-report:${report?.id ?? report?.contentId ?? "unknown"}`;
}

function formatCep(value) {
  const digits = String(value ?? "").replace(/\D/g, "").slice(0, 8);
  if (digits.length <= 5) {
    return digits;
  }
  return `${digits.slice(0, 5)}-${digits.slice(5)}`;
}

function latestIncomingMessageTimestamp(conversation, currentUserId) {
  return (conversation?.messages ?? [])
    .filter((message) => message.senderId !== currentUserId)
    .reduce((latest, message) => {
      const nextValue = new Date(message.createdAt ?? 0).getTime();
      return nextValue > latest ? nextValue : latest;
    }, 0);
}

function sameEntityId(left, right) {
  if (left == null || right == null) {
    return false;
  }

  return String(left) === String(right);
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
  const { t } = useContentText();
  const { termsVersion } = useLegalContent();
  const initialResetState = useMemo(() => createResetStateFromLocation(), []);
  const initialSharedInterestId = useMemo(() => createInitialSharedInterestId(), []);
  const sharedInterestIdRef = useRef(initialSharedInterestId);
  const paymentReturnHandledRef = useRef(false);
  const emailVerificationHandledRef = useRef(false);
  const notificationButtonRef = useRef(null);
  const publicRequestSeq = useRef(0);
  const detailRequestSeq = useRef(0);
  const realtimeHandlerRef = useRef(null);
  const publicInterestDetailRef = useRef(null);
  const myInterestsSectionRef = useRef(null);
  const sentOffersSectionRef = useRef(null);
  const receivedOffersSectionRef = useRef(null);
  const sellerItemsSectionRef = useRef(null);
  const newInterestSectionRef = useRef(null);
  const [session, setSession] = useState(() => getStoredSession());
  const [theme, setTheme] = useState(getStoredTheme);
  const [activeLegalPageSlug, setActiveLegalPageSlug] = useState(getActiveLegalPageSlug);
  const [isOmbudsmanPageActive, setIsOmbudsmanPageActive] = useState(isOmbudsmanRoute);
  const [dashboard, setDashboard] = useState(null);
  const [monetizationAccount, setMonetizationAccount] = useState(null);
  const [categories, setCategories] = useState([]);
  const [interests, setInterests] = useState([]);
  const [selectedInterest, setSelectedInterest] = useState(null);
  const [offers, setOffers] = useState([]);
  const [sellerItems, setSellerItems] = useState([]);
  const [selectedSellerItemId, setSelectedSellerItemId] = useState(null);
  const [showInactiveSellerItems, setShowInactiveSellerItems] = useState(false);
  const [showInactiveInterests, setShowInactiveInterests] = useState(false);
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
  const [ombudsmanForm, setOmbudsmanForm] = useState(() => ({
    ...initialOmbudsmanForm,
    name: getStoredSession()?.user?.name ?? "",
    email: getStoredSession()?.user?.email ?? ""
  }));
  const [ombudsmanProtocol, setOmbudsmanProtocol] = useState("");
  const [isSubmittingOmbudsman, setIsSubmittingOmbudsman] = useState(false);
  const [adminModeration, setAdminModeration] = useState(null);
  const [selectedAdminReportId, setSelectedAdminReportId] = useState(null);
  const [adminOmbudsmanRequests, setAdminOmbudsmanRequests] = useState([]);
  const [ombudsmanResponses, setOmbudsmanResponses] = useState({});
  const [isOmbudsmanAdminLoading, setIsOmbudsmanAdminLoading] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);
  const [moderationRuleForm, setModerationRuleForm] = useState(initialModerationRuleForm);
  const [isSubmittingModerationRule, setIsSubmittingModerationRule] = useState(false);
  const [isModerationActionLoading, setIsModerationActionLoading] = useState(false);
  const [collapsedAdminSections, setCollapsedAdminSections] = useState({
    moderationQueue: true,
    moderationRules: true,
    reports: true,
    ombudsman: true,
    contentCrm: true,
    catalogCrm: true
  });
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
  const [loggedSection, setLoggedSection] = useState(getSectionFromPath);
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
  const [addressLookupState, setAddressLookupState] = useState({
    register: { isLoading: false, message: "" },
    interest: { isLoading: false, message: "" },
    sellerItem: { isLoading: false, message: "" }
  });
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
  const allMyInterests = useMemo(
      () => (dashboard?.myInterests ?? [])
          .filter((interest) => sameEntityId(interest.ownerId, currentUser?.id))
          .filter((interest) => interest.status !== "HIDDEN")
          .slice()
          .sort(byNewest),
      [dashboard?.myInterests, currentUser?.id]
  );
  const activeMyInterests = useMemo(
    () => allMyInterests.filter((interest) => interest.status !== "CLOSED"),
    [allMyInterests]
  );
  const myInterests = useMemo(
    () => showInactiveInterests ? allMyInterests : activeMyInterests,
    [activeMyInterests, allMyInterests, showInactiveInterests]
  );
  const sentOffers = useMemo(() => (dashboard?.offersSent ?? []).slice().sort(byNewest), [dashboard?.offersSent]);
  const receivedOffers = useMemo(() => (dashboard?.offersReceived ?? []).slice().sort(byNewest), [dashboard?.offersReceived]);
  const creditPurchasesEnabled = Boolean(monetizationAccount?.creditPurchasesEnabled);
  const boostPurchasesEnabled = Boolean(monetizationAccount?.boostPurchasesEnabled);
  const creditProducts = useMemo(
    () => creditPurchasesEnabled
      ? (monetizationAccount?.products ?? []).filter((product) => product.type === "CREDIT_PACK")
      : [],
    [creditPurchasesEnabled, monetizationAccount?.products]
  );
  const subscriptionProducts = useMemo(
    () => creditPurchasesEnabled
      ? (monetizationAccount?.products ?? []).filter((product) => product.type === "SUBSCRIPTION")
      : [],
    [creditPurchasesEnabled, monetizationAccount?.products]
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
    () => boostPurchasesEnabled
      ? (monetizationAccount?.products ?? []).filter((product) => product.type === "BOOST")
      : [],
    [boostPurchasesEnabled, monetizationAccount?.products]
  );

  useEffect(() => {
    window.localStorage.setItem("euProcuroTheme", theme);
  }, [theme]);

  function toggleTheme() {
    setTheme((currentTheme) => (currentTheme === "dark" ? "light" : "dark"));
  }

  const visibleHomeInterests = useMemo(
    () => {
      const source = homeMatchFilter?.matchingInterests ?? interests;
      return source.filter((interest) => !sameEntityId(interest.ownerId, currentUser?.id));
    },
    [homeMatchFilter, interests, currentUser?.id]
  );
  const selectedSellerItemGroup = useMemo(
    () => sellerItems.find((group) => group.item?.id === selectedSellerItemId) ?? sellerItems[0] ?? null,
    [sellerItems, selectedSellerItemId]
  );
  const isSelectedInterestMine = sameEntityId(selectedInterest?.ownerId, currentUser?.id);
  const sentOfferForSelectedInterest = useMemo(
    () => sentOffers.find((offer) => offer.interestPostId === selectedInterest?.id) ?? null,
    [sentOffers, selectedInterest?.id]
  );
  const canSendOffer = Boolean(monetizationAccount?.subscriptionActive || (monetizationAccount?.sellerCredits ?? 0) > 0);
  const noCreditsTooltip = t("offers.noCredits");
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

  function updateOmbudsmanForm(field, value) {
    setOmbudsmanForm((current) => ({ ...current, [field]: value }));
  }

  async function handleOmbudsmanSubmit(event) {
    event.preventDefault();
    setIsSubmittingOmbudsman(true);
    setOmbudsmanProtocol("");
    try {
      const response = await createOmbudsmanRequest(ombudsmanForm);
      setOmbudsmanProtocol(response.protocol);
      setOmbudsmanForm({
        ...initialOmbudsmanForm,
        name: session?.user?.name ?? "",
        email: session?.user?.email ?? ""
      });
    } catch (requestError) {
      openFeedback("error", "Não foi possível enviar", requestError.message || "Tente novamente.");
    } finally {
      setIsSubmittingOmbudsman(false);
    }
  }

  function updateAddressLookupState(scope, patch) {
    setAddressLookupState((current) => ({
      ...current,
      [scope]: {
        ...current[scope],
        ...patch
      }
    }));
  }

  async function handlePostalCodeLookup(scope, postalCode, applyAddress) {
    const normalizedPostalCode = String(postalCode ?? "").replace(/\D/g, "");
    if (!normalizedPostalCode) {
      updateAddressLookupState(scope, { isLoading: false, message: "" });
      return;
    }

    if (normalizedPostalCode.length !== 8) {
      updateAddressLookupState(scope, {
        isLoading: false,
        message: t("address.lookup.invalid")
      });
      return;
    }

    updateAddressLookupState(scope, {
      isLoading: true,
      message: t("address.lookup.loading")
    });

    try {
      const address = await lookupAddressByPostalCode(normalizedPostalCode);
      applyAddress(address);
      updateAddressLookupState(scope, {
        isLoading: false,
        message: t("address.lookup.success")
      });
    } catch (requestError) {
      updateAddressLookupState(scope, {
        isLoading: false,
        message: requestError.message || t("address.lookup.error")
      });
    }
  }

  function markNotificationsSeen(notificationList, { refresh = true, clear = false } = {}) {
    if (!currentUser?.id) {
      return;
    }

    const list = Array.isArray(notificationList) ? notificationList : [notificationList];
    const patch = list.reduce((accumulator, notification) => ({
      ...accumulator,
      ...notificationSeenPatch(notification)
    }), {});

    if (Object.keys(patch).length === 0) {
      return;
    }

    writeSeenMessages(currentUser.id, {
      ...readSeenMessages(currentUser.id),
      ...patch
    });

    if (clear) {
      setNotifications([]);
      setHasUnreadMessages(false);
    }

    if (refresh) {
      setMessageSyncKey((current) => current + 1);
    }
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

  function replaceCurrentUrl(pathname, search = "") {
    const nextPath = `${pathname}${search}`;
    window.history.replaceState({}, "", nextPath);
    trackPageView(nextPath);
  }

  function currentSectionPath(section = loggedSection) {
    return sectionRoutes[section] ?? sectionRoutes[loggedSections.EXPLORE];
  }

  function updateInterestUrl(interestId, replace = false) {
    sharedInterestIdRef.current = interestId ?? "";
    const nextPath = interestId
      ? `/interesses/${encodeURIComponent(interestId)}`
      : currentSectionPath();
    window.history[replace ? "replaceState" : "pushState"]({}, "", nextPath);
    trackPageView(nextPath);
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
        openFeedback("error", "Falha ao abrir procura", requestError.message || "Tente novamente.");
      }
    } finally {
      if (requestId === detailRequestSeq.current) {
        setIsLoadingInterestDetail(false);
      }
    }
  }

  function scrollPublicInterestDetailIntoViewOnMobile() {
    if (!window.matchMedia("(max-width: 1080px)").matches) {
      return;
    }

    window.requestAnimationFrame(() => {
      publicInterestDetailRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "start"
      });
    });
  }

  function selectPublicInterest(interest, options = {}) {
    setSelectedInterest(interest);
    setLoggedSection(loggedSections.EXPLORE);
    updateInterestUrl(interest.id, Boolean(options.replace));
    scrollPublicInterestDetailIntoViewOnMobile();
  }

  function buildInterestShareUrl(interest) {
    return `${window.location.origin}/interesses/${encodeURIComponent(interest.id)}`;
  }

  function buildInterestShareText(interest) {
    return t("share.message", { title: interest.title, url: buildInterestShareUrl(interest) });
  }

  function buildWhatsAppShareUrl(interest) {
    return `https://wa.me/?text=${encodeURIComponent(buildInterestShareText(interest))}`;
  }

  function buildXShareUrl(interest) {
    const url = new URL("https://twitter.com/intent/tweet");
    url.searchParams.set("text", t("share.xMessage", { title: interest.title }));
    url.searchParams.set("url", buildInterestShareUrl(interest));
    return url.toString();
  }

  function buildFacebookShareUrl(interest) {
    const url = new URL("https://www.facebook.com/sharer/sharer.php");
    url.searchParams.set("u", buildInterestShareUrl(interest));
    url.searchParams.set("quote", t("share.xMessage", { title: interest.title }));
    return url.toString();
  }

  async function copyInterestLinkToClipboard(interest) {
    const url = buildInterestShareUrl(interest);

    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(url);
      return;
    }

    window.prompt(t("share.prompt"), url);
  }

  async function handleCopyInterestLink(interest) {
    try {
      await copyInterestLinkToClipboard(interest);
      trackEvent("share_interest", {
        method: "copy_link",
        interest_id: interest.id
      });
      openFeedback("success", t("share.feedback.success.title"), t("share.feedback.success.message"));
    } catch (error) {
      window.prompt(t("share.prompt"), buildInterestShareUrl(interest));
    }
  }

  function renderInterestShareActions(interest) {
    if (!interest?.id) {
      return null;
    }

    if (!["APPROVED", "OPEN", "REPORTED"].includes(selectedInterest?.status)) {
      return null;
    }

    return (
      <div className="share-card">
        <div>
          <span className="eyebrow">{t("share.eyebrow")}</span>
          <strong>{t("share.title")}</strong>
        </div>
        <div className="share-card__actions">
          <a
            className="share-button share-button--whatsapp"
            href={buildWhatsAppShareUrl(interest)}
            target="_blank"
            rel="noreferrer"
            aria-label={t("share.whatsapp")}
            onClick={() => trackEvent("share_interest", { method: "whatsapp", interest_id: interest.id })}
          >
            <WhatsAppIcon />
          </a>
          <a
            className="share-button share-button--x"
            href={buildXShareUrl(interest)}
            target="_blank"
            rel="noreferrer"
            aria-label={t("share.x")}
            onClick={() => trackEvent("share_interest", { method: "x", interest_id: interest.id })}
          >
            <XIcon />
          </a>
          <a
            className="share-button share-button--facebook"
            href={buildFacebookShareUrl(interest)}
            target="_blank"
            rel="noreferrer"
            aria-label={t("share.facebook")}
            onClick={() => trackEvent("share_interest", { method: "facebook", interest_id: interest.id })}
          >
            <FacebookIcon />
          </a>
          <button
            type="button"
            className="share-button share-button--link"
            onClick={() => handleCopyInterestLink(interest)}
          >
            <LinkIcon />
            {t("share.copy")}
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
    const nextVisibleInterests = interests.filter((interest) => !sameEntityId(interest.ownerId, currentUser?.id));
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
      .filter((interest) => !sameEntityId(interest.ownerId, currentUser?.id));

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

  function navigateTo(section, options = {}) {
    const shouldScrollToSection = options.scrollIntoView !== false;

    setLoggedSection(section);
    setActiveLegalPageSlug("");
    setIsOmbudsmanPageActive(false);
    sharedInterestIdRef.current = "";
    if (section !== loggedSections.NEW_INTEREST && !editingInterestId) {
      setIsInterestModalVisible(false);
    }

    if (options.updateUrl !== false) {
      const nextPath = currentSectionPath(section);
      window.history[options.replace ? "replaceState" : "pushState"]({}, "", nextPath);
      trackPageView(nextPath);
    }

    if (section === loggedSections.EXPLORE) {
      setSelectedInterest((current) =>
        visibleHomeInterests.find((interest) => interest.id === current?.id) ?? current ?? null
      );
    }

    if (section === loggedSections.MY_INTERESTS) {
      setSelectedInterest((current) => myInterests.find((interest) => interest.id === current?.id) ?? myInterests[0] ?? null);
    }

    if (shouldScrollToSection && section === loggedSections.NEW_INTEREST) {
      window.requestAnimationFrame(() => {
        newInterestSectionRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "start"
        });
      });
    }

    if (shouldScrollToSection && section === loggedSections.MY_INTERESTS) {
      window.requestAnimationFrame(() => {
        myInterestsSectionRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "start"
        });
      });
    }

    if (shouldScrollToSection && section === loggedSections.SENT_OFFERS) {
      window.requestAnimationFrame(() => {
        sentOffersSectionRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "start"
        });
      });
    }

    if (shouldScrollToSection && section === loggedSections.RECEIVED_OFFERS) {
      window.requestAnimationFrame(() => {
        receivedOffersSectionRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "start"
        });
      });
    }

    if (shouldScrollToSection && section === loggedSections.SELLER_ITEMS) {
      window.requestAnimationFrame(() => {
        sellerItemsSectionRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "start"
        });
      });
    }
  }

  function navigateFromDashboardControl(section) {
    navigateTo(section, {
      scrollIntoView: !window.matchMedia("(min-width: 1081px)").matches
    });
  }

  function openNewInterestForm() {
    navigateFromDashboardControl(loggedSections.NEW_INTEREST);
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
          : pageInterests[0] ?? null;
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
      setCategories(loadedCategories?.length ? loadedCategories : fallbackCategories(t));
    } catch (requestError) {
      setCategories(fallbackCategories(t));
      openFeedback("error", t("categories.feedback.loadError.title"), requestError.message || t("errors.retry"));
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
      if (isAuthError(requestError)) {
        clearSession();
        setSession(null);
        setDashboard(null);
        setMonetizationAccount(null);
        setSellerItems([]);
        setAdminModeration(null);
        setIsAdmin(false);
        setLoggedSection(loggedSections.EXPLORE);
        openFeedback("error", "Sessão encerrada", requestError.message || "Entre novamente para continuar.");
        return;
      }

      openFeedback(
        "error",
        "Instabilidade temporária",
        requestError.message || "Não foi possível atualizar seus dados agora. Sua sessão foi mantida."
      );
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
          t("moderation.feedback.rejected.title"),
          envelope.payload?.reason || t("moderation.feedback.rejected.message")
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
    function syncRouteFromLocation() {
      const legalSlug = getActiveLegalPageSlug();
      const ombudsmanRoute = isOmbudsmanRoute();
      const nextSection = getSectionFromPath();
      setActiveLegalPageSlug(legalSlug);
      setIsOmbudsmanPageActive(ombudsmanRoute);
      setLoggedSection(nextSection);
      if (nextSection === loggedSections.NEW_INTEREST) {
        setEditingInterestId(null);
        setInterestForm(initialInterestForm);
        setIsInterestModalVisible(true);
      } else if (!editingInterestId) {
        setIsInterestModalVisible(false);
      }
      sharedInterestIdRef.current = createInitialSharedInterestId();
      if (sharedInterestIdRef.current) {
        loadInterestDetail(sharedInterestIdRef.current, { replace: true }).catch(() => {});
      }
      window.scrollTo({ top: 0, behavior: "smooth" });
    }

    function handleHashChange() {
      const legalSlug = getActiveLegalPageSlug();
      if (legalSlug) {
        replaceCurrentUrl(`/legal/${legalSlug}`);
      }
      syncRouteFromLocation();
    }

    window.addEventListener("hashchange", handleHashChange);
    window.addEventListener("popstate", syncRouteFromLocation);
    return () => {
      window.removeEventListener("hashchange", handleHashChange);
      window.removeEventListener("popstate", syncRouteFromLocation);
    };
  }, []);

  useEffect(() => {
    if (activeLegalPageSlug && window.location.pathname !== `/legal/${activeLegalPageSlug}`) {
      replaceCurrentUrl(`/legal/${activeLegalPageSlug}`);
    }
    if (isOmbudsmanPageActive && window.location.pathname !== "/ouvidoria") {
      replaceCurrentUrl("/ouvidoria");
    }
    if (getSectionFromPath() === loggedSections.NEW_INTEREST) {
      setIsInterestModalVisible(true);
    }
  }, []);

  useEffect(() => {
    if (!SHOULD_RECOVER_SESSION_FROM_COOKIE) {
      setIsLoadingPrivate(false);
      return undefined;
    }

    if (session) {
      return undefined;
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
  }, [session]);

  useEffect(() => {
    if (activeLegalPageSlug) {
      return;
    }

    refreshPublicData({
      query: deferredQuery,
      category: filters.category,
      city: filters.city,
      maxBudget: filters.maxBudget
    }).catch(() => {});
  }, [deferredQuery, filters.category, filters.city, filters.maxBudget, activeLegalPageSlug]);

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
    navigateTo(creditPurchasesEnabled ? loggedSections.CREDITS : loggedSections.EXPLORE);
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
        replaceCurrentUrl(currentSectionPath(creditPurchasesEnabled ? loggedSections.CREDITS : loggedSections.EXPLORE));
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
        openFeedback("error", "Não foi possível carregar propostas", requestError.message || "Tente novamente.");
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
    const origin = window.location.origin;

    if (activeLegalPageSlug) {
      const page = legalPages[activeLegalPageSlug];
      applyPageMeta({
        title: page?.title ? `${page.title} | Eu Procuro` : "Eu Procuro",
        description: page?.summary || page?.label || "Documentos legais da plataforma Eu Procuro.",
        url: `${origin}/legal/${activeLegalPageSlug}`
      });
      return;
    }

    if (isOmbudsmanPageActive) {
      applyPageMeta({
        title: "Ouvidoria | Eu Procuro",
        description: "Canal formal da Ouvidoria Eu Procuro para reclamacoes, contestacoes, sugestoes e problemas com a plataforma.",
        url: `${origin}/ouvidoria`,
        robots: "index,follow"
      });
      return;
    }

    if (selectedInterest?.id && loggedSection === loggedSections.EXPLORE) {
      const location = [selectedInterest.location?.city, selectedInterest.location?.state]
        .filter(Boolean)
        .join("/");
      const description = [
        selectedInterest.description,
        location ? `Localidade: ${location}.` : "",
        "Veja esta procura no Eu Procuro."
      ].filter(Boolean).join(" ");
      applyPageMeta({
        title: `${selectedInterest.title} | Eu Procuro`,
        description: limitText(description, 155),
        url: `${origin}/interesses/${encodeURIComponent(selectedInterest.id)}`
      });
      return;
    }

    const routeMeta = {
      [loggedSections.EXPLORE]: {
        title: "Eu Procuro - Marketplace reverso",
        description: "Publique o que você procura e receba propostas de quem pode atender.",
        robots: "index,follow"
      },
      [loggedSections.NEW_INTEREST]: {
        title: "Publicar procura | Eu Procuro",
        description: "Publique uma procura para receber propostas na plataforma Eu Procuro.",
        robots: "noindex,nofollow"
      },
      [loggedSections.MY_INTERESTS]: {
        title: "Minhas procuras | Eu Procuro",
        description: "Área privada de procuras publicadas no Eu Procuro.",
        robots: "noindex,nofollow"
      },
      [loggedSections.SENT_OFFERS]: {
        title: "Propostas enviadas | Eu Procuro",
        description: "Área privada de propostas enviadas no Eu Procuro.",
        robots: "noindex,nofollow"
      },
      [loggedSections.RECEIVED_OFFERS]: {
        title: "Propostas recebidas | Eu Procuro",
        description: "Área privada de propostas recebidas no Eu Procuro.",
        robots: "noindex,nofollow"
      },
      [loggedSections.SELLER_ITEMS]: {
        title: "Tenho para negociar | Eu Procuro",
        description: "Área privada de itens disponíveis para negociação no Eu Procuro.",
        robots: "noindex,nofollow"
      },
      [loggedSections.CREDITS]: {
        title: "Créditos | Eu Procuro",
        description: "Área privada de créditos e pagamentos no Eu Procuro.",
        robots: "noindex,nofollow"
      },
      [loggedSections.ADMIN]: {
        title: "Admin | Eu Procuro",
        description: "Área administrativa do Eu Procuro.",
        robots: "noindex,nofollow"
      }
    };

    const meta = routeMeta[loggedSection] ?? routeMeta[loggedSections.EXPLORE];
    applyPageMeta({
      ...meta,
      url: `${origin}${currentSectionPath(loggedSection)}`
    });
  }, [activeLegalPageSlug, isOmbudsmanPageActive, selectedInterest?.id, selectedInterest?.title, selectedInterest?.description, loggedSection]);

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
          title: offer.interestTitle ?? "Nova proposta recebida",
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

    const expiringInterestEntries = activeMyInterests
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
          title: interest.title ?? t("listing.expirationNotification.title"),
          message: t("listing.expirationNotification.message", { remaining: formatRemainingListingTime(interest, t) }),
          createdAt: expiresAt?.toISOString() ?? new Date().toISOString()
        };
      })
      .filter(Boolean);

    const moderationEntries = activeMyInterests
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
          title: rejected ? "Procura rejeitada" : "Procura em análise",
          message: rejected
            ? "Sua procura foi rejeitada. Você pode editar e enviar novamente para análise ou excluir."
            : (interest.moderation?.reason ?? "Sua procura está aguardando revisão."),
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
            message: report.reason ?? "Um usuário denunciou uma procura para revisão.",
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
  }, [session, currentUser?.id, receivedOffers, sentOffers, sellerItems, activeMyInterests, isAdmin, adminModeration?.openReports, messageSyncKey]);

  useEffect(() => {
    if (isAdmin && loggedSection === loggedSections.ADMIN) {
      refreshAdminOmbudsmanData().catch(() => {});
    }
  }, [isAdmin, loggedSection]);

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
    } catch (requestError) {
      clearSession();
      setSession(null);

      const message = requestError.message || t("auth.feedback.login.error.message");
      if (message.toLowerCase().includes("confirme seu e-mail")) {
        setLoginInlineError(message);
      } else {
        openFeedback("error", t("auth.feedback.login.error.title"), message);
      }
    } finally {
      setIsSubmittingAuth(false);
    }
  }

  async function handleRegisterSubmit(event) {
    event.preventDefault();
    setIsSubmittingAuth(true);

    if (!registerForm.termsOpened || !registerForm.termsAccepted) {
      openFeedback("error", t("auth.feedback.register.terms.title"), t("auth.feedback.register.terms.message"));
      setIsSubmittingAuth(false);
      return;
    }

    try {
      const response = await register({
        ...registerForm,
        termsAccepted: true,
        termsVersion
      });
      setRegisterForm(initialRegisterForm);
      setHomeMatchFilter(null);
      setLoginForm((current) => ({ ...current, email: registerForm.email, password: "" }));
      setAuthMode("login");
      openFeedback(
        "success",
        t("auth.feedback.register.success.title"),
        response?.message ?? t("auth.feedback.register.success.message")
      );
    } catch (requestError) {
      openFeedback("error", t("auth.feedback.register.error.title"), requestError.message || t("auth.feedback.register.error.message"));
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
      openFeedback("success", t("auth.feedback.forgot.success.title"), response.message);
    } catch (requestError) {
      openFeedback("error", t("auth.feedback.forgot.error.title"), requestError.message || t("errors.retry"));
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
      openFeedback("success", t("auth.feedback.reset.success.title"), t("auth.feedback.reset.success.message"));
    } catch (requestError) {
      openFeedback("error", t("auth.feedback.reset.error.title"), requestError.message || t("auth.feedback.reset.error.message"));
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
      setSelectedInterest(null);
      setActiveLegalPageSlug("");
      sharedInterestIdRef.current = "";
      replaceCurrentUrl(sectionRoutes[loggedSections.EXPLORE]);
      setOffers([]);
      setConversationModal((current) => ({ ...current, visible: false, data: null, draftMessage: "" }));
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
      return;
    }

    if (hasInvalidBudgetRange(interestForm)) {
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
        editingInterestId ? t("interest.feedback.updateReceived.title") : t("interest.feedback.createReceived.title"),
        editingInterestId
          ? t("interest.feedback.updateReceived.message")
          : t("interest.feedback.createReceived.message")
      );
    } catch (requestError) {
      openFeedback(
        "error",
        editingInterestId ? t("interest.feedback.updateError.title") : t("interest.feedback.createError.title"),
        requestError.message || t("auth.feedback.register.error.message")
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
      openFeedback("success", "Proposta enviada", "Sua proposta foi enviada para quem publicou a procura.");
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
      openFeedback("success", "Procura desativada", "Sua procura não aparecerá mais para outros usuários.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível desativar", requestError.message || "Tente novamente.");
    }
  }

  async function handleActivateInterest(interestId) {
    if (!interestId) {
      return;
    }

    try {
      await activateInterest(interestId);
      await Promise.all([refreshPrivateData(), refreshPublicData()]);
      openFeedback("success", "Procura enviada para análise", "Sua procura foi reativada e será validada antes de voltar à vitrine.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível ativar", requestError.message || "Tente novamente.");
    }
  }

  async function handleDeleteInterest(interestId) {
    if (!interestId || !window.confirm("Deseja excluir esta procura definitivamente?")) {
      return;
    }

    try {
      await deleteInterest(interestId);
      await Promise.all([refreshPrivateData(), refreshPublicData()]);
      setSelectedInterest(null);
      openFeedback("success", "Procura excluída", "A procura foi removida da plataforma.");
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
              postalCode: me.postalCode,
              neighborhood: me.neighborhood,
              country: me.country,
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
      openFeedback("success", t("report.feedback.success.title"), t("report.feedback.success.message"));
    } catch (requestError) {
      setReportModal((current) => ({ ...current, isSubmitting: false }));
      openFeedback("error", t("report.feedback.error.title"), requestError.message || t("errors.retry"));
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

  async function refreshAdminOmbudsmanData(status = "") {
    setIsOmbudsmanAdminLoading(true);
    try {
      const data = await fetchAdminOmbudsman(status);
      setAdminOmbudsmanRequests(data ?? []);
    } catch (requestError) {
      openFeedback("error", "Falha ao carregar ouvidoria", requestError.message || "Tente novamente.");
    } finally {
      setIsOmbudsmanAdminLoading(false);
    }
  }

  async function handleOmbudsmanStatusChange(requestId, status) {
    try {
      const updated = await updateAdminOmbudsmanStatus(requestId, status);
      setAdminOmbudsmanRequests((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      openFeedback("success", "Status atualizado", `A manifestação ${updated.protocol} foi atualizada.`);
    } catch (requestError) {
      openFeedback("error", "Falha ao atualizar status", requestError.message || "Tente novamente.");
    }
  }

  async function handleOmbudsmanResponseSubmit(requestItem) {
    const responseText = ombudsmanResponses[requestItem.id]?.trim();
    if (!responseText) {
      openFeedback("error", "Resposta obrigatória", "Informe uma resposta antes de enviar.");
      return;
    }

    try {
      const updated = await respondAdminOmbudsmanRequest(requestItem.id, {
        adminResponse: responseText,
        status: "ANSWERED"
      });
      setAdminOmbudsmanRequests((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      setOmbudsmanResponses((current) => ({ ...current, [requestItem.id]: "" }));
      openFeedback("success", "Resposta enviada", `A manifestação ${updated.protocol} foi respondida.`);
    } catch (requestError) {
      openFeedback("error", "Falha ao responder", requestError.message || "Tente novamente.");
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
      openFeedback("success", t("admin.moderation.rule.saved.title"), t("admin.moderation.rule.saved.message"));
    } catch (requestError) {
      openFeedback("error", t("admin.moderation.rule.saveError.title"), requestError.message || t("errors.retry"));
    } finally {
      setIsSubmittingModerationRule(false);
    }
  }

  async function handleDeleteModerationRule(ruleId) {
    if (!ruleId || !window.confirm(t("admin.moderation.rule.deleteConfirm"))) {
      return;
    }

    setIsModerationActionLoading(true);
    try {
      await deleteModerationRule(ruleId);
      await refreshAdminModerationData();
      openFeedback("success", t("admin.moderation.rule.removed.title"), t("admin.moderation.rule.removed.message"));
    } catch (requestError) {
      openFeedback("error", t("admin.moderation.rule.removeError.title"), requestError.message || t("errors.retry"));
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
      openFeedback("success", t("admin.moderation.decisionApplied.title"), t("admin.moderation.decisionApplied.message", { status: moderationStatusLabel(status, t).toLowerCase() }));
    } catch (requestError) {
      openFeedback("error", "Não foi possível aplicar decisão", requestError.message || "Tente novamente.");
    } finally {
      setIsModerationActionLoading(false);
    }
  }

  async function handleContentReportStatusChange(reportId, status) {
    if (!reportId) {
      return;
    }

    setIsModerationActionLoading(true);
    try {
      const updated = await updateContentReportStatus(reportId, status);
      setSelectedAdminReportId(updated.id);
      await refreshAdminModerationData();
      openFeedback("success", t("admin.moderation.reports.statusUpdated.title"), t("admin.moderation.reports.statusUpdated.message"));
    } catch (requestError) {
      openFeedback("error", t("admin.moderation.reports.statusError.title"), requestError.message || t("errors.retry"));
    } finally {
      setIsModerationActionLoading(false);
    }
  }

  async function handlePurchaseProduct(productCode, paymentMethod = "MERCADO_PAGO") {
    if (!creditPurchasesEnabled) {
      openFeedback("error", "Compra indisponível", "A compra de créditos e planos está desabilitada no momento.");
      return;
    }
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
          : "Agora vamos monitorar procuras compatíveis com ele."
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
      openFeedback("success", "Item pausado", "Você pode reativar este item quando quiser.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível desativar", requestError.message || "Tente novamente.");
    }
  }

  async function handleActivateSellerItem(itemId) {
    if (!itemId) {
      return;
    }

    try {
      await activateSellerItem(itemId);
      await refreshPrivateData();
      openFeedback("success", "Item ativado", "Seu item voltou a participar dos cruzamentos com procuras.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível ativar", requestError.message || "Tente novamente.");
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
      openFeedback("success", "Proposta enviada", "Sua proposta foi enviada usando o item cadastrado.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível compartilhar", requestError.message || "Tente novamente.");
    } finally {
      setSharingSellerItemInterestId(null);
    }
  }

  async function handleBoostInterest(boostCode, interestId = selectedInterest?.id, paymentMethod = "MERCADO_PAGO") {
    if (!boostPurchasesEnabled) {
      openFeedback("error", "Boost indisponível", "A compra de boosts está desabilitada no momento.");
      return;
    }
    if (!interestId) {
      return;
    }

    const product = boostProducts.find((item) => item.code === boostCode);
    setIsProcessingPurchase(true);
    try {
      const checkout = await boostInterest(interestId, { boostCode, paymentMethod });
      const checkoutUrl = checkout.checkoutUrl ?? "";
      const isExternalCheckout = checkoutUrl && !checkoutUrl.startsWith("local://");

      if (isExternalCheckout) {
        setPaymentStatus({
          productCode: boostCode,
          productName: product?.name ?? boostCode,
          paymentMethod: checkout.paymentMethod ?? paymentMethod,
          provider: checkout.provider ?? "MERCADO_PAGO_CHECKOUT_PRO",
          checkoutUrl,
          step: "PAYMENT",
          message: checkout.message || "Finalize o pagamento para ativar o boost."
        });
        openFeedback("success", "Checkout criado", "Voce sera direcionado para concluir o pagamento do boost.");
        window.location.assign(checkoutUrl);
        return;
      }

      await Promise.all([refreshPrivateData(), refreshPublicData(), loadInterestDetail(interestId, { updateUrl: false })]);
      openFeedback("success", "Boost ativado", checkout.message || "Seu interesse foi impulsionado com sucesso.");
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
        creditPurchasesEnabled
          ? "Você precisa de 1 crédito para renovar a procura. Abra a página de créditos para comprar."
          : "Você precisa de 1 crédito para renovar a procura."
      );
      if (creditPurchasesEnabled) {
        navigateTo(loggedSections.CREDITS);
      }
      return;
    }

    try {
      await renewInterest(interestId);
      await Promise.all([refreshPrivateData(), refreshPublicData(), loadInterestDetail(interestId, { updateUrl: false })]);
      openFeedback("success", "Procura renovada", `Sua procura ganhou mais ${LISTING_EXPIRATION_DAYS} dias.`);
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
          [offerId]: latestIncoming,
          [`offer:${offerId}`]: latestIncoming,
          [`new-offer:${offerId}`]: latestIncoming
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
    markNotificationsSeen(notification);

    if (notification.type === "seller-item-match") {
      if (notification.sellerItemId) {
        setSelectedSellerItemId(notification.sellerItemId);
      }
      const matchedGroup = sellerItems.find((group) => group.item?.id === notification.sellerItemId);
      const matchingInterests = (matchedGroup?.matchingInterests ?? [])
        .filter((interest) => !sameEntityId(interest.ownerId, currentUser?.id));
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

    markNotificationsSeen(notifications, { clear: true });
  }

  function renderPublicHome(showHero = true) {
    const canShowRestrictedInterestDetails = isSelectedInterestMine;

    return (
      <>
        {showHero ? (
          <section className="hero hero--public">
            <div className="hero__copy">
              <h1>{t("home.hero.title")}</h1>
              <p>
                {t("home.hero.description")}
              </p>
              <p className="hero__supporting">
                {t("home.hero.complement")}
              </p>
              <div className="hero__actions">
                <button type="button" className="primary-button" onClick={() => openAuthModal("register")}>
                  {t("home.hero.primary")}
                </button>
                <button type="button" className="ghost-button" onClick={() => openAuthModal("login")}>
                  {t("home.hero.secondary")}
                </button>
              </div>
            </div>

            <div className="hero__aside">
              <div className="hero-card">
                <strong>{t("home.hero.card1.title")}</strong>
                <p>{t("home.hero.card1.description")}</p>
                <button type="button" className="primary-button primary-button--compact" onClick={() => openAuthModal("register")}>
                  {t("home.hero.card1.cta")}
                </button>
              </div>
              <div className="hero-card">
                <strong>{t("home.hero.card2.title")}</strong>
                <p>{t("home.hero.card2.description")}</p>
                <button type="button" className="ghost-button" onClick={() => openAuthModal("register")}>
                  {t("home.hero.card2.cta")}
                </button>
              </div>
            </div>
          </section>
        ) : null}

        {showHero ? (
          <section className="how-it-works-section">
            <div className="panel__header">
              <div>
                <span className="eyebrow">{t("home.how.eyebrow")}</span>
                <h2>{t("home.how.title")}</h2>
              </div>
            </div>

            <div className="how-it-works-grid">
              {[1, 2, 3].map((step) => (
                <article key={step} className="how-it-works-card">
                  <span className="how-it-works-card__number">{step}</span>
                  <strong>{t(`home.how.step${step}.title`)}</strong>
                  <p>{t(`home.how.step${step}.description`)}</p>
                </article>
              ))}
            </div>

            <article className="how-it-works-secondary">
              <div>
                <strong>{t("home.how.secondary.title")}</strong>
                <p>{t("home.how.secondary.description")}</p>
              </div>
              <button type="button" className="ghost-button" onClick={() => openAuthModal("register")}>
                {t("home.how.secondary.cta")}
              </button>
            </article>
          </section>
        ) : null}

        <section className="workspace-grid">
          <article className="panel panel--wide">
              <div className="panel__header">
                <div>
                  <span className="eyebrow">{t("home.panel.eyebrow")}</span>
                  <h2>{t("home.panel.title")}</h2>
                </div>
                <div className="panel__header-note">{t("home.panel.note")}</div>
              </div>

            <div className="filters">
              <input
                value={filters.query}
                onChange={(event) =>
                  updateHomeFilters((current) => ({ ...current, query: event.target.value }))
                }
                placeholder={t("home.filters.query")}
              />

              <select
                value={filters.category}
                onChange={(event) =>
                  updateHomeFilters((current) => ({ ...current, category: event.target.value }))
                }
              >
                <option value="">{t("home.filters.category.all")}</option>
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
                placeholder={t("home.filters.city")}
              />

              <input
                type="number"
                min="0"
                value={filters.maxBudget}
                onChange={(event) =>
                  updateHomeFilters((current) => ({ ...current, maxBudget: event.target.value }))
                }
                placeholder={t("home.filters.maxBudget")}
              />
            </div>

            {homeMatchFilter ? (
              <div className="context-filter-card">
                <div>
                  <span className="eyebrow">{t("home.itemFilter.eyebrow")}</span>
                  <strong>{t("home.itemFilter.showing", { title: homeMatchFilter.sellerItemTitle })}</strong>
                </div>
                <button type="button" className="text-button" onClick={clearHomeMatchFilter}>
                  {t("home.itemFilter.clear")}
                </button>
              </div>
            ) : null}

            {isLoadingPublic && !homeMatchFilter ? (
              <div className="loading-card">{t("home.loading.published")}</div>
            ) : visibleHomeInterests.length === 0 ? (
              <EmptyState
                title={homeMatchFilter ? t("home.empty.match.title") : (session ? t("home.empty.logged.title") : t("home.empty.public.title"))}
                description={
                  homeMatchFilter
                    ? t("home.empty.match.description")
                    : session
                    ? t("home.empty.logged.description")
                    : t("home.empty.public.description")
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
                    {isLoadingMorePublic ? t("common.actions.loading") : t("common.actions.loadMore")}
                  </button>
                ) : null}
              </>
            )}
          </article>

          <aside ref={publicInterestDetailRef} className="panel panel--sticky">
            <div className="panel__header">
              <div className="panel-title-stack">
                <span className="eyebrow detail-owner-line">
                  {selectedInterest ? (
                    canShowRestrictedInterestDetails ? (
                      <span>{t("interest.detail.owner.line", { name: firstName(selectedInterest.ownerName, t) })}</span>
                    ) : (
                      <span>{t("interest.detail.publicEyebrow")}</span>
                    )
                  ) : t("interest.detail.select")}
                </span>
                <h2 className="title-with-badge">
                  {selectedInterest?.title ?? t("interest.detail.select")}
                  {isBoostActive(selectedInterest) ? <BoostRocket /> : null}
                </h2>
              </div>
            </div>

            {isLoadingInterestDetail ? (
              <div className="loading-card">{t("interest.detail.loading")}</div>
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
                    <span>{t("interest.detail.category")}</span>
                    <strong>{selectedInterest.category}</strong>
                  </div>
                  <div className="detail-row">
                    <span>{t("interest.detail.location")}</span>
                    <strong>
                      {selectedInterest.location?.city}/{selectedInterest.location?.state}
                    </strong>
                  </div>
                  {canShowRestrictedInterestDetails ? (
                    <>
                      <div className="detail-row">
                        <span>{t("interest.detail.priceRange")}</span>
                        <strong>
                          {t("interest.detail.priceRangeValue", { min: currency(selectedInterest.budgetMin, t), max: currency(selectedInterest.budgetMax, t) })}
                        </strong>
                      </div>
                      <div className="detail-row">
                        <span>{t("interest.detail.owner")}</span>
                        <strong>{selectedInterest.ownerName}</strong>
                      </div>
                    </>
                  ) : null}
                  {isSelectedInterestMine ? (
                    <div className="detail-row">
                      <span>{t("interest.detail.status")}</span>
                      <strong className={`moderation-badge moderation-badge--${moderationStatusTone(selectedInterest.status)}`}>
                        {moderationStatusLabel(selectedInterest.status, t)}
                      </strong>
                    </div>
                  ) : null}
                  {isSelectedInterestMine ? (
                    <div className="detail-row">
                      <span>{t("interest.detail.remaining")}</span>
                      <strong className={expiryPillClass(selectedInterest)}>
                        {isListingExpiringSoon(selectedInterest) ? "⚠ " : ""}
                        {formatRemainingListingTime(selectedInterest, t)}
                      </strong>
                    </div>
                  ) : null}
                  {canShowRestrictedInterestDetails && selectedInterest.allowsWhatsappContact && selectedInterest.whatsappContact ? (
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
                        openFeedback("info", t("interest.report.loginRequired.title"), t("interest.report.loginRequired.message"));
                        openAuthModal("login");
                        return;
                      }
                      openReportModal(selectedInterest);
                    }}
                  >
                    <span aria-hidden="true">⚠</span>
                    <span>{t("interest.detail.report")}</span>
                  </button>
                ) : null}

                {session ? (
                  isSelectedInterestMine ? (
                    <div className="cta-card">
                      <strong>{t("interest.detail.own.title")}</strong>
                      <p>{t("interest.detail.own.description")}</p>
                      <button
                        type="button"
                        className="primary-button"
                        onClick={() => navigateTo(loggedSections.MY_INTERESTS)}
                      >
                        {t("interest.detail.own.cta")}
                      </button>
                    </div>
                  ) : sentOfferForSelectedInterest ? (
                    renderSentOfferSummary(sentOfferForSelectedInterest)
                  ) : (
                    <form className="stacked-form" onSubmit={handleOfferSubmit}>
                      <div className="form-heading">
                        <span className="eyebrow">{t("offer.form.eyebrow")}</span>
                        <h3>{t("offer.form.title")}</h3>
                        <p>{t("offer.form.description")}</p>
                      </div>
                      <input
                        type="number"
                        min="0"
                        placeholder={t("offer.form.price")}
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
                        placeholder={t("offer.form.phone")}
                        value={offerForm.sellerPhone}
                        onChange={(event) =>
                          setOfferForm((current) => ({
                            ...current,
                            sellerPhone: event.target.value
                          }))
                        }
                      />
                      <textarea
                        rows="4"
                        placeholder={t("offer.form.message")}
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
                        placeholder={t("offer.form.highlights")}
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
                        <span>{t("offer.form.delivery")}</span>
                      </label>
                      <button
                        type="submit"
                        className="primary-button"
                        disabled={isSubmittingOffer || !canSendOffer}
                        title={!canSendOffer ? noCreditsTooltip : undefined}
                      >
                        {isSubmittingOffer ? t("offer.form.submitting") : t("offer.form.submit")}
                      </button>
                      {!canSendOffer ? <p className="form-note">{noCreditsTooltip}</p> : null}
                    </form>
                  )
                ) : (
                  <div className="cta-card">
                    <strong>{t("interest.detail.guest.title")}</strong>
                    <p>{t("interest.detail.guest.description")}</p>
                    <div className="cta-card__actions">
                      <button type="button" className="primary-button" onClick={() => openAuthModal("login")}>
                        {t("common.actions.login")}
                      </button>
                      <button type="button" className="ghost-button" onClick={() => openAuthModal("register")}>
                        {t("common.actions.createAccount")}
                      </button>
                    </div>
                  </div>
                )}
              </>
            ) : (
              <EmptyState
                title={t("interest.detail.empty.title")}
                description={t("interest.detail.empty.description")}
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
        className={`accordion-card ${isSelected ? "accordion-card--selected" : ""} ${interest.status === "CLOSED" ? "accordion-card--inactive" : ""}`}
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
                  {moderationStatusLabel(interest.status, t)}
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
              <span>{currency(interest.budgetMax, t)}</span>
              <span>{interest.tags?.slice(0, 3).join(" • ") || "Sem tags"}</span>
              <span className={`${expiryPillClass(interest)} expiry-pill--inline`}>
                {isListingExpiringSoon(interest) ? "⚠ " : ""}
                {formatRemainingListingTime(interest, t)}
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
    const receivedSecondaryLabel = t("offer.list.receivedSecondary", { price: currency(offer.offeredPrice, t), title: offer.interestTitle ?? t("offer.fallback.interest") });
    const secondaryLabel = isIncomingOffer
      ? t("offer.list.incomingSecondary", { price: currency(offer.offeredPrice, t), date: formatTimestamp(offer.createdAt, t) })
      : t("offer.list.outgoingSecondary", { price: currency(offer.offeredPrice, t), seller: offer.sellerName ?? t("offer.fallback.seller") });

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
              <span>{offer.sellerPhone || formatTimestamp(offer.createdAt, t)}</span>
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
          <span className="eyebrow">Proposta enviada</span>
          <h3>Você já respondeu esta procura</h3>
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
            <span>Valor da proposta</span>
            <strong>{currency(offer.offeredPrice, t)}</strong>
          </div>
          <div>
            <span>Contato informado</span>
            <strong>{offer.sellerPhone || "Não informado"}</strong>
          </div>
          <div>
            <span>Enviada em</span>
            <strong>{formatTimestamp(offer.createdAt, t)}</strong>
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
            Ver propostas enviadas
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
                    <span>{formatTimestamp(payment.createdAt, t)}</span>
                  </div>
                  <div>
                    <span>{paymentMethodLabel(payment.paymentMethod, t)}</span>
                    <small>
                      {payment.provider === "MERCADO_PAGO_CHECKOUT_PRO"
                        ? "Mercado Pago"
                        : payment.provider || "Mercado Pago"}
                    </small>
                  </div>
                  <strong>{currency(payment.amount, t)}</strong>
                  <span className={`payment-status-pill payment-status-pill--${tone}`}>
                    {paymentStatusLabel(payment.status, t)}
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
    if (!creditPurchasesEnabled) {
      return (
        <section className="panel panel--spaced credits-page">
          <div className="panel__header">
            <div>
              <span className="eyebrow">Página</span>
              <h2>Monetização indisponível</h2>
            </div>
            <button type="button" className="ghost-button" onClick={() => navigateTo(loggedSections.EXPLORE)}>
              Voltar para home
            </button>
          </div>
          {renderPaymentTracker()}
          {renderPaymentHistory()}
        </section>
      );
    }

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
                    : `${product.credits} propostas para responder procuras de compradores.`;

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
                      <span className="purchase-option__price"><ProductPrice product={product} /></span>
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
                  ? t("credits.purchase.total", { total: currency(selectedPurchaseProduct.price, t) })
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
          <span className="eyebrow">{editingSellerItemId ? t("sellerItem.form.editTitle") : t("sellerItem.form.createTitle")}</span>
          <h3>{editingSellerItemId ? t("sellerItem.form.editSubtitle") : t("sellerItem.form.createSubtitle")}</h3>
        </div>
        <input
          placeholder={t("sellerItem.form.title.placeholder")}
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
          placeholder={t("sellerItem.form.description.placeholder")}
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
            placeholder={t("sellerItem.form.price.placeholder")}
            value={sellerItemForm.desiredPrice}
            onChange={(event) => setSellerItemForm((current) => ({ ...current, desiredPrice: event.target.value }))}
          />
        </div>

        <div className="three-columns">
          <input
            placeholder={t("address.postalCode.placeholder")}
            value={sellerItemForm.postalCode}
            onChange={(event) =>
              setSellerItemForm((current) => ({ ...current, postalCode: formatCep(event.target.value) }))
            }
            onBlur={() => handlePostalCodeLookup("sellerItem", sellerItemForm.postalCode, (address) => {
              setSellerItemForm((current) => ({
                ...current,
                postalCode: address.postalCode ?? current.postalCode,
                city: address.city ?? current.city,
                state: address.state ?? current.state,
                neighborhood: address.neighborhood ?? current.neighborhood,
                country: address.country ?? current.country
              }));
            })}
          />
          <input
            placeholder={t("auth.register.city.placeholder")}
            value={sellerItemForm.city}
            onChange={(event) => setSellerItemForm((current) => ({ ...current, city: event.target.value }))}
          />
          <input
            placeholder={t("auth.register.state.placeholder")}
            value={sellerItemForm.state}
            onChange={(event) => setSellerItemForm((current) => ({ ...current, state: event.target.value }))}
          />
          <input
            placeholder={t("interest.form.neighborhood.placeholder")}
            value={sellerItemForm.neighborhood}
            onChange={(event) => setSellerItemForm((current) => ({ ...current, neighborhood: event.target.value }))}
          />
        </div>
        {addressLookupState.sellerItem.message ? (
          <small
            className={`address-lookup-note ${addressLookupState.sellerItem.isLoading ? "is-loading" : ""}`}
            role="status"
            aria-live="polite"
            aria-busy={addressLookupState.sellerItem.isLoading}
          >
            {addressLookupState.sellerItem.message}
          </small>
        ) : null}

        <input
          placeholder={t("interest.form.tags.placeholder")}
          value={sellerItemForm.tags}
          onChange={(event) => setSellerItemForm((current) => ({ ...current, tags: event.target.value }))}
        />

        <div className="media-field">
          <label htmlFor="seller-item-image">{t("sellerItem.form.image.label")}</label>
          <input id="seller-item-image" type="file" accept="image/*" onChange={handleSellerItemImageChange} />
          {sellerItemForm.referenceImageUrl ? (
            <img
              className="interest-upload-preview"
              src={sellerItemForm.referenceImageUrl}
              alt={t("sellerItem.form.image.previewAlt")}
              decoding="async"
            />
          ) : null}
        </div>

        <div className="form-actions">
          {editingSellerItemId ? (
            <button type="button" className="ghost-button" onClick={cancelSellerItemEditing}>
              {t("interest.form.cancelEdit")}
            </button>
          ) : null}
          <button type="submit" className="primary-button" disabled={isSubmittingSellerItem}>
            {isSubmittingSellerItem
              ? (editingSellerItemId ? t("common.actions.saving") : t("common.actions.registering"))
              : (editingSellerItemId ? t("interest.form.saveChanges") : t("sellerItem.form.submit"))}
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
              <span className="eyebrow">{t("interest.form.eyebrow")}</span>
              <h2>Itens que posso negociar</h2>
            </div>
          </div>

          <div className="cta-card seller-item-create-card">
            <strong>{t("sellerItem.createCard.title")}</strong>
            <p>{t("sellerItem.createCard.description")}</p>
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
                {t("sellerItem.form.submit")}
              </button>
            </div>
          </div>
        </article>

        <aside className="panel panel--sticky">
          <div className="panel__header">
            <div>
              <span className="eyebrow">Tenho para negociar</span>
              <h2>{selectedItem?.title ?? "Cadastre um item disponível"}</h2>
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
                      {!group.item.active ? <em>Pausado</em> : null}
                    </span>
                    <small
                      className="seller-match-count"
                      onClick={(event) => {
                        event.stopPropagation();
                        openSellerItemMatches(group);
                      }}
                    >
                      <strong>{group.matchCount}</strong>
                      <span>pessoas procurando<br />algo parecido</span>
                    </small>
                  </button>
                ))}
              </div>

              {selectedItem ? (
                <div className="seller-item-summary">
                  <div className="seller-item-summary__content">
                    <strong>{currency(selectedItem.desiredPrice, t)}</strong>
                    {!selectedItem.active ? <span className="seller-item-status-badge">Pausado</span> : null}
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
                        Pausar item
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="primary-button primary-button--compact"
                        onClick={() => handleActivateSellerItem(selectedItem.id)}
                      >
                        Ativar item
                      </button>
                    )}
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
                        <span>{currency(interest.budgetMax, t)}</span>
                      </div>
                      <button
                        type="button"
                        className="primary-button primary-button--compact"
                        disabled={shareDisabled || sharingSellerItemInterestId === interest.id}
                        title={!canSendOffer ? noCreditsTooltip : !sellerItemShareForm.sellerPhone.trim() ? "Informe um telefone para enviar a proposta." : undefined}
                        onClick={() => handleShareSellerItem(selectedItem.id, interest)}
                      >
                        {sharingSellerItemInterestId === interest.id ? "Enviando..." : "Compartilhar item"}
                      </button>
                    </article>
                  ))}
                </div>
              ) : (
                <EmptyState
                  title="Nenhuma procura compatível ainda"
                  description="Quando alguém procurar algo parecido com este item, a procura aparecerá aqui."
                />
              )}
            </>
          ) : (
            <EmptyState
              title="Nenhum item disponível cadastrado"
              description="Cadastre um item ou serviço para descobrir pessoas procurando algo parecido."
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
                  <span className="eyebrow">{t("dashboard.nav.sellerItems")}</span>
                  <h2 id="seller-item-form-title">{editingSellerItemId ? t("sellerItem.form.editTitle") : t("sellerItem.form.createTitle")}</h2>
                </div>
                <button
                  type="button"
                  className="modal-close-button"
                  onClick={cancelSellerItemEditing}
                  aria-label={t("common.actions.closeModal")}
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

  function toggleAdminSection(sectionKey) {
    setCollapsedAdminSections((current) => ({
      ...current,
      [sectionKey]: !current[sectionKey]
    }));
  }

  function renderAdminSection({ sectionKey, eyebrow, title, count = 0, priority = false, children }) {
    const isCollapsed = Boolean(collapsedAdminSections[sectionKey]);

    return (
      <article className={`admin-card admin-collapsible ${priority ? "admin-card--priority" : ""} ${isCollapsed ? "admin-collapsible--collapsed" : ""}`}>
        <button
          type="button"
          className="admin-collapsible__header"
          onClick={() => toggleAdminSection(sectionKey)}
          aria-expanded={!isCollapsed}
        >
          <span className="admin-collapsible__title">
            <span className="eyebrow">{eyebrow}</span>
            <strong>{title}</strong>
          </span>
          <span className="admin-collapsible__meta">
            {isCollapsed && count > 0 ? <span className="admin-section-badge">{count}</span> : null}
            <span aria-hidden="true">{isCollapsed ? "+" : "−"}</span>
          </span>
        </button>
        {isCollapsed ? null : <div className="admin-collapsible__body">{children}</div>}
      </article>
    );
  }

  function renderAdminModerationPage() {
    const pendingInterests = adminModeration?.pendingInterests ?? [];
    const rules = adminModeration?.rules ?? [];
    const openReports = adminModeration?.openReports ?? [];
    const processedReports = adminModeration?.processedReports ?? [];
    const allReports = [...openReports, ...processedReports];
    const selectedAdminReport = allReports.find((report) => report.id === selectedAdminReportId) ?? openReports[0] ?? processedReports[0] ?? null;
    const newOmbudsmanCount = adminOmbudsmanRequests.filter((requestItem) =>
      requestItem.status === "OPEN" && !requestItem.adminResponse
    ).length;

    const renderReportItem = (report) => (
      <button
        key={report.id}
        type="button"
        className={`admin-list-item admin-list-item--button ${selectedAdminReport?.id === report.id ? "selected" : ""}`}
        onClick={() => setSelectedAdminReportId(report.id)}
      >
        <div>
          <strong>{report.reason}</strong>
          <span>{report.contentTitle || t("admin.moderation.reportsContent", { id: report.contentId })}</span>
          <small>{contentReportStatusLabel(report.status, t)} · {formatTimestamp(report.createdAt, t)}</small>
        </div>
      </button>
    );

    return (
      <section className="admin-moderation panel panel--spaced">
        <div className="panel__header">
          <div>
            <span className="eyebrow">{t("admin.nav")}</span>
            <h2>{t("admin.moderation.title")}</h2>
          </div>
          <button
            type="button"
            className="ghost-button ghost-button--small"
            onClick={() => refreshAdminModerationData().catch((requestError) => {
              openFeedback("error", t("admin.moderation.refreshError.title"), requestError.message || t("errors.retry"));
            })}
          >
            {t("common.actions.update")}
          </button>
        </div>

        {renderAdminSection({
          sectionKey: "moderationQueue",
          eyebrow: t("admin.moderation.queue"),
          title: t("admin.moderation.queueCount", { count: pendingInterests.length }),
          count: pendingInterests.length,
          priority: true,
          children: (
          <div className="admin-list">
            {pendingInterests.length ? pendingInterests.map((interest) => (
              <article key={interest.id} className="admin-list-item admin-list-item--stacked">
                <div>
                  <strong>{interest.title}</strong>
                  <span>{moderationStatusLabel(interest.status, t)} · {interest.category}</span>
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
                    {t("admin.moderation.approve")}
                  </button>
                  <button
                    type="button"
                    className="ghost-button moderation-action-button"
                    disabled={isModerationActionLoading}
                    onClick={() => handleModerationDecision(interest.id, "HIDDEN")}
                  >
                    {t("admin.moderation.hide")}
                  </button>
                  <button
                    type="button"
                    className="danger-button moderation-action-button"
                    disabled={isModerationActionLoading}
                    onClick={() => handleModerationDecision(interest.id, "REJECTED")}
                  >
                    {t("admin.moderation.reject")}
                  </button>
                </div>
              </article>
            )) : (
              <EmptyState title={t("admin.moderation.empty.title")} description={t("admin.moderation.empty.description")} />
            )}
          </div>
          )
        })}

        <div className="admin-grid">
          {renderAdminSection({
            sectionKey: "moderationRules",
            eyebrow: t("admin.moderation.rules"),
            title: moderationRuleForm.id ? t("admin.moderation.editRule") : t("admin.moderation.newRule"),
            children: (
              <>
            <form className="stacked-form" onSubmit={handleModerationRuleSubmit}>
              <input
                placeholder={t("admin.moderation.rule.placeholder")}
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
                  <option value="HIGH">{t("admin.moderation.rule.high")}</option>
                  <option value="MEDIUM">{t("admin.moderation.rule.medium")}</option>
                  <option value="LOW">{t("admin.moderation.rule.low")}</option>
                </select>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={moderationRuleForm.active}
                    onChange={(event) =>
                      setModerationRuleForm((current) => ({ ...current, active: event.target.checked }))
                    }
                  />
                  <span>{t("admin.moderation.rule.active")}</span>
                </label>
              </div>
              <div className="inline-actions">
                {moderationRuleForm.id ? (
                  <button type="button" className="ghost-button ghost-button--small" onClick={() => setModerationRuleForm(initialModerationRuleForm)}>
                    {t("common.actions.cancel")}
                  </button>
                ) : null}
                <button type="submit" className="primary-button primary-button--compact" disabled={isSubmittingModerationRule}>
                  {isSubmittingModerationRule ? t("common.actions.saving") : t("admin.moderation.rule.save")}
                </button>
              </div>
            </form>

            <div className="admin-list">
              {rules.length ? rules.map((rule) => (
                <article key={rule.id} className="admin-list-item">
                  <div>
                    <strong>{rule.term}</strong>
                    <span>{rule.riskLevel} · {rule.active ? t("common.status.active") : t("common.status.inactive")}</span>
                  </div>
                  <div className="inline-actions">
                    <button type="button" className="ghost-button ghost-button--small" onClick={() => startEditingModerationRule(rule)}>
                      {t("common.actions.edit")}
                    </button>
                    <button type="button" className="danger-button action-button--compact" disabled={isModerationActionLoading} onClick={() => handleDeleteModerationRule(rule.id)}>
                      {t("common.actions.remove")}
                    </button>
                  </div>
                </article>
              )) : (
                <EmptyState title={t("admin.moderation.rule.empty.title")} description={t("admin.moderation.rule.empty.description")} />
              )}
            </div>
              </>
            )
          })}

          {renderAdminSection({
            sectionKey: "reports",
            eyebrow: t("admin.moderation.reports"),
            title: t("admin.moderation.reportsCount", { count: openReports.length }),
            count: openReports.length,
            children: (
            <div className="admin-reports">
              <div className="admin-reports__lists">
                <section className="admin-reports__group">
                  <div className="admin-reports__group-header">
                    <strong>{t("admin.moderation.reports.open.title")}</strong>
                    <span className="admin-section-badge">{openReports.length}</span>
                  </div>
                  <div className="admin-list">
                    {openReports.length ? openReports.map(renderReportItem) : (
                      <EmptyState title={t("admin.moderation.reports.empty.title")} description={t("admin.moderation.reports.empty.description")} />
                    )}
                  </div>
                </section>
                <section className="admin-reports__group">
                  <div className="admin-reports__group-header">
                    <strong>{t("admin.moderation.reports.processed.title")}</strong>
                    <span className="admin-section-badge">{processedReports.length}</span>
                  </div>
                  <div className="admin-list">
                    {processedReports.length ? processedReports.map(renderReportItem) : (
                      <EmptyState title={t("admin.moderation.reports.processed.empty.title")} description={t("admin.moderation.reports.processed.empty.description")} />
                    )}
                  </div>
                </section>
              </div>
              <aside className="admin-report-detail">
                {selectedAdminReport ? (
                  <>
                    <div>
                      <span className="eyebrow">{t("admin.moderation.reports.detail.eyebrow")}</span>
                      <h3>{selectedAdminReport.reason}</h3>
                      <p>{selectedAdminReport.message || t("admin.moderation.reports.detail.noMessage")}</p>
                    </div>
                    <dl>
                      <div>
                        <dt>{t("admin.moderation.reports.detail.content")}</dt>
                        <dd>{selectedAdminReport.contentTitle || selectedAdminReport.contentId}</dd>
                      </div>
                      {selectedAdminReport.contentDescription ? (
                        <div>
                          <dt>{t("admin.moderation.reports.detail.description")}</dt>
                          <dd>{selectedAdminReport.contentDescription}</dd>
                        </div>
                      ) : null}
                      <div>
                        <dt>{t("admin.moderation.reports.detail.status")}</dt>
                        <dd>
                          {contentReportStatusLabel(selectedAdminReport.status, t)}
                          {selectedAdminReport.contentStatus ? ` · ${moderationStatusLabel(selectedAdminReport.contentStatus, t)}` : ""}
                        </dd>
                      </div>
                      <div>
                        <dt>{t("admin.moderation.reports.detail.reportedBy")}</dt>
                        <dd>{selectedAdminReport.reportedBy || t("common.status.unavailable")}</dd>
                      </div>
                      <div>
                        <dt>{t("admin.moderation.reports.detail.createdAt")}</dt>
                        <dd>{formatTimestamp(selectedAdminReport.createdAt, t)}</dd>
                      </div>
                      {selectedAdminReport.reviewedAt ? (
                        <div>
                          <dt>{t("admin.moderation.reports.detail.reviewedAt")}</dt>
                          <dd>{formatTimestamp(selectedAdminReport.reviewedAt, t)}</dd>
                        </div>
                      ) : null}
                    </dl>
                    {selectedAdminReport.status === "OPEN" ? (
                      <div className="inline-actions">
                        <button
                          type="button"
                          className="primary-button primary-button--compact"
                          disabled={isModerationActionLoading}
                          onClick={() => handleContentReportStatusChange(selectedAdminReport.id, "RESOLVED")}
                        >
                          {t("admin.moderation.reports.resolve")}
                        </button>
                        <button
                          type="button"
                          className="ghost-button ghost-button--small"
                          disabled={isModerationActionLoading}
                          onClick={() => handleContentReportStatusChange(selectedAdminReport.id, "DISMISSED")}
                        >
                          {t("admin.moderation.reports.dismiss")}
                        </button>
                      </div>
                    ) : null}
                  </>
                ) : (
                  <EmptyState title={t("admin.moderation.reports.detail.empty.title")} description={t("admin.moderation.reports.detail.empty.description")} />
                )}
              </aside>
            </div>
            )
          })}
        </div>
        {renderAdminSection({
          sectionKey: "ombudsman",
          eyebrow: "Ouvidoria",
          title: "Manifestações recebidas",
          count: newOmbudsmanCount,
          children: renderAdminOmbudsmanPanel()
        })}
        {renderAdminSection({
          sectionKey: "contentCrm",
          eyebrow: "Conteúdo",
          title: "CRM de conteúdo",
          children: <ContentAdminPanel onFeedback={openFeedback} />
        })}
        {renderAdminSection({
          sectionKey: "catalogCrm",
          eyebrow: "Catálogo",
          title: "CRM operacional",
          children: <OperationalCatalogAdminPanel onFeedback={openFeedback} />
        })}
      </section>
    );
  }

  function renderOmbudsmanPage() {
    return (
      <section className="ombudsman-page panel panel--spaced">
        <div className="panel__header">
          <div>
            <span className="eyebrow">Ouvidoria</span>
            <h2>Fale com a Ouvidoria do Eu Procuro</h2>
            <p className="panel__header-note">
              Use este canal para reclamações formais, contestação de moderação, problemas com pagamento ou sugestões.
            </p>
          </div>
        </div>

        {ombudsmanProtocol ? (
          <div className="success-callout">
            <strong>Manifestação registrada</strong>
            <span>Protocolo: {ombudsmanProtocol}</span>
          </div>
        ) : null}

        <form className="stacked-form ombudsman-form" onSubmit={handleOmbudsmanSubmit}>
          <div className="form-grid">
            <input
              placeholder="Nome"
              value={ombudsmanForm.name}
              onChange={(event) => updateOmbudsmanForm("name", event.target.value)}
              maxLength={120}
              required
            />
            <input
              type="email"
              placeholder="E-mail"
              value={ombudsmanForm.email}
              onChange={(event) => updateOmbudsmanForm("email", event.target.value)}
              maxLength={120}
              required
            />
            <select
              value={ombudsmanForm.type}
              onChange={(event) => updateOmbudsmanForm("type", event.target.value)}
              required
            >
              {OMBUDSMAN_TYPES.map((type) => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>
            <input
              placeholder="Assunto"
              value={ombudsmanForm.subject}
              onChange={(event) => updateOmbudsmanForm("subject", event.target.value)}
              maxLength={140}
              required
            />
            <input
              placeholder="Tipo de referência (opcional)"
              value={ombudsmanForm.relatedEntityType}
              onChange={(event) => updateOmbudsmanForm("relatedEntityType", event.target.value)}
              maxLength={120}
            />
            <input
              placeholder="ID relacionado (opcional)"
              value={ombudsmanForm.relatedEntityId}
              onChange={(event) => updateOmbudsmanForm("relatedEntityId", event.target.value)}
              maxLength={120}
            />
          </div>
          <div className="field-with-counter">
            <textarea
              placeholder="Descreva sua manifestação"
              value={ombudsmanForm.message}
              onChange={(event) => updateOmbudsmanForm("message", event.target.value)}
              maxLength={2000}
              rows={8}
              required
            />
            <FieldCounter value={ombudsmanForm.message} max={2000} />
          </div>
          <label className="checkbox-row checkbox-row--panel">
            <input
              type="checkbox"
              checked={ombudsmanForm.truthDeclarationAccepted}
              onChange={(event) => updateOmbudsmanForm("truthDeclarationAccepted", event.target.checked)}
              required
            />
            <span>Declaro que as informações enviadas são verdadeiras.</span>
          </label>
          <button type="submit" className="primary-button primary-button--compact" disabled={isSubmittingOmbudsman}>
            {isSubmittingOmbudsman ? "Enviando..." : "Enviar manifestação"}
          </button>
        </form>
      </section>
    );
  }

  function renderAdminOmbudsmanPanel() {
    const newRequests = adminOmbudsmanRequests.filter((requestItem) =>
      requestItem.status === "OPEN" && !requestItem.adminResponse
    );
    const inProgressRequests = adminOmbudsmanRequests.filter((requestItem) =>
      requestItem.status !== "CLOSED" && (requestItem.status === "IN_REVIEW" || requestItem.status === "ANSWERED" || requestItem.adminResponse)
    );
    const closedRequests = adminOmbudsmanRequests.filter((requestItem) => requestItem.status === "CLOSED");

    const renderOmbudsmanRequest = (requestItem) => (
      <article key={requestItem.id} className="admin-list-item admin-list-item--stacked ombudsman-admin-item">
        <div>
          <strong>{requestItem.protocol} · {requestItem.subject}</strong>
          <span>{requestItem.type} · {requestItem.status} · {formatTimestamp(requestItem.createdAt, t)}</span>
          <p>{requestItem.message}</p>
          <small>{requestItem.name} · {requestItem.email}</small>
          {requestItem.relatedEntityId ? (
            <small>Referência: {requestItem.relatedEntityType || "item"} · {requestItem.relatedEntityId}</small>
          ) : null}
          {requestItem.adminResponse ? (
            <div className="admin-response-box">
              <strong>Resposta enviada</strong>
              <p>{requestItem.adminResponse}</p>
            </div>
          ) : null}
        </div>
        <div className="ombudsman-admin-actions">
          <label className="compact-field-label">
            <span>Status</span>
            <select
              value={requestItem.status}
              onChange={(event) => handleOmbudsmanStatusChange(requestItem.id, event.target.value)}
            >
              <option value="OPEN">Aberta</option>
              <option value="IN_REVIEW">Em atendimento</option>
              <option value="ANSWERED">Respondida</option>
              <option value="CLOSED">Fechada</option>
            </select>
          </label>
          <textarea
            placeholder="Resposta da Ouvidoria"
            value={ombudsmanResponses[requestItem.id] ?? ""}
            onChange={(event) => setOmbudsmanResponses((current) => ({
              ...current,
              [requestItem.id]: event.target.value
            }))}
            rows={4}
          />
          <button
            type="button"
            className="primary-button primary-button--compact"
            onClick={() => handleOmbudsmanResponseSubmit(requestItem)}
          >
            Responder
          </button>
        </div>
      </article>
    );

    const renderOmbudsmanGroup = (title, description, requests, emptyDescription) => (
      <section className="ombudsman-admin-group">
        <div className="ombudsman-admin-group__header">
          <div>
            <strong>{title}</strong>
            <p>{description}</p>
          </div>
          <span className="admin-section-badge">{requests.length}</span>
        </div>
        <div className="admin-list admin-list--ombudsman">
          {requests.length ? requests.map(renderOmbudsmanRequest) : (
            <EmptyState title="Nada por aqui" description={emptyDescription} />
          )}
        </div>
      </section>
    );

    return (
      <div className="admin-card--ombudsman">
        <div className="admin-section-toolbar">
          <p>Acompanhe protocolos, responda usuários e atualize o status.</p>
          <div className="inline-actions">
            <button type="button" className="ghost-button ghost-button--small" onClick={() => refreshAdminOmbudsmanData()}>
              {isOmbudsmanAdminLoading ? "Atualizando..." : "Atualizar"}
            </button>
          </div>
        </div>

        <div className="ombudsman-admin-groups">
          {renderOmbudsmanGroup(
            "Novas",
            "Manifestações abertas que ainda não receberam resposta.",
            newRequests,
            "Novas manifestações da Ouvidoria aparecerão aqui."
          )}
          {renderOmbudsmanGroup(
            "Em atendimento",
            "Manifestações em análise ou já respondidas, mas ainda não fechadas.",
            inProgressRequests,
            "Manifestações em análise ou respondidas aparecerão aqui."
          )}
          {renderOmbudsmanGroup(
            "Fechadas",
            "Protocolos encerrados.",
            closedRequests,
            "Manifestações fechadas aparecerão aqui."
          )}
        </div>
      </div>
    );
  }

  function renderLoggedArea() {
    const isCreateInterestPage = isInterestModalVisible
      && loggedSection === loggedSections.NEW_INTEREST
      && !editingInterestId;
    const statCards = [
      {
        key: loggedSections.MY_INTERESTS,
        label: t("dashboard.stats.myInterests"),
        value: dashboard?.totalActiveInterests ?? "...",
        accent: loggedSection === loggedSections.MY_INTERESTS
      },
      {
        key: loggedSections.SENT_OFFERS,
        label: t("dashboard.stats.sentOffers"),
        value: dashboard?.totalOffersSent ?? "...",
        accent: loggedSection === loggedSections.SENT_OFFERS
      },
      {
        key: loggedSections.RECEIVED_OFFERS,
        label: t("dashboard.stats.receivedOffers"),
        value: dashboard?.totalOffersReceived ?? "...",
        accent: loggedSection === loggedSections.RECEIVED_OFFERS
      },
    ];
    const dashboardNavigationItems = [
      {
        key: loggedSections.EXPLORE,
        icon: "⌂",
        label: t("header.nav.home"),
        active: loggedSection === loggedSections.EXPLORE,
        onClick: () => navigateFromDashboardControl(loggedSections.EXPLORE)
      },
      {
        key: loggedSections.NEW_INTEREST,
        icon: "+",
        label: t("dashboard.nav.newInterest"),
        active: loggedSection === loggedSections.NEW_INTEREST,
        onClick: openNewInterestForm
      },
      {
        key: loggedSections.MY_INTERESTS,
        icon: "●",
        label: t("dashboard.nav.myInterests"),
        active: loggedSection === loggedSections.MY_INTERESTS,
        onClick: () => navigateFromDashboardControl(loggedSections.MY_INTERESTS)
      },
      {
        key: loggedSections.SENT_OFFERS,
        icon: "↗",
        label: t("dashboard.nav.sentOffers"),
        active: loggedSection === loggedSections.SENT_OFFERS,
        onClick: () => navigateFromDashboardControl(loggedSections.SENT_OFFERS)
      },
      {
        key: loggedSections.RECEIVED_OFFERS,
        icon: "↙",
        label: t("dashboard.nav.receivedOffers"),
        active: loggedSection === loggedSections.RECEIVED_OFFERS,
        onClick: () => navigateFromDashboardControl(loggedSections.RECEIVED_OFFERS)
      },
      {
        key: loggedSections.SELLER_ITEMS,
        icon: "▣",
        label: t("dashboard.nav.sellerItems"),
        active: loggedSection === loggedSections.SELLER_ITEMS,
        onClick: () => navigateFromDashboardControl(loggedSections.SELLER_ITEMS)
      }
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
                onClick={() => navigateFromDashboardControl(card.key)}
              />
            ))}
          </div>
        </section>

        <DashboardNavigation items={dashboardNavigationItems} />

        {loggedSection === loggedSections.EXPLORE ? renderPublicHome(false) : null}

        {loggedSection === loggedSections.CREDITS ? renderCreditsPage() : null}

        {loggedSection === loggedSections.MY_INTERESTS ? (
          <section ref={myInterestsSectionRef} className="workspace-grid">
            <article className="panel">
              <div className="panel__header">
                <div>
                  <span className="eyebrow">Página</span>
                  <h2>{showInactiveInterests ? "Minhas procuras" : "Procuras ativas"}</h2>
                </div>
              </div>
              <label className="seller-items-toggle">
                <input
                  type="checkbox"
                  checked={showInactiveInterests}
                  onChange={(event) => setShowInactiveInterests(event.target.checked)}
                />
                <span>Mostrar procuras desativadas</span>
              </label>

              {myInterests.length ? (
                <div className="accordion-list">{myInterests.map(renderInterestListItem)}</div>
              ) : (
                <EmptyState
                  title="Nenhuma procura ativa"
                  description="Publique uma nova procura para começar a receber propostas."
                />
              )}
            </article>

            <aside className="panel panel--sticky">
              <div className="panel__header">
                <div>
                  <span className="eyebrow">Respostas</span>
                  <h2>{selectedInterest?.title ?? "Escolha uma procura"}</h2>
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
                  {["PENDING", "REVIEW_REQUIRED", "REJECTED", "REPORTED", "CLOSED"].includes(selectedInterest.status) ? (
                    <div className={`moderation-callout moderation-callout--${moderationStatusTone(selectedInterest.status)}`}>
                      <strong>{moderationStatusLabel(selectedInterest.status, t)}</strong>
                      <p>
                        {selectedInterest.status === "REJECTED"
                          ? "Sua procura foi rejeitada. Edite para enviar novamente para análise ou exclua se preferir."
                          : selectedInterest.status === "CLOSED"
                            ? "Esta procura está desativada e não aparece para outros usuários."
                          : (selectedInterest.moderation?.reason ?? "Sua procura ainda não está disponível publicamente.")}
                      </p>
                    </div>
                  ) : null}
                  <div className="expiry-renewal-row">
                    <span className={`${expiryPillClass(selectedInterest)} expiry-pill--inline`}>
                      {isListingExpiringSoon(selectedInterest) ? "⚠ " : ""}
                      {formatRemainingListingTime(selectedInterest, t)}
                    </span>
                    {isListingExpiringSoon(selectedInterest) ? (
                      <button
                        type="button"
                        className="renewal-button"
                        onClick={() => handleRenewInterest(selectedInterest.id)}
                        title="Usa 1 crédito para adicionar mais 30 dias à procura"
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
                      Editar procura
                    </button>
                    {selectedInterest.status === "CLOSED" ? (
                      <button
                        type="button"
                        className="primary-button action-button--compact"
                        onClick={() => handleActivateInterest(selectedInterest.id)}
                      >
                        Ativar procura
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="ghost-button action-button--compact"
                        onClick={() => handleCloseInterest(selectedInterest.id)}
                      >
                        Desativar procura
                      </button>
                    )}
                    <button
                      type="button"
                      className="danger-button action-button--compact"
                      onClick={() => handleDeleteInterest(selectedInterest.id)}
                    >
                      Excluir procura
                    </button>
                  </div>
                  {boostPurchasesEnabled && boostProducts.length > 0 && ["APPROVED", "OPEN"].includes(selectedInterest?.status) && (
                      <>
                        <div className="boost-box">
                          <div>
                            <strong>Impulsionar procura</strong>
                            <p>
                              {selectedInterest.boostedUntil
                                  ? t("boost.activeUntil", { date: formatTimestamp(selectedInterest.boostedUntil, t) })
                                  : "Apareça com prioridade na busca e na home."}
                            </p>
                          </div>

                          <div className="boost-box__actions">
                            {boostProducts.map((product) => (
                                <article key={product.code} className="product-chip product-chip--boost">
                                  <div>
                                    <strong>{product.name}</strong>
                                    <ProductPrice product={product} />
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
                            <span className="eyebrow">Propostas recebidas</span>
                            <strong>{offers.length}</strong>
                          </div>

                          {offers.length === 0 ? (
                              <EmptyState
                                  title="Ainda sem propostas"
                                  description="Quando alguém responder à sua procura, as mensagens aparecerão aqui."
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
                  title="Selecione uma procura sua"
                  description="Clique em uma procura ativa para acompanhar as propostas."
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
                <h2>Propostas enviadas</h2>
              </div>
            </div>

            {sentOffers.length ? (
              <div className="accordion-list">
                {sentOffers.map((offer) => renderOfferListItem(offer, "left"))}
              </div>
            ) : (
              <EmptyState
                title="Nenhuma proposta enviada"
                description="As propostas que você enviar para procuras de outras pessoas aparecerão aqui."
              />
            )}
          </section>
        ) : null}

        {loggedSection === loggedSections.RECEIVED_OFFERS ? (
          <section ref={receivedOffersSectionRef} className="panel panel--spaced">
            <div className="panel__header">
              <div>
                <span className="eyebrow">Página</span>
                <h2>Propostas recebidas</h2>
              </div>
            </div>

            {receivedOffers.length ? (
              <div className="accordion-list">
                {receivedOffers.map((offer) => renderOfferListItem(offer, "received"))}
              </div>
            ) : (
              <EmptyState
                title="Nenhuma proposta recebida"
                description="As respostas às suas procuras ficarão listadas aqui."
              />
            )}
          </section>
        ) : null}

        {loggedSection === loggedSections.SELLER_ITEMS ? renderSellerItemsPage() : null}

        {loggedSection === loggedSections.ADMIN && isAdmin ? renderAdminModerationPage() : null}

        {isInterestModalVisible ? (
          <div
            className={isCreateInterestPage ? "interest-form-page-shell" : "modal-overlay"}
            role="presentation"
            onClick={isCreateInterestPage ? undefined : cancelInterestEditing}
          >
            <section
              ref={newInterestSectionRef}
              className={isCreateInterestPage ? "panel panel--form interest-form-page" : "form-modal panel panel--form"}
              role={isCreateInterestPage ? undefined : "dialog"}
              aria-modal={isCreateInterestPage ? undefined : "true"}
              aria-labelledby="interest-form-title"
              onClick={isCreateInterestPage ? undefined : (event) => event.stopPropagation()}
            >
            <div className="feedback-modal__header">
              <div>
                <span className="eyebrow">{t("interest.form.eyebrow")}</span>
                <h2 id="interest-form-title">{editingInterestId ? t("interest.form.editTitle") : t("interest.form.createTitle")}</h2>
              </div>
              {isCreateInterestPage ? (
                <button type="button" className="ghost-button" onClick={() => navigateTo(loggedSections.EXPLORE)}>
                  {t("common.actions.backHome")}
                </button>
              ) : (
                <button
                  type="button"
                  className="modal-close-button"
                  onClick={cancelInterestEditing}
                  aria-label={t("common.actions.closeModal")}
                >
                  X
                </button>
              )}
            </div>

            <form className="stacked-form" onSubmit={handleInterestSubmit}>
              {!editingInterestId ? (
                <>
                  <p className="form-intro">{t("interest.form.guidance")}</p>
                  <div className="intent-notice">{t("interest.form.intentNotice")}</div>
                </>
              ) : null}

              <div className="expiry-note">
                {t("interest.form.expiryNote", { count: LISTING_EXPIRATION_DAYS })}
              </div>

              {editingInterestId ? (
                <div className="cta-card">
                  <strong>{t("interest.form.editing.title")}</strong>
                  <p>{t("interest.form.editing.description")}</p>
                </div>
              ) : null}

              <input
                placeholder={t("interest.form.title.placeholder")}
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
                placeholder={t("interest.form.description.placeholder")}
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
              <div className="field-footer">
                <p className="field-helper">{t("interest.form.description.helper")}</p>
                <FieldCounter value={interestForm.description} max={DESCRIPTION_MAX_LENGTH} />
              </div>
              {hasLink(interestForm.description) ? (
                <p className="form-note form-note--compact">{t("interest.form.linksNotAllowed")}</p>
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
                  placeholder={t("interest.form.tags.placeholder")}
                  value={interestForm.tags}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, tags: event.target.value }))
                  }
                />
              </div>

              <div className="field-group-label">Quanto pretende investir?</div>
              <div className="three-columns">
                <input
                  type="number"
                  min="0"
                  placeholder={t("interest.form.budgetMin.placeholder")}
                  value={interestForm.budgetMin}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, budgetMin: event.target.value }))
                  }
                />
                <input
                  type="number"
                  min="0"
                  placeholder={t("interest.form.budgetMax.placeholder")}
                  value={interestForm.budgetMax}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, budgetMax: event.target.value }))
                  }
                  required
                />
                <input
                  type="number"
                  min="0"
                  placeholder={t("interest.form.radius.placeholder")}
                  value={interestForm.desiredRadiusKm}
                  onChange={(event) =>
                    setInterestForm((current) => ({
                      ...current,
                      desiredRadiusKm: event.target.value
                    }))
                  }
                />
              </div>
              {hasInvalidBudgetRange(interestForm) ? (
                <p className="form-note form-note--compact">{t("interest.feedback.invalidBudget.message")}</p>
              ) : null}

              <div className="three-columns">
                <input
                  placeholder={t("address.postalCode.placeholder")}
                  value={interestForm.postalCode}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, postalCode: formatCep(event.target.value) }))
                  }
                  onBlur={() => handlePostalCodeLookup("interest", interestForm.postalCode, (address) => {
                    setInterestForm((current) => ({
                      ...current,
                      postalCode: address.postalCode ?? current.postalCode,
                      city: address.city ?? current.city,
                      state: address.state ?? current.state,
                      neighborhood: address.neighborhood ?? current.neighborhood,
                      country: address.country ?? current.country
                    }));
                  })}
                  inputMode="numeric"
                />
                <input
                  placeholder={t("auth.register.city.placeholder")}
                  value={interestForm.city}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, city: event.target.value }))
                  }
                  required
                />
                <input
                  placeholder={t("auth.register.state.placeholder")}
                  value={interestForm.state}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, state: event.target.value }))
                  }
                  required
                />
              </div>
              {addressLookupState.interest.message ? (
                <span
                  className={`address-lookup-note ${addressLookupState.interest.isLoading ? "is-loading" : ""}`}
                  role="status"
                  aria-live="polite"
                  aria-busy={addressLookupState.interest.isLoading}
                >
                  {addressLookupState.interest.message}
                </span>
              ) : null}

              <div className="two-columns">
                <input
                  placeholder={t("interest.form.neighborhood.placeholder")}
                  value={interestForm.neighborhood}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, neighborhood: event.target.value }))
                  }
                />
                <input
                  placeholder={t("address.country.placeholder")}
                  value={interestForm.country}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, country: event.target.value }))
                  }
                />
              </div>

              <div className="two-columns">
                <input
                  placeholder={t("interest.form.condition.placeholder")}
                  value={interestForm.preferredCondition}
                  onChange={(event) =>
                    setInterestForm((current) => ({
                      ...current,
                      preferredCondition: event.target.value
                    }))
                  }
                />
                <input
                  placeholder={t("interest.form.contactMode.placeholder")}
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
                <label htmlFor="interest-image">{t("interest.form.referenceImage.label")}</label>
                <input id="interest-image" type="file" accept="image/*" onChange={handleInterestImageChange} />
                {interestForm.referenceImageUrl ? (
                  <img
                    className="interest-upload-preview"
                    src={interestForm.referenceImageUrl}
                    alt={t("interest.form.referenceImage.previewAlt")}
                    decoding="async"
                  />
                ) : null}
              </div>

              <div className="two-columns">
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
                  <span>{t("interest.form.whatsappAllowed")}</span>
                </label>
              </div>

              {interestForm.allowsWhatsappContact ? (
                <input
                  placeholder={t("interest.form.whatsapp.placeholder")}
                  value={interestForm.whatsappContact}
                  onChange={(event) =>
                    setInterestForm((current) => ({ ...current, whatsappContact: event.target.value }))
                  }
                  required
                />
              ) : null}

              <div className="form-actions">
                {editingInterestId ? (
                  <button type="button" className="ghost-button" onClick={cancelInterestEditing}>
                    {t("interest.form.cancelEdit")}
                  </button>
                ) : null}
                <button type="submit" className="primary-button" disabled={isSubmittingInterest}>
                  {isSubmittingInterest
                    ? (editingInterestId ? t("common.actions.saving") : t("interest.form.publishing"))
                    : (editingInterestId ? t("interest.form.saveChanges") : t("interest.form.publish"))}
                </button>
              </div>
            </form>
            </section>
          </div>
        ) : null}
      </>
    );
  }

  if (activeLegalPageSlug) {
    return (
      <div className="app-shell" data-theme={theme}>
        <div className="background-grid" />

        <main className="page">
          <Header
            isLoggedIn={false}
            hideActions
            theme={theme}
            onThemeToggle={toggleTheme}
            onNavigate={() => {
              setActiveLegalPageSlug("");
              replaceCurrentUrl(sectionRoutes[loggedSections.EXPLORE]);
            }}
          />
          <LegalPage slug={activeLegalPageSlug} />
        </main>

        <Footer />
      </div>
    );
  }

  if (isOmbudsmanPageActive) {
    return (
      <div className="app-shell" data-theme={theme}>
        <div className="background-grid" />

        <main className="page">
          <Header
            hideActions
            theme={theme}
            onThemeToggle={toggleTheme}
            onNavigate={() => {
              setIsOmbudsmanPageActive(false);
              replaceCurrentUrl(sectionRoutes[loggedSections.EXPLORE]);
            }}
          />
          {renderOmbudsmanPage()}
        </main>

        <Footer />
      </div>
    );
  }

  return (
    <div className="app-shell" data-theme={theme}>
      <div className="background-grid" />

      <main className="page">
        <Header
          user={currentUser}
          currentSection={loggedSection}
          hasNotifications={hasUnreadMessages}
          sellerCredits={monetizationAccount?.sellerCredits}
          subscriptionActive={monetizationAccount?.subscriptionActive}
          creditPurchasesEnabled={creditPurchasesEnabled}
          isAdmin={isAdmin}
          unreadAdminReportCount={unreadAdminReportCount}
          isLoggedIn={Boolean(session)}
          notificationButtonRef={notificationButtonRef}
          onLoginClick={() => openAuthModal("login")}
          onRegisterClick={() => openAuthModal("register")}
          onCreditsClick={() => creditPurchasesEnabled && navigateTo(loggedSections.CREDITS)}
          onAdminClick={() => {
            markAdminReportsSeen();
            navigateFromDashboardControl(loggedSections.ADMIN);
          }}
          onNotificationClick={openNotificationModal}
          onLogout={handleLogout}
          theme={theme}
          onThemeToggle={toggleTheme}
          onNavigate={(section) => {
            if (section === loggedSections.NEW_INTEREST) {
              openNewInterestForm();
              return;
            }

            navigateTo(section);
          }}
        />

        {isLoadingPrivate && session ? (
          <section className="loading-card loading-card--full">{t("dashboard.loadingPrivate")}</section>
        ) : null}

        {!session ? renderPublicHome(true) : renderLoggedArea()}
      </main>

      <AuthModal
        visible={isAuthModalVisible}
        mode={authMode}
        isSubmitting={isSubmittingAuth}
        loginForm={loginForm}
        registerForm={registerForm}
        registerAddressLookup={addressLookupState.register}
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
        onRegisterPostalCodeLookup={(postalCode) => handlePostalCodeLookup("register", postalCode, (address) => {
          setRegisterForm((current) => ({
            ...current,
            postalCode: address.postalCode ?? current.postalCode,
            city: address.city ?? current.city,
            state: address.state ?? current.state,
            neighborhood: address.neighborhood ?? current.neighborhood,
            country: address.country ?? current.country
          }));
        })}
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
                <h2 id="report-modal-title">Denunciar procura</h2>
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

      <Footer />
    </div>
  );
}
