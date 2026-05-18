import type { MetadataRoute } from "next";
import { serverPublicRequest } from "@/shared/api/client";
import type { Category, OperationalSettings } from "@/shared/api/types";
import { fallbackLegalPages } from "@/features/legal/legal-content";
import { FALLBACK_CATEGORIES, activeCategories, slugifyCategory } from "@/shared/lib/format";
import { SITE_URL } from "@/shared/lib/seo";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const categories = activeCategories((await serverPublicRequest<Category[]>("/categories", 300)) ?? FALLBACK_CATEGORIES);
  const operationalSettings = await serverPublicRequest<OperationalSettings>("/operational/public", 300);
  const stickersEnabled = operationalSettings?.featureFlags?.stickersPageEnabled !== false;
  const now = new Date();
  return [
    { url: SITE_URL, lastModified: now, changeFrequency: "daily", priority: 1 },
    { url: `${SITE_URL}/categorias`, lastModified: now, changeFrequency: "daily", priority: 0.8 },
    ...(stickersEnabled ? [{ url: `${SITE_URL}/figurinhas`, lastModified: now, changeFrequency: "daily" as const, priority: 0.85 }] : []),
    { url: `${SITE_URL}/como-funciona`, lastModified: now, changeFrequency: "monthly", priority: 0.6 },
    { url: `${SITE_URL}/ouvidoria`, lastModified: now, changeFrequency: "monthly", priority: 0.5 },
    ...categories.map((category) => ({
      url: `${SITE_URL}/categorias/${slugifyCategory(category.value)}`,
      lastModified: now,
      changeFrequency: "daily" as const,
      priority: 0.7
    })),
    ...Object.keys(fallbackLegalPages).map((slug) => ({
      url: `${SITE_URL}/legal/${slug}`,
      lastModified: now,
      changeFrequency: "monthly" as const,
      priority: 0.4
    }))
  ];
}
