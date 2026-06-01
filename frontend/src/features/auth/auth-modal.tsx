"use client";

import { useCallback, useEffect, useState } from "react";
import type { ComponentProps } from "react";
import { X } from "lucide-react";
import { useLegalContent } from "@/features/legal/use-legal-content";
import { usePlatform } from "@/features/platform/platform-context";
import { Button } from "@/shared/ui/button";
import { LegalModal } from "@/features/legal/legal-modal";
import { forgotPassword } from "@/shared/api/client";
import { GoogleSignInButton, isGoogleSignInEnabled } from "@/features/auth/google-sign-in-button";
import { FacebookSignInButton, isFacebookSignInEnabled } from "@/features/auth/facebook-sign-in-button";
import { isTurnstileEnabled, TurnstileWidget } from "@/features/auth/turnstile-widget";

type LoginForm = {
  email: string;
  password: string;
};

type RegisterForm = {
  name: string;
  email: string;
  password: string;
  phone: string;
  termsOpened: boolean;
  termsAccepted: boolean;
};

const initialLogin: LoginForm = { email: "", password: "" };
const initialRegister: RegisterForm = {
  name: "",
  email: "",
  password: "",
  phone: "",
  termsOpened: false,
  termsAccepted: false
};

const RESEND_COOLDOWN_SECONDS = 30;

type FormSubmitHandler = NonNullable<ComponentProps<"form">["onSubmit"]>;
type SubmitHandlerEvent = Parameters<FormSubmitHandler>[0];
const TERMS_GATE_MESSAGE = "É necessário abrir os Termos de Uso antes de marcar o aceite.";

function passwordStatus(password: string) {
  if (!password) {
    return "Use pelo menos 8 caracteres, com letras e números.";
  }
  if (password.length < 8) {
    return "Senha curta: use pelo menos 8 caracteres.";
  }
  if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) {
    return "Inclua letras e números para deixar a senha válida.";
  }
  return "Senha válida.";
}

