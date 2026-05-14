import type { Metadata } from "next";
import { MyInterestsPage } from "@/features/dashboard/dashboard-pages";
import { PrivateLayout } from "@/shared/layout/private-layout";

export const metadata: Metadata = { title: "Minhas procuras", robots: "noindex,nofollow" };

export default function MyInterestsRoute() {
  return <PrivateLayout><MyInterestsPage /></PrivateLayout>;
}
