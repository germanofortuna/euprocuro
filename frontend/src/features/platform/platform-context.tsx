"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import {
  AUTH_SESSION_MODE,
  activateInterest,
  boostInterest as requestBoostInterest,
  cancelSubscription,
  clearSession,
  closeInterest,
  connectChatSocket,
  createInterest,
  createOffer,
  createOmbudsmanRequest,
  createSellerItem,
  deleteAccount as requestDeleteAccount,
  deleteInterest,
  fetchAdminModeration,
  fetchCategories,
  fetchDashboard,
  fetchInterest,
  fetchInterests,
  fetchMe,
  fetchOperationalSettings,
  fetchMonetizationAccount,
  fetchSellerItems,
  getStoredSession,
  googleLogin,
  facebookLogin,
  isAuthError,
  login,
  logout,
  purchaseProduct,
  register,
  renewInterest,
  reportInterest,
  storeSession,
  updateInterest,
  updateSellerItem
} from "@/shared/api/client";
import type {
  AdminModeration,
  Category,
  Dashboard,
  Interest,
  MonetizationAccount,
  OperationalSettings,
  SellerItemGroup,
  StoredSession,
  User
} from "@/shared/api/types";
import { activeCategories, FALLBACK_CATEGORIES, isAdminUser } from "@/shared/lib/format";
import type { FeedbackState } from "@/shared/ui/feedback-modal";

type AuthMode = "login" | "register" | "forgot" | "reset";
const PENDING_CREDIT_ONBOARDING_KEY = "euProcuro.pendingCreditOnboardingEmail";
const SEEN_CREDIT_ONBOARDING_PREFIX = "euProcuro.seenCreditOnboarding:";

type PlatformContextValue = {
  session: StoredSession | null;
  currentUser: User | null;
  categories: Category[];
  interests: Interest[];
  selectedInterest: Interest | null;
  dashboard: Dashboard | null;
  monetization: MonetizationAccount | null;
  sellerItems: SellerItemGroup[];
  operationalSettings: OperationalSettings;
  adminModeration: AdminModeration | null;
  hasAdminAccess: boolean;
  isLoadingPublic: boolean;
  hasLoadedPublicData: boolean;
  isLoadingPrivate: boolean;
  isSessionReady: boolean;
  feedback: FeedbackState;
  setFeedback: (feedback: FeedbackState) => void;
  authModal: { visible: boolean; mode: AuthMode; redirectTo?: string | null };
  openAuthModal: (mode?: AuthMode, redirectTo?: string | null) => void;
  closeAuthModal: () => void;
  setAuthMode: (mode: AuthMode) => void;
  signIn: (payload: { email: string; password: string; turnstileToken?: string }) => Promise<void>;
  signInWithGoogle: (accessToken: string, turnstileToken?: string) => Promise<void>;
  signInWithFacebook: (accessToken: string, turnstileToken?: string) => Promise<void>;
  signUp: (payload: Record<string, unknown>) => Promise<void>;
  signOut: () => Promise<void>;
  deleteAccount: () => Promise<void>;
  recoverSession: () => Promise<void>;
  refreshOperationalSettings: () => Promise<void>;
  refreshPublicData: (filters?: Record<string, string | number | undefined | null>) => Promise<void>;
  selectInterest: (interestId: string) => Promise<void>;
  refreshPrivateData: () => Promise<void>;
  saveInterest: (payload: Record<string, unknown>, interestId?: string | null) => Promise<Interest | null>;
  closeOwnInterest: (interestId: string) => Promise<void>;
  activateOwnInterest: (interestId: string) => Promise<void>;
  deleteOwnInterest: (interestId: string) => Promise<void>;
  renewOwnInterest: (interestId: string) => Promise<void>;
  submitOffer: (interestId: string, payload: Record<string, unknown>) => Promise<void>;
  submitReport: (interestId: string, payload: Record<string, unknown>) => Promise<void>;
  submitOmbudsman: (payload: Record<string, unknown>) => Promise<{ protocol?: string } | null>;
  saveSellerItem: (payload: Record<string, unknown>, itemId?: string | null) => Promise<void>;
  buyProduct: (productCode: string, paymentMethod?: string) => Promise<void>;
  boostOwnInterest: (interestId: string, boostCode: string, paymentMethod?: string) => Promise<void>;
  cancelPlan: () => Promise<void>;
};

