import { useContentText } from "../content/ContentContext";
import { useLegalContent } from "../content/useLegalContent";

export default function Footer() {
  const { t } = useContentText();
  const { pages } = useLegalContent();
  const legalNavigation = Object.entries(pages).map(([slug, page]) => ({
    slug,
    label: page.label
  }));

  return (
    <footer className="site-footer">
      <div className="site-footer__brand">
        <strong>{t("global.marketplace.name")}</strong>
        <span>{t("footer.copyright", { year: new Date().getFullYear() })}</span>
      </div>

      <nav className="site-footer__links" aria-label={t("footer.legal.aria")}>
        <a href="/ouvidoria">
          Fale Conosco
        </a>
        {legalNavigation.map((item) => (
          <a key={item.slug} href={`/legal/${item.slug}`}>
            {item.label}
          </a>
        ))}
      </nav>
    </footer>
  );
}
