"use client";

import Link from "next/link";
import type React from "react";
import { usePlatform } from "@/features/platform/platform-context";

type AuthIntentLinkProps = {
  href: string;
  children: React.ReactNode;
  className?: string;
  mode?: "login" | "register";
  onClick?: () => void;
};

export function AuthIntentLink({ href, children, className, mode = "login", onClick }: AuthIntentLinkProps) {
  const { currentUser, openAuthModal } = usePlatform();

  if (currentUser?.id) {
    return <Link className={className} href={href} onClick={onClick}>{children}</Link>;
  }

  return (
    <button
      type="button"
      className={className}
      onClick={() => {
        onClick?.();
        openAuthModal(mode, href);
      }}
    >
      {children}
    </button>
  );
}
