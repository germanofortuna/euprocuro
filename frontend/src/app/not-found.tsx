import Link from "next/link";

export default function NotFound() {
  return (
    <main className="route-shell centered-route">
      <section className="auth-card">
        <span className="pill">404</span>
        <h1>Pagina nao encontrada</h1>
        <p>O endereco acessado nao existe ou foi movido.</p>
        <Link className="button button--primary button--md" href="/">Voltar para a home</Link>
      </section>
    </main>
  );
}