const PlatformContext = createContext<PlatformContextValue | null>(null);
const DEFAULT_OPERATIONAL_SETTINGS: OperationalSettings = {
  featureFlags: { stickersPageEnabled: true, sellerProPlanEnabled: false, captchaEnabled: true },
  operationalFields: { initialFreeCredits: 15, listingRenewalCredits: 1 }
};

function normalizeMe(me: Record<string, unknown>, previousSession: StoredSession | null): StoredSession {
  const user = {
    ...(previousSession?.user ?? {}),
    ...me,
    sellerCredits: me.credits,
    credits: me.credits
  } as User;
  return {
    expiresAt: (me.expiresAt as string | null | undefined) ?? previousSession?.expiresAt ?? null,
    token: previousSession?.token ?? null,
    user
  };
}

function normalizeCategories(categories: Category[]) {
  return activeCategories(categories.length ? categories : FALLBACK_CATEGORIES);
}

function byNewest(left: { latestMessageAt?: string; createdAt?: string }, right: { latestMessageAt?: string; createdAt?: string }) {
  return new Date(right.latestMessageAt ?? right.createdAt ?? 0).getTime() - new Date(left.latestMessageAt ?? left.createdAt ?? 0).getTime();
}

function normalizeDashboard(payload: Dashboard | null): Dashboard | null {
  if (!payload) {
    return null;
  }
  const sentOffers = (payload.sentOffers ?? payload.offersSent ?? []).slice().sort(byNewest);
  const receivedOffers = (payload.receivedOffers ?? payload.offersReceived ?? []).slice().sort(byNewest);
  return {
    ...payload,
    sentOffers,
    receivedOffers,
    offersSent: sentOffers,
    offersReceived: receivedOffers,
    totalOffersSent: payload.totalOffersSent ?? sentOffers.length,
    totalOffersReceived: payload.totalOffersReceived ?? receivedOffers.length,
    totalActiveInterests: payload.totalActiveInterests ?? payload.myInterests?.length ?? 0
  };
}

function normalizeMonetizationAccount(payload: MonetizationAccount | null): MonetizationAccount | null {
  if (!payload) {
    return null;
  }
  const creditPurchasesEnabled = payload.settings?.creditPurchasesEnabled ?? payload.creditPurchasesEnabled ?? false;
  const boostPurchasesEnabled = payload.settings?.boostPurchasesEnabled ?? payload.boostPurchasesEnabled ?? false;
  return {
    ...payload,
    proSubscriptionActive: payload.proSubscriptionActive ?? payload.subscriptionActive ?? false,
    payments: payload.payments ?? payload.paymentHistory ?? [],
    settings: {
      ...(payload.settings ?? {}),
      creditPurchasesEnabled,
      boostPurchasesEnabled
    }
  };
}

function normalizedEmail(value: unknown) {
  return String(value ?? "").trim().toLowerCase();
}

function readPendingCreditOnboardingEmail() {
  if (typeof window === "undefined") {
    return "";
  }
  return normalizedEmail(window.localStorage.getItem(PENDING_CREDIT_ONBOARDING_KEY));
}

function markPendingCreditOnboarding(email: unknown) {
  if (typeof window === "undefined") {
    return;
  }
  const normalized = normalizedEmail(email);
  if (normalized) {
    window.localStorage.setItem(PENDING_CREDIT_ONBOARDING_KEY, normalized);
  }
}

function shouldShowCreditOnboarding(user: User | null | undefined) {
  if (typeof window === "undefined") {
    return false;
  }
  const email = normalizedEmail(user?.email);
  if (!email || readPendingCreditOnboardingEmail() !== email) {
    return false;
  }
  return window.localStorage.getItem(`${SEEN_CREDIT_ONBOARDING_PREFIX}${email}`) !== "1";
}

