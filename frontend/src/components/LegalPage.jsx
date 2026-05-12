import { useContentText } from "../content/ContentContext";
import { useLegalContent } from "../content/useLegalContent";

export default function LegalPage({ slug, onNavigate }) {
  const { t } = useContentText();
  const { pages } = useLegalContent();
  const page = pages[slug];
  const legalNavigation = Object.entries(pages).map(([pageSlug, item]) => ({
    slug: pageSlug,
    title: item.title
  }));

  if (!page) {
    return null;
  }

  return (
    <section className="legal-page">
      <div className="legal-hero">
        <a
          className="back-link"
          href="/"
          onClick={(event) => {
            if (!onNavigate) {
              return;
            }

            event.preventDefault();
            onNavigate("/");
          }}
        >
          {t("common.actions.backHome")}
        </a>
        <span className="eyebrow">{t("footer.legal.aria")}</span>
        <h2>{page.title}</h2>
        <p>{page.summary}</p>
        <div className="legal-meta">
          <span>{t("legal.effectiveSince", { date: page.effectiveAt })}</span>
          <span>{t("legal.updatedAt", { date: page.updatedAt })}</span>
        </div>
      </div>

      <div className="legal-layout">
        <aside className="legal-nav" aria-label={t("footer.legal.aria")}>
          {legalNavigation.map((item) => (
            <a
              key={item.slug}
              className={item.slug === slug ? "active" : ""}
              href={`/legal/${item.slug}`}
              onClick={(event) => {
                if (!onNavigate) {
                  return;
                }

                event.preventDefault();
                onNavigate(`/legal/${item.slug}`);
              }}
            >
              {item.title}
            </a>
          ))}
        </aside>

        <article className="legal-document">
          {page.sections.map((section) => (
            <section key={section.title} className="legal-section">
              <h3>{section.title}</h3>
              {section.paragraphs?.map((paragraph) => (
                <p key={paragraph}>{paragraph}</p>
              ))}
              {section.items?.length ? (
                <ul>
                  {section.items.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              ) : null}
            </section>
          ))}
        </article>
      </div>
    </section>
  );
}
