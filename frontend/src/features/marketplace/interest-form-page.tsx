"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Info, Upload } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import type { ComponentProps } from "react";
import { usePlatform } from "@/features/platform/platform-context";
import { fetchInterest, lookupAddressByPostalCode } from "@/shared/api/client";
import type { Interest } from "@/shared/api/types";
import { formatCep, hasLink, limitText } from "@/shared/lib/format";
import { readImageFile } from "@/shared/lib/image-upload";
import { Button } from "@/shared/ui/button";
import { BackButton } from "@/shared/ui/back-button";
import { FieldCounter } from "@/shared/ui/field-counter";
import { STICKERS_CATEGORY } from "@/features/stickers/stickers-data";

const TITLE_MAX_LENGTH = 80;
const DESCRIPTION_MAX_LENGTH = 250;
type FormSubmitHandler = NonNullable<ComponentProps<"form">["onSubmit"]>;

function initialInterestForm(currentUser?: { city?: string; state?: string; neighborhood?: string; country?: string } | null) {
  return {
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
  };
}

function interestToForm(interest: Interest, currentForm: ReturnType<typeof initialInterestForm>) {
  return {
    ...currentForm,
    title: interest.title ?? "",
    description: interest.description ?? "",
    category: interest.category ?? "",
    tags: (interest.tags ?? []).join(", "),
    budgetMin: interest.budgetMin == null ? "" : String(interest.budgetMin),
    budgetMax: interest.budgetMax == null ? "" : String(interest.budgetMax),
    postalCode: formatCep(String(interest.location?.postalCode ?? "")),
    city: String(interest.location?.city ?? ""),
    state: String(interest.location?.state ?? "").toUpperCase().slice(0, 2),
    neighborhood: String(interest.location?.neighborhood ?? ""),
    country: String(interest.location?.country ?? "Brasil"),
    preferredCondition: String(interest.preferredCondition ?? ""),
    preferredContactMode: String(interest.preferredContactMode ?? ""),
    desiredRadiusKm: String(interest.desiredRadiusKm ?? currentForm.desiredRadiusKm),
    allowsWhatsappContact: Boolean(interest.allowsWhatsappContact),
    whatsappContact: String(interest.whatsappContact ?? ""),
    referenceImageUrl: String(interest.referenceImageUrl ?? "")
  };
}

