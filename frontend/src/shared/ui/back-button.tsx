"use client";

import { ArrowLeft } from "lucide-react";
import { useRouter } from "next/navigation";

export function BackButton({ className = "back-link" }: { className?: string }) {
  const router = useRouter();

  return (
    <button type="button" className={className} onClick={() => router.back()}>
      <ArrowLeft size={16} /> Voltar
    </button>
  );
}