export function AuthModal() {
  const { authModal, closeAuthModal, setAuthMode, signIn, signInWithGoogle, signInWithFacebook, startSignUp, confirmSignUp, setFeedback, operationalSettings } = usePlatform();
  const { termsVersion } = useLegalContent();
  const [loginForm, setLoginForm] = useState(initialLogin);
  const [registerForm, setRegisterForm] = useState(initialRegister);
  const [forgotEmail, setForgotEmail] = useState("");
  const [resetForm, setResetForm] = useState({ token: "", newPassword: "", confirmPassword: "" });
  const [isTermsOpen, setIsTermsOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [termsReminder, setTermsReminder] = useState("");
  const [turnstileToken, setTurnstileToken] = useState("");
  const [turnstileResetKey, setTurnstileResetKey] = useState(0);
  const [loginEmailOpen, setLoginEmailOpen] = useState(false);
  const [registerEmailOpen, setRegisterEmailOpen] = useState(false);
  const [registerStep, setRegisterStep] = useState<"form" | "code">("form");
  const [verificationCode, setVerificationCode] = useState("");
  const [pendingEmail, setPendingEmail] = useState("");
  const [resendIn, setResendIn] = useState(0);

  useEffect(() => {
    setTurnstileToken("");
    setTurnstileResetKey((current) => current + 1);
    setLoginEmailOpen(false);
    setRegisterEmailOpen(false);
    setRegisterStep("form");
    setVerificationCode("");
  }, [authModal.mode, authModal.visible]);

  useEffect(() => {
    if (resendIn <= 0) {
      return;
    }
    const timer = window.setTimeout(() => setResendIn((current) => Math.max(0, current - 1)), 1000);
    return () => window.clearTimeout(timer);
  }, [resendIn]);

  const handleTurnstileToken = useCallback((token: string) => {
    setTurnstileToken(token);
  }, []);

  const handleGoogleCredential = useCallback(async (accessToken: string) => {
    setIsSubmitting(true);
    try {
      await signInWithGoogle(accessToken, turnstileToken || undefined);
    } catch (error) {
      setTurnstileToken("");
      setTurnstileResetKey((current) => current + 1);
      setFeedback({
        type: "error",
        title: "Não foi possível entrar com Google",
        message: error instanceof Error ? error.message : "Tente novamente em instantes."
      });
    } finally {
      setIsSubmitting(false);
    }
  }, [setFeedback, signInWithGoogle, turnstileToken]);

  const handleFacebookCredential = useCallback(async (accessToken: string) => {
    setIsSubmitting(true);
    try {
      await signInWithFacebook(accessToken, turnstileToken || undefined);
    } catch (error) {
      setTurnstileToken("");
      setTurnstileResetKey((current) => current + 1);
      setFeedback({
        type: "error",
        title: "Não foi possível entrar com Facebook",
        message: error instanceof Error ? error.message : "Tente novamente em instantes."
      });
    } finally {
      setIsSubmitting(false);
    }
  }, [setFeedback, signInWithFacebook, turnstileToken]);

  if (!authModal.visible) {
    return null;
  }

  const resetTurnstile = () => {
    setTurnstileToken("");
    setTurnstileResetKey((current) => current + 1);
  };

  const captchaEnabled = operationalSettings.featureFlags?.captchaEnabled !== false;
  const shouldUseTurnstile = captchaEnabled && isTurnstileEnabled;

  const requireTurnstile = () => {
    if (!shouldUseTurnstile || turnstileToken) {
      return true;
    }
    setFeedback({
      type: "warning",
      title: "Verificacao de seguranca",
      message: "Confirme a verificacao de seguranca para continuar."
    });
    return false;
  };

  const submitLogin: FormSubmitHandler = async (event) => {
    event.preventDefault();
    if (!requireTurnstile()) {
      return;
    }
    setIsSubmitting(true);
    try {
      await signIn({ ...loginForm, turnstileToken });
    } catch (error) {
      resetTurnstile();
      const message = error instanceof Error ? error.message : "Confira seu e-mail e senha.";
      setFeedback({
        type: message.toLowerCase().includes("confirme seu e-mail") ? "warning" : "error",
        title: message.toLowerCase().includes("confirme seu e-mail") ? "Confirme seu e-mail" : "Não foi possível entrar",
        message
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const sendRegistrationCode = async () => {
    await startSignUp({
      name: registerForm.name,
      email: registerForm.email,
      password: registerForm.password,
      phone: registerForm.phone,
      termsAccepted: true,
      termsVersion,
      turnstileToken
    });
  };

  const submitRegister: FormSubmitHandler = async (event) => {
    event.preventDefault();
    if (!registerForm.termsOpened || !registerForm.termsAccepted) {
      setFeedback({ type: "error", title: "Aceite os termos", message: "Abra os Termos de Uso e marque o aceite para criar sua conta." });
      return;
    }
    if (!requireTurnstile()) {
      return;
    }
    setIsSubmitting(true);
    try {
      await sendRegistrationCode();
      setPendingEmail(registerForm.email);
      setRegisterStep("code");
      setVerificationCode("");
      setResendIn(RESEND_COOLDOWN_SECONDS);
      resetTurnstile();
      setFeedback({
        type: "info",
        title: "Confirme seu telefone",
        message: "Enviamos um código por SMS. Digite-o para concluir o cadastro."
      });
    } catch (error) {
      resetTurnstile();
      setFeedback({ type: "error", title: "Não foi possível iniciar o cadastro", message: error instanceof Error ? error.message : "Revise os dados e tente novamente." });
    } finally {
      setIsSubmitting(false);
    }
  };

  const submitRegisterCode: FormSubmitHandler = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      await confirmSignUp({ email: pendingEmail, code: verificationCode.trim() });
      setRegisterForm(initialRegister);
    } catch (error) {
      setFeedback({ type: "error", title: "Código inválido", message: error instanceof Error ? error.message : "Confira o código e tente novamente." });
    } finally {
      setIsSubmitting(false);
    }
  };

  const resendCode = async () => {
    if (resendIn > 0 || !requireTurnstile()) {
      return;
    }
    setIsSubmitting(true);
    try {
      await sendRegistrationCode();
      setResendIn(RESEND_COOLDOWN_SECONDS);
      resetTurnstile();
      setFeedback({ type: "info", title: "Código reenviado", message: "Enviamos um novo código por SMS." });
    } catch (error) {
      resetTurnstile();
      setFeedback({ type: "error", title: "Não foi possível reenviar", message: error instanceof Error ? error.message : "Tente novamente em instantes." });
    } finally {
      setIsSubmitting(false);
    }
  };

  async function submitForgot(event: SubmitHandlerEvent) {
    event.preventDefault();
    if (!requireTurnstile()) {
      return;
    }
    setIsSubmitting(true);
    try {
      await forgotPassword({ email: forgotEmail, turnstileToken });
      setFeedback({
        type: "info",
        title: "Solicitacao enviada",
        message: "Se o e-mail existir, as instrucoes serao enviadas."
      });
      setAuthMode("login");
    } catch (error) {
      resetTurnstile();
      setFeedback({
        type: "error",
        title: "Falha ao solicitar redefinicao",
        message: error instanceof Error ? error.message : "Tente novamente em instantes."
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  function submitLocalFeedback(event: SubmitHandlerEvent, kind: "forgot" | "reset") {
    event.preventDefault();
    setFeedback({
      type: "info",
      title: kind === "forgot" ? "Solicitação enviada" : "Senha redefinida",
      message: kind === "forgot" ? "Se o e-mail existir, as instruções serão enviadas." : "Agora você pode entrar com a nova senha."
    });
    setAuthMode("login");
  }

  function openTerms() {
    setRegisterForm((current) => ({ ...current, termsOpened: true }));
    setTermsReminder("");
    setIsTermsOpen(true);
  }

  function remindTermsGate() {
    if (registerForm.termsOpened) {
      return;
    }
    setTermsReminder(TERMS_GATE_MESSAGE);
    setFeedback({ type: "warning", title: "Abra os termos primeiro", message: TERMS_GATE_MESSAGE });
  }

  const title = {
    login: "Acesse sua conta",
    register: "Crie sua conta",
    forgot: "Recuperar acesso",
    reset: "Criar nova senha"
  }[authModal.mode];

  const showSocial = (isGoogleSignInEnabled || isFacebookSignInEnabled)
    && (authModal.mode === "login" || (authModal.mode === "register" && registerStep === "form"));

  const turnstile = shouldUseTurnstile && (authModal.mode === "login" || authModal.mode === "register" || authModal.mode === "forgot")
    ? <TurnstileWidget onToken={handleTurnstileToken} resetKey={turnstileResetKey} />
    : null;

  return (
    <>
      <div className="modal-overlay modal-overlay--auth" role="presentation" onClick={closeAuthModal}>
        <div className={`modal-card auth-modal auth-modal--${authModal.mode}`} role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
          <div className="modal-header">
            <div>
              <span className="pill">Acesso</span>
              <h2>{title}</h2>
            </div>
            <button type="button" className="icon-button" onClick={closeAuthModal} aria-label="Fechar modal">
              <X size={18} />
            </button>
          </div>

          {authModal.mode !== "reset" && authModal.mode !== "forgot" ? (
            <div className="segmented-control">
              <button type="button" className={authModal.mode === "login" ? "is-active" : ""} onClick={() => setAuthMode("login")}>Entrar</button>
              <button type="button" className={authModal.mode === "register" ? "is-active" : ""} onClick={() => setAuthMode("register")}>Criar conta</button>
            </div>
          ) : null}

          {showSocial ? (
            <div className="auth-social-block">
              <div className="auth-social-buttons">
                <GoogleSignInButton
                  disabled={isSubmitting || (shouldUseTurnstile && !turnstileToken)}
                  label={authModal.mode === "register" ? "signup_with" : "continue_with"}
                  onCredential={handleGoogleCredential}
                />
                <FacebookSignInButton
                  disabled={isSubmitting || (shouldUseTurnstile && !turnstileToken)}
                  label={authModal.mode === "register" ? "signup_with" : "continue_with"}
                  onCredential={handleFacebookCredential}
                />
              </div>
              <span>ou use seu e-mail</span>
            </div>
          ) : null}

          {authModal.mode === "login" ? (
            loginEmailOpen ? (
              <form className="stack-form" onSubmit={submitLogin}>
                <label>
                  E-mail
                  <input type="email" value={loginForm.email} onChange={(event) => setLoginForm((current) => ({ ...current, email: event.target.value }))} required />
                </label>
                <label>
                  Senha
                  <input type="password" value={loginForm.password} onChange={(event) => setLoginForm((current) => ({ ...current, password: event.target.value }))} required />
                </label>
                {turnstile}
                <Button type="submit" disabled={isSubmitting}>{isSubmitting ? "Entrando..." : "Entrar na plataforma"}</Button>
                <button type="button" className="text-button" onClick={() => setAuthMode("forgot")}>Esqueci minha senha</button>
              </form>
            ) : (
              <Button type="button" onClick={() => setLoginEmailOpen(true)}>Entrar com e-mail e senha</Button>
            )
          ) : null}

          {authModal.mode === "register" && registerStep === "form" ? (
            registerEmailOpen ? (
              <form className="stack-form" onSubmit={submitRegister}>
                <label>Nome completo<input value={registerForm.name} onChange={(event) => setRegisterForm((current) => ({ ...current, name: event.target.value }))} required /></label>
                <label>E-mail<input type="email" value={registerForm.email} onChange={(event) => setRegisterForm((current) => ({ ...current, email: event.target.value }))} required /></label>
                <label>Celular (com DDD)<input type="tel" inputMode="numeric" placeholder="(11) 91234-5678" value={registerForm.phone} onChange={(event) => setRegisterForm((current) => ({ ...current, phone: event.target.value }))} required /></label>
                <label>Senha<input type="password" value={registerForm.password} onChange={(event) => setRegisterForm((current) => ({ ...current, password: event.target.value }))} required /><small>{passwordStatus(registerForm.password)}</small></label>
                <div className={termsReminder ? "terms-box terms-box--attention" : "terms-box"}>
                  <label className={!registerForm.termsOpened ? "is-disabled checkbox-row" : "checkbox-row"} title={!registerForm.termsOpened ? TERMS_GATE_MESSAGE : undefined} onClickCapture={!registerForm.termsOpened ? (event) => { if ((event.target as HTMLElement).closest(".text-button--inline")) { return; } event.preventDefault(); remindTermsGate(); } : undefined}>
                    <input type="checkbox" checked={registerForm.termsAccepted} aria-disabled={!registerForm.termsOpened} aria-describedby="terms-acceptance-helper" onChange={(event) => { if (!registerForm.termsOpened) { event.preventDefault(); remindTermsGate(); return; } setRegisterForm((current) => ({ ...current, termsAccepted: event.target.checked })); }} />
                    <span>Li e aceito os <button type="button" className="text-button text-button--inline" onClick={openTerms}>Termos de Uso da plataforma</button></span>
                  </label>
                  <small id="terms-acceptance-helper" className={termsReminder ? "terms-helper terms-helper--warning" : "terms-helper"}>{registerForm.termsOpened ? `Versão dos termos: ${termsVersion}` : termsReminder || "Abra os termos para habilitar o aceite."}</small>
                </div>
                {turnstile}
                <Button type="submit" disabled={isSubmitting || !registerForm.termsAccepted}>{isSubmitting ? "Enviando código..." : "Continuar"}</Button>
              </form>
            ) : (
              <Button type="button" onClick={() => setRegisterEmailOpen(true)}>Criar conta com e-mail</Button>
            )
          ) : null}

          {authModal.mode === "register" && registerStep === "code" ? (
            <form className="stack-form" onSubmit={submitRegisterCode}>
              <p className="auth-code-hint">Enviamos um código por SMS para o celular informado. Digite-o abaixo para concluir o cadastro de <strong>{pendingEmail}</strong>.</p>
              <label>Código de verificação
                <input inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={verificationCode} onChange={(event) => setVerificationCode(event.target.value.replace(/\D/g, ""))} required />
              </label>
              <Button type="submit" disabled={isSubmitting || verificationCode.trim().length < 4}>{isSubmitting ? "Confirmando..." : "Confirmar e criar conta"}</Button>
              <button type="button" className="text-button" onClick={resendCode} disabled={isSubmitting || resendIn > 0}>
                {resendIn > 0 ? `Reenviar código em ${resendIn}s` : "Reenviar código"}
              </button>
              <button type="button" className="text-button" onClick={() => { setRegisterStep("form"); setVerificationCode(""); }}>Voltar e revisar dados</button>
            </form>
          ) : null}

          {authModal.mode === "forgot" ? (
            <form className="stack-form" onSubmit={submitForgot}>
              <label>E-mail da conta<input type="email" value={forgotEmail} onChange={(event) => setForgotEmail(event.target.value)} required /></label>
              {turnstile}
              <Button type="submit">Enviar instruções</Button>
              <button type="button" className="text-button" onClick={() => setAuthMode("login")}>Voltar ao login</button>
            </form>
          ) : null}

          {authModal.mode === "reset" ? (
            <form className="stack-form" onSubmit={(event) => submitLocalFeedback(event, "reset")}>
              <label>Token<input value={resetForm.token} onChange={(event) => setResetForm((current) => ({ ...current, token: event.target.value }))} required /></label>
              <label>Nova senha<input type="password" value={resetForm.newPassword} onChange={(event) => setResetForm((current) => ({ ...current, newPassword: event.target.value }))} required /></label>
              <label>Confirmar nova senha<input type="password" value={resetForm.confirmPassword} onChange={(event) => setResetForm((current) => ({ ...current, confirmPassword: event.target.value }))} required /></label>
              <Button type="submit">Salvar nova senha</Button>
            </form>
          ) : null}
        </div>
      </div>
      <LegalModal isOpen={isTermsOpen} onClose={() => setIsTermsOpen(false)} />
    </>
  );
}
