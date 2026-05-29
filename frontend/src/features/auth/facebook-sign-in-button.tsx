"use client";

import Script from "next/script";
import { useCallback, useEffect, useRef, useState } from "react";

const appId = process.env.NEXT_PUBLIC_FACEBOOK_APP_ID?.trim() ?? "";

type FacebookAuthResponse = { accessToken?: string };
type FacebookLoginResponse = { authResponse?: FacebookAuthResponse | null; status?: string };

type FacebookSdk = {
  init: (config: { appId: string; cookie?: boolean; xfbml?: boolean; version: string }) => void;
  login: (
    callback: (response: FacebookLoginResponse) => void,
    options?: { scope?: string }
  ) => void;
};

declare global {
  interface Window {
    FB?: FacebookSdk;
    fbAsyncInit?: () => void;
  }
}

export const isFacebookSignInEnabled = Boolean(appId);

type Label = "signin_with" | "signup_with" | "continue_with" | "signin";

const LABEL_TEXT: Record<Label, string> = {
  signin_with: "Entrar com Facebook",
  signup_with: "Cadastrar com Facebook",
  continue_with: "Entrar com Facebook",
  signin: "Entrar com Facebook"
};

function isSdkReady() {
  return typeof window !== "undefined" && Boolean(window.FB);
}

export function FacebookSignInButton({
  disabled,
  label = "continue_with",
  onCredential
}: {
  disabled?: boolean;
  label?: Label;
  onCredential: (accessToken: string) => void;
}) {
  const [sdkReady, setSdkReady] = useState(() => isSdkReady());
  const callbackRef = useRef(onCredential);

  useEffect(() => {
    callbackRef.current = onCredential;
  });

  useEffect(() => {
    if (!appId || sdkReady) return;
    if (isSdkReady()) {
      window.FB?.init({ appId, cookie: true, xfbml: false, version: "v21.0" });
      setSdkReady(true);
      return;
    }
    const interval = window.setInterval(() => {
      if (isSdkReady()) {
        window.FB?.init({ appId, cookie: true, xfbml: false, version: "v21.0" });
        setSdkReady(true);
        window.clearInterval(interval);
      }
    }, 100);
    return () => window.clearInterval(interval);
  }, [sdkReady]);

  const handleClick = useCallback(() => {
    if (disabled || !window.FB) return;
    window.FB.login(
      (response) => {
        const accessToken = response.authResponse?.accessToken;
        if (accessToken) {
          callbackRef.current(accessToken);
        }
      },
      { scope: "public_profile,email" }
    );
  }, [disabled]);

  if (!appId) {
    return null;
  }

  return (
    <button
      type="button"
      className="facebook-signin"
      onClick={handleClick}
      disabled={disabled || !sdkReady}
      aria-label={LABEL_TEXT[label]}
    >
      <Script src="https://connect.facebook.net/pt_BR/sdk.js" strategy="afterInteractive" crossOrigin="anonymous" />
      <FacebookGlyph />
      <span className="facebook-signin__text">{LABEL_TEXT[label]}</span>
    </button>
  );
}

function FacebookGlyph() {
  return (
    <svg className="facebook-signin__icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
      <path fill="#FFFFFF" d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
    </svg>
  );
}
