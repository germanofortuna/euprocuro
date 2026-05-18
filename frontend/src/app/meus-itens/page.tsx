import type { Metadata } from "next";
import { SellerItemsPage } from "@/features/dashboard/dashboard-pages";
import { PrivateLayout } from "@/shared/layout/private-layout";

export const metadata: Metadata = { title: "Meus itens e matches", robots: "noindex,nofollow" };

export default function SellerItemsRoute() {
  return <PrivateLayout><SellerItemsPage /></PrivateLayout>;
}
