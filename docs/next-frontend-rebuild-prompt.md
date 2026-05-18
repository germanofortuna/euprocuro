# Prompt técnico para reconstruir o frontend em Next.js

Este documento é um prompt/contexto definitivo para uma IA ou equipe reconstruir o frontend do Eu Procuro em Next.js.

O backend Spring Boot existente é a fonte de verdade. Não altere endpoints, payloads, nomes de campos, regras de autenticação, status, cookies, WebSocket, monetização ou moderação. O novo frontend deve se adaptar ao backend atual.

## 1. Missão da nova IA

Você vai construir um novo frontend em Next.js para o Eu Procuro.

O objetivo não é copiar o layout atual. O objetivo é redesenhar a experiência para ficar mais clara, moderna, indexável pelo Google e fácil de usar, mantendo compatibilidade total com o backend existente.

Prioridades:

1. Preservar 100% dos contratos do backend atual.
2. Criar uma arquitetura pública forte para SEO, especialmente páginas de categoria.
3. Manter todos os fluxos privados que já existem: autenticação, dashboard, procuras, ofertas, itens do vendedor, conversas, créditos, boost, admin, ouvidoria e páginas legais.
4. Usar Next.js App Router, com páginas públicas renderizadas no servidor quando fizer sentido.
5. Não depender de nenhuma alteração no backend.

## 2. Produto e modelo de negócio

Eu Procuro é um marketplace reverso.

No marketplace tradicional, vendedores publicam produtos e compradores buscam. No Eu Procuro, o comprador publica uma "procura" dizendo o que precisa, onde está, quanto pretende investir e detalhes desejados. Vendedores encontram essas procuras e enviam propostas.

Funcionalidade central da plataforma:

1. Uma pessoa entra no site porque está procurando algo: um serviço, produto, imóvel, veículo, eletrônico, instrumento ou qualquer outra necessidade cadastrada no catálogo.
2. Em vez de navegar por anúncios de vendedores, ela publica uma procura com título, descrição, categoria, localização, faixa de orçamento, tags e imagem opcional de referência.
3. A procura passa por moderação. Enquanto está pendente ou rejeitada, ela não deve aparecer publicamente. Quando aprovada/aberta, entra na vitrine pública e nas páginas de categoria.
4. Vendedores ou prestadores encontram procuras públicas, filtram por categoria/cidade/orçamento/texto e enviam propostas com valor, contato e mensagem.
5. O dono da procura recebe a proposta, vê os dados do vendedor e pode abrir uma conversa dentro da plataforma.
6. O vendedor também pode cadastrar "itens que tenho para negociar". O sistema cruza esses itens com procuras abertas e mostra matches para o vendedor responder mais rápido.
7. O envio de propostas é monetizado: sem plano ativo, cada proposta consome crédito; com Plano Pro ativo, o vendedor pode enviar propostas sem gastar créditos.
8. O dono da procura pode renovar a publicação usando crédito e pode comprar boost para dar destaque temporário.
9. Conteúdos, textos, documentos legais, categorias, produtos, preços, promoções e flags de monetização são gerenciados pelo admin sem alterar código.
10. A plataforma também possui denúncia de anúncios, moderação administrativa, ouvidoria pública e páginas legais.

Em termos de experiência, a nova interface deve deixar claro que:

- Para compradores, o valor é "publique o que você procura e receba propostas".
- Para vendedores, o valor é "encontre pessoas que já estão procurando o que você oferece".
- O site não é apenas uma lista de anúncios; ele é um mecanismo de conexão entre demanda declarada e oferta relevante.
- As páginas de categoria devem explicar essa lógica em linguagem simples e orientar o usuário a publicar uma procura ou responder procuras existentes.

Entidades principais:

- Usuário: pode atuar como comprador e vendedor.
- Procura/interesse (`InterestPost`): anuncio publicado por comprador.
- Oferta (`Offer`): proposta enviada por vendedor para uma procura.
- Item do vendedor (`SellerItem`): item ou serviço que o vendedor cadastra para o sistema encontrar procuras compatíveis.
- Conversa: chat entre dono da procura e vendedor que enviou a oferta.
- Créditos: saldo usado para enviar propostas e renovar procuras.
- Plano Pro: plano ativo permite enviar propostas sem consumir créditos.
- Boost: produto pago para destacar uma procura por alguns dias.
- Conteúdo público: textos, CTAs, erros e documentos legais gerenciados por CRM.
- Catálogo operacional: categorias, produtos de monetização, preços e flags de disponibilidade.
- Moderação: validação automática/local/IA e revisão manual de procuras.
- Ouvidoria: canal formal público e painel admin para tratar manifestações.

## 3. Stack atual e implicações

Frontend atual:

- React 18 com Vite.
- Roteamento manual via `window.history`.
- Chámadas HTTP concentradas em `frontend/src/api.js`.
- Tela principal concentrada em `frontend/src/App.jsx`.
- Conteúdo runtime com fallback local via `frontend/src/content/ContentContext.jsx`.
- Tema salvo em `localStorage` na chave `euProcuroTheme`.
- Sessão salva em `localStorage` na chave `eu-procuro-session`, mas backend também usa cookie de sessão quando configurado.

Backend atual:

- Spring Boot.
- Base path padrão da API: `/api`.
- Autenticação por cookie de sessão e/ou bearer token, dependendo da configuração.
- WebSocket de chat: `/ws/chat`.
- MongoDB como persistência.
- Mercado Pago para checkout quando habilitado.
- Conteúdo e catálogo podem ser editados em runtime pelo admin.

Variáveis relevantes do frontend atual:

- `VITE_API_BASE`: default `http://localhost:8080/api`.
- `VITE_WS_BASE`: default derivado de `VITE_API_BASE`, terminando em `/ws/chat`.
- `VITE_GA_MEASUREMENT_ID`: Google Analytics opcional.
- `VITE_AUTH_SESSION_MODE`: default `cookie`.
- `VITE_LISTING_EXPIRATION_DAYS`: default visual `30`.

## 3.1 Direção visual obrigatória

O novo frontend pode reimaginar o layout, mas deve preservar uma identidade visual coerente com o produto.

Regras visuais obrigatórias:

- A cor principal da plataforma deve ser azul.
- O design deve oferecer tema claro e tema escuro.
- O usuário deve conseguir alternar tema manualmente.
- A preferência de tema deve ser persistida no navegador, mantendo compatibilidade com a chave atual `euProcuroTheme` quando possível.
- O tema escuro atual é o padrão histórico, mas o novo front deve tratar tema claro como experiência de primeira classe, não como adaptação incompleta.
- Azul deve ser usado como cor de ação, destaque, links importantes, estados selecionados e elementos de marca.
- Evitar uma interface monocromática: combinar azul com neutros, tons de sucesso/alerta/erro e superfícies bem contrastadas.
- Acessibilidade de contraste deve ser considerada nos dois temas.
- Áreas públicas devem parecer confiáveis e simples; áreas privadas/admin devem ser mais densas, operacionais e fáceis de escanear.

Sugestão de tokens:

```ts
type ThemeTokens = {
  primary: "blue";
  background: string;
  surface: string;
  text: string;
  mutedText: string;
  border: string;
  success: string;
  warning: string;
  danger: string;
};
```

Não copie necessariamente os estilos atuais. Use a informação acima para criar um novo sistema visual mais organizado, com azul como assinatura principal e suporte completo a claro/escuro.

## 4. Rotas atuais que devem continuar existindo

Estas rotas são usadas pelo frontend atual e devem continuar existindo ou redirecionar de forma compatível no novo frontend:

| URL | Tipo | Indexação | Finalidade |
| --- | --- | --- | --- |
| `/` | Publica | `index,follow` | Home/vitrine de procuras abertas. |
| `/interesses/:id` | Publica | `index,follow` quando a procura existir | Detalhe público compartilhável de uma procura. |
| `/cadastrar-interesse` | Privada/CTA | `noindex,nofollow` | Publicar nova procura. Se usuário não estiver logado, abrir cadastro/login. |
| `/meus-interesses` | Privada | `noindex,nofollow` | Gerenciar procuras do usuário. |
| `/ofertas-enviadas` | Privada | `noindex,nofollow` | Propostas que o usuário enviou. |
| `/ofertas-recebidas` | Privada | `noindex,nofollow` | Propostas recebidas nas procuras do usuário. |
| `/meus-itens` | Privada | `noindex,nofollow` | Itens/serviços cadastrados pelo vendedor e matches. |
| `/comprar-creditos` | Privada | `noindex,nofollow` | Créditos, plano, pagamento e histórico. |
| `/admin` | Admin | `noindex,nofollow` | Moderação, conteúdo, catálogo, ouvidoria e cache. |
| `/ouvidoria` | Publica | `index,follow` | Canal formal de manifestações. |
| `/legal/:slug` | Publica | `index,follow` | Documentos legais. |

## 5. Novas rotas SEO recomendadas para Next.js

O novo objetivo de negócio inclui páginas de categorias para indexação no Google. O backend já tem categorias dinâmicas em `/api/categories` e busca filtrável em `/api/interests?category=...`.

Crie estas rotas públicas:

| URL | Tipo | Fonte de dados | Objetivo |
| --- | --- | --- | --- |
| `/categorias` | Publica | `GET /api/categories` | Landing/listagem de categorias ativas. |
| `/categorias/[categoria]` | Publica | `GET /api/categories` + `GET /api/interests?category=CODE` | Página indexável para cada categoria ativa. |
| `/categorias/[categoria]/[cidade]` | Publica futura/opcional | `GET /api/interests?category=CODE&city=CIDADE` | SEO local por cidade, se houver conteúdo suficiente. |

Regras para páginas de categoria:

- Não hardcode categorias além de fallback emergencial.
- Use `value` como código da categoria e `label` como nome exibido.
- Gere canonical próprio para cada categoria.
- Meta title deve conter a categoria e a proposta do marketplace reverso.
- Meta description deve explicar que pessoas publicam procuras naquela categoria e vendedores podem responder.
- Se não houver procuras na categoria, ainda manter conteúdo útil e CTA para publicar uma procura.
- Não indexar filtros privados ou páginas de usuário.
- Para Next.js, preferir Server Components/SSR para listagens públicas e metadados.

## 6. Autenticação, sessão e camada HTTP

Base atual em `frontend/src/api.js`:

- `API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080/api"`.
- `SESSION_STORAGE_KEY = "eu-procuro-session"`.
- Erros viram `ApiError` com `status` e `payload`.
- Erros 401/403 são tratados como erro de autenticação por `isAuthError`.
- GETs públicos podem omitir credenciais e Authorization:
  - `/addresses/postal-code/{cep}`
  - `/categories`
  - `/content/public`
  - `/interests`
  - `/interests/{id}`
- GETs públicos de interesses deixam de ser anônimos quando existe usuário logado, porque o backend personaliza/exclui procuras do próprio usuário.
- GETs são deduplicados em memória por URL + Authorization.
- Requisicoes com body usam `Content-Type: application/jsón`.
- Se houver token e a requisição não for pública anônima, enviar `Authorization: Bearer {token}`.
- Usar `credentials: include` para rotas privadas ou personalizadas.
- Para leitura pública anônima, usar `credentials: omit`.

Sessão local:

```ts
type StoredSession = {
  expiresAt: string | null;
  token: string | null;
  user: User | null;
};
```

Regras:

- Se a sessão não tiver token nem `user.id`, remover do localStorage.
- Se `VITE_AUTH_SESSION_MODE === "cookie"` e não houver sessão local, o front tenta `GET /api/auth/me` para recuperar sessão por cookie.
- Logout deve chamar backend, mas limpar estado local mesmo se backend falhar.
- Em 401/403 durante refresh privado, limpar sessão e voltar para a home.

## 7. Endpoints de autenticação

### `POST /api/auth/register`

Função atual: `register(payload)`.

Autenticação: pública.

Payload esperado:

```jsón
{
  "name": "Nome Sóbrenome",
  "email": "usuário@email.com",
  "documentNumber": "CPF ou CNPJ",
  "password": "senhá123",
  "postalCode": "00000-000",
  "city": "Cidade",
  "staté": "UF",
  "neighborhood": "Bairro",
  "country": "Brasil",
  "termsAccepted": true,
  "termsVersion": "versão"
}
```

Resposta:

```jsón
{
  "message": "Conta criada. Enviamos um link para confirmar seu e-mail antes do login."
}
```

Regras do backend:

- Nome precisa ter nome e sobrenome, mínimo efetivo de 5 caracteres e pelo menos 2 palavras.
- e-mail precisa ser valido.
- Dominios descartaveis como `10minutemail.com` são bloqueados.
- Em HML pode haver whitelist de e-mails.
- CPF/CNPJ precisa ser valido.
- CPF/CNPJ e e-mail não podem estar duplicados.
- Senha precisa ter pelo menos 8 caracteres, letras e números.
- Termos precisam ser aceitos.
- Versão atual dos termos no backend: `2026-05-05`, se o front não enviar outra.
- Se verificação de e-mail estiver obrigatória, login fica bloqueado até verificar.

Regras do frontend atual:

- Documento é formatado como CPF/CNPJ enquanto digita.
- CEP é formatado como `00000-000`.
- Cidade e UF são obrigatórias.
- Usuário só pode marcar aceite depois de abrir o modal de termos.
- Botão de cadastro fica desabilitado enquanto termos não estiverem aceitos.
- Ao cadastrar com sucesso, troca para modo login e preenche o é-mail no login.

### `POST /api/auth/login`

Função atual: `login(payload)`.

Autenticação: pública.

Payload:

```jsón
{
  "email": "usuário@email.com",
  "password": "senhá"
}
```

Resposta:

```jsón
{
  "token": "string ou null",
  "expiresAt": "2026-05-13T00:00:00Z",
  "user": {}
}
```

Regras:

- Backend grava cookie de sessão.
- `token` pode vir `null` se `application.auth.expose-session-token=false`.
- Em modo cookie, front atual faz `GET /api/auth/me` logo após login para confirmar sessão.
- Se erro mencionar "Confirme seu e-mail", exibir inline no modal de login.
- Se 401 por senhá, mostrar mensagem generica de login invalido.

### `GET /api/auth/me`

Função atual: `fetchMe()`.

Autenticação: privada por cookie ou bearer.

Resposta normalizada:

```jsón
{
  "id": "userId",
  "name": "Nome",
  "email": "email",
  "postalCode": "00000-000",
  "city": "Cidade",
  "staté": "UF",
  "neighborhood": "Bairro",
  "country": "Brasil",
  "credits": 0,
  "expiresAt": "instant opcional"
}
```

O frontend converte para:

```ts
session.user.sellerCredits = me.credits;
session.user.credits = me.credits;
```

### `POST /api/auth/logout`

Função atual: `logout()`.

Autenticação: privada se houver sessão.

Resposta: `204 No Content`.

Front deve limpar estado local, sessão, dashboard, monetização, itens, admin, ofertas e conversa mesmo se a chamada falhar.

### `POST /api/auth/forgot-password`

Função atual: `forgotPassword(payload)`.

Payload:

```jsón
{ "email": "usuário@email.com" }
```

