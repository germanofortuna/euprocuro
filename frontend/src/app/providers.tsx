"use client";

import { Suspense } from "react";
import { Analytics } from "@/features/analytics/analytics";
import { ContentProvider } from "@/features/content/content-context";
import { PlatformProvider } from "@/features/platform/platform-context";
import { ThemeProvider } from "@/features/theme/theme-provider";
import { FeedbackModal } from "@/shared/ui/feedback-modal";
import { usePlatform } from "@/features/platform/platform-context";
import { AuthModal } from "@/features/auth/auth-modal";

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
        <PlatformProvider>
          <Suspense fallback={null}>
            <Analytics />
          </Suspense>
          {children}
          <GlobalModals />
        </PlatformProvider>
      </ContentProvider>
    </ThemeProvider>
  );
}
