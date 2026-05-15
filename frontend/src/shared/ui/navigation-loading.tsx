"use client";

import { useEffect, useState } from "react";
import { usePathname, useSearchParams } from "next/navigation";

function isInternalNavigationLink(anchor: HTMLAnchorElement) {
  if (anchor.target && anchor.target !== "_self") {
    return false;
  }
  if (anchor.hasAttribute("download")) {
    return false;
  }
  const href = anchor.getAttribute("href");
  if (!href || href.startsWith("#") || href.startsWith("mailto:") || href.startsWith("tel:")) {
    return false;
  }
  const nextUrl = new URL(anchor.href, window.location.href);
  const currentUrl = new URL(window.location.href);
  if (nextUrl.origin !== currentUrl.origin) {
    return false;
  }
  return nextUrl.pathname !== currentUrl.pathname || nextUrl.search !== currentUrl.search;
}

export function NavigationLoading() {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    setIsLoading(false);
    document.documentElement.removeAttribute("data-route-loading");
  }, [pathname, searchParams]);

  useEffect(() => {
    let scheduledLoading: number | null = null;

    function startLoading() {
      if (scheduledLoading) {
        window.clearTimeout(scheduledLoading);
        scheduledLoading = null;
      }
      setIsLoading(true);
      document.documentElement.setAttribute("data-route-loading", "true");
    }

    function scheduleLoading() {
      if (scheduledLoading) {
        return;
      }
      scheduledLoading = window.setTimeout(() => {
        scheduledLoading = null;
        startLoading();
      }, 0);
    }

    function stopLoading() {
      if (scheduledLoading) {
        window.clearTimeout(scheduledLoading);
        scheduledLoading = null;
      }
      setIsLoading(false);
      document.documentElement.removeAttribute("data-route-loading");
    }

    function handleClick(event: MouseEvent) {
      if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
        return;
      }
      const target = event.target instanceof Element ? event.target.closest("a") : null;
      if (target instanceof HTMLAnchorElement && isInternalNavigationLink(target)) {
        startLoading();
      }
    }

    const originalPushState = window.history.pushState;
    const originalReplaceState = window.history.replaceState;
    window.history.pushState = function pushStateWithLoading(...args) {
      const result = originalPushState.apply(this, args);
      scheduleLoading();
      return result;
    };
    window.history.replaceState = function replaceStateWithLoading(...args) {
      const result = originalReplaceState.apply(this, args);
      scheduleLoading();
      return result;
    };

    document.addEventListener("click", handleClick, true);
    window.addEventListener("pageshow", stopLoading);
    window.addEventListener("popstate", startLoading);

    return () => {
      document.removeEventListener("click", handleClick, true);
      window.removeEventListener("pageshow", stopLoading);
      window.removeEventListener("popstate", startLoading);
      window.history.pushState = originalPushState;
      window.history.replaceState = originalReplaceState;
      if (scheduledLoading) {
        window.clearTimeout(scheduledLoading);
      }
      document.documentElement.removeAttribute("data-route-loading");
    };
  }, []);

  return (
    <div className={`route-loading${isLoading ? " is-active" : ""}`} role="status" aria-live="polite" aria-hidden={!isLoading}>
      <span>Carregando...</span>
    </div>
  );
}