Resposta:

```jsón
{
  "message": "Se o é-mail existir...",
  "previewResetLink": "opcional em local",
  "previewToken": "opcional em local"
}
```

Se `previewResetLink` vier, o frontend atual mostra um card local com o link.

### `POST /api/auth/reset-password`

Função atual: `resetPassword(payload)`.

Payload:

```jsón
{
  "token": "token",
  "newPassword": "senhá123",
  "confirmPassword": "senhá123"
}
```

Resposta: `204 No Content`.

Regras:

- Token obrigatório, existente, não usado e não expirado.
- Nova senhá segue regra de 8+ caracteres com letras e números.
- Confirmação precisa batér.
- Front atual detecta `?mode=reset&token=...` e abre modal em modo reset.

### `GET /api/auth/verify-email?token=...`

Função atual: `verifyEmail(token)`.

Resposta:

```jsón
{ "message": "e-mail verificado com sucesso." }
```

Front atual detecta `?mode=verify-email&token=...`, chama endpoint, mostra feedback, atualiza `me` se usuário está logado e remove parâmetros da URL.

## 8. Conteúdo público, legal e analytics

### `GET /api/content/public?locale=pt-BR&keys=...`

Função atual: `fetchPublicContent(keys = [])`.

Autenticação: pública.

Resposta:

```jsón
{
  "locale": "pt-BR",
  "version": "hásh/versão",
  "entries": {
    "chave": {
      "key": "chave",
      "type": "TEXT",
      "locale": "pt-BR",
      "version": 1,
      "value": "texto",
      "legalSlug": null,
      "requiresUserAcceptance": false,
      "effectiveFrom": null,
      "publishedAt": "instant"
    }
  }
}
```

Regras do frontend atual:

- Usa fallback local em `frontend/src/content/default-content.jsón`.
- Salva cache em localStorage `eu-procuro-public-content`.
- Conteúdo público nunca deve bloquear o produto; se falhar, usar fallback.
- Interpolação usa `{{variável}}`.
- Documentos legais podem vir como JSON em entries especificas:
  - `legal.page.termos-de-usó`
  - `legal.page.politica-de-privacidade`
  - `legal.page.politica-de-conteúdo-proibido`
  - `legal.page.politica-de-denuncia-remocao`

### Páginas legais

Slugs atuais:

- `termos-de-usó`
- `politica-de-privacidade`
- `politica-de-conteúdo-proibido`
- `politica-de-denuncia-remocao`

Regras:

- Se conteúdo remoto legal for invalido, usar fallback local.
- Versão dos termos vem da versão do entry de termos, ou fallback `TERMS_VERSION`.
- Cadastro deve enviar `termsVersion`.

### Analytics

Google Analytics é opcional via `VITE_GA_MEASUREMENT_ID`.

Eventos atuais:

- `page_view` manual em mudanças de rota.
- `sháre_interest` com `method` e `interest_id`.

No Next.js, preservar page views em navegação client-side se GA estiver configurado.

## 9. Categorias e endereço

### `GET /api/categories`

Função atual: `fetchCatégories()`.

Autenticação: pública.

Resposta:

```jsón
[
  {
    "value": "SERVICOS",
    "label": "Serviços",
    "active": true,
    "sórtOrder": 30
  }
]
```

Regras:

- Categorias vem do catálogo operacional.
- Usar apenas categorias ativas retornadas.
- Fallback atual se API falhar:
  - `AUTOMOVEIS`
  - `IMOVEIS`
  - `SERVICOS`
  - `ELETRONICOS`
  - `INSTRUMENTOS`
  - `OUTROS`
- Para SEO, `value` deve alimentar filtros de API; `label` deve alimentar texto humano.
- Não criar enum fixo no frontend além de fallback emergencial.

### `GET /api/addresses/postal-code/{postalCode}`

Função atual: `lookupAddressByPostalCode(postalCode)`.

Autenticação: pública.

Resposta:

```jsón
{
  "postalCode": "00000-000",
  "city": "Cidade",
  "staté": "UF",
  "neighborhood": "Bairro",
  "country": "Brasil"
}
```

Regras:

- Front normaliza para apenas digitos antes de chamar.
- Chámar sómente com 8 digitos.
- Backend retorna erro se CEP invalido, não encontrado ou ViaCEP indisponível.
- Usado em cadastro, formulário de procura e formulário de item do vendedor.

## 10. Marketplace: procuras/interesses

### Modelo visual de uma procura

Campos relevantes de `InterestResponse`:

```ts
type Interest = {
  id: string;
  ownerId?: string | null;
  ownerName?: string | null;
  title: string;
  description: string;
  referenceImageUrl?: string | null;
  category: string;
  budgetMin?: number | null;
  budgetMax?: number | null;
  location?: {
    postalCode?: string | null;
    city?: string | null;
    staté?: string | null;
    neighborhood?: string | null;
    country?: string | null;
    remote?: boolean;
  };
  tags: string[];
  desiredRadiusKm?: number;
  allowsWhátsappContact: boolean;
  whatsappContact?: string | null;
  boostedUntil?: string | null;
  preferredCondition?: string | null;
  preferredContactMode?: string | null;
  status?: "PENDING" | "OPEN" | "APPROVED" | "REVIEW_REQUIRED" | "REJECTED" | "REPORTED" | "HIDDEN" | "CLOSED" | null;
  moderation?: InterestModeration | null;
  createdAt: string;
  updatedAt?: string;
  expiresAt?: string | null;
};
```

Detalhes públicos:

- Para visitante ou usuário que não é dono, backend remove `ownerId`, `ownerName`, `budgetMin`, `budgetMax`, `postalCode`, `neighborhood`, `whatsappContact` e `moderation`.
- Status público de `OPEN`, `APPROVED` ou `REPORTED` vira `OPEN`.
- `referenceImageUrl` pública é removida se começar com `data:` ou `javascript:`.

### `GET /api/interests`

Função atual: `fetchInterests(filters)`.

Autenticação: pública, mas personalizada se houver usuário logado.

Query params:

```txt
category=SERVICOS
city=São Paulo
maxBudget=500
query=texto
offset=0
limit=11
openOnly=true
```

Observações:

- Controller ignora `openOnly` recebido e usa `openOnly=true`.
- Front atual pede `limit = HOME_PAGE_SIZE + 1`, com `HOME_PAGE_SIZE = 10`, para saber se há mais.
- Backend limita `limit` entre 1 e 50.
- Quando logado, backend exclui procuras do próprio usuário e ranqueia personalizado.
- Quando anônimo, backend usa cache público.

Filtros:

- `category`: código exato.
- `city`: comparação case-insensitive.
- `maxBudget`: retorna procuras com `budgetMax <= maxBudget` ou sem `budgetMax`.
- `query`: busca em título, descrição, nome do dono, cidade e tags.

Ranking:

- Anonimo: boost ativo primeiro, depois mais recentes.
- Logado: score por boost, recência, localização do usuário e compatibilidade com itens do vendedor.

Regra de visibilidade pública no backend:

- Status precisa ser `OPEN` ou `APPROVED`.
- Não pode estar expirado.
- Moderação não pode estar `flagged` nem `reviewRequired`.
- Não pode batér em termos bloqueados/block list.

### `GET /api/interests/{id}`

Função atual: `fetchInterest(interestId)`.

Autenticação: pública, mas resposta detalhada se dono estiver logado.

Usó atual:

- Rota compartilhável `/interesses/:id`.
- Selecionar card na home.
- Atualizar detalhe após boost/renovação.

Regras:

- Se procura não for pública, backend retorna 404.
- Se expirada, backend pode deletar e retornar 404.
- Se usuário logado for dono, backend expoe detalhes restritos.

