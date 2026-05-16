import defaultContent from "@/content/default-content.json";
import type {
  AdminModeration,
  Category,
  Dashboard,
  Interest,
  MonetizationAccount,
  OfferConversation,
  OmbudsmanRequest,
  PublicContentCatalog,
  SellerItemGroup,
  StoredSession
} from "./types";

export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ??
  process.env.VITE_API_BASE ??
  "http://localhost:8080/api";

export const WS_BASE =
  process.env.NEXT_PUBLIC_WS_BASE ??
  process.env.VITE_WS_BASE ??
  "";

export const AUTH_SESSION_MODE =
  process.env.NEXT_PUBLIC_AUTH_SESSION_MODE ??
  process.env.VITE_AUTH_SESSION_MODE ??
  "cookie";

const SESSION_STORAGE_KEY = "eu-procuro-session";
const GENERIC_REQUEST_ERROR = defaultContent.entries["errors.request.generic"];
const NETWORK_REQUEST_ERROR = defaultContent.entries["errors.request.network"];
const inFlightGetRequests = new Map<string, Promise<unknown>>();
const PUBLIC_GET_PATHS = [
  /^\/addresses\/postal-code\/[^/]+$/,
  /^\/categories$/,
  /^\/content\/public(?:\?|$)/,
  /^\/interests(?:\?|$)/,
  /^\/interests\/[^/]+$/
];

export class ApiError extends Error {
  status: number | null;
  payload: unknown;

  constructor(message: string, { status, payload }: { status?: number | null; payload?: unknown } = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status ?? null;
    this.payload = payload ?? null;
  }
}

export function isAuthError(error: unknown) {
  return error instanceof ApiError && (error.status === 401 || error.status === 403);
}

function browserStorage() {
  return typeof window === "undefined" ? null : window.localStorage;
}

export function getStoredSession(): StoredSession | null {
  const storage = browserStorage();
  const rawValue = storage?.getItem(SESSION_STORAGE_KEY);

  if (!rawValue) {
    return null;
  }

  try {
    const session = JSON.parse(rawValue) as StoredSession;
    const normalizedSession = {
      ...session,
      token: session?.token ?? null
    };
    const hasToken = Boolean(normalizedSession.token);
    const hasUser = Boolean(normalizedSession.user?.id);

    if (!hasToken && !hasUser) {
      storage?.removeItem(SESSION_STORAGE_KEY);
      return null;
    }

    if (normalizedSession.token !== session?.token) {
      storage?.setItem(SESSION_STORAGE_KEY, JSON.stringify(normalizedSession));
    }

    return normalizedSession;
  } catch {
    storage?.removeItem(SESSION_STORAGE_KEY);
    return null;
  }
}

export function storeSession(session: StoredSession | null) {
  const storage = browserStorage();
  if (!storage) {
    return;
  }

  if (!session) {
    storage.removeItem(SESSION_STORAGE_KEY);
    return;
  }

  const sanitizedSession = {
    expiresAt: session.expiresAt ?? null,
    token: session.token ?? null,
    user: session.user ?? null
  };

  if (!sanitizedSession.token && !sanitizedSession.user?.id) {
    storage.removeItem(SESSION_STORAGE_KEY);
    return;
  }

  storage.setItem(SESSION_STORAGE_KEY, JSON.stringify(sanitizedSession));
}

export function clearSession() {
  browserStorage()?.removeItem(SESSION_STORAGE_KEY);
}

function buildErrorMessage(payload: unknown, fallbackMessage: string) {
  if (!payload) {
    return fallbackMessage;
  }

  if (typeof payload === "string" && payload.trim()) {
    return payload;
  }

  if (typeof payload === "object" && payload) {
    const record = payload as { details?: string[]; message?: string };
    if (Array.isArray(record.details) && record.details.length > 0) {
      return record.details.join(" ");
    }
    if (record.message) {
      return record.message;
    }
  }

  return fallbackMessage;
}