export function InterestFormPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const editingInterestId = searchParams.get("editar") ?? searchParams.get("edit");
  const { categories, currentUser, dashboard, openAuthModal, saveInterest, setFeedback } = usePlatform();
  const [form, setForm] = useState(() => initialInterestForm(currentUser));
  const [isSaving, setIsSaving] = useState(false);
  const [isLoadingInterest, setIsLoadingInterest] = useState(false);
  const [isRedirectingToStickers, setIsRedirectingToStickers] = useState(false);
  const loadedEditingInterestRef = useRef<string | null>(null);
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

  useEffect(() => {
    if (!editingInterestId) {
      loadedEditingInterestRef.current = null;
      return;
    }
    if (loadedEditingInterestRef.current === editingInterestId) {
      return;
    }
    const cachedInterest = dashboard?.myInterests?.find((interest) => interest.id === editingInterestId);
    if (cachedInterest) {
      if (cachedInterest.category === STICKERS_CATEGORY) {
        router.replace(`/figurinhas/publicar?editar=${editingInterestId}`);
        return;
      }
      setForm((current) => interestToForm(cachedInterest, current));
      loadedEditingInterestRef.current = editingInterestId;
      return;
    }
    setIsLoadingInterest(true);
    fetchInterest(editingInterestId)
      .then((interest) => {
        if (interest.category === STICKERS_CATEGORY) {
          router.replace(`/figurinhas/publicar?editar=${editingInterestId}`);
          return;
        }
        setForm((current) => interestToForm(interest, current));
        loadedEditingInterestRef.current = editingInterestId;
      })
      .catch((error) => setFeedback({ type: "error", title: "Procura indisponivel", message: error instanceof Error ? error.message : "Nao foi possivel carregar esta procura para edicao." }))
      .finally(() => setIsLoadingInterest(false));
  }, [dashboard?.myInterests, editingInterestId, router, setFeedback]);

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
      setLookupState({ loading: false, message: "Endereço preenchido pelo CEP.", tone: "success" });
    } catch (error) {
      setLookupState({ loading: false, message: error instanceof Error ? error.message : "Não encontramos esse CEP. Preencha cidade e UF manualmente.", tone: "error" });
    }
  }

  const submit: FormSubmitHandler = async (event) => {
    event.preventDefault();
    if (!currentUser?.id) {
      openAuthModal("login");
      return;
    }
    if (form.category === STICKERS_CATEGORY && !editingInterestId) {
      setIsRedirectingToStickers(true);
      router.push("/figurinhas/publicar");
      return;
    }
    if (hasLink(form.description)) {
      setFeedback({ type: "error", title: "Link nao permitido", message: "Remova links da descricao da procura antes de enviar para moderacao." });
      return;
    }
    if (form.budgetMin && form.budgetMax && Number(form.budgetMin) > Number(form.budgetMax)) {
      setFeedback({ type: "error", title: "Orçamento inválido", message: "O orçamento mínimo não pode ser maior que o orçamento máximo." });
      return;
    }
    setIsSaving(true);
    try {
      const savedInterest = await saveInterest({
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
      }, editingInterestId);
      if (savedInterest?.id) {
        setFeedback({
          type: "success",
          title: editingInterestId ? "Alteração recebida" : "Procura recebida",
          message: "Vamos validar sua procura agora. Se houver recusa, voce receberá um aviso para ajustar.",
          afterClose: () => router.push(`/interesses/${savedInterest.id}`)
        });
      }
      if (!editingInterestId) {
        setForm((current) => ({ ...current, title: "", description: "", tags: "", budgetMin: "", budgetMax: "", referenceImageUrl: "" }));
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : "";
      const friendlyMessage = /city|state|cidade|uf/i.test(message)
        ? "Confira cidade e UF. Se você informou o CEP, aguarde o preenchimento automático ou revise os campos de localização."
        : message || "Revise os dados e tente novamente.";
      setFeedback({ type: "error", title: "Não foi possível publicar", message: friendlyMessage });
    } finally {
      setIsSaving(false);
    }
  };

  function update(field: keyof typeof form, value: string | boolean) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function handleCategoryChange(value: string) {
    update("category", value);
    if (value === STICKERS_CATEGORY && !editingInterestId) {
      setIsRedirectingToStickers(true);
      router.push("/figurinhas/publicar");
    }
  }

  async function handleReferenceImageChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    try {
      update("referenceImageUrl", await readImageFile(file));
    } catch (error) {
      setFeedback({ type: "error", title: "Imagem inválida", message: error instanceof Error ? error.message : "Selecione outra imagem." });
    }
  }

  return (
    <section className="route-shell form-route">
      <BackButton />
      <div className="form-heading">
        <h1>{editingInterestId ? "Editar procura" : "O que voce procura?"}</h1>
        <p>{editingInterestId ? "Ajuste os dados da procura e envie novamente para validação." : "Descreva detalhadamente o que você precisa para receber as melhores propostas."}</p>
      </div>
      {isLoadingInterest ? <div className="section-loading" role="status">Carregando procura para edição...</div> : null}
      {isRedirectingToStickers ? <div className="section-loading" role="status">Abrindo o formulário de figurinhas...</div> : null}
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
            <select value={form.category} onChange={(event) => handleCategoryChange(event.target.value)} required disabled={isRedirectingToStickers}>
              <option value="">Selecione uma categoria...</option>
              {categories.map((category) => <option key={category.value} value={category.value}>{category.label}</option>)}
            </select>
          </label>
          <label>
            Descrição detalhada
            <textarea rows={5} value={form.description} onChange={(event) => update("description", limitText(event.target.value, DESCRIPTION_MAX_LENGTH))} placeholder="Descreva marca, modelo, condicao esperada, urgencia..." required />
            <span className="inline-help"><Info size={14} /> Links e contatos não são permitidos.</span>
            <FieldCounter value={form.description} max={DESCRIPTION_MAX_LENGTH} />
          </label>
          <label>
            Tags
            <input value={form.tags} onChange={(event) => update("tags", event.target.value)} placeholder="Ex: urgente, usado, conserto" />
          </label>
          <label className="upload-box">
            {form.referenceImageUrl ? <img className="upload-preview" src={form.referenceImageUrl} alt="Previa da imagem de referência" /> : <Upload size={28} />}
            <strong>{form.referenceImageUrl ? "Trocar imagem de referência" : "Imagem de referência"}</strong>
            <p>Use JPG ou PNG. A compressão será aplicada antes do envio quando houver imagem selecionada.</p>
            <span className="button button--outline button--sm">Selecionar imagem</span>
            <input type="file" accept="image/png,image/jpeg,image/webp" onChange={handleReferenceImageChange} />
          </label>
        </section>
        <section className="form-section">
          <h2>Orçamento e localização</h2>
          <div className="form-grid">
            <label>Orçamento minimo<input type="number" min="0" value={form.budgetMin} onChange={(event) => update("budgetMin", event.target.value)} /></label>
            <label>Orçamento maximo<input type="number" min="0" value={form.budgetMax} onChange={(event) => update("budgetMax", event.target.value)} required /></label>
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
          <Link className="button button--outline" href="/meus-interesses">Cancelar</Link>
          <Button type="submit" disabled={isSaving || isLoadingInterest || isRedirectingToStickers}>{isRedirectingToStickers ? "Abrindo figurinhas..." : isSaving ? (editingInterestId ? "Salvando..." : "Publicando...") : (editingInterestId ? "Salvar alteracoes" : "Publicar Procura")}</Button>
        </div>
      </form>
    </section>
  );
}
