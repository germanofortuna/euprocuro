"use client";

import Link from "next/link";
import { ArrowRight, MapPin, Tag, Zap } from "lucide-react";
import { usePlatform } from "@/features/platform/platform-context";
import { STICKERS_CATEGORY, stickerFlagImageForSelection, stickerSelectionLabel } from "@/features/stickers/stickers-data";
import type { Category, Interest } from "@/shared/api/types";
import { budgetLabel, categoryLabel, isBoostActive, locationLabel } from "@/shared/lib/format";
import { referenceImageSrc } from "@/shared/lib/images";

export function InterestCard({ interest, categories }: { interest: Interest; categories: Category[]; onSelect?: (interest: Interest) => void }) {
  const { currentUser, dashboard } = usePlatform();
  const imageSrc = referenceImageSrc(interest);
  const isOwnInterest = Boolean(
    currentUser?.id && (interest.ownerId === currentUser.id || dashboard?.myInterests?.some((item) => item.id === interest.id))
  );
  const isOwnBoostActive = isOwnInterest && isBoostActive(interest);
  const stickerSelection = interest.category === STICKERS_CATEGORY ? stickerSelectionLabel(interest.stickerDetails?.selection) : "";
  const stickerFlagSrc = interest.category === STICKERS_CATEGORY ? stickerFlagImageForSelection(interest.stickerDetails?.selection) : "";
  return (
    <article className={`interest-card${imageSrc ? " interest-card--with-image" : ""}`}>
      {isOwnInterest ? (
        <div className="interest-card__badges">
          {isOwnBoostActive ? <span className="boost-ribbon"><Zap size={13} /> Boost</span> : null}
          <span className="owner-ribbon">Seu interesse</span>
        </div>
      ) : null}
      {imageSrc ? (
        <Link className="interest-card__media" href={`/interesses/${interest.id}`} aria-label={`Ver detalhes de ${interest.title}`}>
          <img src={imageSrc} alt={`Imagem de referencia da procura ${interest.title}`} loading="lazy" />
        </Link>
      ) : null}
      <div className="interest-card__body">
        <div className="interest-card__title">
          <h3>
            {interest.title}
            {stickerSelection ? <span className="interest-card__sticker-selection">{stickerFlagSrc ? <img src={stickerFlagSrc} alt="" loading="lazy" /> : null}{stickerSelection}</span> : null}
          </h3>
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
