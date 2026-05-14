import type { Metadata } from "next";
import { Suspense } from "react";
import { CategoryLanding } from "@/features/marketplace/public-marketplace";
import { canonical } from "@/shared/lib/seo";
import { PublicLayout } from "@/shared/layout/public-layout";

export const metadata: Metadata = {
  title: "Categorias",
  description: "Explore categorias ativas no Eu Procuro e encontre pessoas procurando produtos, serviços, imóveis, veículos e oportunidades.",
  alternates: { canonical: canonical("/categorias") },
  robots: "index,follow"
};

export default function CategoriesPage() {
  return (
    <PublicLayout>
      <main><Suspense fallback={null}><CategoryLanding /></Suspense></main>
    </PublicLayout>
  );
}