function buildInFlightGetKey(url: string, headers: Headers) {
  return [url, headers.get("Authorization") ?? ""].join("|");
}

function isPublicGetRequest(path: string, method: string) {
  if (method.toUpperCase() !== "GET") {
    return false;
  }

  return PUBLIC_GET_PATHS.some((pattern) => pattern.test(path));
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const session = getStoredSession();
  const headers = new Headers(options.headers ?? {});
  const method = options.method ?? "GET";
  const publicRead = isPublicGetRequest(path, method);
  const sendAuth = Boolean(session?.token) && !publicRead;

  if (!headers.has("Content-Type") && options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  if (sendAuth && session?.token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${session.token}`);
  }

  const url = `${API_BASE}${path}`;
  const requestOptions: RequestInit = {
    ...options,
    credentials: publicRead ? "omit" : "include",
    headers
  };
  const shouldDeduplicate = method.toUpperCase() === "GET" && options.body === undefined;
  const requestKey = shouldDeduplicate ? buildInFlightGetKey(url, headers) : null;

  if (requestKey && inFlightGetRequests.has(requestKey)) {
    return inFlightGetRequests.get(requestKey) as Promise<T>;
  }

  const requestPromise = fetch(url, requestOptions)
    .then(async (response) => {
      if (!response.ok) {
        let payload: unknown = null;
        try {
          payload = await response.clone().json();
        } catch {
          payload = await response.text().catch(() => null);
        }

        throw new ApiError(buildErrorMessage(payload, GENERIC_REQUEST_ERROR), {
          status: response.status,
          payload
        });
      }

      return response.status === 204 ? (null as T) : ((await response.json()) as T);
    })
    .catch((error) => {
      if (error instanceof ApiError) {
        throw error;
      }
      throw new ApiError(NETWORK_REQUEST_ERROR, { payload: error instanceof Error ? error.message : null });
    });

  if (requestKey) {
    inFlightGetRequests.set(requestKey, requestPromise);
    requestPromise.then(
      () => inFlightGetRequests.delete(requestKey),
      () => inFlightGetRequests.delete(requestKey)
    );
  }

  return requestPromise;
}

export async function serverPublicRequest<T>(path: string, revalidate = 60): Promise<T | null> {
  try {
    const response = await fetch(`${API_BASE}${path}`, {
      credentials: "omit",
      next: { revalidate }
    });
    if (!response.ok) {
      return null;
    }
    return (await response.json()) as T;
  } catch {
    return null;
  }
}

function buildWebSocketUrl() {
  if (WS_BASE) {
    return WS_BASE;
  }
  const apiUrl = new URL(API_BASE, typeof window === "undefined" ? "http://localhost:5173" : window.location.origin);
  const defaultProtocol = apiUrl.protocol === "https:" ? "wss:" : "ws:";
  return `${defaultProtocol}//${apiUrl.host}/ws/chat`;
}

export function connectChatSocket({ onMessage, onOpen, onClose, onError }: {
  onMessage?: (payload: unknown) => void;
  onOpen?: () => void;
  onClose?: (event: CloseEvent) => void;
  onError?: (event: Event | Error) => void;
} = {}) {
  if (typeof WebSocket === "undefined") {
    return null;
  }

  const socket = new WebSocket(buildWebSocketUrl());
  socket.onopen = () => onOpen?.();
  socket.onmessage = (event) => {
    try {
      onMessage?.(JSON.parse(event.data));
    } catch (error) {
      onError?.(error instanceof Error ? error : new Error("Invalid socket payload"));
    }
  };
  socket.onerror = (event) => onError?.(event);
  socket.onclose = (event) => onClose?.(event);
  return socket;
}

export const login = (payload: unknown) => request<StoredSession>("/auth/login", { method: "POST", body: JSON.stringify(payload) });
export const register = (payload: unknown) => request<{ message?: string }>("/auth/register", { method: "POST", body: JSON.stringify(payload) });
export const fetchMe = () => request<Record<string, unknown>>("/auth/me");
export const logout = () => request<null>("/auth/logout", { method: "POST" });
export const forgotPassword = (payload: unknown) => request<Record<string, unknown>>("/auth/forgot-password", { method: "POST", body: JSON.stringify(payload) });
export const resetPassword = (payload: unknown) => request<null>("/auth/reset-password", { method: "POST", body: JSON.stringify(payload) });
export const verifyEmail = (token: string) => request<{ message?: string }>(`/auth/verify-email?${new URLSearchParams({ token })}`);
export const fetchPublicContent = (keys: string[] = []) => {
  const params = new URLSearchParams({ locale: "pt-BR" });
  if (keys.length) params.set("keys", keys.join(","));
  return request<PublicContentCatalog>(`/content/public?${params}`);
};
export const fetchDashboard = () => request<Dashboard>("/dashboard");
export const fetchMonetizationAccount = () => request<MonetizationAccount>("/monetization/account");
export const cancelSubscription = () => request<MonetizationAccount>("/monetization/subscription", { method: "DELETE" });
export const purchaseProduct = (payload: unknown) => request<Record<string, unknown>>("/monetization/purchase", { method: "POST", body: JSON.stringify(payload) });
export const syncPayment = (payload: unknown) => request<null>("/monetization/payments/sync", { method: "POST", body: JSON.stringify(payload) });
export const boostInterest = (interestId: string, payload: unknown) => request<Record<string, unknown>>(`/monetization/interests/${interestId}/boost`, { method: "POST", body: JSON.stringify(payload) });
export const fetchCategories = () => request<Category[]>("/categories");
export const lookupAddressByPostalCode = (postalCode: string) => request<Record<string, unknown>>(`/addresses/postal-code/${String(postalCode).replace(/\D/g, "")}`);
export const fetchInterests = (filters: Record<string, string | number | undefined | null> = {}) => {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") params.set(key, String(value));
  });
  return request<Interest[]>(`/interests${params.toString() ? `?${params}` : ""}`);
};
export const fetchInterest = (interestId: string) => request<Interest>(`/interests/${interestId}`);
export const createInterest = (payload: unknown) => request<Interest>("/interests", { method: "POST", body: JSON.stringify(payload) });
export const updateInterest = (interestId: string, payload: unknown) => request<Interest>(`/interests/${interestId}`, { method: "PUT", body: JSON.stringify(payload) });
export const renewInterest = (interestId: string) => request<Interest>(`/interests/${interestId}/renew`, { method: "PATCH" });
export const closeInterest = (interestId: string) => request<Interest>(`/interests/${interestId}/close`, { method: "PATCH" });
export const activateInterest = (interestId: string) => request<Interest>(`/interests/${interestId}/activate`, { method: "PATCH" });
export const deleteInterest = (interestId: string) => request<null>(`/interests/${interestId}`, { method: "DELETE" });
export const fetchOffers = (interestId: string) => request<Record<string, unknown>[]>(`/interests/${interestId}/offers`);
export const createOffer = (interestId: string, payload: unknown) => request<Record<string, unknown>>(`/interests/${interestId}/offers`, { method: "POST", body: JSON.stringify(payload) });
export const reportInterest = (interestId: string, payload: unknown) => request<Record<string, unknown>>(`/interests/${interestId}/reports`, { method: "POST", body: JSON.stringify(payload) });
export const createOmbudsmanRequest = (payload: unknown) => request<OmbudsmanRequest>("/ouvidoria", { method: "POST", body: JSON.stringify(payload) });
export const fetchOfferConversation = (offerId: string) => request<OfferConversation>(`/offers/${offerId}/conversation`);
export const sendOfferMessage = (offerId: string, payload: unknown) => request<Record<string, unknown>>(`/offers/${offerId}/messages`, { method: "POST", body: JSON.stringify(payload) });
export const fetchSellerItems = ({ includeInactive = false } = {}) => request<SellerItemGroup[]>(`/seller-items${includeInactive ? "?includeInactive=true" : ""}`);
export const createSellerItem = (payload: unknown) => request<Record<string, unknown>>("/seller-items", { method: "POST", body: JSON.stringify(payload) });
export const updateSellerItem = (itemId: string, payload: unknown) => request<Record<string, unknown>>(`/seller-items/${itemId}`, { method: "PUT", body: JSON.stringify(payload) });
export const deactivateSellerItem = (itemId: string) => request<Record<string, unknown>>(`/seller-items/${itemId}/deactivate`, { method: "PATCH" });
export const activateSellerItem = (itemId: string) => request<Record<string, unknown>>(`/seller-items/${itemId}/activate`, { method: "PATCH" });
export const shareSellerItemOffer = (itemId: string, interestId: string, payload: unknown) => request<Record<string, unknown>>(`/seller-items/${itemId}/interests/${interestId}/offer`, { method: "POST", body: JSON.stringify(payload) });
export const fetchAdminModeration = () => request<AdminModeration>("/admin/moderation");
export const fetchAdminOmbudsman = (status = "") => request<OmbudsmanRequest[]>(`/admin/ouvidoria${status ? `?${new URLSearchParams({ status })}` : ""}`);
export const respondAdminOmbudsmanRequest = (requestId: string, payload: unknown) => request<OmbudsmanRequest>(`/admin/ouvidoria/${requestId}/response`, { method: "POST", body: JSON.stringify(payload) });
export const updateAdminOmbudsmanStatus = (requestId: string, status: string) => request<OmbudsmanRequest>(`/admin/ouvidoria/${requestId}/status`, { method: "PATCH", body: JSON.stringify({ status }) });
export const fetchAdminContent = () => request<Record<string, unknown>>("/admin/content");
export const saveContentEntry = (entryId: string | null, payload: unknown) => request<Record<string, unknown>>(entryId ? `/admin/content/${entryId}` : "/admin/content", { method: entryId ? "PUT" : "POST", body: JSON.stringify(payload) });
export const publishContentEntry = (entryId: string) => request<Record<string, unknown>>(`/admin/content/${entryId}/publish`, { method: "POST" });
export const archiveContentEntry = (entryId: string) => request<Record<string, unknown>>(`/admin/content/${entryId}/archive`, { method: "POST" });
export const applyDefaultContentEntry = (entryId: string) => request<Record<string, unknown>>(`/admin/content/${entryId}/apply-default`, { method: "POST" });
export const dismissDefaultContentEntry = (entryId: string) => request<Record<string, unknown>>(`/admin/content/${entryId}/dismiss-default`, { method: "POST" });
export const fetchAdminCatalog = () => request<Record<string, unknown>>("/admin/catalog");
export const saveAdminCatalog = (payload: unknown) => request<Record<string, unknown>>("/admin/catalog", { method: "PUT", body: JSON.stringify(payload) });
export const saveOperationalFlags = (payload: unknown) => request<Record<string, unknown>>("/admin/operational-flags", { method: "PUT", body: JSON.stringify(payload) });
export const invalidatePublicCache = (scope = "all") => request<Record<string, unknown>>(`/admin/cache/invalidate?${new URLSearchParams({ scope })}`, { method: "POST" });
export const saveModerationRule = (ruleId: string | null, payload: unknown) => request<Record<string, unknown>>(ruleId ? `/admin/moderation/rules/${ruleId}` : "/admin/moderation/rules", { method: ruleId ? "PUT" : "POST", body: JSON.stringify(payload) });
export const deleteModerationRule = (ruleId: string) => request<null>(`/admin/moderation/rules/${ruleId}`, { method: "DELETE" });
export const decideInterestModeration = (interestId: string, payload: unknown) => request<Record<string, unknown>>(`/admin/moderation/interests/${interestId}/decision`, { method: "POST", body: JSON.stringify(payload) });
export const updateContentReportStatus = (reportId: string, status: string) => request<Record<string, unknown>>(`/admin/moderation/reports/${reportId}/status`, { method: "PATCH", body: JSON.stringify({ status }) });
