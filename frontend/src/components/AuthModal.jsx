import { useState } from "react";

import { useContentText } from "../content/ContentContext";
import { useLegalContent } from "../content/useLegalContent";
import LegalModal from "./LegalModal";

function handleChange(setter, field, value) {
  setter((current) => ({ ...current, [field]: value }));
}

function formatCpfCnpj(value) {
  const digits = value.replace(/\D/g, "").slice(0, 14);
  if (digits.length <= 11) {
    return digits
      .replace(/^(\d{3})(\d)/, "$1.$2")
      .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
      .replace(/\.(\d{3})(\d)/, ".$1-$2");
  }

  return digits
    .replace(/^(\d{2})(\d)/, "$1.$2")
    .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1/$2")
    .replace(/(\d{4})(\d)/, "$1-$2");
}

function formatCep(value) {
  const digits = value.replace(/\D/g, "").slice(0, 8);
  if (digits.length <= 5) {
    return digits;
  }
  return `${digits.slice(0, 5)}-${digits.slice(5)}`;
}

function passwordStatus(password, t) {
  const value = password ?? "";
  if (!value) {
    return {
      valid: false,
      message: t("auth.password.empty")
    };
  }

  if (value.length < 8) {
    return {
      valid: false,
      message: t("auth.password.short")
    };
  }

  if (!/[A-Za-z]/.test(value) || !/\d/.test(value)) {
    return {
      valid: false,
      message: t("auth.password.invalid")
    };
  }

  return {
    valid: true,
    message: t("auth.password.valid")
  };
}

