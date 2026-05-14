import type { Metadata } from "next";
import { Providers } from "./providers";
import "./styles.css";

export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "https://euprocuro.com"),
  title: {
    default: "Eu Procuro - Marketplace reverso",
    template: "%s | Eu Procuro"
  },
  description: "Publique o que você procura e receba propostas de vendedores e prestadores interessados.",
  robots: "index,follow",
  openGraph: {
    type: "website",
    siteName: "Eu Procuro",
    title: "Eu Procuro - Marketplace reverso",
    description: "Publique o que você procura e receba propostas de vendedores interessados.",
    url: "/"
  },
  twitter: {
    card: "summary",
    title: "Eu Procuro - Marketplace reverso",
    description: "Publique o que você procura e receba propostas de vendedores interessados."
  }
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" data-theme="dark" suppressHydrationWarning>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