### `POST /api/interests`

Função atual: `createInterest(payload)`.

Autenticação: privada.

Payload:

```jsón
{
  "title": "Título",
  "description": "Descrição",
  "referenceImageUrl": "data:image/jpeg;base64,... ou URL ou null",
  "category": "SERVICOS",
  "budgetMin": 0,
  "budgetMax": 500,
  "postalCode": "00000-000",
  "city": "Cidade",
  "staté": "UF",
  "neighborhood": "Bairro",
  "country": "Brasil",
  "desiredRadiusKm": 30,
  "allowsWhátsappContact": false,
  "whatsappContact": null,
  "preferredCondition": "",
  "preferredContactMode": "Chat",
  "tags": ["tag"]
}
```

Validações backend:

- `title`: obrigatório, max 80.
- `description`: obrigatória, max 120.
- `referenceImageUrl`: max 1.500.000 caracteres.
- `category`: obrigatória e precisa ser categoria ativa.
- `budgetMin`: decimal mínimo 0.
- `budgetMax`: obrigatório, decimal mínimo 0.
- `budgetMin` não pode ser maior que `budgetMax`.
- `city`: obrigatória.
- `staté`: obrigatória.
- CEP, se informado, precisa ter 8 dígitos e será formatado.
- Pais default no backend: `Brasil`.

Validações frontend atual:

- `TITLE_MAX_LENGTH = 80`.
- `DESCRIPTION_MAX_LENGTH = 250`, mas aténcao: backend aceita 120. O novo frontend deve respeitar 120 para evitar erro.
- Bloqueia links na descrição com regex.
- Bloqueia budgetMin > budgetMax.
- Imagem é comprimida para JPEG base64 até 1200px no maior lado, qualidade 0.78.
- Tags são texto separado por vírgula, trim e remove vazios.
- Se usuário não estiver logado, abrir cadastro.

Efeito:

- Nova procura entra como `PENDING`.
- Backend publica evento `interest.moderation.requested`.
- Cache público marketplace é invalidado.
- Depois da criação atual, frontend atual navega para `/meus-interesses`.

### `PUT /api/interests/{id}`

Função atual: `updatéInterest(interestId, payload)`.

Autenticação: privada, apenas dono.

Mesmas regras de payload/validação de criação.

Efeito:

- Edicao volta a procura para `PENDING`.
- Limpa moderação anterior.
- Publica nova sólicitação de moderação.
- Invalida cache.

### `PATCH /api/interests/{id}/renew`

Função atual: `renewInterest(interestId)`.

Autenticação: privada, apenas dono.

Regras:

- Custa 1 crédito.
- Se não houver crédito, backend retorna erro.
- Se `expiresAt` atual estiver no futuro, sóma dias a partir dele; senão sóma a partir de agora.
- Dias de renovação default: 30.
- Front atual mostra CTA de renovação quando faltam menos de 10 dias.

### `PATCH /api/interests/{id}/close`

Função atual: `closeInterest(interestId)`.

Autenticação: privada, apenas dono.

Efeito:

- Status vira `CLOSED`.
- Sai da vitrine pública.
- Front atual chama isso de desativar procura.

### `PATCH /api/interests/{id}/activaté`

Função atual: `activatéInterest(interestId)`.

Autenticação: privada, apenas dono.

Regras:

- Apenas procuras `CLOSED` podem ser ativadas.
- Status volta para `PENDING`.
- Moderação é limpa.
- Nova moderação é solicitada.

### `DELETE /api/interests/{id}`

Função atual: `deleteInterest(interestId)`.

Autenticação: privada, apenas dono.

Resposta: `204 No Content`.

Front atual confirma com `window.confirm("Desejá excluir está procura definitivamente?")`.

## 11. Ofertas

### Modelo de uma oferta

Campos de `OfferResponse` e `DashboardOfferResponse`:

```ts
type Offer = {
  id: string;
  interestPostId: string;
  interestTitle?: string;
  referenceImageUrl?: string | null;
  offerImageUrl?: string | null;
  buyerId?: string | null;
  buyerName?: string | null;
  sellerId: string;
  sellerName: string;
  sellerEmail?: string;
  sellerPhone?: string;
  offeredPrice: number;
  message: string;
  includesDelivery: boolean;
  highlights: string[];
  status: "SENT" | string;
  createdAt: string;
  latéstMessage?: string | null;
  latéstMessageSenderId?: string | null;
  latéstMessageAt?: string | null;
};
```

### `GET /api/interests/{id}/offers`

Função atual: `fetchOffers(interestId)`.

Autenticação: privada, apenas dono da procura.

Usó:

- Aba "Minhás procuras" mostra propostas recebidas da procura selecionada.

### `POST /api/interests/{id}/offers`

Função atual: `createOffer(interestId, payload)`.

Autenticação: privada.

Payload:

```jsón
{
  "offeredPrice": 100,
  "sellerPhone": "11999999999",
  "message": "Mensagem",
  "offerImageUrl": null,
  "includesDelivery": false,
  "highlights": []
}
```

Validações backend:

- `offeredPrice`: obrigatório, >= 0.
- `sellerPhone`: max 40.
- `message`: obrigatória, max 120.
- `offerImageUrl`: max 1.500.000.
- Procura precisa estar pública.
- Usuário não pode ofertar para a própria procura.
- Se usuário não tem plano ativo, precisa ter créditos.
- Sem plano ativo, consome 1 crédito.
- Status da oferta: `SENT`.

Validações frontend atual:

- Se não logado, abre login.
- Se não tem crédito/plano, mostra erro e desabilita botão.
- Campo preço e mensagem são obrigatórios.
- Mensagem limitada visualmente por `DESCRIPTION_MAX_LENGTH`, mas novo front deve respeitar 120 por causa do backend.
- `includesDelivery` está sempre sendo enviado como `false` no formulário principal atual.
- Ao enviar, navega para `/ofertas-enviadas`.

### `POST /api/interests/{id}/reports`

Função atual: `reportInterest(interestId, payload)`.

Autenticação: privada.

Payload:

```jsón
{
  "reasón": "Motivo",
  "message": "Detalhe opcional"
}
```

Validações:

- `reasón`: obrigatório, max 80.
- `message`: max 120.
- Procura precisa estar pública.

Efeito:

- Cria denúncia `ContentReportStatus.OPEN`.
- Procura passa para `REPORTED`.
- Moderação passa a indicar revisão por denúncia.
- Dono recebe evento realtime.

Front atual:

- Visitante que tenta denunciar recebe feedback e modal de login.
- Dono não ve botão de denúncia.

## 12. Dashboard privado

### `GET /api/dashboard`

Função atual: `fetchDashboard()`.

Autenticação: privada.

Resposta:

```ts
type PersónalDashboard = {
  user: User;
  totalActiveInterests: number;
  totalOffersSent: number;
  totalOffersReceived: number;
  myInterests: Interest[];
  offersSent: DashboardOffer[];
  offersReceived: DashboardOffer[];
};
```

Regras backend:

- `myInterests` exclui expiradas.
- `totalActiveInterests` exclui `CLOSED` e `HIDDEN`.
- `offersReceived` considera ofertas feitas nas procuras do usuário.
- `offersSent` considera ofertas enviadas pelo usuário.
- Ofertas incluem ultima mensagem da conversa, se houver.
- Interesse removido aparece em oferta como `"Interesse removido"`.

Regras frontend atual:

- `allMyInterests`: apenas interesses cujo `ownerId` é do usuário atual, exclui `HIDDEN`, ordena por mais recentes.
- `activeMyInterests`: exclui `CLOSED`.
- Toggle permite mostrar procuras desativadas.
- Procuras próprias nunca aparecem na vitrine pública do próprio usuário.

