"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { AlertTriangle, CreditCard, LayoutDashboard, MessageSquare, Package, Search, Settings, Sparkles, Trash2 } from "lucide-react";
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
            {currentUser.email ? <small className="sidebar-user-email">{currentUser.email}</small> : null}
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
