import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { PublishStickersPage } from "@/features/stickers/stickers-pages";
import { serverPublicRequest } from "@/shared/api/client";
import type { OperationalSettings } from "@/shared/api/types";
import { PrivateLayout } from "@/shared/layout/private-layout";

export const metadata: Metadata = { title: "Publicar figurinhas", robots: "noindex,nofollow" };

export default async function PublishFigurinhasRoute() {
  const settings = await serverPublicRequest<OperationalSettings>("/operational/public", 60);
  if (settings?.featureFlags?.stickersPageEnabled === false) {
    notFound();
  }
  return <PrivateLayout><PublishStickersPage /></PrivateLayout>;
}
