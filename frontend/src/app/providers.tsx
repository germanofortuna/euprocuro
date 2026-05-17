"use client";

import { Suspense } from "react";
import { ContentProvider } from "@/features/content/content-context";
import { PlatformProvider } from "@/features/platform/platform-context";
import { CookieConsentBanner } from "@/features/privacy/cookie-consent-banner";
import { CookieConsentProvider } from "@/features/privacy/cookie-consent-context";
import { CookiePreferencesModal } from "@/features/privacy/cookie-preferences-modal";
import { OptionalScripts } from "@/features/privacy/optional-scripts";
import { ThemeProvider } from "@/features/theme/theme-provider";
import { FeedbackModal } from "@/shared/ui/feedback-modal";
import { usePlatform } from "@/features/platform/platform-context";
import { AuthModal } from "@/features/auth/auth-modal";
import { NavigationLoading } from "@/shared/ui/navigation-loading";

function GlobalModals() {
  const { feedback, setFeedback } = usePlatform();
  return (
    <>
      <AuthModal />
      <FeedbackModal modal={feedback} onClose={() => setFeedback(null)} />
    </>
  );
}

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider>
      <ContentProvider>
        <CookieConsentProvider>
          <PlatformProvider>
            <Suspense fallback={null}>
              <OptionalScripts />
              <NavigationLoading />
            </Suspense>
            {children}
            <CookieConsentBanner />
            <CookiePreferencesModal />
            <GlobalModals />
          </PlatformProvider>
        </CookieConsentProvider>
      </ContentProvider>
    </ThemeProvider>
  );
}
