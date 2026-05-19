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
            options: {
              theme?: "outline" | "filled_blue" | "filled_black";
              size?: "large" | "medium" | "small";
              shape?: "rectangular" | "pill" | "circle" | "square";
              text?: "signin_with" | "signup_with" | "continue_with" | "signin";
              locale?: string;
              width?: number;
            }
          ) => void;
        };
      };
    };
  }
}

export const isGoogleSignInEnabled = Boolean(clientId);

export function GoogleSignInButton({
  disabled,
  label = "continue_with",
  onCredential
}: {
  disabled?: boolean;
  label?: "signin_with" | "signup_with" | "continue_with" | "signin";
  onCredential: (idToken: string) => void;
}) {
  const buttonRef = useRef<HTMLDivElement | null>(null);
  const [scriptReady, setScriptReady] = useState(false);

  useEffect(() => {
    if (!clientId || !scriptReady || !buttonRef.current || !window.google?.accounts?.id) {
      return;
    }
    // render button using container width and current theme (light/dark)
    const render = () => {
      if (!buttonRef.current || !window.google?.accounts?.id) return;
      const container = buttonRef.current;
      const containerWidth = Math.min(Math.max(container.clientWidth || 400, 240), 400);

      container.innerHTML = "";
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => {
          if (!disabled && response.credential) {
            onCredential(response.credential);
          }
        }
      });

      const isDark = typeof document !== "undefined" && document.documentElement?.dataset?.theme === "dark";

      window.google.accounts.id.renderButton(container, {
        theme: isDark ? "filled_black" : "outline",
        size: "large",
        shape: "rectangular",
        text: label,
        locale: "pt-BR",
        width: containerWidth
      });

    };

    render();

    // Re-render when container resizes so the google button width follows
    let ro: ResizeObserver | undefined;
    if (typeof ResizeObserver !== "undefined" && buttonRef.current) {
      ro = new ResizeObserver(() => render());
      ro.observe(buttonRef.current);
    }

    return () => {
      if (ro && buttonRef.current) {
        ro.unobserve(buttonRef.current);
      }
    };
  }, [disabled, label, onCredential, scriptReady]);

  if (!clientId) {
    return null;
  }

  return (
    <div className={disabled ? "google-signin is-disabled" : "google-signin"}>
      <Script
        src="https://accounts.google.com/gsi/client"
        strategy="afterInteractive"
        onLoad={() => setScriptReady(true)}
        onReady={() => setScriptReady(true)}
      />
      <div className="google-signin__button" ref={buttonRef} />
    </div>
  );
}