function markCreditOnboardingSeen(user: User | null | undefined) {
  if (typeof window === "undefined") {
    return;
  }
  const email = normalizedEmail(user?.email);
  if (!email) {
    return;
  }
  window.localStorage.setItem(`${SEEN_CREDIT_ONBOARDING_PREFIX}${email}`, "1");
  if (readPendingCreditOnboardingEmail() === email) {
    window.localStorage.removeItem(PENDING_CREDIT_ONBOARDING_KEY);
  }
}

function creditOnboardingMessage(
  user: User | null | undefined,
  monetizationAccount: MonetizationAccount | null,
  settings: OperationalSettings
) {
  const balance = monetizationAccount?.sellerCredits ?? user?.sellerCredits ?? user?.credits ?? settings.operationalFields?.initialFreeCredits ?? 0;
  const boostProducts = (monetizationAccount?.products ?? [])
    .filter((product) => String(product.type).toUpperCase() === "BOOST" && product.enabled !== false && Number(product.credits ?? 0) > 0)
    .sort((left, right) => Number(left.durationDays ?? 0) - Number(right.durationDays ?? 0));
  const boostSummary = boostProducts.length
    ? boostProducts
        .map((product) => `${product.name}: ${Number(product.credits)} créditos${product.durationDays ? ` por ${product.durationDays} dias` : ""}`)
        .join("; ")
    : "os custos de boost aparecem na página de detalhes da sua procura";
  const renewalCredits = Math.max(0, Number(settings.operationalFields?.listingRenewalCredits ?? 1));
  const renewalSummary = renewalCredits === 0
    ? "renovar uma procura nao consome creditos"
    : `renovar uma procura custa ${renewalCredits} ${renewalCredits === 1 ? "credito" : "creditos"}`;
  return `Você tem ${balance} créditos disponíveis. Publicar uma procura não desconta créditos. Eles servem para manter suas procuras ativas e com mais destaque: ${renewalSummary} e os boosts seguem a configuração atual (${boostSummary}). Para comprar mais créditos ou consultar os planos, acesse "Créditos e Plano" no painel.`;
}

