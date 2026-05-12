export default function DashboardNavigation({ items }) {
  return (
    <section className="section-nav" aria-label="Navegacao da area logada">
      {items.map((item) => (
        <button
          key={item.key}
          type="button"
          className={item.active ? "active" : ""}
          onClick={item.onClick}
        >
          <span className="nav-icon" aria-hidden="true">{item.icon}</span>
          {item.label}
        </button>
      ))}
    </section>
  );
}
