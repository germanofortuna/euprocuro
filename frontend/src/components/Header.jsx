import logoDark from "../assets/eu-procuro-logo-dark.svg";
import { useContentText } from "../content/ContentContext";

const loggedSections = {
  EXPLORE: "EXPLORE",
  CREDITS: "CREDITS",
  NEW_INTEREST: "NEW_INTEREST",
  ADMIN: "ADMIN"
};

function BellIcon() {
  return (
    <svg
      className="notification-button__icon"
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M18 8.8c0-3.31-2.69-6-6-6s-6 2.69-6 6v3.08c0 .5-.2.98-.55 1.34L4 14.66V17h16v-2.34l-1.45-1.44A1.9 1.9 0 0 1 18 11.88V8.8Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M9.75 19.2a2.5 2.5 0 0 0 4.5 0"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </svg>
  );
}

function ThemeIcon() {
  return (
    <svg
      className="theme-toggle-button__icon"
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M9 18h6"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <path
        d="M10 21h4"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <path
        d="M8.8 14.8c-1.5-1.05-2.3-2.66-2.3-4.5A5.5 5.5 0 0 1 12 4.8a5.5 5.5 0 0 1 5.5 5.5c0 1.84-.8 3.45-2.3 4.5-.75.52-1.2 1.33-1.2 2.2h-4c0-.87-.45-1.68-1.2-2.2Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export default function Header({
  user,
  isLoggedIn,
  currentSection,
  hasNotifications,
  sellerCredits,
  subscriptionActive,
  creditPurchasesEnabled = false,
  isAdmin = false,
  unreadAdminReportCount = 0,
  notificationButtonRef,
  onNavigate,
  onCreditsClick,
  onAdminClick,
  onNotificationClick,
  onLoginClick,
  onRegisterClick,
  onLogout,
  theme = "dark",
  onThemeToggle,
  hideActions = false
}) {
  const { t } = useContentText();
  const authenticated = Boolean(isLoggedIn && user?.id);
  const firstName = user?.name?.trim().split(/\s+/)[0] ?? "";
  const hasNoCredits = !subscriptionActive && (sellerCredits ?? 0) <= 0;
  const notificationLabel = hasNotifications ? t("header.notifications.unread") : t("header.notifications.default");
  const nextThemeLabel = theme === "dark" ? "Usar tema claro" : "Usar tema escuro";

  return (
    <header className="topbar">
      <button
        type="button"
        className="topbar__brand topbar__brand-button"
        onClick={() => onNavigate(loggedSections.EXPLORE)}
      >
        <img className="brand-logo" src={logoDark} alt={t("header.logo.alt")} />
      </button>

      {hideActions ? null : authenticated ? (
        <div className="topbar__actions">
          <nav className="topbar__nav">
            <button
              type="button"
              className={currentSection === loggedSections.NEW_INTEREST ? "active" : ""}
              onClick={() => onNavigate(loggedSections.NEW_INTEREST)}
            >
              <span className="nav-icon" aria-hidden="true">+</span>
              {t("header.nav.publish")}
            </button>
            {isAdmin ? (
              <button
                type="button"
                className={`admin-header-button ${currentSection === loggedSections.ADMIN ? "active" : ""}`}
                onClick={onAdminClick}
              >
                <span className="nav-icon" aria-hidden="true">⚙</span>
                {t("admin.moderation.nav")}
                {unreadAdminReportCount > 0 ? (
                  <strong className="admin-nav-badge">{unreadAdminReportCount}</strong>
                ) : null}
              </button>
            ) : null}
          </nav>

          {creditPurchasesEnabled ? (
            <button
              type="button"
              className={`credits-badge ${hasNoCredits ? "credits-badge--empty" : ""}`}
              onClick={onCreditsClick}
              title={t("header.credits.title")}
            >
              <strong>{subscriptionActive ? t("header.credits.pro") : (sellerCredits ?? 0)}</strong>
              <span>{subscriptionActive ? t("header.credits.planActive") : t("header.credits.credits")}</span>
            </button>
          ) : null}

          <button
            ref={notificationButtonRef}
            type="button"
            className={`notification-button ${hasNotifications ? "notification-button--active" : ""}`}
            onClick={onNotificationClick}
            aria-label={notificationLabel}
            title={notificationLabel}
          >
            <BellIcon />
            {hasNotifications ? <span className="notification-badge" /> : null}
          </button>

          <button
            type="button"
            className="theme-toggle-button"
            onClick={onThemeToggle}
            aria-label={nextThemeLabel}
            title={nextThemeLabel}
          >
            <ThemeIcon />
          </button>

          <div className="profile-badge">
            <strong>{firstName}</strong>
            <span>{user?.city}/{user?.state}</span>
          </div>

          <button type="button" className="ghost-button" onClick={onLogout}>
            {t("header.auth.logout")}
          </button>
        </div>
      ) : (
        <div className="topbar__actions">
          <button type="button" className="ghost-button" onClick={onLoginClick}>
            {t("header.auth.login")}
          </button>
          <button type="button" className="primary-button primary-button--compact" onClick={onRegisterClick}>
            {t("header.auth.register")}
          </button>
        </div>
      )}
    </header>
  );
}
