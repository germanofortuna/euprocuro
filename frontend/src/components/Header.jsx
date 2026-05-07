import logoDark from "../assets/eu-procuro-logo-dark.svg";
import { useContentText } from "../content/ContentContext";

const loggedSections = {
  EXPLORE: "EXPLORE",
  CREDITS: "CREDITS",
  NEW_INTEREST: "NEW_INTEREST"
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

export default function Header({
  user,
  isLoggedIn,
  currentSection,
  hasNotifications,
  sellerCredits,
  subscriptionActive,
  creditPurchasesEnabled = false,
  notificationButtonRef,
  onNavigate,
  onCreditsClick,
  onNotificationClick,
  onLoginClick,
  onRegisterClick,
  onLogout,
  hideActions = false
}) {
  const { t } = useContentText();
  const authenticated = Boolean(isLoggedIn && user?.id);
  const firstName = user?.name?.trim().split(/\s+/)[0] ?? "";
  const hasNoCredits = !subscriptionActive && (sellerCredits ?? 0) <= 0;
  const notificationLabel = hasNotifications ? t("header.notifications.unread") : t("header.notifications.default");

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
