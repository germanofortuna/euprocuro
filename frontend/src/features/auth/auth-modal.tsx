"use client";

import { useEffect, useState } from "react";
import type { ComponentProps } from "react";
import { X } from "lucide-react";
import { useLegalContent } from "@/features/legal/use-legal-content";
import { usePlatform } from "@/features/platform/platform-context";
import { formatCep, formatCpfCnpj } from "@/shared/lib/format";
import { Button } from "@/shared/ui/button";
import { LegalModal } from "@/features/legal/legal-modal";
import { lookupAddressByPostalCode } from "@/shared/api/client";

type LoginForm = {
  email: string;
  password: string;
};

type RegisterForm = {
  name: string;
  email: string;
  documentNumber: string;
  password: string;
  postalCode: string;
  city: string;
  state: string;
  neighborhood: string;
  country: string;
  termsOpened: boolean;
  termsAccepted: boolean;
};

const initialLogin: LoginForm = { email: "", password: "" };
const initialRegister: RegisterForm = {
  name: "",
  email: "",
  documentNumber: "",
  password: "",
  postalCode: "",
  city: "",
  state: "",
  neighborhood: "",
  country: "Brasil",
  termsOpened: false,
  termsAccepted: false
};

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
  const { authModal, closeAuthModal, setAuthMode, signIn, signUp, setFeedback } = usePlatform();
  const { termsVersion } = useLegalContent();
  const [loginForm, setLoginForm] = useState(initialLogin);
  const [registerForm, setRegisterForm] = useState(initialRegister);
  const [forgotEmail, setForgotEmail] = useState("");
  const [resetForm, setResetForm] = useState({ token: "", newPassword: "", confirmPassword: "" });
  const [isTermsOpen, setIsTermsOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [termsReminder, setTermsReminder] = useState("");
  const [lookupState, setLookupState] = useState<{ loading: boolean; message: string; tone: "muted" | "success" | "error" }>({
    loading: false,
    message: "",
    tone: "muted"
  });

  useEffect(() => {
    if (!authModal.visible || authModal.mode !== "register") {
      return;
    }
    const normalizedPostalCode = registerForm.postalCode.replace(/\D/g, "");
    if (normalizedPostalCode.length !== 8) {
      return;
    }
    const timer = window.setTimeout(() => {
      handlePostalCodeLookup(normalizedPostalCode);
    }, 380);
    return () => window.clearTimeout(timer);
  }, [authModal.mode, authModal.visible, registerForm.postalCode]);

  if (!authModal.visible) {
    return null;
  }

  const submitLogin: FormSubmitHandler = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      await signIn(loginForm);
    } catch (error) {
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

  const submitRegister: FormSubmitHandler = async (event) => {
    event.preventDefault();
    if (!registerForm.termsOpened || !registerForm.termsAccepted) {
      setFeedback({ type: "error", title: "Aceite os termos", message: "Abra os Termos de Uso e marque o aceite para criar sua conta." });
      return;
    }
    setIsSubmitting(true);
    try {
      await signUp({
        name: registerForm.name,
        email: registerForm.email,
        documentNumber: registerForm.documentNumber,
        password: registerForm.password,
        postalCode: registerForm.postalCode,
        city: registerForm.city,
        state: registerForm.state,
        neighborhood: registerForm.neighborhood,
        country: registerForm.country,
        termsAccepted: true,
        termsVersion
      });
      setLoginForm({ email: registerForm.email, password: "" });
    } catch (error) {
      setFeedback({ type: "error", title: "Não foi possível criar a conta", message: error instanceof Error ? error.message : "Revise os dados e tente novamente." });
    } finally {
      setIsSubmitting(false);
    }
  };

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

  async function handlePostalCodeLookup(postalCode = registerForm.postalCode) {
    const normalizedPostalCode = String(postalCode).replace(/\D/g, "");
    if (!normalizedPostalCode) {
      setLookupState({ loading: false, message: "", tone: "muted" });
      return;
    }
    if (normalizedPostalCode.length !== 8) {
      setLookupState({ loading: false, message: "Digite um CEP com 8 numeros.", tone: "error" });
      return;
    }
    setLookupState({ loading: true, message: "Buscando endereco pelo CEP...", tone: "muted" });
    try {
      const address = await lookupAddressByPostalCode(normalizedPostalCode);
      setRegisterForm((current) => ({
        ...current,
        postalCode: formatCep(String(address.postalCode ?? current.postalCode)),
        city: String(address.city ?? current.city ?? ""),
        state: String(address.state ?? current.state ?? "").toUpperCase().slice(0, 2),
        neighborhood: String(address.neighborhood ?? current.neighborhood ?? ""),
        country: String(address.country ?? current.country ?? "Brasil")
      }));
      setLookupState({ loading: false, message: "Endereco preenchido pelo CEP.", tone: "success" });
    } catch (error) {
      setLookupState({ loading: false, message: error instanceof Error ? error.message : "Nao encontramos esse CEP. Preencha cidade e UF manualmente.", tone: "error" });
    }
  }

  const title = {
    login: "Acesse sua conta",
    register: "Crie sua conta",
    forgot: "Recuperar acesso",
    reset: "Criar nova senha"
  }[authModal.mode];

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

          {authModal.mode === "login" ? (
            <form className="stack-form" onSubmit={submitLogin}>
              <label>
                E-mail
                <input type="email" value={loginForm.email} onChange={(event) => setLoginForm((current) => ({ ...current, email: event.target.value }))} required />
              </label>
              <label>
                Senha
                <input type="password" value={loginForm.password} onChange={(event) => setLoginForm((current) => ({ ...current, password: event.target.value }))} required />
              </label>
              <Button type="submit" disabled={isSubmitting}>{isSubmitting ? "Entrando..." : "Entrar na plataforma"}</Button>
              <button type="button" className="text-button" onClick={() => setAuthMode("forgot")}>Esqueci minha senha</button>
            </form>
          ) : null}

          {authModal.mode === "register" ? (
            <form className="stack-form" onSubmit={submitRegister}>
              <label>Nome completo<input value={registerForm.name} onChange={(event) => setRegisterForm((current) => ({ ...current, name: event.target.value }))} required /></label>
              <label>E-mail<input type="email" value={registerForm.email} onChange={(event) => setRegisterForm((current) => ({ ...current, email: event.target.value }))} required /></label>
              <label>CPF ou CNPJ<input value={registerForm.documentNumber} onChange={(event) => setRegisterForm((current) => ({ ...current, documentNumber: formatCpfCnpj(event.target.value) }))} maxLength={18} required /></label>
              <label>Senha<input type="password" value={registerForm.password} onChange={(event) => setRegisterForm((current) => ({ ...current, password: event.target.value }))} required /><small>{passwordStatus(registerForm.password)}</small></label>
              <div className="form-grid form-grid--3">
                <label>CEP<input inputMode="numeric" value={registerForm.postalCode} onChange={(event) => setRegisterForm((current) => ({ ...current, postalCode: formatCep(event.target.value) }))} onBlur={() => handlePostalCodeLookup()} /></label>
                <label>Cidade<input value={registerForm.city} onChange={(event) => setRegisterForm((current) => ({ ...current, city: event.target.value }))} required /></label>
                <label>UF<input value={registerForm.state} onChange={(event) => setRegisterForm((current) => ({ ...current, state: event.target.value.toUpperCase().slice(0, 2) }))} required /></label>
              </div>
              {lookupState.message ? <span className={`address-lookup-note address-lookup-note--${lookupState.tone}`} role="status" aria-live="polite" aria-busy={lookupState.loading}>{lookupState.message}</span> : null}
              <div className="form-grid">
                <label>Bairro<input value={registerForm.neighborhood} onChange={(event) => setRegisterForm((current) => ({ ...current, neighborhood: event.target.value }))} /></label>
                <label>País<input value={registerForm.country} onChange={(event) => setRegisterForm((current) => ({ ...current, country: event.target.value }))} /></label>
              </div>
              <div className={termsReminder ? "terms-box terms-box--attention" : "terms-box"}>
                <label className={!registerForm.termsOpened ? "is-disabled checkbox-row" : "checkbox-row"} title={!registerForm.termsOpened ? TERMS_GATE_MESSAGE : undefined} onClickCapture={!registerForm.termsOpened ? (event) => { if ((event.target as HTMLElement).closest(".text-button--inline")) { return; } event.preventDefault(); remindTermsGate(); } : undefined}>
                  <input type="checkbox" checked={registerForm.termsAccepted} aria-disabled={!registerForm.termsOpened} aria-describedby="terms-acceptance-helper" onChange={(event) => { if (!registerForm.termsOpened) { event.preventDefault(); remindTermsGate(); return; } setRegisterForm((current) => ({ ...current, termsAccepted: event.target.checked })); }} />
                  <span>Li e aceito os <button type="button" className="text-button text-button--inline" onClick={openTerms}>Termos de Uso da plataforma</button></span>
                </label>
                <small id="terms-acceptance-helper" className={termsReminder ? "terms-helper terms-helper--warning" : "terms-helper"}>{registerForm.termsOpened ? `Versão dos termos: ${termsVersion}` : termsReminder || "Abra os termos para habilitar o aceite."}</small>
              </div>
              <Button type="submit" disabled={isSubmitting || !registerForm.termsAccepted}>{isSubmitting ? "Criando..." : "Criar conta"}</Button>
            </form>
          ) : null}

          {authModal.mode === "forgot" ? (
            <form className="stack-form" onSubmit={(event) => submitLocalFeedback(event, "forgot")}>
              <label>E-mail da conta<input type="email" value={forgotEmail} onChange={(event) => setForgotEmail(event.target.value)} required /></label>
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
