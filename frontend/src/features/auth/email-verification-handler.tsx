"use client";

import { useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { verifyEmail } from "@/shared/api/client";
import { usePlatform } from "@/features/platform/platform-context";

const VERIFIED_TOKEN_PREFIX = "euProcuro.emailVerification.verified:";

function cleanVerificationParams() {
  if (typeof window === "undefined") {
    return "/";
  }
  const params = new URLSearchParams(window.location.search);
  params.delete("mode");
  params.delete("token");
  const nextQuery = params.toString();
  return `${window.location.pathname || "/"}${nextQuery ? `?${nextQuery}` : ""}`;
}

export function EmailVerificationHandler() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { setFeedback, openAuthModal } = usePlatform();
  const handledTokenRef = useRef("");

  useEffect(() => {
    const mode = searchParams.get("mode");
    const token = searchParams.get("token")?.trim() ?? "";

    if (mode !== "verify-email" || !token || handledTokenRef.current === token) {
      return;
    }

    handledTokenRef.current = token;
    const storageKey = `${VERIFIED_TOKEN_PREFIX}${token}`;
    const alreadyVerified = typeof window !== "undefined" && window.sessionStorage.getItem(storageKey) === "1";

    if (alreadyVerified) {
      setFeedback({
        type: "success",
        title: "E-mail verificado",
        message: "Seu e-mail ja foi verificado. Agora voce pode entrar."
      });
      openAuthModal("login");
      router.replace(cleanVerificationParams(), { scroll: false });
      return;
    }

    verifyEmail(token)
      .then(() => {
        if (typeof window !== "undefined") {
          window.sessionStorage.setItem(storageKey, "1");
        }
        setFeedback({
          type: "success",
          title: "E-mail verificado",
          message: "Seu e-mail foi verificado com sucesso. Agora voce pode entrar."
        });
        openAuthModal("login");
        router.replace(cleanVerificationParams(), { scroll: false });
      })
      .catch((error) => {
        const message = error instanceof Error ? error.message : "O link de verificacao pode ter expirado.";
        setFeedback({
          type: "error",
          title: "Nao foi possivel verificar",
          message
        });
        router.replace(cleanVerificationParams(), { scroll: false });
      });
  }, [openAuthModal, router, searchParams, setFeedback]);

  return null;
}
