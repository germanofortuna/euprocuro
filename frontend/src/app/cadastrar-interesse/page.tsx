import type { Metadata } from "next";
import { InterestFormPage } from "@/features/marketplace/interest-form-page";
import { PrivateLayout } from "@/shared/layout/private-layout";

export const metadata: Metadata = {
  title: "Publicar procura",
  robots: "noindex,nofollow"
};

export default function CreateInterestPage() {
  return (
    <PrivateLayout>
      <InterestFormPage />
    </PrivateLayout>
  );
}
