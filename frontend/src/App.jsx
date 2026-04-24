import { useDeferredValue, useEffect, useMemo, useRef, useState } from "react";

import {
  clearSession,
  createInterest,
  createOffer,
  fetchCategories,
  fetchDashboard,
  fetchInterests,
  fetchMe,
  fetchOfferConversation,
  fetchOffers,
  forgotPassword,
  getStoredSession,
  login,
  logout,
  register,
  resetPassword,
  sendOfferMessage,
  updateInterest,
  storeSession
} from "./api";
import logo from "./assets/eu-procuro-logo.png";
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

const initialLoginForm = {
  email: "",
  password: ""
};

const initialRegisterForm = {
  name: "",
  email: "",
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
  NEW_INTEREST: "NEW_INTEREST"
};

const MESSAGE_SEEN_STORAGE_KEY = "eu-procuro-message-seen";

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

function createResetStateFromLocation() {
  const params = new URLSearchParams(window.location.search);
  return {
    mode: params.get("mode") === "reset" ? "reset" : "login",
    token: params.get("token") ?? ""
  };
}

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result ?? ""));
    reader.onerror = () => reject(new Error("Não foi possível ler a imagem."));
    reader.readAsDataURL(file);
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
    boostEnabled: interestForm.boostEnabled,
    preferredCondition: interestForm.preferredCondition,
    preferredContactMode: interestForm.preferredContactMode,
    tags: interestForm.tags
      .split(",")
      .map((tag) => tag.trim())
      .filter(Boolean)
  };
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

function latestIncomingMessageTimestamp(conversation, currentUserId) {
  return (conversation?.messages ?? [])
    .filter((message) => message.senderId !== currentUserId)
    .reduce((latest, message) => {
      const nextValue = new Date(message.createdAt ?? 0).getTime();
      return nextValue > latest ? nextValue : latest;
    }, 0);
}