export function PlatformProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [session, setSession] = useState<StoredSession | null>(null);
  const [categories, setCategories] = useState<Category[]>(FALLBACK_CATEGORIES);
  const [interests, setInterests] = useState<Interest[]>([]);
  const [selectedInterest, setSelectedInterest] = useState<Interest | null>(null);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [monetization, setMonetization] = useState<MonetizationAccount | null>(null);
  const [sellerItems, setSellerItems] = useState<SellerItemGroup[]>([]);
  const [operationalSettings, setOperationalSettings] = useState<OperationalSettings>(DEFAULT_OPERATIONAL_SETTINGS);
  const [adminModeration, setAdminModeration] = useState<AdminModeration | null>(null);
  const [hasAdminAccess, setHasAdminAccess] = useState(false);
  const [isLoadingPublic, setIsLoadingPublic] = useState(false);
  const [hasLoadedPublicData, setHasLoadedPublicData] = useState(false);
  const [isLoadingPrivate, setIsLoadingPrivate] = useState(false);
  const [isSessionReady, setIsSessionReady] = useState(false);
  const [feedback, setFeedback] = useState<FeedbackState>(null);
  const [authModal, setAuthModal] = useState<{ visible: boolean; mode: AuthMode; redirectTo?: string | null }>({ visible: false, mode: "login", redirectTo: null });
  const socketRef = useRef<WebSocket | null>(null);

  const currentUser = session?.user ?? null;
  const shouldLoadSellerItems = pathname === "/meus-itens";
  // Carrega moderação admin se:
  // 1. Usuário já confirmado como admin pelo backend (currentUser.admin === true)
  // 2. Está na página /admin
  // 3. hasAdminAccess foi confirmado por probe anterior
  // 4. O campo admin ainda não chegou do backend (null/undefined) → faz probe uma vez;
  //    quando o backend retornar admin: false, a probe para automaticamente.
  const shouldLoadAdminModeration =
    pathname === "/admin" ||
    isAdminUser(currentUser) ||
    hasAdminAccess ||
    (currentUser?.id != null && currentUser.admin == null);

  const refreshPublicData = useCallback(async (filters: Record<string, string | number | undefined | null> = {}) => {
    setIsLoadingPublic(true);
    try {
      const [categoryPayload, interestPayload] = await Promise.all([
        fetchCategories().catch(() => FALLBACK_CATEGORIES),
        fetchInterests(filters).catch(() => [] as Interest[])
      ]);
      const nextCategories = normalizeCategories(categoryPayload);
      const nextInterests = interestPayload;
      setCategories(nextCategories);
      setInterests(nextInterests);
      setSelectedInterest((current) => nextInterests.find((item) => item.id === current?.id) ?? nextInterests[0] ?? null);
    } finally {
      setHasLoadedPublicData(true);
      setIsLoadingPublic(false);
    }
  }, []);

  const refreshOperationalSettings = useCallback(async () => {
    const settings = await fetchOperationalSettings().catch(() => DEFAULT_OPERATIONAL_SETTINGS);
    setOperationalSettings({
      featureFlags: {
        stickersPageEnabled: settings.featureFlags?.stickersPageEnabled ?? true,
        sellerProPlanEnabled: settings.featureFlags?.sellerProPlanEnabled ?? false,
        captchaEnabled: settings.featureFlags?.captchaEnabled ?? true
      },
      operationalFields: {
        initialFreeCredits: settings.operationalFields?.initialFreeCredits ?? 15,
        listingRenewalCredits: settings.operationalFields?.listingRenewalCredits ?? 1
      }
    });
  }, []);

  const refreshPrivateData = useCallback(async () => {
    if (!getStoredSession()) {
      return;
    }
    setIsLoadingPrivate(true);
    try {
      const adminRequest = shouldLoadAdminModeration ? fetchAdminModeration() : Promise.resolve(null);
      const [dashboardResult, monetizationResult, sellerItemsResult, adminResult] = await Promise.allSettled([
        fetchDashboard(),
        fetchMonetizationAccount(),
        shouldLoadSellerItems ? fetchSellerItems({ includeInactive: true }) : Promise.resolve(null),
        adminRequest
      ]);

      const authError = [dashboardResult, monetizationResult, sellerItemsResult]
        .some((result) => result.status === "rejected" && isAuthError(result.reason));
      if (authError) {
        clearSession();
        setSession(null);
        setDashboard(null);
        setMonetization(null);
        setSellerItems([]);
        setAdminModeration(null);
        setHasAdminAccess(false);
        return;
      }

      if (dashboardResult.status === "fulfilled") {
        setDashboard(normalizeDashboard(dashboardResult.value));
      }
      if (monetizationResult.status === "fulfilled") {
        setMonetization(normalizeMonetizationAccount(monetizationResult.value));
      }
      if (sellerItemsResult.status === "fulfilled" && sellerItemsResult.value) {
        setSellerItems(sellerItemsResult.value);
      }
      if (adminResult.status === "fulfilled" && adminResult.value) {
        setAdminModeration(adminResult.value);
        setHasAdminAccess(true);
      } else if (!shouldLoadAdminModeration) {
        setAdminModeration(null);
      }
    } catch (error) {
      if (isAuthError(error)) {
        clearSession();
        setSession(null);
        setHasAdminAccess(false);
      }
    } finally {
      setIsLoadingPrivate(false);
    }
  }, [currentUser?.id, hasAdminAccess, shouldLoadAdminModeration, shouldLoadSellerItems]);

  const recoverSession = useCallback(async () => {
    const stored = getStoredSession();
    if (stored) {
      setSession(stored);
    }
    if (AUTH_SESSION_MODE !== "cookie" && !stored) {
      setIsSessionReady(true);
      return;
    }
    try {
      const me = await fetchMe();
      const nextSession = normalizeMe(me, stored);
      storeSession(nextSession);
      setSession(nextSession);
    } catch {
      clearSession();
      setSession(null);
    } finally {
      setIsSessionReady(true);
    }
  }, []);

  useEffect(() => {
    recoverSession().catch(() => {});
    refreshOperationalSettings().catch(() => {});
    refreshPublicData().catch(() => {});
  }, [recoverSession, refreshOperationalSettings, refreshPublicData]);

  useEffect(() => {
    if (!session?.user?.id) {
      socketRef.current?.close();
      socketRef.current = null;
      return;
    }

    refreshPrivateData().catch(() => {});
    socketRef.current?.close();
    socketRef.current = connectChatSocket({
      onMessage: () => refreshPrivateData().catch(() => {})
    });

    return () => {
      socketRef.current?.close();
      socketRef.current = null;
    };
  }, [session?.user?.id, refreshPrivateData]);

  const selectInterest = useCallback(async (interestId: string) => {
    const cached = interests.find((interest) => interest.id === interestId);
    if (cached) {
      setSelectedInterest(cached);
    }
    try {
      const detail = await fetchInterest(interestId);
      setSelectedInterest(detail);
      setInterests((current) => (current.some((item) => item.id === detail.id) ? current.map((item) => (item.id === detail.id ? detail : item)) : [detail, ...current]));
    } catch {
      if (!cached) {
        setSelectedInterest(null);
      }
    }
  }, [interests]);

  const openAuthModal = useCallback((mode: AuthMode = "login", redirectTo: string | null = null) => setAuthModal({ visible: true, mode, redirectTo }), []);
  const closeAuthModal = useCallback(() => setAuthModal((current) => ({ ...current, visible: false })), []);
  const setAuthMode = useCallback((mode: AuthMode) => setAuthModal((current) => ({ ...current, mode })), []);

  const signIn = useCallback(async (payload: { email: string; password: string; turnstileToken?: string }) => {
    const auth = await login(payload);
    const nextSession = auth?.user?.id ? auth : normalizeMe(await fetchMe(), auth);
    storeSession(nextSession);
    setSession(nextSession);
    if (shouldShowCreditOnboarding(nextSession.user)) {
      const monetizationAccount = normalizeMonetizationAccount(await fetchMonetizationAccount().catch(() => null));
      if (monetizationAccount) {
        setMonetization(monetizationAccount);
      }
      setFeedback({
        type: "info",
        title: "Seus créditos iniciais",
        message: creditOnboardingMessage(nextSession.user, monetizationAccount, operationalSettings),
        afterClose: () => markCreditOnboardingSeen(nextSession.user)
      });
    }
    const redirectTo = authModal.redirectTo;
    closeAuthModal();
    if (redirectTo) {
      router.push(redirectTo);
    }
  }, [authModal.redirectTo, closeAuthModal, operationalSettings, router]);

  const completeSocialSignIn = useCallback(async (auth: StoredSession) => {
    const nextSession = auth?.user?.id ? auth : normalizeMe(await fetchMe(), auth);
    storeSession(nextSession);
    setSession(nextSession);

    const email = normalizedEmail(nextSession.user?.email);
    if (email && window.localStorage.getItem(`${SEEN_CREDIT_ONBOARDING_PREFIX}${email}`) !== "1") {
      const monetizationAccount = normalizeMonetizationAccount(await fetchMonetizationAccount().catch(() => null));
      if (monetizationAccount) {
        setMonetization(monetizationAccount);
      }
      setFeedback({
        type: "info",
        title: "Seus creditos iniciais",
        message: creditOnboardingMessage(nextSession.user, monetizationAccount, operationalSettings),
        afterClose: () => markCreditOnboardingSeen(nextSession.user)
      });
    }

    const redirectTo = authModal.redirectTo;
    closeAuthModal();
    if (redirectTo) {
      router.push(redirectTo);
    }
  }, [authModal.redirectTo, closeAuthModal, operationalSettings, router]);

  const signInWithGoogle = useCallback(async (accessToken: string, turnstileToken?: string) => {
    const auth = await googleLogin({ accessToken, turnstileToken });
    await completeSocialSignIn(auth);
  }, [completeSocialSignIn]);

  const signInWithFacebook = useCallback(async (accessToken: string, turnstileToken?: string) => {
    const auth = await facebookLogin({ accessToken, turnstileToken });
    await completeSocialSignIn(auth);
  }, [completeSocialSignIn]);

  const signUp = useCallback(async (payload: Record<string, unknown>) => {
    await register(payload);
    markPendingCreditOnboarding(payload.email);
    setAuthModal((current) => ({ visible: true, mode: "login", redirectTo: current.redirectTo ?? null }));
    setFeedback({ type: "success", title: "Confirme seu e-mail", message: "Sua conta foi criada. Confirme seu e-mail para entrar." });
  }, []);

  const signOut = useCallback(async () => {
    try {
      await logout();
    } catch {
      // Logout must clear local state even if the backend call fails.
    } finally {
      clearSession();
      setSession(null);
      setDashboard(null);
      setMonetization(null);
      setSellerItems([]);
      setAdminModeration(null);
      setHasAdminAccess(false);
    }
  }, []);

  const deleteAccount = useCallback(async () => {
    await requestDeleteAccount();
    clearSession();
    setSession(null);
    setDashboard(null);
    setMonetization(null);
    setSellerItems([]);
    setAdminModeration(null);
    setHasAdminAccess(false);
    setAuthModal({ visible: false, mode: "login", redirectTo: null });
    setFeedback({ type: "success", title: "Conta excluída", message: "Sua conta e seus conteúdos foram excluídos." });
  }, []);

  const saveInterest = useCallback(async (payload: Record<string, unknown>, interestId?: string | null) => {
    const saved = interestId ? await updateInterest(interestId, payload) : await createInterest(payload);
    setFeedback({
      type: "success",
      title: interestId ? "Alteração recebida" : "Procura recebida",
      message: "Vamos validar sua procura agora. Se houver recusa, você receberá um aviso para ajustar."
    });
    await Promise.all([refreshPublicData(), refreshPrivateData()]);
    return saved;
  }, [refreshPrivateData, refreshPublicData]);

  const closeOwnInterest = useCallback(async (interestId: string) => {
    await closeInterest(interestId);
    await refreshPrivateData();
  }, [refreshPrivateData]);

  const activateOwnInterest = useCallback(async (interestId: string) => {
    await activateInterest(interestId);
    await refreshPrivateData();
  }, [refreshPrivateData]);

  const deleteOwnInterest = useCallback(async (interestId: string) => {
    await deleteInterest(interestId);
    await Promise.all([refreshPublicData(), refreshPrivateData()]);
  }, [refreshPrivateData, refreshPublicData]);

  const renewOwnInterest = useCallback(async (interestId: string) => {
    await renewInterest(interestId);
    setFeedback({ type: "success", title: "Procura renovada", message: "A procura voltou a ter prazo de exibição atualizado." });
    await refreshPrivateData();
  }, [refreshPrivateData]);

  const submitOffer = useCallback(async (interestId: string, payload: Record<string, unknown>) => {
    if (!session?.user?.id) {
      openAuthModal("login");
      return;
    }
    await createOffer(interestId, payload);
    setFeedback({ type: "success", title: "Proposta enviada", message: "Sua proposta foi enviada para quem publicou a procura." });
    await refreshPrivateData();
  }, [openAuthModal, refreshPrivateData, session?.user?.id]);

  const submitReport = useCallback(async (interestId: string, payload: Record<string, unknown>) => {
    if (!session?.user?.id) {
      openAuthModal("login");
      return;
    }
    await reportInterest(interestId, payload);
    setFeedback({ type: "success", title: "Denúncia enviada", message: "Obrigado. O conteúdo será analisado pela moderação." });
  }, [openAuthModal, session?.user?.id]);

  const submitOmbudsman = useCallback(async (payload: Record<string, unknown>) => {
    const response = await createOmbudsmanRequest(payload);
    setFeedback({ type: "success", title: "Manifestação enviada", message: `Protocolo ${response.protocol ?? "gerado"} recebido pela ouvidoria.` });
    return response;
  }, []);

  const saveSellerItem = useCallback(async (payload: Record<string, unknown>, itemId?: string | null) => {
    if (itemId) {
      await updateSellerItem(itemId, payload);
    } else {
      await createSellerItem(payload);
    }
    setFeedback({ type: "success", title: "Item salvo", message: "Vamos buscar procuras compatíveis com o que você tem para negociar." });
    await refreshPrivateData();
  }, [refreshPrivateData]);

  const buyProduct = useCallback(async (productCode: string, paymentMethod = "MERCADO_PAGO") => {
    const checkout = await purchaseProduct({ productCode, paymentMethod }) as { checkoutUrl?: string; message?: string };
    if (typeof checkout.checkoutUrl === "string" && checkout.checkoutUrl && !checkout.checkoutUrl.startsWith("local://")) {
      window.location.assign(checkout.checkoutUrl);
      return;
    }
    setFeedback({ type: "success", title: "Pedido criado", message: String(checkout.message ?? "Pedido criado. Aguardando confirmação do pagamento.") });
    await refreshPrivateData();
  }, [refreshPrivateData]);

  const boostOwnInterest = useCallback(async (interestId: string, boostCode: string, paymentMethod = "MERCADO_PAGO") => {
    const checkout = await requestBoostInterest(interestId, { boostCode, paymentMethod }) as { checkoutUrl?: string; message?: string };
    if (typeof checkout.checkoutUrl === "string" && checkout.checkoutUrl && !checkout.checkoutUrl.startsWith("local://")) {
      window.location.assign(checkout.checkoutUrl);
      return;
    }
    setFeedback({
      type: "success",
      title: paymentMethod === "CREDITS" ? "Boost ativado" : "Pedido criado",
      message: String(checkout.message ?? "Boost solicitado com sucesso.")
    });
    await Promise.all([refreshPrivateData(), refreshPublicData()]);
  }, [refreshPrivateData, refreshPublicData]);

  const cancelPlan = useCallback(async () => {
    await cancelSubscription();
    setFeedback({ type: "success", title: "Plano cancelado", message: "O Plano Pro foi cancelado para esta conta." });
    await refreshPrivateData();
  }, [refreshPrivateData]);

  const value = useMemo<PlatformContextValue>(() => ({
    session,
    currentUser,
    categories,
    interests,
    selectedInterest,
    dashboard,
    monetization,
    sellerItems,
    operationalSettings,
    adminModeration,
    hasAdminAccess,
    isLoadingPublic,
    hasLoadedPublicData,
    isLoadingPrivate,
    isSessionReady,
    feedback,
    setFeedback,
    authModal,
    openAuthModal,
    closeAuthModal,
    setAuthMode,
    signIn,
    signInWithGoogle,
    signInWithFacebook,
    signUp,
    signOut,
    deleteAccount,
    recoverSession,
    refreshOperationalSettings,
    refreshPublicData,
    selectInterest,
    refreshPrivateData,
    saveInterest,
    closeOwnInterest,
    activateOwnInterest,
    deleteOwnInterest,
    renewOwnInterest,
    submitOffer,
    submitReport,
    submitOmbudsman,
    saveSellerItem,
    buyProduct,
    boostOwnInterest,
    cancelPlan
  }), [
    activateOwnInterest,
    adminModeration,
    authModal,
    buyProduct,
    boostOwnInterest,
    cancelPlan,
    categories,
    closeAuthModal,
    closeOwnInterest,
    currentUser,
    dashboard,
    deleteOwnInterest,
    deleteAccount,
    feedback,
    interests,
    hasLoadedPublicData,
    isLoadingPrivate,
    isLoadingPublic,
    isSessionReady,
    monetization,
    openAuthModal,
    recoverSession,
    refreshPrivateData,
    refreshPublicData,
    renewOwnInterest,
    saveInterest,
    saveSellerItem,
    selectInterest,
    selectedInterest,
    sellerItems,
    operationalSettings,
    session,
    hasAdminAccess,
    setAuthMode,
    signIn,
    signInWithGoogle,
    signInWithFacebook,
    signOut,
    signUp,
    submitOffer,
    submitOmbudsman,
    submitReport
  ]);

  return <PlatformContext.Provider value={value}>{children}</PlatformContext.Provider>;
}

export function usePlatform() {
  const context = useContext(PlatformContext);
  if (!context) {
    throw new Error("usePlatform must be used within PlatformProvider.");
  }
  return context;
}
