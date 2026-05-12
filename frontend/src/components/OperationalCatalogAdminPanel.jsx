import { useEffect, useMemo, useState } from "react";

import { fetchAdminCatalog, saveAdminCatalog } from "../api";
import { useContentText } from "../content/ContentContext";

const PRODUCT_TYPES = ["CREDIT_PACK", "SUBSCRIPTION", "BOOST"];

const emptyCategory = {
  code: "",
  label: "",
  active: true,
  sortOrder: 0
};

const emptyProduct = {
  code: "",
  name: "",
  description: "",
  type: "CREDIT_PACK",
  price: "",
  originalPrice: "",
  promotional: false,
  promotionLabel: "",
  credits: "",
  durationDays: "",
  enabled: true,
  sortOrder: 0
};

const defaultMonetizationSettings = {
  creditPurchasesEnabled: false,
  boostPurchasesEnabled: false
};

const defaultModerationSettings = {
  userBlockListEnabled: true
};

function normalizeCategory(category, index) {
  return {
    ...emptyCategory,
    ...category,
    sortOrder: category?.sortOrder ?? (index + 1) * 10
  };
}

function normalizeProduct(product, index) {
  return {
    ...emptyProduct,
    ...product,
    price: product?.price ?? "",
    originalPrice: product?.originalPrice ?? "",
    credits: product?.credits ?? "",
    durationDays: product?.durationDays ?? "",
    sortOrder: product?.sortOrder ?? (index + 1) * 10
  };
}

function numberOrNull(value) {
  return value === "" || value === null || value === undefined ? null : Number(value);
}

