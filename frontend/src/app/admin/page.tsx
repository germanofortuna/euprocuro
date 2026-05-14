import type { Metadata } from "next";
import { AdminPage } from "@/features/admin/admin-page";
import { PrivateLayout } from "@/shared/layout/private-layout";

export const metadata: Metadata = { title: "Admin", robots: "noindex,nofollow" };

export default function AdminRoute() {
  return <PrivateLayout><AdminPage /></PrivateLayout>;
}
