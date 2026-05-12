import logoDark from "../assets/eu-procuro-logo-dark.svg";
import logoLight from "../assets/eu-procuro-logo-light.svg";
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

function SunIcon() {
  return (
    <svg
      className="theme-toggle-button__icon"
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle cx="12" cy="12" r="4.2" stroke="currentColor" strokeWidth="1.8" />
      <path
        d="M12 2.8v2.4M12 18.8v2.4M4.2 4.2l1.7 1.7M18.1 18.1l1.7 1.7M2.8 12h2.4M18.8 12h2.4M4.2 19.8l1.7-1.7M18.1 5.9l1.7-1.7"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </svg>
  );
}

function MoonIcon() {
  return (
    <svg
      className="theme-toggle-button__icon"
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        <mask id="themeMoonCutout">
          <rect width="24" height="24" fill="white" />
          <circle cx="15.4" cy="8.5" r="7.5" fill="black" />
        </mask>
      </defs>
      <circle cx="11.3" cy="12" r="8.4" fill="currentColor" mask="url(#themeMoonCutout)" />
      <circle cx="6.9" cy="8.7" r="1.1" fill="currentColor" opacity="0.6" />
    </svg>
  );
}

function LogoutIcon() {
  return (
    <svg
      className="topbar__logout-icon"
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M10.5 5H6.8A2.8 2.8 0 0 0 4 7.8v8.4A2.8 2.8 0 0 0 6.8 19h3.7"
        stroke="currentColor"
        strokeWidth="1.9"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M15 8l4 4-4 4M19 12H9.5"
        stroke="currentColor"
        strokeWidth="1.9"
        strokeLinecap="round"
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
  hideActions = false,
  isScrolled = false
}) {
  const { t } = useContentText();
  const authenticated = Boolean(isLoggedIn && user?.id);
  const firstName = user?.name?.trim().split(/\s+/)[0] ?? "";
  const brandLogo = theme === "light" ? logoLight : logoDark;
  const hasNoCredits = !subscriptionActive && (sellerCredits ?? 0) <= 0;
  const notificationLabel = hasNotifications ? t("header.notifications.unread") : t("header.notifications.default");
  const nextThemeLabel = theme === "dark" ? "Usar tema claro" : "Usar tema escuro";
  const themeIcon = theme === "dark" ? <MoonIcon /> : <SunIcon />;

  return (
    <header className={`topbar ${authenticated ? "topbar--authenticated" : "topbar--guest"} ${!authenticated && isScrolled ? "topbar--scrolled" : ""}`}>
      <button
        type="button"
        className="topbar__brand topbar__brand-button"
        onClick={() => onNavigate(loggedSections.EXPLORE)}
      >
        <img className="brand-logo" src={brandLogo} alt={t("header.logo.alt")} />
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
            {themeIcon}
          </button>

          <div className="profile-badge">
            <strong>{firstName}</strong>
          </div>

          <button
            type="button"
            className="ghost-button topbar__logout-button"
            onClick={onLogout}
            aria-label={t("header.auth.logout")}
            title={t("header.auth.logout")}
          >
            <LogoutIcon />
          </button>
        </div>
      ) : (
        <div className="topbar__actions topbar__actions--guest">
          <button type="button" className="ghost-button topbar__login-button" onClick={onLoginClick}>
            {t("header.auth.login")}
          </button>
          <button
            type="button"
            className="theme-toggle-button"
            onClick={onThemeToggle}
            aria-label={nextThemeLabel}
            title={nextThemeLabel}
          >
            {themeIcon}
          </button>
          <button type="button" className="primary-button primary-button--compact topbar__register-button" onClick={onRegisterClick}>
            {t("header.auth.register")}
          </button>
        </div>
      )}
    </header>
  );
}
