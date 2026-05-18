"use client";

import Script from "next/script";
import { usePathname, useSearchParams } from "next/navigation";
import { useEffect } from "react";
import { hasCookieConsent } from "@/features/privacy/cookie-consent";

const measurementId = process.env.NEXT_PUBLIC_GA_MEASUREMENT_ID?.trim();

declare global {
  interface Window {
    dataLayer?: unknown[];
    gtag?: (...args: unknown[]) => void;
  }
}

export function trackPageView(path: string) {
  if (!measurementId || !hasCookieConsent("analytics") || typeof window.gtag !== "function") {
    return;
  }
  window.gtag("event", "page_view", {
    page_path: path,
    page_location: window.location.href,
    page_title: document.title
  });
}

export function trackEvent(name: string, params: Record<string, unknown> = {}) {
  if (!measurementId || !hasCookieConsent("analytics") || typeof window.gtag !== "function") {
    return;
  }
  window.gtag("event", name, params);
}

function AnalyticsRouteTracker() {
  const pathname = usePathname();
  const searchParams = useSearchParams();

  useEffect(() => {
    const query = searchParams.toString();
    trackPageView(`${pathname}${query ? `?${query}` : ""}`);
  }, [pathname, searchParams]);

  return null;
}

export function Analytics() {
  if (!measurementId || !hasCookieConsent("analytics")) {
    return null;
  }

  return (
    <>
      <Script src={`https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(measurementId)}`} strategy="afterInteractive" />
      <Script id="ga4-init" strategy="afterInteractive">
        {`
          window.dataLayer = window.dataLayer || [];
          function gtag(){dataLayer.push(arguments);}
          window.gtag = gtag;
          gtag('js', new Date());
          gtag('config', '${measurementId}', { send_page_view: false });
        `}
      </Script>
      <AnalyticsRouteTracker />
    </>
  );
}
