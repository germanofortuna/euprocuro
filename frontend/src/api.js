import defaultContent from "./content/default-content.json";

const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080/api";
const SESSION_STORAGE_KEY = "eu-procuro-session";
const GENERIC_REQUEST_ERROR = defaultContent.entries["errors.request.generic"];

function buildWebSocketUrl() {
  const configuredBase = import.meta.env.VITE_WS_BASE;
  const apiUrl = new URL(API_BASE, window.location.origin);
  const defaultProtocol = apiUrl.protocol === "https:" ? "wss:" : "ws:";
  const defaultBase = `${defaultProtocol}//${apiUrl.host}/ws/chat`;
  const url = new URL(configuredBase || defaultBase, window.location.origin);

  return url.toString();
}

function buildErrorMessage(payload, fallbackMessage) {
  if (!payload) {
    return fallbackMessage;
  }

  if (typeof payload === "string" && payload.trim()) {
    return payload;
  }

  if (Array.isArray(payload.details) && payload.details.length > 0) {
    return payload.details.join(" ");
  }

  if (payload.message) {
    return payload.message;
  }

  return fallbackMessage;
}

export class ApiError extends Error {
  constructor(message, { status, payload } = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status ?? null;
    this.payload = payload ?? null;
  }
}

export function isAuthError(error) {
  return error instanceof ApiError && (error.status === 401 || error.status === 403);
}

