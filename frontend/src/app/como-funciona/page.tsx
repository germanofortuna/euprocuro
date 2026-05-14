import type { Metadata } from "next";
import Link from "next/link";
import { CheckCircle2, Clock, MessageSquare, Shield, Target, Zap } from "lucide-react";
import { canonical } from "@/shared/lib/seo";
import { PublicLayout } from "@/shared/layout/public-layout";

export const metadata: Metadata = {
  title: "Como funciona",
  description: "Entenda como compradores publicam procuras e vendedores enviam propostas no marketplace reverso Eu Procuro.",
  alternates: { canonical: canonical("/como-funciona") },
  robots: "index,follow"
};

export default function HowItWorksPage() {
  return (
    <PublicLayout>
      <main className="route-shell content-route">
        <section className="centered-intro">
          <h1>Como funciona o Eu Procuro?</h1>
          <p>Um marketplace reverso onde você diz o que precisa e vendedores ou prestadores vêm até você com propostas.</p>
        </section>
        <section className="how-grid">
          <article>
            <h2><Target size={24} /> Para quem está procurando</h2>
            {["Publique sua necessidade", "Receba propostas", "Escolha e negocie"].map((title, index) => (
              <div className="step-row" key={title}>
                <span>{index + 1}</span>
                <div><h3>{title}</h3><p>{index === 0 ? "Descreva o que você precisa, onde está e qual é seu orçamento." : index === 1 ? "Vendedores encontram sua procura e enviam propostas relevantes." : "Compare propostas, converse pelo chat e decida com segurança."}</p></div>
              </div>
            ))}
            <Link className="button button--primary" href="/cadastrar-interesse">Publicar uma Procura</Link>
          </article>
          <article>
            <h2><Zap size={24} /> Para quem quer vender</h2>
            {["Encontre clientes reais", "Cadastre seus itens", "Envie ofertas"].map((title, index) => (
              <div className="step-row step-row--green" key={title}>
                <span>{index + 1}</span>
                <div><h3>{title}</h3><p>{index === 0 ? "Navegue por pessoas que já declararam exatamente o que precisam." : index === 1 ? "Adicione produtos ou serviços e receba matches com procuras compatíveis." : "Use créditos ou Plano Pro para enviar propostas com agilidade."}</p></div>
              </div>
            ))}
            <Link className="button button--outline" href="/meus-itens">Começar a vender</Link>
          </article>
        </section>
        <section className="benefit-band">
          <h2>Por que usar o Eu Procuro?</h2>
          <div className="benefit-grid">
            <article><Clock /><h3>Economia de tempo</h3><p>Você pede uma vez e recebe opções de quem pode atender.</p></article>
            <article><Shield /><h3>Mais segurança</h3><p>A plataforma é moderada e evita exposição pública desnecessária.</p></article>
            <article><MessageSquare /><h3>Negociação direta</h3><p>Use o chat interno para alinhar detalhes antes de fechar.</p></article>
            <article><CheckCircle2 /><h3>Demanda real</h3><p>Vendedores respondem pessoas que já têm intenção declarada.</p></article>
          </div>
        </section>
      </main>
    </PublicLayout>
  );
}
