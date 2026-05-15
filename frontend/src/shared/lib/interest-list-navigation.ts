const LAST_INTEREST_LIST_KEY = "eu-procuro-last-interest-list";

function isInterestListPath(pathname: string) {
  return pathname === "/categorias" || pathname.startsWith("/categorias/") || pathname === "/meus-interesses";
}

export function rememberInterestListHref(href: string) {
  if (typeof window === "undefined") {
    return;
  }
  const url = new URL(href, window.location.origin);
  if (url.origin === window.location.origin && isInterestListPath(url.pathname)) {
    window.sessionStorage.setItem(LAST_INTEREST_LIST_KEY, `${url.pathname}${url.search}`);
  }
}

export function readInterestListHref() {
  if (typeof window === "undefined") {
    return "/categorias";
  }
  const stored = window.sessionStorage.getItem(LAST_INTEREST_LIST_KEY);
  if (!stored) {
    return "/categorias";
  }
  const url = new URL(stored, window.location.origin);
  return isInterestListPath(url.pathname) ? `${url.pathname}${url.search}` : "/categorias";
}