export default function AuthModal({
  visible,
  mode,
  isSubmitting,
  loginForm,
  loginInlineError,
  registerForm,
  forgotForm,
  resetForm,
  registerAddressLookup,
  passwordRecoveryPreview,
  onClose,
  onModeChange,
  onLoginChange,
  onRegisterChange,
  onForgotChange,
  onResetChange,
  onLoginSubmit,
  onRegisterSubmit,
  onRegisterPostalCodeLookup,
  onForgotSubmit,
  onResetSubmit
}) {
  const { t } = useContentText();
  const { termsVersion } = useLegalContent();
  const [isTermsModalOpen, setIsTermsModalOpen] = useState(false);

  if (!visible) {
    return null;
  }

  const titleByMode = {
    login: t("auth.login.title"),
    register: t("auth.register.title"),
    forgot: t("auth.forgot.title"),
    reset: t("auth.reset.title")
  };

  const tabs = [
    { value: "login", label: t("auth.tabs.login") },
    { value: "register", label: t("auth.tabs.register") }
  ];
  const currentPasswordStatus = passwordStatus(registerForm.password, t);

  function openTermsModal() {
    handleChange(onRegisterChange, "termsOpened", true);
    setIsTermsModalOpen(true);
  }

  return (
    <>
      <div className="modal-overlay" role="presentation" onClick={onClose}>
        <div className="auth-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
          <div className="feedback-modal__header">
            <div>
              <span className="eyebrow">{t("auth.eyebrow")}</span>
              <h2>{titleByMode[mode] ?? t("auth.login.title")}</h2>
            </div>
            <button
              type="button"
              className="modal-close-button"
              onClick={onClose}
              aria-label={t("common.actions.closeModal")}
            >
              X
            </button>
          </div>

          {mode !== "reset" ? (
            <div className="auth-tabs">
              {tabs.map((tab) => (
                <button
                  key={tab.value}
                  type="button"
                  className={mode === tab.value ? "active" : ""}
                  onClick={() => onModeChange(tab.value)}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          ) : null}

          {mode === "login" ? (
            <form className="stacked-form" onSubmit={onLoginSubmit}>
              <input
                type="email"
                placeholder={t("auth.login.email.placeholder")}
                value={loginForm.email}
                onChange={(event) => handleChange(onLoginChange, "email", event.target.value)}
                required
              />
              <input
                type="password"
                placeholder={t("auth.login.password.placeholder")}
                value={loginForm.password}
                onChange={(event) => handleChange(onLoginChange, "password", event.target.value)}
                required
              />
              {loginInlineError ? (
                <span className="form-inline-error">
                  {loginInlineError}
                </span>
              ) : null}
              <button
                type="button"
                className="text-button"
                onClick={() => onModeChange("forgot")}
              >
                {t("auth.login.forgotPassword")}
              </button>
              <button type="submit" className="primary-button" disabled={isSubmitting}>
                {isSubmitting ? t("auth.login.submitting") : t("auth.login.submit")}
              </button>
            </form>
          ) : null}

          {mode === "register" ? (
            <form className="stacked-form" onSubmit={onRegisterSubmit}>
              <input
                placeholder={t("auth.register.name.placeholder")}
                value={registerForm.name}
                onChange={(event) => handleChange(onRegisterChange, "name", event.target.value)}
                required
              />
              <input
                type="email"
                placeholder={t("auth.register.email.placeholder")}
                value={registerForm.email}
                onChange={(event) => handleChange(onRegisterChange, "email", event.target.value)}
                required
              />
              <input
                placeholder={t("auth.register.document.placeholder")}
                value={registerForm.documentNumber}
                onChange={(event) => handleChange(onRegisterChange, "documentNumber", formatCpfCnpj(event.target.value))}
                maxLength={18}
                required
              />
              <input
                type="password"
                placeholder={t("auth.register.password.placeholder")}
                value={registerForm.password}
                onChange={(event) => handleChange(onRegisterChange, "password", event.target.value)}
                required
              />
              {registerForm.password ? (
                <span className={`password-status ${currentPasswordStatus.valid ? "password-status--valid" : "password-status--invalid"}`}>
                  {currentPasswordStatus.message}
                </span>
              ) : null}
              <div className="three-columns">
                <input
                  placeholder={t("auth.register.postalCode.placeholder")}
                  value={registerForm.postalCode}
                  onChange={(event) => handleChange(onRegisterChange, "postalCode", formatCep(event.target.value))}
                  onBlur={() => onRegisterPostalCodeLookup?.(registerForm.postalCode)}
                  inputMode="numeric"
                />
                <input
                  placeholder={t("auth.register.city.placeholder")}
                  value={registerForm.city}
                  onChange={(event) => handleChange(onRegisterChange, "city", event.target.value)}
                  required
                />
                <input
                  placeholder={t("auth.register.state.placeholder")}
                  value={registerForm.state}
                  onChange={(event) => handleChange(onRegisterChange, "state", event.target.value)}
                  required
                />
              </div>
              {registerAddressLookup?.message ? (
                <span
                  className={`address-lookup-note ${registerAddressLookup.isLoading ? "is-loading" : ""}`}
                  role="status"
                  aria-live="polite"
                  aria-busy={registerAddressLookup.isLoading}
                >
                  {registerAddressLookup.message}
                </span>
              ) : null}
              <div className="two-columns">
                <input
                  placeholder={t("auth.register.neighborhood.placeholder")}
                  value={registerForm.neighborhood}
                  onChange={(event) => handleChange(onRegisterChange, "neighborhood", event.target.value)}
                />
                <input
                  placeholder={t("auth.register.country.placeholder")}
                  value={registerForm.country}
                  onChange={(event) => handleChange(onRegisterChange, "country", event.target.value)}
                />
              </div>
              <div className="terms-acceptance">
                <div className={`terms-acceptance__row ${!registerForm.termsOpened ? "is-disabled" : ""}`}>
                  <input
                    id="register-terms-accepted"
                    type="checkbox"
                    checked={Boolean(registerForm.termsAccepted)}
                    disabled={!registerForm.termsOpened}
                    onChange={(event) => handleChange(onRegisterChange, "termsAccepted", event.target.checked)}
                  />
                  <span>
                    {t("auth.register.terms.prefix")}{" "}
                    <button type="button" className="text-button text-button--inline" onClick={openTermsModal}>
                      {t("auth.register.terms.link")}
                    </button>
                  </span>
                </div>
                <small>
                  {registerForm.termsOpened
                    ? t("auth.register.terms.helper.opened", { version: termsVersion })
                    : t("auth.register.terms.helper.closed")}
                </small>
              </div>
              <button
                type="submit"
                className="primary-button"
                disabled={isSubmitting || !registerForm.termsAccepted}
              >
                {isSubmitting ? t("auth.register.submitting") : t("auth.register.submit")}
              </button>
            </form>
          ) : null}

          {mode === "forgot" ? (
            <form className="stacked-form" onSubmit={onForgotSubmit}>
              <input
                type="email"
                placeholder={t("auth.forgot.email.placeholder")}
                value={forgotForm.email}
                onChange={(event) => handleChange(onForgotChange, "email", event.target.value)}
                required
              />
              <button type="submit" className="primary-button" disabled={isSubmitting}>
                {isSubmitting ? t("auth.forgot.submitting") : t("auth.forgot.submit")}
              </button>
            </form>
          ) : null}

          {mode === "reset" ? (
            <form className="stacked-form" onSubmit={onResetSubmit}>
              <input
                placeholder={t("auth.reset.token.placeholder")}
                value={resetForm.token}
                onChange={(event) => handleChange(onResetChange, "token", event.target.value)}
                required
              />
              <input
                type="password"
                placeholder={t("auth.reset.newPassword.placeholder")}
                value={resetForm.newPassword}
                onChange={(event) => handleChange(onResetChange, "newPassword", event.target.value)}
                required
              />
              <input
                type="password"
                placeholder={t("auth.reset.confirmPassword.placeholder")}
                value={resetForm.confirmPassword}
                onChange={(event) => handleChange(onResetChange, "confirmPassword", event.target.value)}
                required
              />
              <button type="submit" className="primary-button" disabled={isSubmitting}>
                {isSubmitting ? t("auth.reset.submitting") : t("auth.reset.submit")}
              </button>
            </form>
          ) : null}

          {passwordRecoveryPreview?.previewResetLink ? (
            <div className="preview-card">
              <strong>{t("auth.localReset.title")}</strong>
              <p>{t("auth.localReset.description")}</p>
              <a href={passwordRecoveryPreview.previewResetLink}>
                {passwordRecoveryPreview.previewResetLink}
              </a>
            </div>
          ) : null}
        </div>
      </div>
      <LegalModal isOpen={isTermsModalOpen} onClose={() => setIsTermsModalOpen(false)} />
    </>
  );
}
