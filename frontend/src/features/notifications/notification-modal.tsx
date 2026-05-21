"use client";

import Link from "next/link";
import { Bell, CheckCheck, MessageSquare, X } from "lucide-react";
import type { CSSProperties } from "react";
import type { Offer } from "@/shared/api/types";
import { formatDateTime } from "@/shared/lib/format";
import { Button } from "@/shared/ui/button";

export type AppNotification = {
  id: string;
  title: string;
  message: string;
  createdAt?: string;
  href: string;
  source: "received" | "sent";
};

export function buildOfferNotifications(receivedOffers: Offer[] = []): AppNotification[] {
  return receivedOffers
    .map((offer) => ({
      id: `received:${offer.id}`,
      title: offer.interestTitle ? `Nova proposta em "${offer.interestTitle}"` : "Nova proposta recebida",
      message: offer.sellerName ? `${offer.sellerName} enviou uma proposta.` : offer.message || "Uma proposta chegou para uma procura sua.",
      createdAt: offer.latestMessageAt ?? offer.createdAt,
      href: "/ofertas-recebidas",
      source: "received" as const
    }))
    .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
    .slice(0, 8);
}

export function NotificationModal({
  visible,
  notifications,
  seenIds,
  anchorRect,
  onClose,
  onMarkAllRead
}: {
  visible: boolean;
  notifications: AppNotification[];
  seenIds: string[];
  anchorRect?: DOMRect | null;
  onClose: () => void;
  onMarkAllRead: () => void;
}) {
  if (!visible) {
    return null;
  }

  const modalStyle = anchorRect && typeof window !== "undefined"
    ? ({
      "--notification-top": `${Math.round(anchorRect.bottom + 10)}px`,
      "--notification-right": `${Math.max(12, Math.round(window.innerWidth - anchorRect.right))}px`
    } as CSSProperties)
    : undefined;

  return (
    <div className="modal-overlay modal-overlay--topbar modal-overlay--plain" role="presentation" onClick={onClose}>
      <aside className="notification-modal" style={modalStyle} role="dialog" aria-modal="true" aria-labelledby="notification-title" onClick={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title">
            <Bell size={18} />
            <div>
              <span className="pill">Notificacoes</span>
              <h2 id="notification-title">Atualizacoes recentes</h2>
            </div>
          </div>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Fechar notificacoes"><X size={17} /></button>
        </div>

        {notifications.length ? (
          <>
            <button type="button" className="text-button notification-read-all" onClick={onMarkAllRead}>
              <CheckCheck size={15} /> Marcar todas como lidas
            </button>
            <div className="notification-list">
              {notifications.map((notification) => {
                const isUnread = !seenIds.includes(notification.id);
                return (
                  <Link key={notification.id} href={notification.href} className={`notification-item ${isUnread ? "notification-item--unread" : ""}`} onClick={onClose}>
                    <span className="notification-dot" aria-hidden="true" />
                    <strong>{notification.title}</strong>
                    <p>{notification.message}</p>
                    <small>{formatDateTime(notification.createdAt)}</small>
                  </Link>
                );
              })}
            </div>
          </>
        ) : (
          <div className="empty-state empty-state--compact">
            <MessageSquare size={28} />
            <h3>Nada novo por aqui</h3>
            <p>Quando surgirem propostas ou respostas, elas aparecem neste painel.</p>
            <Link className="button button--outline button--sm" href="/meus-interesses" onClick={onClose}>Ir para o painel</Link>
          </div>
        )}

        <div className="modal-actions">
          <Button type="button" variant="outline" onClick={onClose}>Fechar</Button>
          <Link className="button button--primary" href="/ofertas-recebidas" onClick={onClose}>Ver propostas</Link>
        </div>
      </aside>
    </div>
  );
}
