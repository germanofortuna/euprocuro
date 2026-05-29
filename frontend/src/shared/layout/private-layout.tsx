"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { AlertTriangle, CreditCard, LayoutDashboard, MessageSquare, Package, Search, Settings, ShieldCheck, Sparkles, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { AppHeader } from "./app-header";
import { AppFooter } from "./app-footer";
import { usePlatform } from "@/features/platform/platform-context";
import { isAdminUser } from "@/shared/lib/format";
import { Button } from "@/shared/ui/button";

const navItems = [
  { href: "/meus-interesses", label: "Minhas Procuras", icon: Search },
  { href: "/ofertas-recebidas", label: "Propostas Recebidas", icon: MessageSquare },
  { href: "/ofertas-enviadas", label: "Propostas Enviadas", icon: LayoutDashboard },
  { href: "/meus-itens", label: "Meus Itens e Matches", icon: Package },
  { href: "/comprar-creditos", label: "Créditos e Plano", icon: CreditCard },
  { href: "/admin", label: "Admin", icon: Settings }
];

export function PrivateLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { currentUser, monetization, isSessionReady, hasAdminAccess, sellerItems, deleteAccount, setFeedback } = usePlatform();
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);
  const credits = monetization?.sellerCredits ?? currentUser?.sellerCredits ?? currentUser?.credits ?? 0;
  const creditPurchasesEnabled = Boolean(monetization?.settings?.creditPurchasesEnabled);
  const visibleNavItems = navItems.filter((item) => {
    if (item.href === "/comprar-creditos" && !creditPurchasesEnabled) {
      return false;
    }
    return item.href !== "/admin" || pathname === "/admin" || isAdminUser(currentUser, hasAdminAccess);
  });

  useEffect(() => {
    if (isSessionReady && !currentUser?.id) {
      router.replace("/");
    }
  }, [currentUser?.id, isSessionReady, router]);

  if (!isSessionReady || !currentUser?.id) {
    return (
      <div className="app-shell private-shell">
        <AppHeader />
        <main className="private-content private-content--standalone">
          <section className="auth-gate">
            <span className="pill">Área logada</span>
            <h1>{isSessionReady ? "Redirecionando..." : "Carregando sua sessão..."}</h1>
          </section>
        </main>
      </div>
    );
  }

  async function confirmDeleteAccount() {
    setIsDeletingAccount(true);
    try {
      await deleteAccount();
      setIsDeleteModalOpen(false);
      router.replace("/");
    } catch (error) {
      setFeedback({
        type: "error",
        title: "Não foi possível excluir a conta",
        message: error instanceof Error ? error.message : "Tente novamente em instantes."
      });
    } finally {
      setIsDeletingAccount(false);
    }
  }

  return (
    <div className="app-shell private-shell">
      <AppHeader />
      <div className="private-workspace">
        <aside className="dashboard-sidebar">
          <div className="sidebar-title">
            <span>Painel do usuário</span>
            <strong>{currentUser.name || "Sua conta"}</strong>
            {currentUser.email ? (
              <small className="sidebar-user-email">
                {currentUser.email}
                {isAdminUser(currentUser, hasAdminAccess) ? <AdminBadge /> : null}
                {currentUser.googleLinked ? <GoogleLinkedBadge /> : null}
                {currentUser.facebookLinked ? <FacebookLinkedBadge /> : null}
              </small>
            ) : null}
          </div>
          <nav>
            {visibleNavItems.map((item) => {
              const Icon = item.icon;
              const active = pathname === item.href;
              return (
                <Link key={item.href} href={item.href} className={active ? "is-active" : ""}>
                  <Icon size={18} />
                  {item.label}
                </Link>
              );
            })}
          </nav>
          <div className="sidebar-quick-actions">
            <strong>Ações rápidas</strong>
            <Link href="/categorias"><Search size={17} /><span>Explorar procuras</span></Link>
            <Link href="/meus-itens"><Sparkles size={17} /><span>Meus matches</span><small>{sellerItems.length} itens</small></Link>
            {creditPurchasesEnabled ? <Link href="/comprar-creditos"><CreditCard size={17} /><span>Créditos e plano</span></Link> : null}
          </div>
          {creditPurchasesEnabled ? <div className="sidebar-credit-card">
            <span>Saldo Atual</span>
            <strong>{credits} Créditos</strong>
            <Link href="/comprar-creditos">Comprar mais</Link>
          </div> : null}
          <button type="button" className="sidebar-delete-account-button" onClick={() => setIsDeleteModalOpen(true)}>
            <Trash2 size={16} aria-hidden="true" />
            <span>Excluir minha conta</span>
          </button>
        </aside>
        <main className="private-content">{children}</main>
      </div>
      <AppFooter />
      {isDeleteModalOpen ? (
        <div className="modal-overlay modal-overlay--plain" role="presentation">
          <section className="modal-card account-delete-modal" role="dialog" aria-modal="true" aria-labelledby="account-delete-title">
            <div className="modal-header">
              <div className="modal-title">
                <span className="status-icon status-icon--warning"><AlertTriangle size={20} aria-hidden="true" /></span>
                <strong id="account-delete-title">Excluir conta</strong>
              </div>
            </div>
            <div className="account-delete-warning">
              <p>Ao excluir sua conta, seus dados de perfil serão removidos e seus conteúdos serão excluídos ou anonimizados, incluindo chats, propostas, procuras e itens cadastrados.</p>
              <p>Alguns registros mínimos poderão ser mantidos quando necessários por obrigação legal, segurança, antifraude ou exercício regular de direitos. Essa ação é permanente e não poderá ser desfeita.</p>
            </div>
            <div className="modal-actions account-delete-actions">
              <Button type="button" onClick={() => setIsDeleteModalOpen(false)} disabled={isDeletingAccount}>Cancelar</Button>
              <button type="button" className="account-delete-confirm" onClick={confirmDeleteAccount} disabled={isDeletingAccount}>
                {isDeletingAccount ? "Excluindo..." : "Sim, quero excluir"}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  );
}

function AdminBadge() {
  return (
    <span className="admin-badge" title="Conta administradora" aria-label="Conta administradora">
      <ShieldCheck size={14} aria-hidden="true" />
    </span>
  );
}

function GoogleLinkedBadge() {
  return (
    <span className="google-linked-badge" title="Conta vinculada com Google" aria-label="Conta vinculada com Google">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="14" height="14" aria-hidden="true">
        <path fill="#FFC107" d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z"/>
        <path fill="#FF3D00" d="M6.306 14.691l6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691z"/>
        <path fill="#4CAF50" d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238C29.211 35.091 26.715 36 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z"/>
        <path fill="#1976D2" d="M43.611 20.083H42V20H24v8h11.303c-.792 2.237-2.231 4.166-4.087 5.571.001-.001.002-.001.003-.002l6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z"/>
      </svg>
    </span>
  );
}

function FacebookLinkedBadge() {
  return (
    <span className="facebook-linked-badge" title="Conta vinculada com Facebook" aria-label="Conta vinculada com Facebook">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
        <path fill="#FFFFFF" d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
      </svg>
    </span>
  );
}
