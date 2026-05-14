import type { Interest } from "@/shared/api/types";

export const sampleInterests: Interest[] = [
  {
    id: "sample-1",
    title: "Procuro eletricista para instalação residencial",
    description: "Preciso instalar tomadas novas e revisar o quadro de energia em um apartamento. Preferência por profissional com disponibilidade nesta semana.",
    category: "SERVICOS",
    budgetMax: 450,
    location: { city: "São Paulo", state: "SP", country: "Brasil" },
    tags: ["Urgente", "Residencial"],
    boostedUntil: new Date(Date.now() + 1000 * 60 * 60 * 24 * 3).toISOString(),
    status: "OPEN",
    createdAt: new Date().toISOString()
  },
  {
    id: "sample-2",
    title: "Procuro notebook usado para estudar",
    description: "Busco notebook em bom estado para faculdade, com bateria funcional e SSD. Pode ser usado, desde que esteja revisado.",
    category: "ELETRONICOS",
    budgetMax: 1500,
    location: { city: "Curitiba", state: "PR", country: "Brasil" },
    tags: ["Estudo", "Usado"],
    status: "OPEN",
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 4).toISOString()
  },
  {
    id: "sample-3",
    title: "Procuro professor particular de violão",
    description: "Aulas online para iniciante, uma vez por semana, com foco em MPB e exercícios básicos.",
    category: "SERVICOS",
    budgetMax: 80,
    location: { remote: true, country: "Brasil" },
    tags: ["Música", "Online"],
    status: "OPEN",
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 8).toISOString()
  },
  {
    id: "sample-4",
    title: "Procuro apartamento para alugar em Campinas",
    description: "Apartamento de 2 quartos, preferencialmente com vaga. Tenho interesse em Cambuí, Taquaral ou Barão Geraldo.",
    category: "IMOVEIS",
    budgetMax: 2500,
    location: { city: "Campinas", state: "SP", country: "Brasil" },
    tags: ["Aluguel", "2 Quartos"],
    boostedUntil: new Date(Date.now() + 1000 * 60 * 60 * 24).toISOString(),
    status: "OPEN",
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 12).toISOString()
  }
];