async function request(path, options = {}) {
  const session = getStoredSession();
  const headers = new Headers(options.headers ?? {});

  if (!headers.has("Content-Type") && options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  if (session?.token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${session.token}`);
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    credentials: "include",
    headers
  });

  if (!response.ok) {
    let payload = null;
    try {
      payload = await response.clone().json();
    } catch (error) {
      payload = await response.text().catch(() => null);
    }

    throw new ApiError(
      buildErrorMessage(payload, GENERIC_REQUEST_ERROR),
      { status: response.status, payload }
    );
  }

  return response.status === 204 ? null : response.json();
}

export function getStoredSession() {
  const rawValue = window.localStorage.getItem(SESSION_STORAGE_KEY);

  if (!rawValue) {
    return null;
  }

  try {
    const session = JSON.parse(rawValue);

    const hasToken = Boolean(session?.token);
    const hasUser = Boolean(session?.user?.id);

    if (!hasToken && !hasUser) {
      window.localStorage.removeItem(SESSION_STORAGE_KEY);
      return null;
    }

    return session;
  } catch (error) {
    window.localStorage.removeItem(SESSION_STORAGE_KEY);
    return null;
  }
}

export function storeSession(session) {
  if (!session) {
    window.localStorage.removeItem(SESSION_STORAGE_KEY);
    return;
  }

  const sanitizedSession = {
    expiresAt: session.expiresAt ?? null,
    token: session.token ?? null,
    user: session.user ?? null
  };

  const hasToken = Boolean(sanitizedSession.token);
  const hasUser = Boolean(sanitizedSession.user?.id);

  if (!hasToken && !hasUser) {
    window.localStorage.removeItem(SESSION_STORAGE_KEY);
    return;
  }

  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(sanitizedSession));
}

export function clearSession() {
  window.localStorage.removeItem(SESSION_STORAGE_KEY);
}

export function connectChatSocket({ onMessage, onOpen, onClose, onError } = {}) {
  if (typeof WebSocket === "undefined") {
    return null;
  }

  const socket = new WebSocket(buildWebSocketUrl());

  socket.onopen = () => {
    onOpen?.();
  };

  socket.onmessage = (event) => {
    try {
      onMessage?.(JSON.parse(event.data));
    } catch (error) {
      onError?.(error);
    }
  };

  socket.onerror = (event) => {
    onError?.(event);
  };

  socket.onclose = (event) => {
    onClose?.(event);
  };

  return socket;
}

export async function login(payload) {
  return request("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function register(payload) {
  return request("/auth/register", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function fetchMe() {
  return request("/auth/me");
}

export async function logout() {
  return request("/auth/logout", {
    method: "POST"
  });
}

export async function forgotPassword(payload) {
  return request("/auth/forgot-password", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function resetPassword(payload) {
  return request("/auth/reset-password", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function verifyEmail(token) {
  const params = new URLSearchParams({ token });
  return request(`/auth/verify-email?${params.toString()}`);
}

export async function fetchPublicContent(keys = []) {
  const params = new URLSearchParams({ locale: "pt-BR" });
  if (keys.length) {
    params.set("keys", keys.join(","));
  }
  return request(`/content/public?${params.toString()}`);
}

export async function fetchDashboard() {
  return request("/dashboard");
}

export async function fetchMonetizationAccount() {
  return request("/monetization/account");
}

export async function cancelSubscription() {
  return request("/monetization/subscription", {
    method: "DELETE"
  });
}

export async function purchaseProduct(payload) {
  return request("/monetization/purchase", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function syncPayment(payload) {
  return request("/monetization/payments/sync", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function boostInterest(interestId, payload) {
  return request(`/monetization/interests/${interestId}/boost`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function fetchCategories() {
  return request("/categories");
}

export async function lookupAddressByPostalCode(postalCode) {
  const normalizedPostalCode = String(postalCode ?? "").replace(/\D/g, "");
  return request(`/addresses/postal-code/${normalizedPostalCode}`);
}

export async function fetchInterests(filters = {}) {
  const params = new URLSearchParams();

  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, value);
    }
  });

  const queryString = params.toString();
  return request(`/interests${queryString ? `?${queryString}` : ""}`);
}

export async function fetchInterest(interestId) {
  return request(`/interests/${interestId}`);
}

export async function createInterest(payload) {
  return request("/interests", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function updateInterest(interestId, payload) {
  return request(`/interests/${interestId}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export async function renewInterest(interestId) {
  return request(`/interests/${interestId}/renew`, {
    method: "PATCH"
  });
}

export async function closeInterest(interestId) {
  return request(`/interests/${interestId}/close`, {
    method: "PATCH"
  });
}

export async function activateInterest(interestId) {
  return request(`/interests/${interestId}/activate`, {
    method: "PATCH"
  });
}

export async function deleteInterest(interestId) {
  return request(`/interests/${interestId}`, {
    method: "DELETE"
  });
}

export async function fetchOffers(interestId) {
  return request(`/interests/${interestId}/offers`);
}

export async function createOffer(interestId, payload) {
  return request(`/interests/${interestId}/offers`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function reportInterest(interestId, payload) {
  return request(`/interests/${interestId}/reports`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function createOmbudsmanRequest(payload) {
  return request("/ouvidoria", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function fetchOfferConversation(offerId) {
  return request(`/offers/${offerId}/conversation`);
}

export async function sendOfferMessage(offerId, payload) {
  return request(`/offers/${offerId}/messages`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function fetchSellerItems({ includeInactive = false } = {}) {
  const query = includeInactive ? "?includeInactive=true" : "";
  return request(`/seller-items${query}`);
}

export async function createSellerItem(payload) {
  return request("/seller-items", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function updateSellerItem(itemId, payload) {
  return request(`/seller-items/${itemId}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export async function deactivateSellerItem(itemId) {
  return request(`/seller-items/${itemId}/deactivate`, {
    method: "PATCH"
  });
}

export async function activateSellerItem(itemId) {
  return request(`/seller-items/${itemId}/activate`, {
    method: "PATCH"
  });
}

export async function shareSellerItemOffer(itemId, interestId, payload) {
  return request(`/seller-items/${itemId}/interests/${interestId}/offer`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function fetchAdminModeration() {
  return request("/admin/moderation");
}

export async function fetchAdminOmbudsman(status = "") {
  const query = status ? `?${new URLSearchParams({ status }).toString()}` : "";
  return request(`/admin/ouvidoria${query}`);
}

export async function respondAdminOmbudsmanRequest(requestId, payload) {
  return request(`/admin/ouvidoria/${requestId}/response`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function updateAdminOmbudsmanStatus(requestId, status) {
  return request(`/admin/ouvidoria/${requestId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status })
  });
}

export async function fetchAdminContent() {
  return request("/admin/content");
}

export async function saveContentEntry(entryId, payload) {
  const path = entryId ? `/admin/content/${entryId}` : "/admin/content";
  return request(path, {
    method: entryId ? "PUT" : "POST",
    body: JSON.stringify(payload)
  });
}

export async function publishContentEntry(entryId) {
  return request(`/admin/content/${entryId}/publish`, {
    method: "POST"
  });
}

export async function archiveContentEntry(entryId) {
  return request(`/admin/content/${entryId}/archive`, {
    method: "POST"
  });
}

export async function applyDefaultContentEntry(entryId) {
  return request(`/admin/content/${entryId}/apply-default`, {
    method: "POST"
  });
}

export async function dismissDefaultContentEntry(entryId) {
  return request(`/admin/content/${entryId}/dismiss-default`, {
    method: "POST"
  });
}

export async function fetchAdminCatalog() {
  return request("/admin/catalog");
}

export async function saveAdminCatalog(payload) {
  return request("/admin/catalog", {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export async function invalidatePublicCache(scope = "all") {
  const params = new URLSearchParams({ scope });
  return request(`/admin/cache/invalidate?${params.toString()}`, {
    method: "POST"
  });
}

export async function saveModerationRule(ruleId, payload) {
  const path = ruleId ? `/admin/moderation/rules/${ruleId}` : "/admin/moderation/rules";
  return request(path, {
    method: ruleId ? "PUT" : "POST",
    body: JSON.stringify(payload)
  });
}

export async function deleteModerationRule(ruleId) {
  return request(`/admin/moderation/rules/${ruleId}`, {
    method: "DELETE"
  });
}

export async function decideInterestModeration(interestId, payload) {
  return request(`/admin/moderation/interests/${interestId}/decision`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function updateContentReportStatus(reportId, status) {
  return request(`/admin/moderation/reports/${reportId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status })
  });
}
