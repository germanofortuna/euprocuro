import { AppFooter } from "./app-footer";
import { AppHeader } from "./app-header";

export function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="app-shell">
      <AppHeader />
      {children}
      <AppFooter />
    </div>
  );
}
