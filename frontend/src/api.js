const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080/api";
const SESSION_STORAGE_KEY = "eu-procuro-session";

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
      payload = await response.json();
    } catch (error) {
      payload = await response.text();
    }

    throw new Error(buildErrorMessage(payload, "Nao foi possivel completar a requisicao."));
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
    if (session && typeof session === "object" && "token" in session) {
      delete session.token;
      window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
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
    user: session.user ?? null
  };
  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(sanitizedSession));
}

export function clearSession() {
  window.localStorage.removeItem(SESSION_STORAGE_KEY);
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

export async function fetchDashboard() {
  return request("/dashboard");
}

export async function fetchCategories() {
  return request("/categories");
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

export async function fetchOffers(interestId) {
  return request(`/interests/${interestId}/offers`);
}

export async function createOffer(interestId, payload) {
  return request(`/interests/${interestId}/offers`, {
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