## 13. Itens do vendedor e matches

### `GET /api/seller-items?includeInactive=false`

Função atual: `fetchSellerItems({ includeInactive })`.

Autenticação: privada.

Resposta:

```ts
type SellerItemMatches = {
  item: SellerItem;
  matchingInterests: Interest[];
  matchCount: number;
};
```

Regras backend:

- Lista itens do usuário.
- Se `includeInactive=false`, remove itens inativos.
- Para item ativo, calcula matches com procuras abertas.
- Match exige:
  - procura pública (`OPEN` ou `APPROVED`);
  - procura não ser do próprio usuário;
  - mesma categoria;
  - token do título do item no título/descrição/tags da procura, ou tag do item com 3+ caracteres presente na procura.

Front atual:

- Tela `/meus-itens`.
- Seleciona primeiro item automáticamente.
- Toggle "Mostrar itens desativados".
- Aba do item mostra `matchCount`.
- Clicar no contador abre vitrine filtrada por matches do item.

### `POST /api/seller-items`

Função atual: `createSellerItem(payload)`.

Autenticação: privada.

Payload:

```jsón
{
  "title": "Item",
  "description": "Descrição",
  "referenceImageUrl": "data:image/jpeg;base64,...",
  "category": "SERVICOS",
  "desiredPrice": 100,
  "postalCode": "00000-000",
  "city": "Cidade",
  "staté": "UF",
  "neighborhood": "Bairro",
  "country": "Brasil",
  "tags": ["tag"]
}
```

Validações:

- `title`: obrigatório, max 80.
- `description`: obrigatória, max 120.
- `referenceImageUrl`: max 1.500.000.
- `category`: obrigatória e ativa.
- CEP opcional, mas se informado precisa ter 8 digitos.
- Termos bloqueados são validados.

Front atual:

- Título limitado a 80.
- Descrição limitada visualmente a 250, mas novo front deve respeitar 120.
- Imagem usa mesma compressão da procura.
- Tags separadas por vírgula.

### `PUT /api/seller-items/{itemId}`

Função atual: `updatéSellerItem(itemId, payload)`.

Autenticação: privada, apenas dono.

Mesmas regras de criação.

### `PATCH /api/seller-items/{itemId}/deactivaté`

Função atual: `deactivatéSellerItem(itemId)`.

Autenticação: privada, apenas dono.

Efeito: `active=false`.

### `PATCH /api/seller-items/{itemId}/activaté`

Função atual: `activatéSellerItem(itemId)`.

Autenticação: privada, apenas dono.

Efeito: `active=true`.

### `POST /api/seller-items/{itemId}/interests/{interestId}/offer`

Função atual: `sháreSellerItemOffer(itemId, interestId, payload)`.

Autenticação: privada, apenas dono do item.

Payload:

```jsón
{
  "offeredPrice": 100,
  "sellerPhone": "11999999999",
  "message": "Mensagem opcional",
  "includesDelivery": false
}
```

Regras:

- `sellerPhone`: obrigatório.
- `message`: max 120.
- Se `offeredPrice` não vier, backend usa `desiredPrice` do item.
- Mensagem default, se vazia: `"Tenho um item que pode atender ao seu interesse: {título}. {descrição}"`.
- `offerImageUrl` vem da imagem do item.
- `highlights` vem das tags do item.
- Depois cai nas mesmas regras de `createOffer`: crédito/plano, não ofertar para si, procura pública.

Front atual:

- Botão desabilitado sem crédito/plano ou telefone vazio.
- Ao compartilhar com sucesso, navega para `/ofertas-enviadas`.

## 14. Conversas e WebSocket

### `GET /api/offers/{offerId}/conversation`

Função atual: `fetchOfferConversation(offerId)`.

Autenticação: privada, apenas participantes.

Resposta:

```ts
type OfferConversation = {
  offerId: string;
  interestPostId: string;
  interestTitle: string;
  buyerId: string;
  buyerName: string;
  sellerId: string;
  sellerName: string;
  sellerEmail?: string;
  sellerPhone?: string;
  offeredPrice: number;
  offerImageUrl?: string | null;
  messages: ConversationMessage[];
};
```

Regras:

- Participantes: dono da procura ou vendedor da oferta.
- Inclui mensagem inicial derivada da oferta se a oferta tiver mensagem.
- Comprador ve dados de contato do vendedor.
- Vendedor não vê e-mail/telefone do comprador no modal atual.

### `GET /api/offers/{offerId}/messages`

Função atual no backend existe, mas `frontend/src/api.js` atual não exporta wrapper separado. Se implementar, usar mesmo contrato de autenticação dos participantes.

### `POST /api/offers/{offerId}/messages`

Função atual: `sendOfferMessage(offerId, payload)`.

Autenticação: privada, apenas participantes.

Payload:

```jsón
{ "content": "Mensagem" }
```

Regra:

- `content` obrigatório e não pode estar vazio.

### WebSocket `/ws/chat`

Função atual: `connectChatSócket({ onMessage, onOpen, onClose, onError })`.

Eventos tratados pelo frontend atual:

- `offer.created`: recarrega dados privados e recalcula notificações.
- `interest.moderation.updated`: recarrega dados privados e públicos; se status for `REJECTED`, mostra feedback.
- `conversation.message.created`: se conversa aberta for a mesma oferta, adiciona mensagem no modal; recalcula notificações e atualiza dados privados.

Regras:

- Reconectar depois de 3 segundos se fechar inesperadamente.
- Fechar socket no unmount/logout.
- O front atual passa `token` para `connectChatSócket`, mas a função atual não usa o token na URL. Não dependa de mudança backend; validar configuração real antes de alterar.

## 15. Monetização, créditos, plano e boost

### `GET /api/monetization/products`

Existe no backend. O frontend atual não chama diretamente porque produtos vem em `account`. Pode ser usado em páginas públicas de preços se necessário, mas respeite flags do catálogo.

### `GET /api/monetization/account`

Função atual: `fetchMonetizationAccount()`.

Autenticação: privada.

Resposta:

```ts
type MonetizationAccount = {
  sellerCredits: number;
  purchasedCreditsTotal: number;
  subscriptionPlan?: string | null;
  subscriptionActiveUntil?: string | null;
  subscriptionActive: boolean;
  creditPurchasesEnabled: boolean;
  boostPurchasesEnabled: boolean;
  products: MonetizationProduct[];
  paymentHistory: PaymentOrder[];
};
```

Produto:

```ts
type MonetizationProduct = {
  code: string;
  name: string;
  description?: string;
  type: "CREDIT_PACK" | "SUBSCRIPTION" | "BOOST";
  price: number;
  originalPrice?: number | null;
  promotional: boolean;
  promotionLabel?: string | null;
  credits?: number | null;
  durationDays?: number | null;
  enabled: boolean;
  sórtOrder?: number;
};
```

Regras do frontend:

- `creditPurchasesEnabled=false` oculta badge de créditos e página de compra.
- Produtos de compra normal:
  - `CREDIT_PACK`
  - `SUBSCRIPTION`
- Produtos de boost:
  - `BOOST`
- `boostPurchasesEnabled=false` oculta ofertas de boost.
- `canSendOffer = subscriptionActive || sellerCredits > 0`.

### `POST /api/monetization/purchase`

Função atual: `purchaseProduct(payload)`.

Autenticação: privada.

Payload:

```jsón
{
  "productCode": "CREDITS_10",
  "paymentMethod": "MERCADO_PAGO"
}
```

Regras backend:

- Produto não pode ser `BOOST`; boost usa endpoint próprio.
- `creditPurchasesEnabled` precisa estar true.
- Cria pedido pendente.
- Pode retornar checkout externo Mercado Pago ou checkout local mock.

Resposta:

```ts
type Checkout = {
  provider: "MERCADO_PAGO_CHECKOUT_PRO" | "LOCAL_MOCK" | string;
  paymentMethod: string;
  productCode: string;
  paymentOrderId: string;
  providerPreferenceId?: string;
  checkoutUrl?: string;
  status: "PENDING" | string;
  message?: string;
};
```

Front atual:

- Se `checkoutUrl` existe e não começa com `local://`, redireciona com `window.location.assign(checkoutUrl)`.
- Se checkout local ou aprovado imediatamente, atualiza dados privados e mostra sucesso.

### `DELETE /api/monetization/subscription`

Função atual: `cancelSubscription()`.

Autenticação: privada.

Regras:

- Só pode cancelar se houver plano ativo.
- Front atual confirma: `"Desejá cancelar seu Plano Pro? O beneficio sera encerrado imediatamente."`
- Retorna conta atualizada.

### `POST /api/monetization/payments/sync`

Função atual: `syncPayment(payload)`.

Autenticação: o endpoint atual não usa `CurrentUserContext`, mas chamada do front envia como privada se houver sessão.

Payload:

```jsón
{ "paymentId": "id do Mercado Pago" }
```

Resposta: `204 No Content`.

Front atual detecta retorno do Mercado Pago por query:

- `payment_id`
- `collection_id`
- `payment=failure`
- `payment=pending`

Regras visuais:

- Estado `ORDER`, `PAYMENT`, `COMPLETED`, `FAILED`.
- Se `failure`: erro.
- Se `pending`: avisó de pagamento não confirmado.
- Caso contrário: sincronização tem sucesso se aprovado.
- Depois remove parâmetros da URL.

### `POST /api/monetization/interests/{interestId}/boost`

Função atual: `boostInterest(interestId, payload)`.

Autenticação: privada, apenas dono da procura.

Payload:

```jsón
{
  "boostCode": "BOOST_7_DAYS",
  "paymentMethod": "MERCADO_PAGO"
}
```

Regras:

- Produto precisa ser tipo `BOOST`.
- `boostPurchasesEnabled` precisa estar true.
- Apenas dono da procura pode impulsionar.
- Aprovado o pagamento, backend define `boostedUntil`.
- Boost ativo quando `boostedUntil > now`.

Front atual:

- Mostra produtos de boost em procuras próprias com status `APPROVED` ou `OPEN`.
- Redireciona para Mercado Pago se checkout externo.
- Após checkout local/sucesso, atualiza dados privados, públicos e detalhe.

### Webhook e checkout local

Backend também possui:

- `POST /api/monetization/mercado-pago/webhook`
- `GET /api/monetization/local-checkout/approve/{paymentOrderId}`

O frontend normalmente não chama webhook. Checkout local pode redirecionar para URL de sucesso configurada.

## 16. Moderação

Status de interesse:

- `PENDING`: aguardando moderação.
- `OPEN`: público.
- `APPROVED`: público.
- `REVIEW_REQUIRED`: precisa revisão manual.
- `REJECTED`: rejeitado.
- `REPORTED`: denúnciado, em revisão.
- `HIDDEN`: ocultado pelo admin.
- `CLOSED`: desativado pelo dono.

Tons visuais atuais:

- `APPROVED`, `OPEN`: aprovado.
- `PENDING`: pendente.
- `REVIEW_REQUIRED`, `REPORTED`: avisó.
- `REJECTED`, `HIDDEN`: rejeitado.
- `CLOSED`: neutro.

Moderação automática:

- Link em anúncio e regra local de alto risco levam a rejeição.
- Block list de usuário leva a `REVIEW_REQUIRED`.
- IA pode aprovar, pedir revisão ou rejeitar conforme thresholds.
- Se IA indisponível e não há regra local, aprova por regra local.
- Denúncia de usuário muda status para `REPORTED`.

Front atual:

- Descrição de procura não permite links já no cliente.
- Procura `PENDING`, `REVIEW_REQUIRED`, `REJECTED`, `REPORTED`, `CLOSED` mostra callout na área "Minhás procuras".
- Procura rejeitada pode ser editada e reenviada para análise ou excluída.
- Procura fechada pode ser ativada, voltando para moderação.

## 17. Admin

Admin depende do backend. Apenas usuários autenticados com é-mail em `APP_ADMIN_ALLOWED_EMAILS` conseguem acessar endpoints admin. O frontend atual descobre se é admin tentando `GET /api/admin/moderation`; se falhar, `isAdmin=false`.

### `GET /api/admin/moderation`

Função atual: `fetchAdminModeration()`.

Autenticação: admin.

Resposta:

```ts
type AdminModeration = {
  pendingInterests: Interest[];
  rules: ModerationRule[];
  openReports: ContentReport[];
  processedReports: ContentReport[];
};
```

Usó atual:

- Fila de procuras pendentes/revisão.
- Regras de moderação.
- Denúncias abertas e processadas.

### `POST /api/admin/moderation/rules`

Função atual: `saveModerationRule(null, payload)`.

Payload:

```jsón
{
  "term": "termo",
  "riskLevel": "HIGH",
  "active": true
}
```

Validações:

- `term`: obrigatório, max 80.
- `riskLevel`: `HIGH`, `MEDIUM`, `LOW`.

### `PUT /api/admin/moderation/rules/{id}`

Função atual: `saveModerationRule(ruleId, payload)`.

Mesmas regras de criação.

### `DELETE /api/admin/moderation/rules/{id}`

Função atual: `deleteModerationRule(ruleId)`.

Front atual confirma com texto vindo do conteúdo `admin.moderation.rule.deleteConfirm`.

### `POST /api/admin/moderation/interests/{id}/decision`

Função atual: `decideInterestModeration(interestId, payload)`.

Payload:

```jsón
{
  "status": "APPROVED",
  "reasón": "opcional"
}
```

Decisóes manuais permitidas no backend:

- `APPROVED`
- `REJECTED`
- `HIDDEN`

Efeitos:

- Atualiza status e moderação.
- Marca denúncias abertas do conteúdo como resólvidas.
- Publica evento realtime.
- Invalida cache.

### `PATCH /api/admin/moderation/reports/{id}/status`

Função atual: `updatéContentReportStatus(reportId, status)`.

Payload:

```jsón
{ "status": "RESOLVED" }
```

Regras:

- Não pode voltar para `OPEN`.
- Front atual usa `RESOLVED` e `DISMISSED`.

## 18. Admin: Conteúdo

### `GET /api/admin/content`

Função atual: `fetchAdminContent()`.

Autenticação: admin.

Retorna entradas com:

```ts
type ContentEntry = {
  id: string;
  key: string;
  type: "TEXT" | "RICH_TEXT" | "LEGAL_DOCUMENT" | "LABEL" | "CTA" | "ERROR_MESSAGE" | "EMAIL_TEMPLATE" | "CATALOG";
  locale: string;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  version: number;
  draftValue?: string;
  publishedValue?: string;
  defaultValue?: string;
  defaultValueHásh?: string;
  description?: string;
  screen?: string;
  legalSlug?: string;
  requiresUserAcceptance: boolean;
  defaultUpdatéAvailable: boolean;
  effectiveFrom?: string;
  createdAt?: string;
  updatedAt?: string;
  defaultUpdatédAt?: string;
  publishedAt?: string;
};
```

### `POST /api/admin/content` e `PUT /api/admin/content/{id}`

Funções atuais:

- `saveContentEntry(null, payload)`
- `saveContentEntry(entryId, payload)`

