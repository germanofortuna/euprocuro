import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { StickersLandingPage } from "@/features/stickers/stickers-pages";
import { serverPublicRequest } from "@/shared/api/client";
import type { OperationalSettings } from "@/shared/api/types";
import { canonical } from "@/shared/lib/seo";
import { PublicLayout } from "@/shared/layout/public-layout";

export const metadata: Metadata = {
  title: "Figurinhas da Copa 2026",
  description: "Troque figurinhas faltantes e repetidas da Copa 2026 com outros colecionadores.",
  alternates: { canonical: canonical("/figurinhas") },
  robots: "index,follow"
};

export default async function FigurinhasRoute() {
  const settings = await serverPublicRequest<OperationalSettings>("/operational/public", 60);
  if (settings?.featureFlags?.stickersPageEnabled === false) {
    notFound();
  }
  return <PublicLayout><main><StickersLandingPage /></main></PublicLayout>;
}
