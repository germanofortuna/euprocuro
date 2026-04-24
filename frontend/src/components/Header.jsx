import logo from "../assets/eu-procuro-logo.png";

const loggedSections = {
  EXPLORE: "EXPLORE",
  NEW_INTEREST: "NEW_INTEREST"
};

export default function Header({
  user,
  isLoggedIn,
  currentSection,
  hasNotifications,
  notificationButtonRef,
  onNavigate,
  onNotificationClick,
  onLoginClick,
  onRegisterClick,
  onLogout
}) {
  return (
    <header className="topbar">
      <button
        type="button"
        className="topbar__brand topbar__brand-button"
        onClick={() => onNavigate(loggedSections.EXPLORE)}
      >
        <img src={logo} alt="Eu Procuro" />
        <div className="topbar__brand-copy">
          <span className="eyebrow">Marketplace reverso</span>
        </div>
      </button>

      {isLoggedIn ? (
        <div className="topbar__actions">
          <nav className="topbar__nav">
            <button
              type="button"
              className={currentSection === loggedSections.EXPLORE ? "active" : ""}
              onClick={() => onNavigate(loggedSections.EXPLORE)}
            >
              Home
            </button>
            <button
              type="button"
              className={currentSection === loggedSections.NEW_INTEREST ? "active" : ""}
              onClick={() => onNavigate(loggedSections.NEW_INTEREST)}
            >
              Novo interesse
            </button>
          </nav>

          <button
            ref={notificationButtonRef}
            type="button"
            className={`notification-button ${hasNotifications ? "notification-button--active" : ""}`}
            onClick={onNotificationClick}
            aria-label={hasNotifications ? "Você tem novas mensagens" : "Notificações"}
            title={hasNotifications ? "Você tem novas mensagens" : "Notificações"}
          >
            <span aria-hidden="true">🔔</span>
            {hasNotifications ? <span className="notification-badge" /> : null}
          </button>

          <div className="profile-badge">
            <strong>{user?.name}</strong>
            <span>
              {user?.city}/{user?.state}
            </span>
          </div>

          <button type="button" className="ghost-button" onClick={onLogout}>
            Sair
          </button>
        </div>
      ) : (
        <div className="topbar__actions">
          <button type="button" className="ghost-button" onClick={onLoginClick}>
            Entrar
          </button>
          <button type="button" className="primary-button primary-button--compact" onClick={onRegisterClick}>
            Criar conta
          </button>
        </div>
      )}
    </header>
  );
}
