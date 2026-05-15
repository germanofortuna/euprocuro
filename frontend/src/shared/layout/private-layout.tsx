"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { CreditCard, LayoutDashboard, MessageSquare, Package, Search, Settings, Sparkles } from "lucide-react";
import { AppHeader } from "./app-header";
import { AppFooter } from "./app-footer";
import { usePlatform } from "@/features/platform/platform-context";
import { isAdminUser } from "@/shared/lib/format";

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
  const { currentUser, monetization, openAuthModal, adminModeration, sellerItems } = usePlatform();
  const credits = monetization?.sellerCredits ?? currentUser?.sellerCredits ?? currentUser?.credits ?? 0;
  const creditPurchasesEnabled = Boolean(monetization?.settings?.creditPurchasesEnabled);
  const visibleNavItems = navItems.filter((item) => {
    if (item.href === "/comprar-creditos" && !creditPurchasesEnabled) {
      return false;
    }
    return item.href !== "/admin" || pathname === "/admin" || isAdminUser(currentUser, Boolean(adminModeration));
  });

  return (
    <div className="app-shell private-shell">
      <AppHeader />
      <div className="private-workspace">
        <aside className="dashboard-sidebar">
          <div className="sidebar-title">
            <span>Painel do usuário</span>
            <strong>{currentUser?.name || "Sua conta"}</strong>
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
        </aside>
        <main className="private-content">
          {!currentUser?.id ? (
            <section className="auth-gate">
              <span className="pill">Área logada</span>
              <h1>Entre para continuar</h1>
              <p>{creditPurchasesEnabled ? "Use sua conta para gerenciar procuras, propostas, itens, créditos e administração." : "Use sua conta para gerenciar procuras, propostas, itens e administração."}</p>
              <button type="button" className="button button--primary" onClick={() => openAuthModal("login")}>Entrar</button>
            </section>
          ) : children}
        </main>
      </div>
      <AppFooter />
    </div>
  );
}
