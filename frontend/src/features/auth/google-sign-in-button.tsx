"use client";

import Script from "next/script";
import { useCallback, useEffect, useRef, useState } from "react";

const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID?.trim() ?? "";

type TokenResponse = { access_token?: string; error?: string; error_description?: string };

type TokenClient = {
  requestAccessToken: (options?: { prompt?: string }) => void;
};

declare global {
  interface Window {
    google?: {
      accounts?: {
        oauth2?: {
          initTokenClient: (config: {
            client_id: string;
            scope: string;
            callback: (response: TokenResponse) => void;
            error_callback?: (error: { type?: string; message?: string }) => void;
          }) => TokenClient;
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

function isOauthReady() {
  return typeof window !== "undefined" && Boolean(window.google?.accounts?.oauth2);
}

export function GoogleSignInButton({
  disabled,
  label = "continue_with",
  onCredential
}: {
  disabled?: boolean;
  label?: Label;
  onCredential: (accessToken: string) => void;
}) {
  const [oauthReady, setOauthReady] = useState(() => isOauthReady());
  const tokenClientRef = useRef<TokenClient | null>(null);
  const callbackRef = useRef(onCredential);

  useEffect(() => {
    callbackRef.current = onCredential;
  });

  useEffect(() => {
    if (oauthReady) return;
    if (isOauthReady()) {
      setOauthReady(true);
      return;
    }
    const interval = window.setInterval(() => {
      if (isOauthReady()) {
        setOauthReady(true);
        window.clearInterval(interval);
      }
    }, 100);
    return () => window.clearInterval(interval);
  }, [oauthReady]);

  useEffect(() => {
    if (!clientId || !oauthReady || !window.google?.accounts?.oauth2) {
      return;
    }
    tokenClientRef.current = window.google.accounts.oauth2.initTokenClient({
      client_id: clientId,
      scope: "openid email profile",
      callback: (response) => {
        if (response.access_token) {
          callbackRef.current(response.access_token);
        }
      }
    });
  }, [oauthReady]);

  const handleClick = useCallback(() => {
    if (disabled || !tokenClientRef.current) return;
    tokenClientRef.current.requestAccessToken();
  }, [disabled]);

  if (!clientId) {
    return null;
  }

  return (
    <button
      type="button"
      className="google-signin"
      onClick={handleClick}
      disabled={disabled || !oauthReady}
      aria-label={LABEL_TEXT[label]}
    >
      <Script src="https://accounts.google.com/gsi/client" strategy="afterInteractive" />
      <GoogleGlyph />
      <span className="google-signin__text">{LABEL_TEXT[label]}</span>
    </button>
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
