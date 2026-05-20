"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Bell, LogOut, Menu, Moon, Search, Sun, User, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import logoDark from "@/assets/eu-procuro-logo-dark.svg";
import logoLight from "@/assets/eu-procuro-logo-light.svg";
import { buildOfferNotifications, NotificationModal } from "@/features/notifications/notification-modal";
import { usePlatform } from "@/features/platform/platform-context";
import { useTheme } from "@/features/theme/theme-provider";
import { Button } from "@/shared/ui/button";
import { AuthIntentLink } from "@/shared/ui/auth-intent-link";

const baseNavItems = [
  { href: "/categorias", label: "Explorar" },
  { href: "/figurinhas", label: "Figurinhas", feature: "stickers" },
  { href: "/como-funciona", label: "Como funciona" }
];

function navItemClassName(href: string) {
  return href === "/figurinhas" ? "nav-link nav-link--stickers" : "nav-link";
}

function readSeenNotificationIds(rawValue: string | null): string[] {
  if (!rawValue) {
    return [];
  }
  const parsed = JSON.parse(rawValue) as unknown;
  if (Array.isArray(parsed)) {
    return parsed.filter((item): item is string => typeof item === "string");
  }
  if (parsed && typeof parsed === "object") {
    return Object.keys(parsed);
  }
  return [];
}

export function AppHeader() {
  const router = useRouter();
  const { theme, toggleTheme } = useTheme();
  const { currentUser, openAuthModal, signOut, monetization, dashboard, operationalSettings } = usePlatform();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);
  const [seenNotifications, setSeenNotifications] = useState<string[]>([]);
  const [notificationAnchor, setNotificationAnchor] = useState<DOMRect | null>(null);
  const [themePulseKey, setThemePulseKey] = useState(0);
  const [isThemeAnimating, setIsThemeAnimating] = useState(false);
  const notificationButtonRef = useRef<HTMLButtonElement | null>(null);
  const credits = monetization?.sellerCredits ?? currentUser?.sellerCredits ?? currentUser?.credits ?? 0;
  const creditPurchasesEnabled = Boolean(monetization?.settings?.creditPurchasesEnabled);
  const notifications = buildOfferNotifications(dashboard?.receivedOffers, dashboard?.sentOffers);
  const navItems = baseNavItems.filter((item) => item.feature !== "stickers" || operationalSettings.featureFlags?.stickersPageEnabled !== false);
  const messageSeenKey = currentUser?.id ? `eu-procuro-message-seen:${currentUser.id}` : null;
  const unreadCount = notifications.filter((notification) => !seenNotifications.includes(notification.id)).length;

  useEffect(() => {
    if (!messageSeenKey || typeof window === "undefined") {
      return;
    }
    try {
      const stored = window.localStorage.getItem(messageSeenKey);
      setSeenNotifications(readSeenNotificationIds(stored));
    } catch {
      setSeenNotifications([]);
    }
  }, [messageSeenKey]);

  function markAllNotificationsRead() {
    const nextSeen = notifications.map((notification) => notification.id);
    setSeenNotifications(nextSeen);
    if (messageSeenKey) {
      window.localStorage.setItem(messageSeenKey, JSON.stringify(nextSeen));
    }
  }

  async function handleSignOut() {
    await signOut();
    setIsMenuOpen(false);
    setIsNotificationsOpen(false);
    router.replace("/");
  }

  function handleThemeToggle() {
    setIsThemeAnimating(true);
    setThemePulseKey((current) => current + 1);
    toggleTheme();
    window.setTimeout(() => setIsThemeAnimating(false), 460);
  }

  function openNotifications() {
    setNotificationAnchor(notificationButtonRef.current?.getBoundingClientRect() ?? null);
    setIsNotificationsOpen(true);
  }

  return (
    <header className="site-header">
      <div className="header-inner">
        <Link href="/" className="brand-link" aria-label="Eu Procuro">
          <Image src={theme === "dark" ? logoDark : logoLight} alt="Eu Procuro" width={178} height={44} priority />
        </Link>

        <form className="header-search" action="/categorias" role="search">
          <Search size={18} aria-hidden="true" />
          <input name="query" placeholder="Buscar procuras..." />
        </form>

        <nav className="desktop-nav" aria-label="Navegacao principal">
          {navItems.map((item) => <Link key={item.href} href={item.href} className={navItemClassName(item.href)}>{item.label}</Link>)}
        </nav>

        <div className="header-actions">
          <button type="button" className={`icon-button theme-toggle${isThemeAnimating ? " is-animating" : ""}`} onClick={handleThemeToggle} aria-label={theme === "dark" ? "Usar tema claro" : "Usar tema escuro"} title="Alternar tema">
            <span key={themePulseKey} className="theme-toggle__pulse" aria-hidden="true" />
            <Sun className="theme-icon theme-sun" size={19} aria-hidden="true" />
            <Moon className="theme-icon theme-moon" size={19} aria-hidden="true" />
          </button>
          {currentUser?.id ? (
            <>
              {creditPurchasesEnabled ? <Link className="credit-chip" href="/comprar-creditos">{credits} creditos</Link> : null}
              <button ref={notificationButtonRef} type="button" className="icon-button notification-trigger" onClick={openNotifications} aria-label="Abrir notificacoes">
                <Bell size={18} />
                {unreadCount ? <span className="notification-badge">{unreadCount}</span> : null}
              </button>
              <Link className="button button--outline button--sm" href="/meus-interesses"><User size={16} /> Painel</Link>
              <button type="button" className="icon-button" onClick={handleSignOut} aria-label="Sair"><LogOut size={18} /></button>
            </>
          ) : (
            <>
              <Button variant="ghost" size="sm" type="button" onClick={() => openAuthModal("login")}>Entrar</Button>
              <AuthIntentLink className="button button--primary button--sm" href="/cadastrar-interesse" mode="login">Publicar Procura</AuthIntentLink>
            </>
          )}
          <button type="button" className="icon-button mobile-menu-button" onClick={() => setIsMenuOpen((current) => !current)} aria-label="Abrir menu">
            {isMenuOpen ? <X size={18} /> : <Menu size={18} />}
          </button>
        </div>
      </div>

      {isMenuOpen ? (
        <div className="mobile-menu">
          <form className="mobile-search" action="/categorias" role="search">
            <Search size={18} />
            <input name="query" placeholder="Buscar interesses" />
          </form>
          {navItems.map((item) => <Link key={item.href} href={item.href} className={navItemClassName(item.href)} onClick={() => setIsMenuOpen(false)}>{item.label}</Link>)}
          {currentUser?.id ? (
            <>
              <Link href="/meus-interesses" onClick={() => setIsMenuOpen(false)}>Minha conta</Link>
              <Link href="/ofertas-recebidas" onClick={() => setIsMenuOpen(false)}>Propostas recebidas</Link>
            </>
          ) : (
            <button type="button" className="mobile-menu-action" onClick={() => { setIsMenuOpen(false); openAuthModal("login"); }}>Entrar</button>
          )}
          <AuthIntentLink className="mobile-menu-cta" href="/cadastrar-interesse" mode="login" onClick={() => setIsMenuOpen(false)}>Publicar procura</AuthIntentLink>
        </div>
      ) : null}
      <NotificationModal
        visible={isNotificationsOpen}
        notifications={notifications}
        seenIds={seenNotifications}
        anchorRect={notificationAnchor}
        onClose={() => setIsNotificationsOpen(false)}
        onMarkAllRead={markAllNotificationsRead}
      />
    </header>
  );
}
