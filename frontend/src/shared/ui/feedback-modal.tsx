"use client";

import { AlertTriangle, CheckCircle2, Info, X } from "lucide-react";
import { Button } from "./button";

export type FeedbackState = {
  type?: "success" | "error" | "warning" | "info";
  title: string;
  message: string;
  afterClose?: () => void;
} | null;

const icons = {
  success: CheckCircle2,
  error: AlertTriangle,
  warning: AlertTriangle,
  info: Info
};

export function FeedbackModal({ modal, onClose }: { modal: FeedbackState; onClose: () => void }) {
  if (!modal) {
    return null;
  }
  const Icon = icons[modal.type ?? "info"];
  function close() {
    const afterClose = modal?.afterClose;
    onClose();
    afterClose?.();
  }

  return (
    <div className="modal-overlay" role="presentation" onClick={close}>
      <div className={`modal-card feedback feedback--${modal.type ?? "info"}`} role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title">
            <span className="status-icon"><Icon size={18} /></span>
            <strong>{modal.title}</strong>
          </div>
          <button type="button" className="icon-button" onClick={close} aria-label="Fechar modal">
            <X size={18} />
          </button>
        </div>
        <p>{modal.message}</p>
        <div className="modal-actions">
          <Button type="button" onClick={close}>Entendi</Button>
        </div>
      </div>
    </div>
  );
}
