import type { Metadata } from "next";
import { InterestDetailPage } from "@/features/marketplace/interest-detail-page";
import { serverPublicRequest } from "@/shared/api/client";
import type { Interest } from "@/shared/api/types";
import { canonical, truncateDescription } from "@/shared/lib/seo";
import { PublicLayout } from "@/shared/layout/public-layout";

export async function generateMetadata({ params }: { params: Promise<{ id: string }> }): Promise<Metadata> {
  const { id } = await params;
  const interest = await serverPublicRequest<Interest>(`/interests/${id}`, 60);
  return {
    title: interest?.title ?? "Detalhe da procura",
    description: truncateDescription(interest?.description),
    alternates: { canonical: canonical(`/interesses/${id}`) },
    robots: interest ? "index,follow" : "noindex,nofollow"
  };
}

export default async function InterestPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const interest = await serverPublicRequest<Interest>(`/interests/${id}`, 60);
  return (
    <PublicLayout>
      <main><InterestDetailPage initialInterest={interest} /></main>
    </PublicLayout>
  );
}
