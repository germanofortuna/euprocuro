"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { ArrowRight, Search, Sparkles, TrendingUp } from "lucide-react";
import { useEffect } from "react";
import { usePlatform } from "@/features/platform/platform-context";
import { InterestCard } from "./interest-card";
import { MarketplaceFilters } from "./marketplace-filters";
import { budgetLabel, categoryLabel, categorySearchPlaceholder, locationLabel, slugifyCategory } from "@/shared/lib/format";
import { rememberInterestListHref } from "@/shared/lib/interest-list-navigation";
import { Button } from "@/shared/ui/button";
import { AuthIntentLink } from "@/shared/ui/auth-intent-link";
import { STICKERS_CATEGORY } from "@/features/stickers/stickers-data";

export function PublicHome() {
  const { categories, interests, selectedInterest, selectInterest, refreshPublicData, openAuthModal, isLoadingPublic, hasLoadedPublicData } = usePlatform();
  const searchParams = useSearchParams();
  const query = (searchParams.get("query") ?? "").trim().toLowerCase();
  const isResolvingResults = isLoadingPublic || !hasLoadedPublicData;
  const visibleInterests = query
    ? interests.filter((interest) => [interest.title, interest.description, ...(interest.tags ?? [])].join(" ").toLowerCase().includes(query))
    : interests;

  return (
    <>
      <section className="hero-section">
        <div className="hero-content">
          <span className="hero-pill">O Marketplace Reverso do Brasil</span>
          <h1>O que você procura hoje?</h1>
          <p>Diga o que você precisa. Receba propostas de quem tem o que você procura. Simples, rápido e direto ao ponto.</p>
          <div className="hero-actions">
            <AuthIntentLink className="button button--secondary button--lg" href="/cadastrar-interesse" mode="login">Publicar uma procura</AuthIntentLink>
            <Button variant="primary" size="lg" type="button" onClick={() => openAuthModal("register")}>Responder procuras</Button>
          </div>
        </div>
      </section>
      <section className="marketplace-section">
        <div className="marketplace-layout">
          <MarketplaceFilters categories={categories} onApply={refreshPublicData} />
          <div className="feed-column">
            <div className="section-heading">
              <h2><TrendingUp size={24} /> Procuras Recentes</h2>
              <Link href="/categorias">Ver todas</Link>
            </div>
            <div className="interest-list">
              {isResolvingResults ? <div className="section-loading" role="status">Carregando procuras...</div> : visibleInterests.map((interest) => <InterestCard key={interest.id} interest={interest} categories={categories} onSelect={(item) => selectInterest(item.id)} />)}
            </div>
          </div>
          <aside className="detail-preview">
            {selectedInterest ? (
              <>
                <span className="pill">Procura selecionada</span>
                <h2>{selectedInterest.title}</h2>
                <p>{selectedInterest.description}</p>
                <dl>
                  <div><dt>Categoria</dt><dd>{categoryLabel(categories, selectedInterest.category)}</dd></div>
                  <div><dt>Localidade</dt><dd>{locationLabel(selectedInterest)}</dd></div>
                  <div><dt>Orçamento</dt><dd>{budgetLabel(selectedInterest)}</dd></div>
                </dl>
                <Link className="button button--primary" href={`/interesses/${selectedInterest.id}`}>Ver detalhes e enviar proposta <ArrowRight size={16} /></Link>
              </>
            ) : (
              <div className="empty-state"><Search size={28} /><strong>Selecione uma procura</strong><p>Escolha uma procura publicada para ver detalhes.</p></div>
            )}
          </aside>
        </div>
      </section>
    </>
  );
}

export function CategoryLanding({ categorySlug }: { categorySlug?: string }) {
  const { categories, interests, refreshPublicData, selectInterest, isLoadingPublic, hasLoadedPublicData } = usePlatform();
  const searchParams = useSearchParams();
  useEffect(() => {
    rememberInterestListHref(`${window.location.pathname}${window.location.search}`);
  }, [categorySlug, searchParams]);
  const query = (searchParams.get("query") ?? "").trim().toLowerCase();
  const currentCategory = categorySlug
    ? categories.find((category) => slugifyCategory(category.value) === categorySlug || slugifyCategory(category.label) === categorySlug)
    : null;
  const baseInterests = currentCategory ? interests.filter((interest) => interest.category === currentCategory.value) : interests;
  const categoryInterests = query
    ? baseInterests.filter((interest) => [interest.title, interest.description, ...(interest.tags ?? [])].join(" ").toLowerCase().includes(query))
    : baseInterests;
  const title = currentCategory?.label ?? "Categorias";
  const isSearchPage = Boolean(query && !currentCategory);
  const isResolvingResults = isLoadingPublic || !hasLoadedPublicData;

  return (
    <>
      <section className={`category-hero${isSearchPage ? " category-hero--search" : ""}`}>
        <span className="pill">{isSearchPage ? "Busca" : "Categoria"}</span>
        <h1>{isSearchPage ? `Resultados para "${query}"` : title}</h1>
        {isSearchPage ? <p className="search-results-copy">Procuras encontradas em todas as categorias.</p> : null}
        <p>
          {currentCategory
            ? `Pessoas estão procurando ${currentCategory.label.toLowerCase()} agora mesmo. Encontre demandas reais e envie propostas relevantes.`
            : "Explore categorias ativas do Eu Procuro e encontre pessoas declarando exatamente o que precisam."}
        </p>
        <AuthIntentLink className="button button--primary" href="/cadastrar-interesse" mode="login">
          {currentCategory ? `Publicar procura em ${currentCategory.label}` : "Publicar uma procura"}
        </AuthIntentLink>
      </section>
      <section className="marketplace-section">
        <div className="marketplace-layout marketplace-layout--wide">
          <MarketplaceFilters categories={categories} category={currentCategory?.value} searchPlaceholder={categorySearchPlaceholder(currentCategory)} onApply={refreshPublicData} />
          <div className="feed-column">
            {!currentCategory && !isSearchPage ? (
              <div className="category-grid">
                {categories.map((category) => {
                  const isStickersCategory = category.value === STICKERS_CATEGORY;
                  return (
                    <Link
                      className={isStickersCategory ? "category-tile category-tile--stickers" : "category-tile"}
                      key={category.value}
                      href={isStickersCategory ? "/figurinhas" : `/categorias/${slugifyCategory(category.value)}`}
                    >
                      <strong>{isStickersCategory ? <><Sparkles size={17} /> {category.label}</> : category.label}</strong>
                      <span>{isStickersCategory ? "Troque faltantes e repetidas da Copa 2026" : "Ver procuras indexáveis"}</span>
                    </Link>
                  );
                })}
              </div>
            ) : null}
            <div className="section-heading">
              <h2>{isResolvingResults ? "Carregando procuras..." : query ? `${categoryInterests.length} resultados para "${query}"` : `${categoryInterests.length} procuras ativas`}</h2>
            </div>
            <div className="interest-list">
              {isResolvingResults ? <div className="section-loading" role="status">Carregando resultados...</div> : categoryInterests.map((interest) => <InterestCard key={interest.id} interest={interest} categories={categories} onSelect={(item) => selectInterest(item.id)} />)}
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
