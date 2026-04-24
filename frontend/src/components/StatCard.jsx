export default function StatCard({ label, value, accent, clickable, onClick }) {
  const className = `stat-card ${accent ? "accent" : ""} ${clickable ? "stat-card--button" : ""}`;

  if (clickable) {
    return (
      <button type="button" className={className} onClick={onClick}>
        <span>{label}</span>
        <strong>{value}</strong>
      </button>
    );
  }

  return (
    <article className={className}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}
