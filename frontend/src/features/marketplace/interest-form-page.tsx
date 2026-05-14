"use client";

import Link from "next/link";
import { ArrowLeft, Info, Upload } from "lucide-react";
import { useEffect, useState } from "react";
import { usePlatform } from "@/features/platform/platform-context";
import { lookupAddressByPostalCode } from "@/shared/api/client";
import { formatCep, hasLink, limitText } from "@/shared/lib/format";
import { Button } from "@/shared/ui/button";
import { FieldCounter } from "@/shared/ui/field-counter";

const TITLE_MAX_LENGTH = 80;
const DESCRIPTION_MAX_LENGTH = 250;

export function InterestFormPage() {
  const { categories, currentUser, openAuthModal, saveInterest, setFeedback } = usePlatform();
  const [form, setForm] = useState({
    title: "",
    description: "",
    category: "",
    tags: "",
    budgetMin: "",
    budgetMax: "",
    postalCode: "",
    city: currentUser?.city ?? "",
    state: currentUser?.state ?? "",
    neighborhood: currentUser?.neighborhood ?? "",
    country: currentUser?.country ?? "Brasil",
    preferredCondition: "",
    preferredContactMode: "",
    desiredRadiusKm: "25",
    allowsWhatsappContact: false,
    whatsappContact: "",
    referenceImageUrl: ""
  });
  const [isSaving, setIsSaving] = useState(false);
  const [lookupState, setLookupState] = useState<{ loading: boolean; message: string; tone: "muted" | "success" | "error" }>({
    loading: false,
    message: "",
    tone: "muted"
  });

  useEffect(() => {
    const normalizedPostalCode = form.postalCode.replace(/\D/g, "");
    if (normalizedPostalCode.length !== 8) {
      return;
    }
    const timer = window.setTimeout(() => {
      handlePostalCodeLookup(normalizedPostalCode);
    }, 380);
    return () => window.clearTimeout(timer);
  }, [form.postalCode]);

  async function handlePostalCodeLookup(postalCode = form.postalCode) {
    const normalizedPostalCode = String(postalCode).replace(/\D/g, "");
    if (!normalizedPostalCode) {
      setLookupState({ loading: false, message: "", tone: "muted" });
      return;
    }
    if (normalizedPostalCode.length !== 8) {
      setLookupState({ loading: false, message: "Digite um CEP com 8 numeros.", tone: "error" });
      return;
    }
    setLookupState({ loading: true, message: "Buscando endereco pelo CEP...", tone: "muted" });
    try {
      const address = await lookupAddressByPostalCode(normalizedPostalCode);
      setForm((current) => ({
        ...current,
        postalCode: formatCep(String(address.postalCode ?? current.postalCode)),
        city: String(address.city ?? current.city ?? ""),
        state: String(address.state ?? current.state ?? "").toUpperCase().slice(0, 2),
        neighborhood: String(address.neighborhood ?? current.neighborhood ?? ""),
        country: String(address.country ?? current.country ?? "Brasil")
      }));
      setLookupState({ loading: false, message: "Endereco preenchido pelo CEP.", tone: "success" });
    } catch (error) {
      setLookupState({ loading: false, message: error instanceof Error ? error.message : "Nao encontramos esse CEP. Preencha cidade e UF manualmente.", tone: "error" });
    }
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!currentUser?.id) {
      openAuthModal("login");
      return;
    }
    if (hasLink(form.description)) {
      setFeedback({ type: "error", title: "Link não permitido", message: "Remova links da descrição da procura antes de enviar para moderação." });
      return;
    }
    if (form.budgetMin && form.budgetMax && Number(form.budgetMin) > Number(form.budgetMax)) {
      setFeedback({ type: "error", title: "Orçamento inválido", message: "O orçamento mínimo não pode ser maior que o orçamento máximo." });
      return;
    }
    setIsSaving(true);
    try {
      await saveInterest({
        title: form.title,
        description: form.description,
        category: form.category,
        budgetMin: form.budgetMin ? Number(form.budgetMin) : null,
        budgetMax: form.budgetMax ? Number(form.budgetMax) : null,
        tags: form.tags.split(",").map((tag) => tag.trim()).filter(Boolean),
        desiredRadiusKm: Number(form.desiredRadiusKm || 0),
        allowsWhatsappContact: form.allowsWhatsappContact,
        whatsappContact: form.whatsappContact,
        preferredCondition: form.preferredCondition,
        preferredContactMode: form.preferredContactMode,
        referenceImageUrl: form.referenceImageUrl || null,
        postalCode: form.postalCode,
        city: form.city,
        state: form.state,
        neighborhood: form.neighborhood,
        country: form.country
      });
      setForm((current) => ({ ...current, title: "", description: "", tags: "", budgetMin: "", budgetMax: "", referenceImageUrl: "" }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "";
      const friendlyMessage = /city|state|cidade|uf/i.test(message)
        ? "Confira cidade e UF. Se voce informou o CEP, aguarde o preenchimento automatico ou revise os campos de localizacao."
        : message || "Revise os dados e tente novamente.";
      setFeedback({ type: "error", title: "Nao foi possivel publicar", message: friendlyMessage });
    } finally {
      setIsSaving(false);
    }
  }

  function update(field: keyof typeof form, value: string | boolean) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  return (
    <section className="route-shell form-route">
      <Link href="/" className="back-link"><ArrowLeft size={16} /> Voltar</Link>
      <div className="form-heading">
        <h1>O que você procura?</h1>
        <p>Descreva detalhadamente o que você precisa para receber as melhores propostas.</p>
      </div>
      <form className="feature-form" onSubmit={submit}>
        <section className="form-section">
          <h2>Informações principais</h2>
          <p>O título e a descrição são os itens mais importantes da sua procura.</p>
          <label>
            Título da procura
            <input value={form.title} onChange={(event) => update("title", limitText(event.target.value, TITLE_MAX_LENGTH))} placeholder="Ex: Procuro eletricista para instalação residencial" required />
            <FieldCounter value={form.title} max={TITLE_MAX_LENGTH} />
          </label>
          <label>
            Categoria
            <select value={form.category} onChange={(event) => update("category", event.target.value)} required>
              <option value="">Selecione uma categoria...</option>
              {categories.map((category) => <option key={category.value} value={category.value}>{category.label}</option>)}
            </select>
          </label>
          <label>
            Descrição detalhada
            <textarea rows={5} value={form.description} onChange={(event) => update("description", limitText(event.target.value, DESCRIPTION_MAX_LENGTH))} placeholder="Descreva marca, modelo, condição esperada, urgência..." required />
            <span className="inline-help"><Info size={14} /> Links e contatos não são permitidos.</span>
            <FieldCounter value={form.description} max={DESCRIPTION_MAX_LENGTH} />
          </label>
          <label>
            Tags
            <input value={form.tags} onChange={(event) => update("tags", event.target.value)} placeholder="Ex: urgente, usado, conserto" />
          </label>
          <div className="upload-box">
            <Upload size={28} />
            <strong>Imagem de referência</strong>
            <p>Use JPG ou PNG. A compressão será aplicada antes do envio quando houver imagem selecionada.</p>
          </div>
        </section>
        <section className="form-section">
          <h2>Orçamento e localização</h2>
          <div className="form-grid">
            <label>Orçamento mínimo<input type="number" min="0" value={form.budgetMin} onChange={(event) => update("budgetMin", event.target.value)} /></label>
            <label>Orçamento máximo<input type="number" min="0" value={form.budgetMax} onChange={(event) => update("budgetMax", event.target.value)} required /></label>
          </div>
          <div className="form-grid form-grid--3">
            <label>CEP<input value={form.postalCode} onChange={(event) => update("postalCode", formatCep(event.target.value))} onBlur={() => handlePostalCodeLookup()} placeholder="00000-000" /></label>
            <label>Cidade<input value={form.city} onChange={(event) => update("city", event.target.value)} required /></label>
            <label>UF<input value={form.state} onChange={(event) => update("state", event.target.value.toUpperCase().slice(0, 2))} required /></label>
          </div>
          {lookupState.message ? <span className={`address-lookup-note address-lookup-note--${lookupState.tone}`} role="status" aria-live="polite" aria-busy={lookupState.loading}>{lookupState.message}</span> : null}
          <div className="form-grid">
            <label>Bairro<input value={form.neighborhood} onChange={(event) => update("neighborhood", event.target.value)} /></label>
            <label>País<input value={form.country} onChange={(event) => update("country", event.target.value)} /></label>
          </div>
          <div className="form-grid">
            <label>Condição preferida<input value={form.preferredCondition} onChange={(event) => update("preferredCondition", event.target.value)} placeholder="Novo, usado, indiferente" /></label>
            <label>Modo de contato<select value={form.preferredContactMode} onChange={(event) => update("preferredContactMode", event.target.value)}><option value="">Selecione...</option><option value="CHAT">Chat da plataforma</option><option value="WHATSAPP">WhatsApp</option><option value="EMAIL">E-mail</option></select></label>
          </div>
          <label className="checkbox-row">
            <input type="checkbox" checked={form.allowsWhatsappContact} onChange={(event) => update("allowsWhatsappContact", event.target.checked)} />
            <span>Permitir contato via WhatsApp</span>
          </label>
          {form.allowsWhatsappContact ? <label>WhatsApp<input value={form.whatsappContact} onChange={(event) => update("whatsappContact", event.target.value)} /></label> : null}
          <div className="notice-box">Esta procura ficará ativa por até 30 dias e depois poderá ser renovada com crédito.</div>
        </section>
        <div className="form-actions">
          <Link className="button button--outline" href="/">Cancelar</Link>
          <Button type="submit" disabled={isSaving}>{isSaving ? "Publicando..." : "Publicar Procura"}</Button>
        </div>
      </form>
    </section>
  );
}
