import type { Metadata } from "next";
import { CreditsPage } from "@/features/dashboard/dashboard-pages";
import { PrivateLayout } from "@/shared/layout/private-layout";

export const metadata: Metadata = { title: "Créditos e Plano", robots: "noindex,nofollow" };

export default function CreditsRoute() {
  return <PrivateLayout><CreditsPage /></PrivateLayout>;
}
