import type { MetadataRoute } from "next";
import { SITE_URL } from "@/shared/lib/seo";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/admin", "/meus-interesses", "/ofertas-enviadas", "/ofertas-recebidas", "/meus-itens", "/comprar-creditos", "/cadastrar-interesse"]
    },
    sitemap: `${SITE_URL}/sitemap.xml`
  };
}
