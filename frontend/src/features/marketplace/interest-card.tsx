"use client";

import Link from "next/link";
import { ArrowRight, MapPin, Tag } from "lucide-react";
import type { Category, Interest } from "@/shared/api/types";
import { budgetLabel, categoryLabel, isBoostActive, locationLabel } from "@/shared/lib/format";

export function InterestCard({ interest, categories }: { interest: Interest; categories: Category[]; onSelect?: (interest: Interest) => void }) {
  return (
    <article className="interest-card">
      {isBoostActive(interest) ? <span className="boost-ribbon">Boost</span> : null}
      <div className="interest-card__body">
        <div className="interest-card__title">
          <h3>{interest.title}</h3>
          <div className="interest-meta">
            <span><Tag size={15} /> {categoryLabel(categories, interest.category)}</span>
            <span><MapPin size={15} /> {locationLabel(interest)}</span>
          </div>
        </div>
        <p>{interest.description}</p>
        <div className="tag-list">
          {(interest.tags ?? []).slice(0, 4).map((tag) => <span key={tag}>{tag}</span>)}
        </div>
        <strong className="budget-label">Orçamento: {budgetLabel(interest)}</strong>
      </div>
      <div className="interest-card__footer">
        <Link className="button button--outline" href={`/interesses/${interest.id}`}>
          Ver detalhes <ArrowRight size={16} />
        </Link>
      </div>
    </article>
  );
}
