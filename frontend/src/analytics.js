const measurementId = import.meta.env.VITE_GA_MEASUREMENT_ID?.trim();

let initialized = false;

function isEnabled() {
  return Boolean(measurementId && typeof window !== "undefined" && typeof document !== "undefined");
}

export function initAnalytics() {
  if (!isEnabled() || initialized) {
    return;
  }

  initialized = true;
  window.dataLayer = window.dataLayer || [];
  window.gtag = function gtag() {
    window.dataLayer.push(arguments);
  };

  const script = document.createElement("script");
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(measurementId)}`;
  document.head.appendChild(script);

  window.gtag("js", new Date());
  window.gtag("config", measurementId, {
    send_page_view: false
  });

  trackPageView();
}

export function trackPageView(path = `${window.location.pathname}${window.location.search}`) {
  if (!isEnabled() || typeof window.gtag !== "function") {
    return;
  }

  window.gtag("event", "page_view", {
    page_path: path,
    page_location: window.location.href,
    page_title: document.title
  });
}

export function trackEvent(name, params = {}) {
  if (!isEnabled() || typeof window.gtag !== "function") {
    return;
  }

  window.gtag("event", name, params);
}
