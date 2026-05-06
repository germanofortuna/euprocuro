import { useEffect, useMemo, useState } from "react";

import {
  archiveContentEntry,
  fetchAdminContent,
  invalidatePublicCache,
  publishContentEntry,
  saveContentEntry
} from "../api";
import { useContentText } from "../content/ContentContext";
import EmptyState from "./EmptyState";

const CONTENT_TYPES = [
  "TEXT",
  "RICH_TEXT",
  "LEGAL_DOCUMENT",
  "LABEL",
  "CTA",
  "ERROR_MESSAGE",
  "EMAIL_TEMPLATE",
  "CATALOG"
];

const STATUS_OPTIONS = ["ALL", "DRAFT", "PUBLISHED", "ARCHIVED"];

const emptyForm = {
  id: "",
  key: "",
  type: "TEXT",
  locale: "pt-BR",
  screen: "",
  description: "",
  legalSlug: "",
  requiresUserAcceptance: false,
  draftValue: ""
};

function toForm(entry) {
  if (!entry) {
    return emptyForm;
  }

  return {
    id: entry.id ?? "",
    key: entry.key ?? "",
    type: entry.type ?? "TEXT",
    locale: entry.locale ?? "pt-BR",
    screen: entry.screen ?? "",
    description: entry.description ?? "",
    legalSlug: entry.legalSlug ?? "",
    requiresUserAcceptance: Boolean(entry.requiresUserAcceptance),
    draftValue: entry.draftValue ?? entry.publishedValue ?? ""
  };
}

function normalizeSearch(value) {
  return value.trim().toLowerCase();
}

