import type { Metadata } from "next";
import { OmbudsmanPage } from "@/features/ombudsman/ombudsman-page";
import { canonical } from "@/shared/lib/seo";
import { PublicLayout } from "@/shared/layout/public-layout";

export const metadata: Metadata = {
  title: "Ouvidoria",
  description: "Canal formal do Eu Procuro para reclamações, denúncias, contestação de moderação, problemas de pagamento e sugestões.",
  alternates: { canonical: canonical("/ouvidoria") },
  robots: "index,follow"
};

export default function OmbudsmanRoute() {
  return <PublicLayout><main><OmbudsmanPage /></main></PublicLayout>;
}
