import type { Metadata } from "next";
import { OffersPage } from "@/features/dashboard/offers-page";
import { PrivateLayout } from "@/shared/layout/private-layout";

export const metadata: Metadata = { title: "Propostas enviadas", robots: "noindex,nofollow" };

export default function SentOffersRoute() {
  return <PrivateLayout><OffersPage type="sent" /></PrivateLayout>;
}
