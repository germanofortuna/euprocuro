export default function InterestCard({ interest, selected, onClick }) {
  const isBoosted = Boolean(
    interest.boostEnabled
    && interest.boostedUntil
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
        <strong>
          {interest.title}
          {isBoosted ? (
            <span className="boost-rocket" aria-label="Interesse impulsionado" title="Interesse impulsionado">
              🚀
            </span>
          ) : null}
        </strong>
      </div>
    </button>
  );
}
