"use client";

import Script from "next/script";
import { usePathname, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
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

function AnalyticsRouteTracker({ isReady }: { isReady: boolean }) {
  const pathname = usePathname();
  const searchParams = useSearchParams();

  useEffect(() => {
    if (!isReady) {
      return;
    }
    const query = searchParams.toString();
    trackPageView(`${pathname}${query ? `?${query}` : ""}`);
  }, [isReady, pathname, searchParams]);

  return null;
}

export function Analytics() {
  const [isReady, setIsReady] = useState(false);

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
      <Script id="ga4-ready" strategy="afterInteractive" onReady={() => setIsReady(true)}>
        {`window.__euProcuroGaReady = true;`}
      </Script>
      <AnalyticsRouteTracker isReady={isReady} />
    </>
  );
}