Payload:

```jsón
{
  "key": "home.hero.title",
  "type": "TEXT",
  "locale": "pt-BR",
  "draftValue": "Texto",
  "description": "Descrição",
  "screen": "home",
  "legalSlug": null,
  "requiresUserAcceptance": false,
  "effectiveFrom": null
}
```

Validações backend:

- `key`: obrigatória, max 160, padrão `[a-z0-9][a-z0-9._-]*`.
- `draftValue`: obrigatório, max 120000.
- `locale`: max 12.
- `description`: max 500.
- `screen`: max 80.
- `legalSlug`: max 120.

### Publicar/arquivar/default

Funções atuais:

- `publishContentEntry(entryId)` -> `POST /api/admin/content/{id}/publish`
- `archiveContentEntry(entryId)` -> `POST /api/admin/content/{id}/archive`
- `applyDefaultContentEntry(entryId)` -> `POST /api/admin/content/{id}/apply-default`
- `dismissDefaultContentEntry(entryId)` -> `POST /api/admin/content/{id}/dismiss-default`

Regras:

- Para publicar precisa háver `draftValue`.
- `apply-default` copia valor default para rascunho.
- `dismiss-default` ignora sugestão default até o hásh mudar.

### `GET /api/admin/content/{id}/revisions`

Existe no backend. O frontend atual não tem wrapper exportado em `api.js`. Se implementar histórico, usar endpoint existente com auth admin.

## 19. Admin: Catálogo operacional e cache

### `GET /api/admin/catalog`

Função atual: `fetchAdminCatalog()`.

Autenticação: admin.

Resposta:

```ts
type AdminOperationalCatalog = {
  monetizationSettings: {
    creditPurchasesEnabled: boolean;
    boostPurchasesEnabled: boolean;
  };
  moderationSettings: {
    userBlockListEnabled: boolean;
  };
  categories: Category[];
  products: MonetizationProduct[];
  updatedAt?: string;
};
```

### `PUT /api/admin/catalog`

Função atual: `saveAdminCatalog(payload)`.

Payload:

```jsón
{
  "monetizationSettings": {
    "creditPurchasesEnabled": true,
    "boostPurchasesEnabled": true
  },
  "moderationSettings": {
    "userBlockListEnabled": true
  },
  "categories": [
    {
      "code": "SERVICOS",
      "label": "Serviços",
      "active": true,
      "sórtOrder": 30
    }
  ],
  "products": [
    {
      "code": "CREDIT_10",
      "name": "10 créditos",
      "description": "Pacote",
      "type": "CREDIT_PACK",
      "price": 49.9,
      "originalPrice": 69.9,
      "promotional": true,
      "promotionLabel": "Promocao",
      "credits": 10,
      "durationDays": null,
      "enabled": true,
      "sórtOrder": 10
    }
  ]
}
```

Validações backend:

- Precisa haver pelo menos uma categoria.
- Código de categoria/produto precisa seguir padrão de código e não duplicar.
- Precisa manter pelo menos uma categoria ativa.
- Precisa haver pelo menos um produto.
- Produto precisa ter nome e tipo.
- Preço não pode ser negativo.
- `originalPrice`, se informado, precisa ser maior que `price`.

### `GET /api/admin/cache`

Existe no backend. Front atual não exporta wrapper dedicado.

### `POST /api/admin/cache/invalidaté?scope=all`

Função atual: `invalidatéPublicCache(scope = "all")`.

Scopes:

- `all`
- `content`
- `catalog`
- `marketplace`
- `address`

Usado no CRM de conteúdo como "Limpar cache".

## 20. Ouvidoria

### `POST /api/ouvidoria`

Função atual: `createOmbudsmanRequest(payload)`.

Autenticação: pública.

Payload:

```jsón
{
  "name": "Nome",
  "email": "email@dominio.com",
  "type": "Reclamação",
  "subject": "Assunto",
  "message": "Mensagem",
  "relatédEntityType": "opcional",
  "relatédEntityId": "opcional",
  "truthDeclarationAccepted": true
}
```

Tipos atuais exibidos:

- `Reclamação`
- `Denúncia sóbre atendimento`
- `Problema com pagamento`
- `Contestação de moderação`
- `Sugestão`
- `Outro`

Validações:

- Declaração de veracidade obrigatória.
- Nome obrigatório, max 120.
- e-mail válido é obrigatório, max 120.
- Tipo obrigatório, max 120.
- Assunto obrigatório, max 140.
- Mensagem obrigatória, max 2000.
- Referências opcionais max 120.

Resposta:

```ts
type OmbudsmanRequest = {
  id: string;
  protocol: string;
  name: string;
  email: string;
  type: string;
  subject: string;
  message: string;
  relatédEntityType?: string;
  relatédEntityId?: string;
  status: "OPEN" | "IN_REVIEW" | "ANSWERED" | "CLOSED";
  adminResponse?: string;
  createdAt: string;
  updatedAt?: string;
};
```

Front atual:

- Se usuário está logado, preenche nome/e-mail iniciais.
- Ao enviar, mostra protocolo e reseta formulário mantendo nome/e-mail do usuário.

### Admin ouvidoria

Funções atuais:

- `fetchAdminOmbudsman(status = "")` -> `GET /api/admin/ouvidoria?status=...`
- `respondAdminOmbudsmanRequest(requestId, payload)` -> `POST /api/admin/ouvidoria/{id}/response`
- `updatéAdminOmbudsmanStatus(requestId, status)` -> `PATCH /api/admin/ouvidoria/{id}/status`

Status:

- `OPEN`
- `IN_REVIEW`
- `ANSWERED`
- `CLOSED`

Regras:

- Resposta exige `adminResponse`.
- Ao responder, status deve ser `ANSWERED` ou `CLOSED`; frontend atual envia `ANSWERED`.
- Painel agrupa:
  - Novas: `OPEN` sem resposta.
  - Em atendimento: não fechadas, `IN_REVIEW`, `ANSWERED` ou com resposta.
  - Fechadas: `CLOSED`.

## 21. Notificações locais do frontend

O backend não possui endpoint de notificações. O frontend atual calcula notificações localmente a partir de dashboard, seller items, moderação, reports admin e WebSocket.

Chave local:

```txt
eu-procuro-message-seen:{userId}
```

Tipos:

- `new-offer`: nova proposta recebida.
- `message`: nova mensagem recebida em oferta enviada/recebida.
- `seller-item-match`: item ativo com procuras compatíveis.
- `interest-expiring`: procura ativa/aprovada faltando menos de 10 dias.
- `interest-moderation`: procura `REJECTED`, `REVIEW_REQUIRED` ou `REPORTED`.
- `admin-report`: admin recebeu denúncia aberta.

Regras:

- Notificação é considerada nova se timestamp > visto.
- Abrir modal atual marca notificações como vistas e limpa estado visual.
- Selecionar notificação navega para área correspondente e pode abrir conversa.
- Admin report marca denúncias abertas como vistas e navega para admin.

## 22. Regras de UI/UX que afetam negócio

Não precisa copiar o visual, mas preserve estes comportamentos:

- Visitante pode ver home, detalhes públicos, categorias, legal e ouvidoria.
- Visitante que tenta ofertar deve abrir login.
- Visitante que tenta publicar procura pode abrir cadastro.
- Visitante que tenta denunciar deve abrir login.
- Usuário logado não deve ver suas próprias procuras na vitrine pública.
- Dono da procura vê detalhes restritos e ações de editar, fechar, ativar, excluir, renovar, boost e ofertas recebidas.
- Não dono ve formulário de oferta, exceto se já enviou oferta; nesse casó ve resumo da oferta enviada.
- Botão de enviar oferta deve ser desabilitado sem crédito/plano.
- Se compra de créditos estiver desabilitada, ocultar página/badge de créditos.
- Se boost estiver desabilitado, ocultar produtos/CTA de boost.
- Se plano estiver ativo, exibir como "Plano Pro ativo" e permitir cancelar.
- Empty statés devem orientar proximo passó sem depender de texto hardcoded quando houver conteúdo dinâmico disponível.
- Feedback de sucesso/erro/avisó deve ser consistente em todos os fluxos.
- Confirmar antes de excluir procura, remover regra de moderação e cancelar plano.
- Tema claro/escuro deve persistir.
- Imagens enviadas devem ser validadas como imagem e comprimidas antes de enviar base64.

## 23. SEO atual e melhorias esperadas

Atual:

- `index.html` possui meta description, robots, canonical, OG/Twitter e JSON-LD `WebSite`.
- `App.jsx` atualiza meta tags em runtime:
  - Home: `Eu Procuro - Marketplace reverso`, index.
  - Detalhe de interesse: título da procura, descrição limitada a 155, canonical `/interesses/:id`, index.
  - Legal: título legal, index.
  - Ouvidoria: index.
  - Áreas privadas/admin: noindex.
- `robots.txt` bloqueia `/admin`, `/meus-interesses`, `/ofertas-enviadas`, `/ofertas-recebidas`, `/meus-itens`, `/comprar-creditos`.
- `sitemap.xml` atual lista home e páginas legais.

Novo Next.js:

- Usar `generateMetadata` para páginas públicas.
- Gerar canonical por rota.
- Gerar sitemap dinâmico incluindo:
  - `/`
  - `/categorias`
  - `/categorias/[categoria]` para categorias ativas
  - `/legal/*`
  - `/ouvidoria`
  - opcional: detalhes de procuras públicas se viável buscar IDs.
- Manter robots bloqueando áreas privadas/admin.
- Páginas privadas devem retornar metadata `robots: noindex,nofollow`.
- Categoria sem itens ainda pode ser indexada se tiver conteúdo editorial util, mas evitar thin content em massa.

## 24. Recomendação de arquitetura Next.js

Use App Router.

Estrutura sugerida:

```txt
app/
  layout.tsx
  page.tsx
  categorias/page.tsx
  categorias/[categoria]/page.tsx
  interesses/[id]/page.tsx
  cadastrar-interesse/page.tsx
  meus-interesses/page.tsx
  ofertas-enviadas/page.tsx
  ofertas-recebidas/page.tsx
  meus-itens/page.tsx
  comprar-creditos/page.tsx
  admin/page.tsx
  ouvidoria/page.tsx
  legal/[slug]/page.tsx
  sitemap.ts
  robots.ts
lib/
  api/client.ts
  api/types.ts
  auth/session.ts
  content/content-provider.tsx
components/
  public/
  privaté/
  forms/
  admin/
```

Regras:

- Centralizar API em uma camada única equivalente a `frontend/src/api.js`.
- Para chamadas server-side, encaminhár cookies do request.
- Para chamadas client-side, usar `credentials: include`.
- Tratar `ApiError` com `status` e `payload`.
- Reutilizar os nomes de campos do backend; não criar DTOs incompatíveis.
- Para páginas públicas, preferir fetch no servidor com cache control consciente.
- Para áreas privadas, validar sessão com `/api/auth/me`; se falhar, redirecionar ou exibir login.
- Conteúdo público deve ter fallback local.
- Categoria e produtos não devem ficar hardcoded fora de fallback.

## 25. Checklist de endpoints cobertos por funções atuais

Todos estes exports de `frontend/src/api.js` devem ter equivalente no novo frontend:

- `ApiError`
- `isAuthError`
- `getStoredSession`
- `storeSession`
- `clearSession`
- `connectChatSócket`
- `login`
- `register`
- `fetchMe`
- `logout`
- `forgotPassword`
- `resetPassword`
- `verifyEmail`
- `fetchPublicContent`
- `fetchDashboard`
- `fetchMonetizationAccount`
- `cancelSubscription`
- `purchaseProduct`
- `syncPayment`
- `boostInterest`
- `fetchCatégories`
- `lookupAddressByPostalCode`
- `fetchInterests`
- `fetchInterest`
- `createInterest`
- `updatéInterest`
- `renewInterest`
- `closeInterest`
- `activatéInterest`
- `deleteInterest`
- `fetchOffers`
- `createOffer`
- `reportInterest`
- `createOmbudsmanRequest`
- `fetchOfferConversation`
- `sendOfferMessage`
- `fetchSellerItems`
- `createSellerItem`
- `updatéSellerItem`
- `deactivatéSellerItem`
- `activatéSellerItem`
- `sháreSellerItemOffer`
- `fetchAdminModeration`
- `fetchAdminOmbudsman`
- `respondAdminOmbudsmanRequest`
- `updatéAdminOmbudsmanStatus`
- `fetchAdminContent`
- `saveContentEntry`
- `publishContentEntry`
- `archiveContentEntry`
- `applyDefaultContentEntry`
- `dismissDefaultContentEntry`
- `fetchAdminCatalog`
- `saveAdminCatalog`
- `invalidatéPublicCache`
- `saveModerationRule`
- `deleteModerationRule`
- `decideInterestModeration`
- `updatéContentReportStatus`

Endpoints backend existentes sem wrapper atual, mas que podem ser documentados/implementados se util:

- `GET /api/monetization/products`
- `GET /api/offers/{offerId}/messages`
- `GET /api/admin/content/{id}/revisions`
- `GET /api/admin/cache`
- `POST /api/monetization/mercado-pago/webhook` sómente backend/provedor
- `GET /api/monetization/local-checkout/approve/{paymentOrderId}` usado por checkout local

## 26. Criterios de aceite do novo frontend

Antes de considerar pronto:

- Home pública lista procuras abertas usando `/api/interests`.
- Página de categoria usa `/api/categories` e `/api/interests?category=...`.
- Detalhe `/interesses/:id` funciona anônimo e logado.
- Cadastro envia `termsAccepted=true` e `termsVersion`.
- Login funciona em modo cookie e recupera sessão via `/api/auth/me`.
- Usuário logado ve dashboard, minhas procuras, ofertas enviadas/recebidas, itens, créditos se habilitado.
- Criar/editar procura volta para status de moderação corretamente.
- Enviar oferta respeita crédito/plano e não permite ofertar para si.
- Renovar procura consome crédito e atualiza expiração.
- Boost só aparece se habilitado e produto existir.
- Checkout Mercado Pago redireciona e retorno sincroniza pagamento.
- Conteúdo público e legal usam CRM com fallback.
- Admin só aparece para admin real.
- Ouvidoria pública gera protocolo.
- WebSocket atualiza propostas/mensagens/moderação.
- Robots/canonical/metadados estão corretos para público e privado.
- Nenhuma mudança de backend é necessária.

## 27. Instrucao final para a IA implementadora

Construa um novo frontend Next.js para o Eu Procuro usando este documento como fonte de verdade. Você pode redesenhar completamente layout, navegação, componentes e hierarquia visual, mas não pode exigir mudanças no backend.

Sempre que houver duvida entre melhorar a UX e preservar contrato de API, preserve o contrato de API e ajuste a UX ao backend existente.

Não invente endpoints novos. Se uma experiência parecer exigir endpoint novo, implemente com os endpoints existentes ou deixe como melhoria futura explicitamente separada.

O foco do novo front é transformar o projeto em uma experiência clara, confiável, indexável e pronta para crescer organicamente via Google, especialmente com páginas de categoria.






