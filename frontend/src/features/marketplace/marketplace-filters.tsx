"use client";

import { useState } from "react";
import { Filter } from "lucide-react";
import type { Category } from "@/shared/api/types";
import { Button } from "@/shared/ui/button";

export function MarketplaceFilters({
  categories,
  category,
  searchPlaceholder = "Ex: encanador, notebook",
  onApply
}: {
  categories: Category[];
  category?: string;
  searchPlaceholder?: string;
  onApply: (filters: Record<string, string>) => void;
}) {
  const [filters, setFilters] = useState({
    query: "",
    category: category ?? "",
    city: "",
    maxBudget: ""
  });

  return (
    <aside className="filter-panel">
      <h2><Filter size={19} /> Filtros</h2>
      <form
        className="stack-form"
        onSubmit={(event) => {
          event.preventDefault();
          onApply(filters);
        }}
      >
        <label>
          Categoria
          <select value={filters.category} onChange={(event) => setFilters((current) => ({ ...current, category: event.target.value }))}>
            <option value="">Todas as categorias</option>
            {categories.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
          </select>
        </label>
        <label>
          Cidade/UF
          <input value={filters.city} onChange={(event) => setFilters((current) => ({ ...current, city: event.target.value }))} placeholder="Ex: São Paulo, SP" />
        </label>
        <label>
          Termo de busca
          <input value={filters.query} onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value }))} placeholder={searchPlaceholder} />
        </label>
        <label>
          Orçamento máximo
          <input type="number" min="0" value={filters.maxBudget} onChange={(event) => setFilters((current) => ({ ...current, maxBudget: event.target.value }))} placeholder="R$ 0,00" />
        </label>
        <Button type="submit">Aplicar filtros</Button>
      </form>
    </aside>
  );
}
