import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async redirects() {
    return [
      {
        source: "/publicar",
        destination: "/cadastrar-interesse",
        permanent: false
      },
      {
        source: "/procura/:id",
        destination: "/interesses/:id",
        permanent: true
      },
      {
        source: "/dashboard",
        destination: "/meus-interesses",
        permanent: false
      },
      {
        source: "/dashboard/minhas-procuras",
        destination: "/meus-interesses",
        permanent: false
      },
      {
        source: "/dashboard/propostas",
        destination: "/ofertas-recebidas",
        permanent: false
      },
      {
        source: "/dashboard/itens",
        destination: "/meus-itens",
        permanent: false
      },
      {
        source: "/dashboard/creditos",
        destination: "/comprar-creditos",
        permanent: false
      },
      {
        source: "/dashboard/configuracoes",
        destination: "/admin",
        permanent: false
      }
    ];
  }
};

export default nextConfig;
