"use client";

import Script from "next/script";
import { useEffect, useRef, useState } from "react";

const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID?.trim() ?? "";

declare global {
  interface Window {
    google?: {
      accounts?: {
        id?: {
          initialize: (options: {
            client_id: string;
            callback: (response: { credential?: string }) => void;
          }) => void;
          renderButton: (
            element: HTMLElement,
            options: { type?: "standard" | "icon"; size?: "large" | "medium" | "small"; width?: number }
          ) => void;
        };
      };
    };
  }
}

export const isGoogleSignInEnabled = Boolean(clientId);

type Label = "signin_with" | "signup_with" | "continue_with" | "signin";

const LABEL_TEXT: Record<Label, string> = {
  signin_with: "Entrar com Google",
  signup_with: "Cadastrar com Google",
  continue_with: "Entrar com Google",
  signin: "Entrar com Google"
};

function isGisReady() {
  return typeof window !== "undefined" && Boolean(window.google?.accounts?.id);
}

export function GoogleSignInButton({
  disabled,
  label = "continue_with",
  onCredential
}: {
  disabled?: boolean;
  label?: Label;
  onCredential: (idToken: string) => void;
}) {
  const overlayRef = useRef<HTMLDivElement | null>(null);
  const [gisReady, setGisReady] = useState(() => isGisReady());
  const callbackRef = useRef(onCredential);
  const disabledRef = useRef(disabled);

  useEffect(() => {
    callbackRef.current = onCredential;
    disabledRef.current = disabled;
  });

  useEffect(() => {
    if (gisReady) {
      return;
    }
    if (isGisReady()) {
      setGisReady(true);
      return;
    }
    const interval = window.setInterval(() => {
      if (isGisReady()) {
        setGisReady(true);
        window.clearInterval(interval);
      }
    }, 100);
    return () => window.clearInterval(interval);
  }, [gisReady]);

  useEffect(() => {
    if (!clientId || !gisReady || !overlayRef.current || !window.google?.accounts?.id) {
      return;
    }
    const container = overlayRef.current;
    const render = () => {
      if (!window.google?.accounts?.id) return;
      const width = Math.min(Math.max(container.clientWidth || 320, 200), 400);
      container.innerHTML = "";
      window.google!.accounts!.id!.initialize({
        client_id: clientId,
        callback: (response) => {
          if (!disabledRef.current && response.credential) {
            callbackRef.current(response.credential);
          }
        }
      });
      window.google!.accounts!.id!.renderButton(container, { type: "standard", size: "large", width });
    };
    render();
    let observer: ResizeObserver | undefined;
    if (typeof ResizeObserver !== "undefined") {
      let lastWidth = container.clientWidth;
      observer = new ResizeObserver(() => {
        if (Math.abs(container.clientWidth - lastWidth) > 4) {
          lastWidth = container.clientWidth;
          render();
        }
      });
      observer.observe(container);
    }
    return () => observer?.disconnect();
  }, [gisReady]);

  if (!clientId) {
    return null;
  }

  return (
    <div
      className={disabled ? "google-signin is-disabled" : "google-signin"}
      role="button"
      aria-label={LABEL_TEXT[label]}
      aria-disabled={disabled || undefined}
    >
      <Script src="https://accounts.google.com/gsi/client" strategy="afterInteractive" />
      <GoogleGlyph />
      <span className="google-signin__text">{LABEL_TEXT[label]}</span>
      <div className="google-signin__overlay" ref={overlayRef} aria-hidden="true" />
    </div>
  );
}

function GoogleGlyph() {
  return (
    <svg className="google-signin__icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="20" height="20" aria-hidden="true">
      <path fill="#FFC107" d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z"/>
      <path fill="#FF3D00" d="M6.306 14.691l6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691z"/>
      <path fill="#4CAF50" d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238C29.211 35.091 26.715 36 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z"/>
      <path fill="#1976D2" d="M43.611 20.083H42V20H24v8h11.303c-.792 2.237-2.231 4.166-4.087 5.571.001-.001.002-.001.003-.002l6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z"/>
    </svg>
  );
}
