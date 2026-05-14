"use client";

import { useState } from "react";
import { usePlatform } from "@/features/platform/platform-context";
import { Button } from "@/shared/ui/button";

const types = ["Reclamação", "Denúncia sobre atendimento", "Problema com pagamento", "Contestação de moderação", "Sugestão", "Outro"];

export function OmbudsmanPage() {
  const { currentUser, submitOmbudsman } = usePlatform();
  const [form, setForm] = useState({
    name: currentUser?.name ?? "",
    email: currentUser?.email ?? "",
    type: types[0],
    subject: "",
    message: "",
    relatedEntityType: "",
    relatedEntityId: "",
    truthDeclarationAccepted: false
  });
  const [protocol, setProtocol] = useState("");

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const response = await submitOmbudsman(form);
    setProtocol(response?.protocol ?? "");
    setForm((current) => ({ ...current, subject: "", message: "", relatedEntityType: "", relatedEntityId: "", truthDeclarationAccepted: false }));
  }

  return (
    <section className="route-shell form-route">
      <div className="form-heading">
        <span className="pill">Ouvidoria</span>
        <h1>Canal formal de manifestações</h1>
        <p>Envie reclamações, sugestões, contestações de moderação ou problemas de pagamento.</p>
      </div>
      {protocol ? <div className="success-banner">Protocolo gerado: <strong>{protocol}</strong></div> : null}
      <form className="feature-form" onSubmit={submit}>
        <section className="form-section">
          <div className="form-grid">
            <label>Nome<input value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} maxLength={120} required /></label>
            <label>E-mail<input type="email" value={form.email} onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))} maxLength={120} required /></label>
          </div>
          <label>Tipo<select value={form.type} onChange={(event) => setForm((current) => ({ ...current, type: event.target.value }))}>{types.map((type) => <option key={type}>{type}</option>)}</select></label>
          <label>Assunto<input value={form.subject} onChange={(event) => setForm((current) => ({ ...current, subject: event.target.value }))} maxLength={140} required /></label>
          <label>Mensagem<textarea rows={7} value={form.message} onChange={(event) => setForm((current) => ({ ...current, message: event.target.value }))} maxLength={2000} required /></label>
          <div className="form-grid">
            <label>Tipo de referência<input value={form.relatedEntityType} onChange={(event) => setForm((current) => ({ ...current, relatedEntityType: event.target.value }))} maxLength={120} /></label>
            <label>ID relacionado<input value={form.relatedEntityId} onChange={(event) => setForm((current) => ({ ...current, relatedEntityId: event.target.value }))} maxLength={120} /></label>
          </div>
          <label className="checkbox-row"><input type="checkbox" checked={form.truthDeclarationAccepted} onChange={(event) => setForm((current) => ({ ...current, truthDeclarationAccepted: event.target.checked }))} required /><span>Declaro que as informações são verdadeiras.</span></label>
          <Button type="submit">Enviar manifestação</Button>
        </section>
      </form>
    </section>
  );
}