export default function OperationalCatalogAdminPanel({ onFeedback }) {
  const { t } = useContentText();
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [monetizationSettings, setMonetizationSettings] = useState(defaultMonetizationSettings);
  const [moderationSettings, setModerationSettings] = useState(defaultModerationSettings);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [updatedAt, setUpdatedAt] = useState(null);

  const activeCategoriesCount = useMemo(
    () => categories.filter((category) => category.active).length,
    [categories]
  );

  async function loadCatalog() {
    setIsLoading(true);
    try {
      const payload = await fetchAdminCatalog();
      setMonetizationSettings({
        ...defaultMonetizationSettings,
        ...(payload?.monetizationSettings ?? {})
      });
      setModerationSettings({
        ...defaultModerationSettings,
        ...(payload?.moderationSettings ?? {})
      });
      setCategories((payload?.categories ?? []).map((category, index) => normalizeCategory({
        code: category.value,
        label: category.label,
        active: category.active,
        sortOrder: category.sortOrder
      }, index)));
      setProducts((payload?.products ?? []).map(normalizeProduct));
      setUpdatedAt(payload?.updatedAt ?? null);
    } catch (error) {
      onFeedback?.("error", t("catalogAdmin.feedback.loadError.title"), error.message);
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    loadCatalog();
  }, []);

  function updateCategory(index, field, value) {
    setCategories((current) => current.map((category, currentIndex) => (
      currentIndex === index ? { ...category, [field]: value } : category
    )));
  }

  function updateProduct(index, field, value) {
    setProducts((current) => current.map((product, currentIndex) => (
      currentIndex === index ? { ...product, [field]: value } : product
    )));
  }

  function removeCategory(index) {
    setCategories((current) => current.filter((_, currentIndex) => currentIndex !== index));
  }

  function removeProduct(index) {
    setProducts((current) => current.filter((_, currentIndex) => currentIndex !== index));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setIsSaving(true);
    try {
      const payload = await saveAdminCatalog({
        monetizationSettings,
        moderationSettings,
        categories: categories.map((category, index) => ({
          code: category.code,
          label: category.label,
          active: category.active,
          sortOrder: numberOrNull(category.sortOrder) ?? (index + 1) * 10
        })),
        products: products.map((product, index) => ({
          code: product.code,
          name: product.name,
          description: product.description,
          type: product.type,
          price: numberOrNull(product.price),
          originalPrice: product.promotional ? numberOrNull(product.originalPrice) : null,
          promotional: product.promotional,
          promotionLabel: product.promotionLabel,
          credits: numberOrNull(product.credits),
          durationDays: numberOrNull(product.durationDays),
          enabled: product.enabled,
          sortOrder: numberOrNull(product.sortOrder) ?? (index + 1) * 10
        }))
      });
      setMonetizationSettings({
        ...defaultMonetizationSettings,
        ...(payload?.monetizationSettings ?? {})
      });
      setModerationSettings({
        ...defaultModerationSettings,
        ...(payload?.moderationSettings ?? {})
      });
      setCategories((payload?.categories ?? []).map((category, index) => normalizeCategory({
        code: category.value,
        label: category.label,
        active: category.active,
        sortOrder: category.sortOrder
      }, index)));
      setProducts((payload?.products ?? []).map(normalizeProduct));
      setUpdatedAt(payload?.updatedAt ?? null);
      onFeedback?.("success", t("catalogAdmin.feedback.saveSuccess.title"), t("catalogAdmin.feedback.saveSuccess.message"));
    } catch (error) {
      onFeedback?.("error", t("catalogAdmin.feedback.saveError.title"), error.message);
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <article className="admin-card admin-card--catalog">
      <form className="catalog-admin" onSubmit={handleSubmit}>
        <div className="content-admin__header">
          <div>
            <span className="eyebrow">{t("catalogAdmin.eyebrow")}</span>
            <h3>{t("catalogAdmin.title")}</h3>
            <p>{t("catalogAdmin.subtitle")}</p>
          </div>
          <div className="inline-actions inline-actions--fixed">
            <button type="button" className="ghost-button ghost-button--small" onClick={loadCatalog} disabled={isLoading}>
              {isLoading ? t("common.actions.loading") : t("catalogAdmin.refresh")}
            </button>
            <button type="submit" className="primary-button primary-button--compact" disabled={isSaving}>
              {isSaving ? t("common.actions.saving") : t("catalogAdmin.save")}
            </button>
          </div>
        </div>

        <div className="content-admin__safety">
          {t("catalogAdmin.help.publicSafety")}
          {updatedAt ? <span> {t("catalogAdmin.updatedAt", { date: new Date(updatedAt).toLocaleString("pt-BR") })}</span> : null}
        </div>

        <div className="catalog-admin__grid">
          <section className="catalog-admin__section catalog-admin__section--full">
            <div className="catalog-admin__section-header">
              <div>
                <h4>{t("catalogAdmin.monetization.title")}</h4>
                <p>{t("catalogAdmin.monetization.description")}</p>
              </div>
            </div>

            <div className="catalog-admin__settings">
              <label className="checkbox-row checkbox-row--panel">
                <input
                  type="checkbox"
                  checked={monetizationSettings.creditPurchasesEnabled}
                  onChange={(event) => setMonetizationSettings((current) => ({
                    ...current,
                    creditPurchasesEnabled: event.target.checked
                  }))}
                />
                <span>{t("catalogAdmin.monetization.creditPurchasesEnabled")}</span>
              </label>
              <label className="checkbox-row checkbox-row--panel">
                <input
                  type="checkbox"
                  checked={monetizationSettings.boostPurchasesEnabled}
                  onChange={(event) => setMonetizationSettings((current) => ({
                    ...current,
                    boostPurchasesEnabled: event.target.checked
                  }))}
                />
                <span>{t("catalogAdmin.monetization.boostPurchasesEnabled")}</span>
              </label>
            </div>
          </section>

          <section className="catalog-admin__section catalog-admin__section--full">
            <div className="catalog-admin__section-header">
              <div>
                <h4>{t("catalogAdmin.moderation.title")}</h4>
                <p>{t("catalogAdmin.moderation.description")}</p>
              </div>
            </div>

            <div className="catalog-admin__settings">
              <label className="checkbox-row checkbox-row--panel">
                <input
                  type="checkbox"
                  checked={moderationSettings.userBlockListEnabled}
                  onChange={(event) => setModerationSettings((current) => ({
                    ...current,
                    userBlockListEnabled: event.target.checked
                  }))}
                />
                <span>{t("catalogAdmin.moderation.userBlockListEnabled")}</span>
              </label>
            </div>
          </section>

          <section className="catalog-admin__section">
            <div className="catalog-admin__section-header">
              <div>
                <h4>{t("catalogAdmin.categories.title")}</h4>
                <p>{t("catalogAdmin.categories.count", { count: activeCategoriesCount })}</p>
              </div>
              <button
                type="button"
                className="ghost-button ghost-button--small"
                onClick={() => setCategories((current) => [...current, { ...emptyCategory, sortOrder: (current.length + 1) * 10 }])}
              >
                {t("catalogAdmin.categories.add")}
              </button>
            </div>

            <div className="catalog-admin__rows">
              {categories.map((category, index) => (
                <article key={`${category.code}-${index}`} className="catalog-row catalog-row--category">
                  <input value={category.code} placeholder={t("catalogAdmin.categories.code")} onChange={(event) => updateCategory(index, "code", event.target.value)} required />
                  <input value={category.label} placeholder={t("catalogAdmin.categories.label")} onChange={(event) => updateCategory(index, "label", event.target.value)} required />
                  <input type="number" value={category.sortOrder} placeholder={t("catalogAdmin.sortOrder")} onChange={(event) => updateCategory(index, "sortOrder", event.target.value)} />
                  <label className="checkbox-row">
                    <input type="checkbox" checked={category.active} onChange={(event) => updateCategory(index, "active", event.target.checked)} />
                    <span>{t("common.status.active")}</span>
                  </label>
                  <button type="button" className="danger-button action-button--compact" onClick={() => removeCategory(index)}>
                    {t("common.actions.remove")}
                  </button>
                </article>
              ))}
            </div>
          </section>

          <section className="catalog-admin__section">
            <div className="catalog-admin__section-header">
              <div>
                <h4>{t("catalogAdmin.products.title")}</h4>
                <p>{t("catalogAdmin.products.description")}</p>
              </div>
              <button
                type="button"
                className="ghost-button ghost-button--small"
                onClick={() => setProducts((current) => [...current, { ...emptyProduct, sortOrder: (current.length + 1) * 10 }])}
              >
                {t("catalogAdmin.products.add")}
              </button>
            </div>

            <div className="catalog-admin__rows">
              {products.map((product, index) => (
                <article key={`${product.code}-${index}`} className="catalog-row catalog-row--product">
                  <input value={product.code} placeholder={t("catalogAdmin.products.code")} onChange={(event) => updateProduct(index, "code", event.target.value)} required />
                  <input value={product.name} placeholder={t("catalogAdmin.products.name")} onChange={(event) => updateProduct(index, "name", event.target.value)} required />
                  <select value={product.type} onChange={(event) => updateProduct(index, "type", event.target.value)}>
                    {PRODUCT_TYPES.map((type) => <option key={type} value={type}>{t(`catalogAdmin.products.type.${type}`)}</option>)}
                  </select>
                  <input type="number" min="0" step="0.01" value={product.price} placeholder={t("catalogAdmin.products.price")} onChange={(event) => updateProduct(index, "price", event.target.value)} required />
                  <input type="number" min="0" step="0.01" value={product.originalPrice} placeholder={t("catalogAdmin.products.originalPrice")} onChange={(event) => updateProduct(index, "originalPrice", event.target.value)} disabled={!product.promotional} />
                  <input type="number" min="0" value={product.credits} placeholder={t("catalogAdmin.products.credits")} onChange={(event) => updateProduct(index, "credits", event.target.value)} />
                  <input type="number" min="0" value={product.durationDays} placeholder={t("catalogAdmin.products.durationDays")} onChange={(event) => updateProduct(index, "durationDays", event.target.value)} />
                  <input type="number" value={product.sortOrder} placeholder={t("catalogAdmin.sortOrder")} onChange={(event) => updateProduct(index, "sortOrder", event.target.value)} />
                  <input className="catalog-row__wide" value={product.description ?? ""} placeholder={t("catalogAdmin.products.descriptionField")} onChange={(event) => updateProduct(index, "description", event.target.value)} />
                  <input className="catalog-row__wide" value={product.promotionLabel ?? ""} placeholder={t("catalogAdmin.products.promotionLabel")} onChange={(event) => updateProduct(index, "promotionLabel", event.target.value)} disabled={!product.promotional} />
                  <label className="checkbox-row">
                    <input type="checkbox" checked={product.promotional} onChange={(event) => updateProduct(index, "promotional", event.target.checked)} />
                    <span>{t("catalogAdmin.products.promotional")}</span>
                  </label>
                  <label className="checkbox-row">
                    <input type="checkbox" checked={product.enabled} onChange={(event) => updateProduct(index, "enabled", event.target.checked)} />
                    <span>{t("common.status.active")}</span>
                  </label>
                  <button type="button" className="danger-button action-button--compact" onClick={() => removeProduct(index)}>
                    {t("common.actions.remove")}
                  </button>
                </article>
              ))}
            </div>
          </section>
        </div>
      </form>
    </article>
  );
}