export default function ContentAdminPanel({ onFeedback }) {
  const { t } = useContentText();
  const [entries, setEntries] = useState([]);
  const [selectedId, setSelectedId] = useState("");
  const [form, setForm] = useState(emptyForm);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [isInvalidatingCache, setIsInvalidatingCache] = useState(false);

  const selectedEntry = useMemo(
    () => entries.find((entry) => entry.id === selectedId) ?? null,
    [entries, selectedId]
  );

  const filteredEntries = useMemo(() => {
    const normalizedSearch = normalizeSearch(search);
    return entries.filter((entry) => {
      const matchesStatus = statusFilter === "ALL" || entry.status === statusFilter;
      const searchable = [
        entry.key,
        entry.screen,
        entry.description,
        entry.draftValue,
        entry.publishedValue
      ].filter(Boolean).join(" ").toLowerCase();
      return matchesStatus && (!normalizedSearch || searchable.includes(normalizedSearch));
    });
  }, [entries, search, statusFilter]);

  function showFeedback(type, titleKey, messageKey, fallbackMessage) {
    onFeedback?.(type, t(titleKey), messageKey ? t(messageKey) : fallbackMessage);
  }

  async function loadContent() {
    setIsLoading(true);
    try {
      const payload = await fetchAdminContent();
      const loadedEntries = payload?.entries ?? [];
      setEntries(loadedEntries);
      setSelectedId((current) => loadedEntries.some((entry) => entry.id === current) ? current : loadedEntries[0]?.id ?? "");
    } catch (error) {
      showFeedback("error", "contentAdmin.feedback.loadError.title", null, error.message);
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    loadContent();
  }, []);

  useEffect(() => {
    setForm(toForm(selectedEntry));
  }, [selectedEntry?.id]);

  function updateForm(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function startNewEntry() {
    setSelectedId("");
    setForm(emptyForm);
  }

  async function handleSave(event) {
    event.preventDefault();
    setIsSaving(true);
    try {
      const saved = await saveContentEntry(form.id || null, {
        key: form.key,
        type: form.type,
        locale: form.locale,
        screen: form.screen,
        description: form.description,
        legalSlug: form.legalSlug,
        requiresUserAcceptance: form.requiresUserAcceptance,
        draftValue: form.draftValue
      });
      setEntries((current) => {
        const exists = current.some((entry) => entry.id === saved.id);
        return exists
          ? current.map((entry) => (entry.id === saved.id ? saved : entry))
          : [saved, ...current];
      });
      setSelectedId(saved.id);
      showFeedback("success", "contentAdmin.feedback.saveSuccess.title", "contentAdmin.feedback.saveSuccess.message");
    } catch (error) {
      onFeedback?.("error", t("contentAdmin.feedback.saveError.title"), error.message);
    } finally {
      setIsSaving(false);
    }
  }

  async function handlePublish() {
    if (!form.id) {
      return;
    }

    setIsPublishing(true);
    try {
      const published = await publishContentEntry(form.id);
      setEntries((current) => current.map((entry) => (entry.id === published.id ? published : entry)));
      setSelectedId(published.id);
      showFeedback("success", "contentAdmin.feedback.publishSuccess.title", "contentAdmin.feedback.publishSuccess.message");
    } catch (error) {
      onFeedback?.("error", t("contentAdmin.feedback.publishError.title"), error.message);
    } finally {
      setIsPublishing(false);
    }
  }

  async function handleArchive() {
    if (!form.id) {
      return;
    }

    setIsPublishing(true);
    try {
      const archived = await archiveContentEntry(form.id);
      setEntries((current) => current.map((entry) => (entry.id === archived.id ? archived : entry)));
      setSelectedId(archived.id);
      showFeedback("success", "contentAdmin.feedback.archiveSuccess.title", "contentAdmin.feedback.archiveSuccess.message");
    } catch (error) {
      onFeedback?.("error", t("contentAdmin.feedback.archiveError.title"), error.message);
    } finally {
      setIsPublishing(false);
    }
  }

  async function handleInvalidateCache() {
    setIsInvalidatingCache(true);
    try {
      await invalidatePublicCache("all");
      showFeedback("success", "contentAdmin.feedback.cacheInvalidated.title", "contentAdmin.feedback.cacheInvalidated.message");
    } catch (error) {
      onFeedback?.("error", t("contentAdmin.feedback.cacheInvalidationError.title"), error.message);
    } finally {
      setIsInvalidatingCache(false);
    }
  }

  return (
    <article className="admin-card admin-card--content">
      <div className="content-admin__header">
        <div>
          <span className="eyebrow">{t("contentAdmin.eyebrow")}</span>
          <h3>{t("contentAdmin.title")}</h3>
          <p>{t("contentAdmin.subtitle")}</p>
        </div>
        <div className="inline-actions">
          <button type="button" className="ghost-button ghost-button--small" onClick={handleInvalidateCache} disabled={isInvalidatingCache}>
            {isInvalidatingCache ? t("common.actions.loading") : t("contentAdmin.cache.invalidate")}
          </button>
          <button type="button" className="ghost-button ghost-button--small" onClick={loadContent} disabled={isLoading}>
            {isLoading ? t("common.actions.loading") : t("contentAdmin.refresh")}
          </button>
          <button type="button" className="primary-button primary-button--compact" onClick={startNewEntry}>
            {t("contentAdmin.new")}
          </button>
        </div>
      </div>

      <div className="content-admin__safety">
        {t("contentAdmin.help.publicSafety")}
      </div>

      <div className="content-admin">
        <aside className="content-admin__list">
          <div className="content-admin__filters">
            <input
              value={search}
              placeholder={t("contentAdmin.search.placeholder")}
              onChange={(event) => setSearch(event.target.value)}
            />
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              {STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>
                  {status === "ALL" ? t("contentAdmin.status.all") : t(`contentAdmin.status.${status}`)}
                </option>
              ))}
            </select>
          </div>

          {filteredEntries.length ? (
            <div className="content-admin__entries">
              {filteredEntries.map((entry) => (
                <button
                  key={entry.id}
                  type="button"
                  className={`content-entry-button ${entry.id === selectedId ? "active" : ""}`}
                  onClick={() => setSelectedId(entry.id)}
                >
                  <strong>{entry.key}</strong>
                  <span>{entry.screen || entry.locale}</span>
                  <small>{t(`contentAdmin.status.${entry.status}`)} · v{entry.version}</small>
                </button>
              ))}
            </div>
          ) : (
            <EmptyState
              title={t("contentAdmin.empty.title")}
              description={t("contentAdmin.empty.description")}
            />
          )}
        </aside>

        <form className="content-admin__form stacked-form" onSubmit={handleSave}>
          {!form.id && !form.key ? (
            <div className="content-admin__empty-note">
              <strong>{t("contentAdmin.form.emptyTitle")}</strong>
              <p>{t("contentAdmin.form.emptyDescription")}</p>
            </div>
          ) : null}

          <div className="two-columns">
            <input
              value={form.key}
              placeholder={t("contentAdmin.form.key")}
              onChange={(event) => updateForm("key", event.target.value)}
              required
            />
            <input
              value={form.locale}
              placeholder={t("contentAdmin.form.locale")}
              onChange={(event) => updateForm("locale", event.target.value)}
              required
            />
          </div>

          <div className="two-columns">
            <select value={form.type} onChange={(event) => updateForm("type", event.target.value)}>
              {CONTENT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {t(`contentAdmin.type.${type}`)}
                </option>
              ))}
            </select>
            <input
              value={form.screen}
              placeholder={t("contentAdmin.form.screen")}
              onChange={(event) => updateForm("screen", event.target.value)}
            />
          </div>

          <input
            value={form.description}
            placeholder={t("contentAdmin.form.description")}
            onChange={(event) => updateForm("description", event.target.value)}
          />

          <div className="two-columns">
            <div className="field-with-help">
              <input
                value={form.legalSlug}
                placeholder={t("contentAdmin.form.legalPageIdentifier")}
                title={t("contentAdmin.form.legalPageIdentifierHelp")}
                onChange={(event) => updateForm("legalSlug", event.target.value)}
              />
              <small>{t("contentAdmin.form.legalPageIdentifierHelp")}</small>
            </div>
            <label className="checkbox-row checkbox-row--with-help" title={t("contentAdmin.form.requestAcceptanceOnPublishHelp")}>
              <input
                type="checkbox"
                checked={form.requiresUserAcceptance}
                onChange={(event) => updateForm("requiresUserAcceptance", event.target.checked)}
              />
              <span>
                {t("contentAdmin.form.requestAcceptanceOnPublish")}
                <small>{t("contentAdmin.form.requestAcceptanceOnPublishHelp")}</small>
              </span>
            </label>
          </div>

          <textarea
            rows="12"
            value={form.draftValue}
            placeholder={t("contentAdmin.form.value")}
            onChange={(event) => updateForm("draftValue", event.target.value)}
            required
          />

          <div className="content-admin__meta">
            {selectedEntry ? (
              <>
                <span>{t(`contentAdmin.status.${selectedEntry.status}`)}</span>
                <span>v{selectedEntry.version}</span>
                {selectedEntry.publishedAt ? <span>{new Date(selectedEntry.publishedAt).toLocaleString("pt-BR")}</span> : null}
              </>
            ) : null}
          </div>

          <div className="inline-actions">
            <button type="submit" className="primary-button primary-button--compact" disabled={isSaving}>
              {isSaving ? t("common.actions.saving") : t("common.actions.saveDraft")}
            </button>
            <button type="button" className="ghost-button ghost-button--small" onClick={handlePublish} disabled={!form.id || isPublishing}>
              {t("common.actions.publish")}
            </button>
            <button type="button" className="danger-button action-button--compact" onClick={handleArchive} disabled={!form.id || isPublishing}>
              {t("common.actions.archive")}
            </button>
          </div>
        </form>
      </div>
    </article>
  );
}
