function currency(value) {
  if (value === null || value === undefined) {
    return "A combinar";
  }

  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
  }).format(Number(value));
}

export default function InterestCard({ interest, selected, onClick }) {
  return (
    <button
      type="button"
      className={`interest-card ${selected ? "selected" : ""}`}
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
      ) : null}

      <div className="interest-card__head">
        <span className="pill">{interest.category}</span>
        {interest.boostEnabled ? <span className="pill pill--boost">Destaque</span> : null}
      </div>

      <h3>{interest.title}</h3>
      <p>{interest.description}</p>

      <div className="interest-card__meta">
        <span>
          {interest.location?.city}/{interest.location?.state}
        </span>
        <strong>{currency(interest.budgetMax)}</strong>
      </div>

      <div className="interest-card__footer">
        <span>{interest.ownerName}</span>
        <span>{interest.tags?.slice(0, 2).join(" • ")}</span>
      </div>
    </button>
  );
}
