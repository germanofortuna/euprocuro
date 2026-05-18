import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { fallbackLegalPages, LEGAL_SLUGS } from "@/features/legal/legal-content";
import { CookiePreferencesButton } from "@/features/privacy/cookie-preferences-button";
import { canonical, truncateDescription } from "@/shared/lib/seo";
import { PublicLayout } from "@/shared/layout/public-layout";

export async function generateStaticParams() {
  return Object.keys(fallbackLegalPages).map((slug) => ({ slug }));
}

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const { slug } = await params;
  const page = fallbackLegalPages[slug];
  return {
    title: page?.title ?? "Documento legal",
    description: truncateDescription(page?.summary),
    alternates: { canonical: canonical(`/legal/${slug}`) },
    robots: page ? "index,follow" : "noindex,nofollow"
  };
}

export default async function LegalPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const page = fallbackLegalPages[slug];
  if (!page) {
    notFound();
  }
  return (
    <PublicLayout>
      <main className="route-shell content-route">
        <article className="legal-document">
          <span className="pill">Atualizado em {page.updatedAt}</span>
          <h1>{page.title}</h1>
          {page.summary ? <p className="legal-summary">{page.summary}</p> : null}
          {slug === LEGAL_SLUGS.privacy ? <CookiePreferencesButton /> : null}
          {(page.sections ?? []).map((section) => (
            <section key={section.title}>
              <h2>{section.title}</h2>
              {(section.paragraphs ?? []).map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
              {section.items?.length ? <ul>{section.items.map((item) => <li key={item}>{item}</li>)}</ul> : null}
            </section>
          ))}
        </article>
      </main>
    </PublicLayout>
  );
}
