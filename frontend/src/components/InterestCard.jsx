import BoostRocket from "./BoostRocket";

export default function InterestCard({ interest, selected, onClick }) {
  const isBoosted = Boolean(
    interest.boostedUntil
    && new Date(interest.boostedUntil).getTime() > Date.now()
  );

  return (
    <button
      type="button"
      className={`interest-card interest-card--summary ${selected ? "selected" : ""}`}
      onClick={() => onClick(interest)}
    >
      {interest.referenceImageUrl ? (
        <img
          className="interest-card__image"
          src={interest.referenceImageUrl}
          alt={interest.title}
          loading="lazy"
          decoding="async"
        />
      ) : (
        <div className="interest-card__image interest-card__image--placeholder">
          {interest.title?.charAt(0) ?? "I"}
        </div>
      )}

      <div className="interest-card__summary-copy">
        <span>Procura publicada</span>
        <strong>
          {interest.title}
          {isBoosted ? <BoostRocket /> : null}
        </strong>
      </div>
    </button>
  );
}