export default function App() {
  const initialResetState = useMemo(() => createResetStateFromLocation(), []);
  const notificationButtonRef = useRef(null);
  const myInterestsSectionRef = useRef(null);
  const sentOffersSectionRef = useRef(null);
  const receivedOffersSectionRef = useRef(null);
  const newInterestSectionRef = useRef(null);
  const [session, setSession] = useState(() => getStoredSession());
  const [dashboard, setDashboard] = useState(null);
  const [categories, setCategories] = useState([]);
  const [interests, setInterests] = useState([]);
  const [selectedInterest, setSelectedInterest] = useState(null);
  const [offers, setOffers] = useState([]);
  const [authMode, setAuthMode] = useState(initialResetState.mode);
  const [isAuthModalVisible, setIsAuthModalVisible] = useState(initialResetState.mode === "reset");
  const [loginForm, setLoginForm] = useState(initialLoginForm);
  const [registerForm, setRegisterForm] = useState(initialRegisterForm);
  const [forgotForm, setForgotForm] = useState(initialForgotForm);
  const [resetForm, setResetForm] = useState({
    token: initialResetState.token,
    newPassword: "",
    confirmPassword: ""
  });
  const [interestForm, setInterestForm] = useState(initialInterestForm);
  const [editingInterestId, setEditingInterestId] = useState(null);
  const [offerForm, setOfferForm] = useState(initialOfferForm);
  const [filters, setFilters] = useState({
    query: "",
    category: "",
    city: "",
    maxBudget: ""
  });
  const [loggedSection, setLoggedSection] = useState(loggedSections.EXPLORE);
  const [passwordRecoveryPreview, setPasswordRecoveryPreview] = useState(null);
  const [isLoadingPublic, setIsLoadingPublic] = useState(true);
  const [isLoadingPrivate, setIsLoadingPrivate] = useState(Boolean(getStoredSession()));
  const [isSubmittingAuth, setIsSubmittingAuth] = useState(false);
  const [isSubmittingInterest, setIsSubmittingInterest] = useState(false);
  const [isSubmittingOffer, setIsSubmittingOffer] = useState(false);
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

  const deferredQuery = useDeferredValue(filters.query);
  const currentUser = session?.user ?? null;
  const myInterests = (dashboard?.myInterests ?? []).slice().sort(byNewest);
  const sentOffers = (dashboard?.offersSent ?? []).slice().sort(byNewest);
  const receivedOffers = (dashboard?.offersReceived ?? []).slice().sort(byNewest);
  const isSelectedInterestMine = selectedInterest?.ownerId === currentUser?.id;

  function openFeedback(type, title, message) {
    setFeedbackModal({ type, title, message });
  }

  function openAuthModal(mode) {
    setAuthMode(mode);
    setIsAuthModalVisible(true);
  }

  function navigateTo(section) {
    setLoggedSection(section);

    if (section === loggedSections.EXPLORE) {
      setSelectedInterest((current) => interests.find((interest) => interest.id === current?.id) ?? interests[0] ?? null);
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
  }

  function openNewInterestForm() {
    setEditingInterestId(null);
    setInterestForm(initialInterestForm);
    navigateTo(loggedSections.NEW_INTEREST);
  }

  function startEditingInterest(interest) {
    setEditingInterestId(interest.id);
    setInterestForm(mapInterestToForm(interest));
    navigateTo(loggedSections.NEW_INTEREST);
  }

  function cancelInterestEditing() {
    setEditingInterestId(null);
    setInterestForm(initialInterestForm);
    navigateTo(loggedSections.MY_INTERESTS);
  }

  function closeAuthModal() {
    setIsAuthModalVisible(false);
    setPasswordRecoveryPreview(null);
    if (authMode === "reset") {
      setAuthMode("login");
    }
  }

  async function refreshPublicData(nextFilters = filters, preserveSelection = true) {
    setIsLoadingPublic(true);

    try {
      const [categoryData, interestData] = await Promise.all([
        fetchCategories(),
        fetchInterests({
          query: nextFilters.query,
          category: nextFilters.category,
          city: nextFilters.city,
          maxBudget: nextFilters.maxBudget || undefined
        })
      ]);

      setCategories(categoryData);
      setInterests(interestData);
      setSelectedInterest((currentSelected) => {
        if (preserveSelection && currentSelected) {
          return interestData.find((interest) => interest.id === currentSelected.id) ?? interestData[0] ?? null;
        }

        return interestData[0] ?? null;
      });
    } catch (requestError) {
      openFeedback("error", "Falha ao carregar", requestError.message || "Não foi possível carregar a plataforma.");
    } finally {
      setIsLoadingPublic(false);
    }
  }

  async function refreshPrivateData() {
    if (!session?.token) {
      return;
    }

    setIsLoadingPrivate(true);

    try {
      const [me, dashboardData] = await Promise.all([fetchMe(), fetchDashboard()]);
      const nextSession = {
        token: session.token,
        expiresAt: me.expiresAt,
        user: me.user
      };

      setSession(nextSession);
      storeSession(nextSession);
      setDashboard(dashboardData);
    } catch (requestError) {
      clearSession();
      setSession(null);
      setDashboard(null);
      setLoggedSection(loggedSections.EXPLORE);
      openFeedback("error", "Sessão encerrada", requestError.message || "Entre novamente para continuar.");
    } finally {
      setIsLoadingPrivate(false);
    }
  }

  useEffect(() => {
    refreshPublicData(
      {
        query: deferredQuery,
        category: filters.category,
        city: filters.city,
        maxBudget: filters.maxBudget
      },
      true
    );
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
    if (!session?.token) {
      setIsLoadingPrivate(false);
      setDashboard(null);
      return;
    }

    refreshPrivateData();
  }, [session?.token]);

  useEffect(() => {
    if (!session?.token || !selectedInterest || !isSelectedInterestMine) {
      setOffers([]);
      return;
    }

    fetchOffers(selectedInterest.id)
      .then(setOffers)
      .catch((requestError) => {
        setOffers([]);
        openFeedback("error", "Não foi possível carregar ofertas", requestError.message || "Tente novamente.");
      });
  }, [session?.token, selectedInterest?.id, isSelectedInterestMine]);

  useEffect(() => {
    if (loggedSection === loggedSections.MY_INTERESTS && myInterests.length > 0 && !isSelectedInterestMine) {
      setSelectedInterest(myInterests[0]);
    }
  }, [loggedSection, myInterests, isSelectedInterestMine]);

  useEffect(() => {
    if (!session?.token || !currentUser?.id) {
      setHasUnreadMessages(false);
      setNotifications([]);
      return;
    }

    const receivedIds = new Set(receivedOffers.map((offer) => offer.id));
    const allOffers = [...receivedOffers, ...sentOffers];
    if (allOffers.length === 0) {
      setHasUnreadMessages(false);
      setNotifications([]);
      return;
    }

    let isCancelled = false;

    Promise.all(
      allOffers.map(async (offer) => ({
        offerId: offer.id,
        section: receivedIds.has(offer.id) ? loggedSections.RECEIVED_OFFERS : loggedSections.SENT_OFFERS,
        conversation: await fetchOfferConversation(offer.id)
      }))
    )
      .then((results) => {
        if (isCancelled) {
          return;
        }

        const seenMap = readSeenMessages(currentUser.id);
        const unreadEntries = results
          .map((result) => {
            const messages = result.conversation?.messages ?? [];
            const incomingMessages = messages
              .filter((message) => message.senderId !== currentUser.id)
              .sort((left, right) => new Date(right.createdAt ?? 0).getTime() - new Date(left.createdAt ?? 0).getTime());
            const latestMessage = incomingMessages[0];
            if (!latestMessage) {
              return null;
            }

            const latestIncoming = new Date(latestMessage.createdAt ?? 0).getTime();
            const lastSeen = Number(seenMap[result.offerId] ?? 0);
            if (latestIncoming <= lastSeen) {
              return null;
            }

            return {
              offerId: result.offerId,
              section: result.section,
              title: result.conversation?.interestTitle ?? "Nova mensagem",
              message: latestMessage.content,
              createdAt: latestMessage.createdAt
            };
          })
          .filter(Boolean)
          .sort((left, right) => new Date(right.createdAt ?? 0).getTime() - new Date(left.createdAt ?? 0).getTime());

        const unreadEntry = unreadEntries[0];
        setNotifications(unreadEntries);
        setHasUnreadMessages(unreadEntries.length > 0);
      })
      .catch(() => {
        if (!isCancelled) {
          setHasUnreadMessages(false);
          setNotifications([]);
        }
      });

    return () => {
      isCancelled = true;
    };
  }, [session?.token, currentUser?.id, receivedOffers, sentOffers, messageSyncKey]);

  async function handleLoginSubmit(event) {
    event.preventDefault();
    setIsSubmittingAuth(true);

    try {
      const authResponse = await login(loginForm);
      const nextSession = {
        token: authResponse.token,
        expiresAt: authResponse.expiresAt,
        user: authResponse.user
      };

      storeSession(nextSession);
      setSession(nextSession);
      setPasswordRecoveryPreview(null);
      setLoginForm(initialLoginForm);
      closeAuthModal();
      openFeedback("success", "Login realizado", "Você entrou com sucesso na plataforma.");
    } catch (requestError) {
      openFeedback("error", "Não foi possível entrar", requestError.message || "Confira seu e-mail e senha.");
    } finally {
      setIsSubmittingAuth(false);
    }
  }

  async function handleRegisterSubmit(event) {
    event.preventDefault();
    setIsSubmittingAuth(true);

    try {
      const authResponse = await register(registerForm);
      const nextSession = {
        token: authResponse.token,
        expiresAt: authResponse.expiresAt,
        user: authResponse.user
      };

      storeSession(nextSession);
      setSession(nextSession);
      setRegisterForm(initialRegisterForm);
      openNewInterestForm();
      closeAuthModal();
      openFeedback("success", "Conta criada", "Sua conta foi criada e você já pode cadastrar interesses.");
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

    if (!session?.token) {
      openAuthModal("register");
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
      await Promise.all([refreshPrivateData(), refreshPublicData()]);
      navigateTo(loggedSections.MY_INTERESTS);
      openFeedback(
        "success",
        editingInterestId ? "Anúncio atualizado" : "Interesse publicado",
        editingInterestId
          ? "Seu anúncio foi atualizado com sucesso."
          : "Seu interesse foi publicado com sucesso."
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

    if (!session?.token) {
      openAuthModal("login");
      return;
    }

    if (!selectedInterest) {
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
    navigateTo(notification.section ?? loggedSections.RECEIVED_OFFERS);
    await openConversation(notification.offerId);
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
      [notification.offerId]: new Date(notification.createdAt ?? 0).getTime()
    }), seenMap);

    writeSeenMessages(currentUser.id, nextSeenMap);
    setNotifications([]);
    setHasUnreadMessages(false);
    setIsNotificationModalVisible(false);
    setMessageSyncKey((current) => current + 1);
  }

  function renderPublicHome(showHero = true) {
    return (
      <>
        {showHero ? (
          <section className="hero hero--public">
            <div className="hero__copy">
              <div className="brand-inline">
                <img src={logo} alt="Eu Procuro" />
                <span className="eyebrow">Busca inteligente</span>
              </div>
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
                  setFilters((current) => ({ ...current, query: event.target.value }))
                }
                placeholder="Buscar por produto, serviço ou palavra-chave"
              />

              <select
                value={filters.category}
                onChange={(event) =>
                  setFilters((current) => ({ ...current, category: event.target.value }))
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
                  setFilters((current) => ({ ...current, city: event.target.value }))
                }
                placeholder="Cidade"
              />

              <input
                type="number"
                min="0"
                value={filters.maxBudget}
                onChange={(event) =>
                  setFilters((current) => ({ ...current, maxBudget: event.target.value }))
                }
                placeholder="Orçamento máximo"
              />
            </div>

            {isLoadingPublic ? (
              <div className="loading-card">Carregando interesses publicados...</div>
            ) : interests.length === 0 ? (
              <EmptyState
                title="Nada publicado ainda"
                description="Quando os primeiros interesses forem cadastrados, eles aparecerão aqui."
              />
            ) : (
              <div className="interest-list">
                {interests.map((interest) => (
                  <InterestCard
                    key={interest.id}
                    interest={interest}
                    selected={interest.id === selectedInterest?.id}
                    onClick={setSelectedInterest}
                  />
                ))}
              </div>
            )}
          </article>

          <aside className="panel panel--sticky">
            <div className="panel__header">
              <div>
                <span className="eyebrow">Detalhe</span>
                <h2>{selectedInterest?.title ?? "Selecione um interesse"}</h2>
              </div>
            </div>

            {selectedInterest ? (
              <>
                {selectedInterest.referenceImageUrl ? (
                  <img
                    className="detail-image"
                    src={selectedInterest.referenceImageUrl}
                    alt={selectedInterest.title}
                  />
                ) : null}

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
                </div>

                <p className="detail-description">{selectedInterest.description}</p>

                <div className="tag-cluster">
                  {selectedInterest.tags?.map((tag) => (
                    <span key={tag}>{tag}</span>
                  ))}
                </div>

                {session?.token ? (
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
                          setOfferForm((current) => ({ ...current, message: event.target.value }))
                        }
                        required
                      />
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
                        disabled={isSubmittingOffer}
                      >
                        {isSubmittingOffer ? "Enviando..." : "Enviar oferta"}
                      </button>
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

  function renderLoggedArea() {
    const statCards = [
      {
        key: loggedSections.MY_INTERESTS,
        label: "Interesses Ativos",
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
      }
    ];

    return (
      <>
        <section className="hero hero--private">
          <div className="hero__copy">
            <span className="eyebrow">Área logada</span>
            <h1>Anuncie seus interesses e aguarde por uma proposta!</h1>
            <p>
              Explore o que está publicado na home ou navegue pelas páginas de interesses ativos,
              ofertas enviadas e ofertas recebidas.
            </p>
          </div>

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
            className={loggedSection === loggedSections.MY_INTERESTS ? "active" : ""}
            onClick={() => navigateTo(loggedSections.MY_INTERESTS)}
          >
            Interesses ativos
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
            className={loggedSection === loggedSections.NEW_INTEREST ? "active" : ""}
            onClick={openNewInterestForm}
          >
            Cadastrar interesse
          </button>
        </section>

        {loggedSection === loggedSections.EXPLORE ? renderPublicHome(false) : null}

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
                <div className="interest-list">
                  {myInterests.map((interest) => (
                    <InterestCard
                      key={interest.id}
                      interest={interest}
                      selected={interest.id === selectedInterest?.id}
                      onClick={setSelectedInterest}
                    />
                  ))}
                </div>
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
                    />
                  ) : null}

                  <p className="detail-description">{selectedInterest.description}</p>

                  <button
                    type="button"
                    className="ghost-button"
                    onClick={() => startEditingInterest(selectedInterest)}
                  >
                    Editar anúncio
                  </button>

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
                      offers.map((offer) => (
                        <button
                          key={offer.id}
                          type="button"
                          className="offer-card offer-card--button"
                          onClick={() => openConversation(offer.id)}
                        >
                          <div className="offer-card__head">
                            <div>
                              <strong>{offer.sellerName}</strong>
                              <span>{offer.sellerEmail}</span>
                            </div>
                            <strong>{currency(offer.offeredPrice)}</strong>
                          </div>
                          <p>{offer.message}</p>
                          <div className="offer-card__footer">
                            <span>{offer.sellerPhone}</span>
                            <span>{formatTimestamp(offer.createdAt)}</span>
                          </div>
                        </button>
                      ))
                    )}
                  </div>
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
              <div className="activity-grid">
                {sentOffers.map((offer) => (
                  <button
                    key={offer.id}
                    type="button"
                    className="activity-card activity-card--text activity-card--button"
                    onClick={() => openConversation(offer.id)}
                  >
                    <strong>{offer.interestTitle}</strong>
                    <p>{offer.message}</p>
                    <div className="offer-card__footer">
                      <span>{currency(offer.offeredPrice)}</span>
                      <span>{formatTimestamp(offer.createdAt)}</span>
                    </div>
                  </button>
                ))}
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
              <div className="activity-grid">
                {receivedOffers.map((offer) => (
                  <button
                    key={offer.id}
                    type="button"
                    className="activity-card activity-card--text activity-card--button"
                    onClick={() => openConversation(offer.id)}
                  >
                    <strong>{offer.interestTitle}</strong>
                    <p>{offer.message}</p>
                    <div className="offer-card__footer">
                      <span>{offer.sellerName}</span>
                      <span>{currency(offer.offeredPrice)}</span>
                    </div>
                  </button>
                ))}
              </div>
            ) : (
              <EmptyState
                title="Nenhuma oferta recebida"
                description="As respostas aos seus interesses ficarão listadas aqui."
              />
            )}
          </section>
        ) : null}

        {loggedSection === loggedSections.NEW_INTEREST ? (
          <section ref={newInterestSectionRef} className="panel panel--form panel--spaced">
            <div className="panel__header">
              <div>
                <span className="eyebrow">Página</span>
                <h2>{editingInterestId ? "Editar anúncio" : "Cadastrar interesse"}</h2>
              </div>
            </div>

            <form className="stacked-form" onSubmit={handleInterestSubmit}>
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
                  setInterestForm((current) => ({ ...current, title: event.target.value }))
                }
                required
              />
              <textarea
                rows="4"
                placeholder="Descreva o item ou serviço que você procura"
                value={interestForm.description}
                onChange={(event) =>
                  setInterestForm((current) => ({ ...current, description: event.target.value }))
                }
                required
              />

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
          isLoggedIn={Boolean(session?.token)}
          notificationButtonRef={notificationButtonRef}
          onLoginClick={() => openAuthModal("login")}
          onRegisterClick={() => openAuthModal("register")}
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

        {isLoadingPrivate && session?.token ? (
          <section className="loading-card loading-card--full">Carregando sua área logada...</section>
        ) : null}

        {!session?.token ? renderPublicHome(true) : renderLoggedArea()}
      </main>

      <AuthModal
        visible={isAuthModalVisible}
        mode={authMode}
        isSubmitting={isSubmittingAuth}
        loginForm={loginForm}
        registerForm={registerForm}
        forgotForm={forgotForm}
        resetForm={resetForm}
        passwordRecoveryPreview={passwordRecoveryPreview}
        onClose={closeAuthModal}
        onModeChange={setAuthMode}
        onLoginChange={setLoginForm}
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

      <FeedbackModal modal={feedbackModal} onClose={() => setFeedbackModal(null)} />

      <footer className="app-footer">
        <small>Todos os direitos reservados para Germano Antonio Fortuna</small>
      </footer>
    </div>
  );
}
