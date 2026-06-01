"use client";

import { useState } from "react";
import type { ComponentProps } from "react";
import { Smartphone } from "lucide-react";
import { usePlatform } from "@/features/platform/platform-context";
import { Button } from "@/shared/ui/button";

type FormSubmitHandler = NonNullable<ComponentProps<"form">["onSubmit"]>;

/**
 * Mostra o convite "verifique seu telefone e ganhe creditos" para usuarios logados que ainda
 * nao verificaram um telefone (tipicamente quem entrou via Google/Facebook). Ao confirmar o
 * codigo, o backend concede os creditos gratuitos uma unica vez por telefone.
 */
export function PhoneVerificationCard() {
  const { currentUser, operationalSettings, verifyPhoneStart, verifyPhoneConfirm, setFeedback } = usePlatform();
  const [step, setStep] = useState<"idle" | "code">("idle");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!currentUser?.id || currentUser.phoneVerified) {
    return null;
  }

  const freeCredits = Math.max(0, Number(operationalSettings.operationalFields?.initialFreeCredits ?? 0));
  const creditsLabel = freeCredits > 0 ? `${freeCredits} créditos grátis` : "seus créditos grátis";

  const start: FormSubmitHandler = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      await verifyPhoneStart(phone);
      setStep("code");
      setCode("");
      setFeedback({ type: "info", title: "Código enviado", message: "Enviamos um código por SMS para o número informado." });
    } catch (error) {
      setFeedback({ type: "error", title: "Não foi possível enviar o código", message: error instanceof Error ? error.message : "Confira o número e tente novamente." });
    } finally {
      setIsSubmitting(false);
    }
  };

  const confirm: FormSubmitHandler = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      await verifyPhoneConfirm(phone, code.trim());
      setStep("idle");
      setPhone("");
      setCode("");
      setFeedback({ type: "success", title: "Telefone verificado", message: freeCredits > 0 ? `Pronto! Adicionamos ${freeCredits} créditos à sua conta.` : "Telefone verificado com sucesso." });
    } catch (error) {
      setFeedback({ type: "error", title: "Código inválido", message: error instanceof Error ? error.message : "Confira o código e tente novamente." });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <section className="dashboard-section phone-verify-card">
      <div className="phone-verify-card__head">
        <span className="phone-verify-card__icon"><Smartphone size={20} /></span>
        <div>
          <h2>Verifique seu telefone e ganhe {creditsLabel}</h2>
          <p>Confirme um número de celular por SMS para liberar seus créditos. Cada telefone libera o bônus uma única vez.</p>
        </div>
      </div>
      {step === "idle" ? (
        <form className="phone-verify-card__form" onSubmit={start}>
          <label>Celular (com DDD)
            <input type="tel" inputMode="numeric" placeholder="(11) 91234-5678" value={phone} onChange={(event) => setPhone(event.target.value)} required />
          </label>
          <Button type="submit" disabled={isSubmitting}>{isSubmitting ? "Enviando..." : "Enviar código por SMS"}</Button>
        </form>
      ) : (
        <form className="phone-verify-card__form" onSubmit={confirm}>
          <label>Código de verificação
            <input inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))} required />
          </label>
          <div className="inline-actions">
            <Button type="submit" disabled={isSubmitting || code.trim().length < 4}>{isSubmitting ? "Confirmando..." : "Confirmar e liberar créditos"}</Button>
            <Button type="button" variant="outline" onClick={() => { setStep("idle"); setCode(""); }}>Trocar número</Button>
          </div>
        </form>
      )}
    </section>
  );
}
