"use client";

import Image from "next/image";
import Link from "next/link";
import logoDark from "@/assets/eu-procuro-logo-dark.svg";
import logoLight from "@/assets/eu-procuro-logo-light.svg";
import { useLegalContent } from "@/features/legal/use-legal-content";
import { usePlatform } from "@/features/platform/platform-context";
import { useTheme } from "@/features/theme/theme-provider";
import { slugifyCategory } from "@/shared/lib/format";
import { AuthIntentLink } from "@/shared/ui/auth-intent-link";

export function AppFooter() {
  const { theme } = useTheme();
  const { categories } = usePlatform();
  const { navigation } = useLegalContent();
  const year = new Date().getFullYear();

  return (
    <footer className="site-footer">
      <div className="footer-grid">
        <section className="footer-brand">
          <Image src={theme === "dark" ? logoDark : logoLight} alt="Eu Procuro" width={150} height={38} />
          <p>Marketplace reverso para publicar o que voce procura e receber propostas de quem pode atender.</p>
        </section>
        <nav aria-label="Produto">
          <h2>Produto</h2>
          <Link href="/">Home</Link>
          <Link href="/categorias">Explorar categorias</Link>
          <Link href="/como-funciona">Como funciona</Link>
          <AuthIntentLink className="footer-link-button" href="/cadastrar-interesse">Publicar procura</AuthIntentLink>
          <Link href="/ouvidoria">Ouvidoria</Link>
        </nav>
        <nav aria-label="Categorias">
          <h2>Principais categorias</h2>
          {categories.slice(0, 6).map((category) => (
            <Link key={category.value} href={`/categorias/${slugifyCategory(category.value)}`}>{category.label}</Link>
          ))}
        </nav>
        <nav aria-label="Politicas da plataforma">
          <h2>Legal</h2>
          {navigation.map((item) => <Link key={item.slug} href={item.href}>{item.label}</Link>)}
          <Link href="/ouvidoria">Ouvidoria</Link>
        </nav>
      </div>
      <div className="footer-bottom">
        <p>&copy; {year} Eu Procuro. Todos os direitos reservados.</p>
      </div>
    </footer>
  );
}
