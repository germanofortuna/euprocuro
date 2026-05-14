import type { Metadata } from "next";
import { OffersPage } from "@/features/dashboard/offers-page";
import { PrivateLayout } from "@/shared/layout/private-layout";

export const metadata: Metadata = { title: "Propostas recebidas", robots: "noindex,nofollow" };

export default function ReceivedOffersRoute() {
  return <PrivateLayout><OffersPage type="received" /></PrivateLayout>;
}
