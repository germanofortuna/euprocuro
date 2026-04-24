function handleChange(setter, field, value) {
  setter((current) => ({ ...current, [field]: value }));
}

export default function AuthModal({
  visible,
  mode,
  isSubmitting,
  loginForm,
  registerForm,
  forgotForm,
  resetForm,
  passwordRecoveryPreview,
  onClose,
  onModeChange,
  onLoginChange,
  onRegisterChange,
  onForgotChange,
  onResetChange,
  onLoginSubmit,
  onRegisterSubmit,
  onForgotSubmit,
  onResetSubmit
}) {
  if (!visible) {
    return null;
  }

  const titleByMode = {
    login: "Entrar na plataforma",
    register: "Criar sua conta",
    forgot: "Recuperar acesso",
    reset: "Criar nova senha"
  };

  const tabs = [
    { value: "login", label: "Entrar" },
    { value: "register", label: "Criar conta" }
  ];

  return (
    <div className="modal-overlay" role="presentation" onClick={onClose}>
      <div className="auth-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
        <div className="feedback-modal__header">
          <div>
            <span className="eyebrow">Acesso</span>
            <h2>{titleByMode[mode] ?? "Entrar na plataforma"}</h2>
          </div>
          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            aria-label="Fechar modal"
          >
            ×
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
              placeholder="Seu e-mail"
              value={loginForm.email}
              onChange={(event) => handleChange(onLoginChange, "email", event.target.value)}
              required
            />
            <input
              type="password"
              placeholder="Sua senha"
              value={loginForm.password}
              onChange={(event) => handleChange(onLoginChange, "password", event.target.value)}
              required
            />
            <button
              type="button"
              className="text-button"
              onClick={() => onModeChange("forgot")}
            >
              Esqueci minha senha
            </button>
            <button type="submit" className="primary-button" disabled={isSubmitting}>
              {isSubmitting ? "Entrando..." : "Entrar"}
            </button>
          </form>
        ) : null}

        {mode === "register" ? (
          <form className="stacked-form" onSubmit={onRegisterSubmit}>
            <input
              placeholder="Nome completo"
              value={registerForm.name}
              onChange={(event) => handleChange(onRegisterChange, "name", event.target.value)}
              required
            />
            <input
              type="email"
              placeholder="E-mail"
              value={registerForm.email}
              onChange={(event) => handleChange(onRegisterChange, "email", event.target.value)}
              required
            />
            <input
              type="password"
              placeholder="Senha com ao menos 6 caracteres"
              value={registerForm.password}
              onChange={(event) => handleChange(onRegisterChange, "password", event.target.value)}
              required
            />
            <div className="two-columns">
              <input
                placeholder="Cidade"
                value={registerForm.city}
                onChange={(event) => handleChange(onRegisterChange, "city", event.target.value)}
                required
              />
              <input
                placeholder="Estado"
                value={registerForm.state}
                onChange={(event) => handleChange(onRegisterChange, "state", event.target.value)}
                required
              />
            </div>
            <textarea
              rows="4"
              placeholder="Uma bio curta sobre você"
              value={registerForm.bio}
              onChange={(event) => handleChange(onRegisterChange, "bio", event.target.value)}
            />
            <button type="submit" className="primary-button" disabled={isSubmitting}>
              {isSubmitting ? "Criando..." : "Criar conta"}
            </button>
          </form>
        ) : null}

        {mode === "forgot" ? (
          <form className="stacked-form" onSubmit={onForgotSubmit}>
            <input
              type="email"
              placeholder="E-mail da conta"
              value={forgotForm.email}
              onChange={(event) => handleChange(onForgotChange, "email", event.target.value)}
              required
            />
            <button type="submit" className="primary-button" disabled={isSubmitting}>
              {isSubmitting ? "Enviando..." : "Enviar instruções"}
            </button>
          </form>
        ) : null}

        {mode === "reset" ? (
          <form className="stacked-form" onSubmit={onResetSubmit}>
            <input
              placeholder="Token de redefinição"
              value={resetForm.token}
              onChange={(event) => handleChange(onResetChange, "token", event.target.value)}
              required
            />
            <input
              type="password"
              placeholder="Nova senha"
              value={resetForm.newPassword}
              onChange={(event) => handleChange(onResetChange, "newPassword", event.target.value)}
              required
            />
            <input
              type="password"
              placeholder="Confirmar nova senha"
              value={resetForm.confirmPassword}
              onChange={(event) => handleChange(onResetChange, "confirmPassword", event.target.value)}
              required
            />
            <button type="submit" className="primary-button" disabled={isSubmitting}>
              {isSubmitting ? "Salvando..." : "Salvar nova senha"}
            </button>
          </form>
        ) : null}

        {passwordRecoveryPreview?.previewResetLink ? (
          <div className="preview-card">
            <strong>Teste local de redefinição</strong>
            <p>Como o SMTP não está configurado, use o link abaixo para completar o fluxo.</p>
            <a href={passwordRecoveryPreview.previewResetLink}>
              {passwordRecoveryPreview.previewResetLink}
            </a>
          </div>
        ) : null}
      </div>
    </div>
  );
}
