import type { Metadata } from "next";
import { Suspense } from "react";
import { CategoryLanding } from "@/features/marketplace/public-marketplace";
import { canonical } from "@/shared/lib/seo";
import { PublicLayout } from "@/shared/layout/public-layout";

export async function generateMetadata({ params }: { params: Promise<{ categoria: string }> }): Promise<Metadata> {
  const { categoria } = await params;
  const label = categoria.replace(/-/g, " ");
  return {
    title: `Procuras em ${label}`,
    description: `Veja procuras abertas em ${label} no Eu Procuro. Pessoas publicam o que precisam e vendedores podem responder com propostas.`,
    alternates: { canonical: canonical(`/categorias/${categoria}`) },
    robots: "index,follow"
  };
}

export default async function CategoryPage({ params }: { params: Promise<{ categoria: string }> }) {
  const { categoria } = await params;
  return (
    <PublicLayout>
      <main><Suspense fallback={null}><CategoryLanding categorySlug={categoria} /></Suspense></main>
    </PublicLayout>
  );
}
