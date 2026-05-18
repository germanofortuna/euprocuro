import { BackButton } from "@/shared/ui/back-button";

export default function NotFound() {
  return (
    <main className="route-shell centered-route">
      <section className="auth-card">
        <span className="pill">404</span>
        <h1>Pagina nao encontrada</h1>
        <p>O endereco acessado nao existe ou foi movido.</p>
        <BackButton className="button button--primary button--md" />
      </section>
    </main>
  );
}
