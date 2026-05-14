import { Suspense } from "react";
import { PublicHome } from "@/features/marketplace/public-marketplace";
import { PublicLayout } from "@/shared/layout/public-layout";

export default function HomePage() {
  return (
    <PublicLayout>
      <main>
        <Suspense fallback={null}>
          <PublicHome />
        </Suspense>
      </main>
    </PublicLayout>
  );
}
