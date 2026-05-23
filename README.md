# Eu Procuro

Marketplace reverso full stack inspirado no plano de negocios do **Eu Procuro**: o comprador publica o que procura, define categoria, localidade e faixa de preco, e recebe ofertas de vendedores interessados.

## Stack

- Backend: Spring Boot 2.7 + Java 11 + MongoDB + Lombok
- Frontend: React 18 + Vite
- Mensageria: RabbitMQ
- E-mail: Spring Mail

## Arquitetura do backend

O backend segue uma separacao clara entre camadas:

- `domain/model`: entidades e enums do dominio
- `domain/gateway`: contratos de persistencia, notificacao e eventos
- `application/command`: comandos e filtros dos casos de uso
- `application/service`: implementacoes da regra de negocio
- `application/usecase`: interfaces da aplicacao
- `application/view`: visoes agregadas para respostas compostas
- `infrastructure/persistence`: documentos Mongo, adapters, repositories e mapeadores
- `infrastructure/messaging`: publicacao de eventos para RabbitMQ
- `infrastructure/notification`: envio de e-mail
- `entrypoints/rest`: controllers, DTOs, seguranca HTTP e handlers
- `shared/config`: configuracao transversal e beans de infraestrutura

## O que esta pronto

- Home publica com busca por interesses publicados
- Login e cadastro com senha criptografada em BCrypt
- Sessao por cookie HTTP-only em producao
- Recuperacao de senha por e-mail
- Fallback local para reset de senha quando SMTP nao estiver configurado
- Area logada separada em paginas:
  - interesses ativos
  - ofertas enviadas
  - ofertas recebidas
  - cadastro de novo interesse
- Publicacao de interesses com imagem de referencia
- Busca publica por texto, categoria, cidade e teto de orcamento
- Monetizacao MVP com creditos para vendedores, plano Pro e boost pago de interesses apos a publicacao
- CRM administrativo para textos, politicas legais, categorias, precos, planos e promocoes em runtime
- Block list automatica por CPF/CNPJ para usuarios com procuras rejeitadas pela moderacao
- Cache publico server-side para conteudo, catalogo, CEP e vitrine de interesses, com invalidacao manual no admin
- Indices Mongo para as consultas publicas mais frequentes e contrato de busca dedicado para evoluir para Atlas Search/OpenSearch
- Checkout local simulado e Checkout Pro Mercado Pago com confirmacao por webhook
- E-mails transacionais para reset de senha, nova oferta, mensagem, compra e boost
- Modais de feedback para mensagens de sucesso ou erro
- RabbitMQ configurado para eventos de autenticacao, criacao de interesse e criacao de oferta
- Headers basicos de seguranca, CORS por ambiente e rate limit nas rotas sensiveis
- Pipeline de CI no GitHub Actions com build backend/frontend
- Cobertura minima de 90% no core do backend, validada por JaCoCo

## Observacao sobre dados iniciais

A base agora sobe limpa, sem usuarios ficticios e sem interesses de exemplo.

## Containers locais com Rancher Desktop

Na raiz do projeto:

```bash
docker compose up -d
```

Servicos expostos:

- MongoDB: `mongodb://localhost:27017/euprocuro`
- Mongo Express: `http://localhost:8081`
- RabbitMQ: `amqp://guest:guest@localhost:5672`
- RabbitMQ Management: `http://localhost:15672`

## Rodando o backend

Na pasta `backend`:

```bash
mvn spring-boot:run
```

Para rodar usando explicitamente o MongoDB local:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Para rodar usando MongoDB Atlas:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=atlas
```

Ou:

```bash
mvn clean package
```

Para subir com configuracoes de producao localmente:

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

## MongoDB local x MongoDB Atlas

O projeto esta preparado para alternar entre o MongoDB local e o MongoDB Atlas sem alterar codigo.

Perfis disponiveis:

- `local`: usa `mongodb://localhost:27017/euprocuro`
- `atlas`: usa a variavel de ambiente `MONGO_ATLAS_URI`

Arquivos de profile:

- [application-local.yml](/C:/projetos/euprocuro/backend/src/main/resources/application-local.yml)
- [application-atlas.yml](/C:/projetos/euprocuro/backend/src/main/resources/application-atlas.yml)

### Configurando o Atlas no ambiente local

No MongoDB Atlas:

1. Acesse o projeto no Atlas.
2. Va em `Security` > `Network Access`.
3. Abra a aba `IP Access List`.
4. Clique em `Add IP Address`.
5. Use `Add Current IP Address`.
6. Confirme e aguarde a regra ser aplicada.

O Atlas permite conexoes apenas de IPs cadastrados na IP Access List. Se sua internet trocar de IP, sera necessario adicionar o novo IP.

Depois, configure a variavel de ambiente no Windows PowerShell:

```powershell
[Environment]::SetEnvironmentVariable(
  "MONGO_ATLAS_URI",
  "mongodb+srv://usuario:senha@cluster.mongodb.net/euprocuro?retryWrites=true&w=majority&appName=euprocuro",
  "User"
)
```

Se a senha tiver caracteres especiais, eles precisam estar codificados na URI. Exemplo: `@` vira `%40`.

Depois de criar ou alterar essa variavel, reinicie o IntelliJ ou o terminal para que a aplicacao enxergue o novo valor.

Para conferir:

```powershell
[Environment]::GetEnvironmentVariable("MONGO_ATLAS_URI", "User")
```

Nao grave a URI real do Atlas em arquivos versionados. Use `.env` local ou variaveis de ambiente. Os arquivos `.env` ja estao ignorados pelo Git.

## Rodando o frontend

Na pasta `frontend`:

```bash
npm install
npm run dev
```

Por padrao, o frontend consome a API em `http://localhost:8080/api`.

Em hospedagem, configure `VITE_API_BASE` para a URL publica da API, por exemplo:

```bash
VITE_API_BASE=https://api.seudominio.com/api
```

### Google Analytics

O frontend suporta Google Analytics 4 via Google tag (`gtag.js`). Para ativar, crie uma propriedade GA4, adicione uma stream Web para o dominio da aplicacao e copie o ID de medicao, que comeca com `G-`.

Configure no ambiente do frontend:

```bash
VITE_GA_MEASUREMENT_ID=G-XXXXXXXXXX
```

Se a variavel ficar vazia, o Analytics nao e carregado. Isso permite deixar local e HML sem medicao, ou usar propriedades separadas por ambiente.

A implementacao registra pageviews nas rotas do SPA e eventos basicos de compartilhamento de interesse (`share_interest`). Nao envie dados pessoais ao Google Analytics; use apenas identificadores tecnicos e propriedades agregadas.

## Ambiente local esperado

- Java 11
- Maven
- Node.js + npm
- Rancher Desktop

## Configuracao de e-mail

No ambiente local, o profile `local` carrega automaticamente um arquivo `.env.local` na raiz do projeto.
Esse arquivo fica ignorado pelo Git, entao pode conter credenciais locais sem ir para o repositorio.

Para preparar sua maquina:

1. Copie `.env.local.example` para `.env.local`, se o arquivo ainda nao existir.
2. Preencha as credenciais do SMTP sandbox, como Mailtrap, Ethereal ou MailerSend.
3. Reinicie o backend com o profile `local` pelo IntelliJ ou terminal.

Exemplo de `.env.local`:

```bash
SPRING_PROFILES_ACTIVE=local
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=usuario-do-mailtrap
MAIL_PASSWORD=senha-do-mailtrap
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
APP_EMAIL_FROM=no-reply@euprocuro.local
APP_RESET_BASE_URL=http://localhost:5173
```

Para MailerSend via SMTP, use exatamente o `Username` e `Password` do usuario SMTP, nao o login da conta nem o API token. O `APP_EMAIL_FROM` precisa ser um e-mail do dominio validado ou do dominio de teste do MailerSend, por exemplo `no-reply@seu-dominio.mlsender.net`.

Para usar templates HTML salvos no MailerSend, configure o envio pela API em vez do SMTP texto puro:

```bash
APP_EMAIL_PROVIDER=MAILERSEND_API
APP_EMAIL_FROM=no-reply@seudominio.com
APP_EMAIL_FROM_NAME=Eu Procuro
APP_EMAIL_APP_URL=https://app.seudominio.com
APP_EMAIL_TERMS_URL=https://app.seudominio.com#termos-de-uso
APP_EMAIL_PRIVACY_URL=https://app.seudominio.com#politica-de-privacidade
APP_EMAIL_SUPPORT_URL=mailto:suporte@euprocuro.com
MAILERSEND_API_KEY=sua-chave-api-mailersend
MAILERSEND_TEMPLATE_ID_DEFAULT=id-do-template-html
```

Tambem e possivel informar templates diferentes por evento com `MAILERSEND_TEMPLATE_ID_EMAIL_VERIFICATION`, `MAILERSEND_TEMPLATE_ID_PASSWORD_RESET`, `MAILERSEND_TEMPLATE_ID_OFFER_RECEIVED`, `MAILERSEND_TEMPLATE_ID_CONVERSATION_MESSAGE`, `MAILERSEND_TEMPLATE_ID_PURCHASE_CONFIRMATION` e `MAILERSEND_TEMPLATE_ID_BOOST_ACTIVATED`. Se essas variaveis ficarem vazias, a aplicacao usa `MAILERSEND_TEMPLATE_ID_DEFAULT`.

Sem SMTP valido, a API registra no log o link de verificacao/redefinicao como fallback local.
No profile `prod`, o preview de reset fica desabilitado por padrao.

Para testar a verificacao de e-mail localmente:

1. Rode o backend com o profile `local`.
2. Rode o frontend em `http://localhost:5173`.
3. Crie uma conta nova.
4. Abra o e-mail no inbox do SMTP sandbox.
5. Clique no link `?mode=verify-email&token=...`.
6. A aplicacao marcara o usuario como `emailVerified=true`.

Eventos que ja disparam e-mail:

- verificacao de e-mail no cadastro
- link de redefinicao de senha
- nova oferta recebida em um interesse
- nova mensagem na conversa
- confirmacao de compra de creditos ou plano
- confirmacao de boost ativado

## Moderacao por IA

A moderacao por OpenAI fica habilitada por padrao em todos os ambientes. Para que a chamada real aconteca, configure uma chave valida via variavel de ambiente, nunca dentro do `application.yml`.

No ambiente local, inclua no `.env.local`:

```bash
APP_OPENAI_MODERATION_ENABLED=true
OPENAI_API_KEY=sua-chave-openai
APP_OPENAI_MODERATION_MODEL=omni-moderation-latest
```

No HML/Render, configure as mesmas variaveis em **Environment**:

```bash
APP_OPENAI_MODERATION_ENABLED=true
OPENAI_API_KEY=sua-chave-openai
APP_OPENAI_MODERATION_MODEL=omni-moderation-latest
```

Se a IA estiver habilitada mas a chave estiver ausente, invalida ou a OpenAI estiver indisponivel, o anuncio nao sera aprovado automaticamente: ele ficara como `REVIEW_REQUIRED` para revisao manual no painel admin.

Para liberar o painel admin, configure tambem:

```bash
APP_ADMIN_ALLOWED_EMAILS=seu-email@dominio.com,outro-admin@dominio.com
```

Depois reinicie o backend local ou faca redeploy no Render. Ao logar com um e-mail liberado, a opcao `Moderacao` aparece na area logada.

### Block list por CPF/CNPJ

A plataforma possui uma block list operacional em MongoDB para reduzir risco de reincidencia em conteudo rejeitado.

Como funciona:

1. Toda procura nasce como `PENDING` e passa pelo fluxo de moderacao.
2. Se a moderacao automatica rejeitar a procura por regra local de alto risco ou pela IA, o CPF/CNPJ do dono da procura e usado para criar uma entrada em `user_block_list`.
3. Em novas publicacoes, se o CPF/CNPJ estiver ativo na block list, a procura nao e publicada automaticamente. Ela fica como `REVIEW_REQUIRED` com provider `BLOCK_LIST`.
4. O admin decide manualmente se aprova, rejeita ou arquiva a procura pelo painel de moderacao.

Para reduzir exposicao de dado sensivel, a collection `user_block_list` nao duplica o CPF/CNPJ bruto: ela armazena `documentHash`, `documentLast4`, tipo do documento, origem da rejeicao e contadores de reincidencia. O frontend publico nao recebe a block list nem dados sensiveis de documento.

Em producao, configure um segredo para fortalecer o hash deterministico:

```bash
APP_SECURITY_DOCUMENT_HASH_PEPPER=um-segredo-longo-e-randomico
```

A block list fica ativada por padrao. Para desativar temporariamente em local/HML/admin, acesse `Moderacao` > `CRM operacional` > `Politicas de moderacao` e desmarque `Ativar block list automatica por CPF/CNPJ`. Ao salvar, o backend para de consultar e de gravar entradas novas enquanto a opcao estiver desligada.

## CRM administrativo

A aba `Moderacao` tambem concentra o CRM interno da plataforma. O acesso e sempre protegido pelo backend: somente usuarios autenticados cujo e-mail esteja em `APP_ADMIN_ALLOWED_EMAILS` conseguem ler ou alterar dados administrativos.

O CRM possui duas frentes:

- **Conteudo**: textos da interface, mensagens, CTAs, erros e documentos legais. Rascunhos, historico e autoria ficam restritos ao admin; o site publico consome apenas entradas `PUBLISHED` por `GET /api/content/public`.
- **Catalogo operacional**: categorias de anuncios, disponibilidade da monetizacao, produtos, precos, planos, boosts e promocoes. O site recebe apenas categorias ativas e modalidades de monetizacao habilitadas; campos internos de admin nao sao expostos ao front publico.

Fluxo de conteudo:

1. O backend semeia `backend/src/main/resources/content/default-content.json` na primeira subida.
2. O admin edita um rascunho pelo painel.
3. Ao publicar, a versao passa a ser carregada em runtime pelo frontend, sem redeploy.
4. Documentos legais publicados alimentam as paginas do footer e o modal de aceite dos Termos de Uso.

Atualizacoes do `default-content.json`:

- O arquivo `default-content.json` e tratado como baseline de produto, nao como uma sobrescrita automatica do CRM.
- Em cada subida, o backend compara o valor padrao atual com o conteudo ja existente no Mongo.
- Chaves novas sao criadas e publicadas automaticamente, como no seed inicial.
- Chaves existentes nao sao sobrescritas. Se o valor padrao mudou e ainda nao esta publicado nem em rascunho, a entrada recebe `defaultUpdateAvailable=true`.
- No CRM de conteudo, essas entradas aparecem com o selo `Atualizacao padrao` e no filtro `Atualizacoes padrao`.
- O admin pode clicar em `Aplicar como rascunho` para copiar a versao do arquivo para o rascunho e depois publicar normalmente.
- O admin tambem pode clicar em `Ignorar sugestao`; essa versao padrao nao volta a aparecer ate o hash do default mudar de novo em outro deploy.
- Isso permite que novas copies vindas do codigo sejam revisadas, aprovadas e publicadas em runtime sem apagar customizacoes feitas diretamente no CRM.

Se a collection `content_entries` for apagada em local/HML, o proximo boot recria tudo a partir do `default-content.json`. Em producao, prefira backup e fluxo de aprovacao pelo CRM, porque apagar a collection remove rascunhos, publicacoes, flags legais, autoria e historico operacional associado.

Fluxo de catalogo operacional:

1. O admin altera categorias ou produtos em `CRM operacional`.
2. Ao salvar, o backend valida codigos, duplicidades, preco promocional, disponibilidade da monetizacao e pelo menos uma categoria ativa.
3. Categorias ativas sao refletidas em `GET /api/categories`.
4. Produtos habilitados sao refletidos em `GET /api/monetization/products` e na conta de monetizacao somente quando a modalidade correspondente tambem estiver habilitada.

Disponibilidade da monetizacao:

- `creditPurchasesEnabled=false` oculta o saldo/botao de creditos no topo, remove a aba `Comprar creditos`, nao entrega pacotes de credito/plano na conta de monetizacao e bloqueia `POST /api/monetization/purchase`.
- `boostPurchasesEnabled=false` nao entrega produtos `BOOST`, oculta a area de impulsionamento do interesse e bloqueia `POST /api/monetization/interests/{interestId}/boost`.
- O padrao semeado pelo backend e subir tudo desativado. Para liberar primeiro apenas boosts, habilite somente `Permitir compra de boosts` no CRM operacional e mantenha creditos/planos desligados.

Promocoes:

- `price` e o preco atual cobrado.
- `originalPrice` e exibido como preco "de" somente quando `promotional=true`.
- `promotionLabel` permite mostrar um selo curto, como `Oferta de lancamento`.

Categorias:

- O codigo deve ser estavel, em caixa alta, usando letras, numeros, `_` ou `-`.
- Interesses e itens de vendedor gravam o codigo da categoria, permitindo novas categorias sem enum fixo no codigo.
- Categorias inativas deixam de aparecer nos formularios e filtros, mas registros antigos continuam preservados.

Boost:

- O usuario nao marca boost ao cadastrar interesse.
- O interesse nasce sem destaque.
- Depois de publicado/aprovado, o usuario pode comprar um produto do tipo `BOOST`.
- Quando o pagamento e processado, o backend grava `boostedUntil`; a busca usa esse campo para priorizar o anuncio enquanto estiver vigente.

## Entrega dinamica de procuras

A vitrine agora possui uma etapa de ranking deterministico para usuarios logados. A regra central e:

> Toda procura tem uma area natural de entrega. O boost nao muda o conteudo da procura; ele aumenta alcance e prioridade.

O endpoint continua sendo `GET /api/interests`; nao houve mudanca de contrato para o frontend. Quando a requisicao possui usuario autenticado, o backend usa `CurrentUserContext.optionalUserId` para aplicar uma ordenacao personalizada. Quando nao ha usuario logado, a vitrine continua usando a ordenacao publica cacheavel: boost ativo primeiro e depois mais recentes.

Pontuacao inicial usada pelo `InterestDeliveryRankingService`:

- boost ativo: `+30`
- procura criada nas ultimas 24h: `+10`; ate 7 dias: `+6`; ate 30 dias: `+3`
- mesma cidade do usuario: `+40`
- mesmo estado do usuario: `+20`
- mesmo pais do usuario: `+5`
- categoria igual a algum item ativo em `Tenho para negociar`: `+25`
- mesma cidade de algum item ativo: `+20`
- mesmo estado de algum item ativo: `+10`
- tags iguais entre procura e item ativo: `+12` por tag, limitado a `+36`
- tokens parecidos entre titulo/descricao/tags da procura e titulo/descricao/tags dos itens ativos: `+8` por token, limitado a `+40`

Preferencias usadas hoje:

- localizacao do perfil do usuario (`city`, `state`, `country`)
- itens ativos cadastrados em `Tenho para negociar`
- categoria, tags, titulo e descricao desses itens

Ainda nao existe uma tabela de preferencias explicitas. Isso foi proposital para manter a primeira versao simples: o usuario ja personaliza a entrega indiretamente ao preencher endereco e cadastrar itens que possui.

Comportamento com cache:

- visitantes anonimos continuam usando cache `marketplace`
- usuarios logados nao usam o cache publico compartilhado para a lista personalizada
- a busca Mongo continua atras do contrato `InterestSearchGateway`
- para ranking personalizado, o backend busca uma janela maior de candidatos e reordena em memoria antes de aplicar `offset` e `limit`

Como testar localmente:

1. Crie ou use um usuario com cidade/estado preenchidos, por exemplo `Erechim/RS`.
2. Cadastre um item ativo em `Tenho para negociar`, por exemplo `Celta 2012`, categoria `AUTOMOVEIS`, tags `celta`, `chevrolet`.
3. Com outro usuario, publique procuras variadas:
   - uma procura por `Celta 2012` em `Erechim/RS`
   - uma procura generica mais recente de outra categoria
   - uma procura com boost ativo, se boosts estiverem habilitados
4. Acesse a home logado com o usuario que possui o item. A procura por `Celta 2012` deve ganhar prioridade por categoria, texto, tags e localizacao.
5. Acesse a home deslogado. A ordenacao deve voltar ao comportamento publico: boost ativo e recencia.

Testes automatizados relacionados:

```bash
mvn -q "-Dtest=InterestDeliveryRankingServiceTest,MarketplaceServiceTest" test
```

Proximos passos recomendados:

- Criar preferencias explicitas no perfil do usuario: categorias favoritas, palavras-chave/tags, cidades/regioes e raio padrao.
- Permitir editar essas preferencias em uma tela simples de perfil.
- Registrar sinais de interacao, como procuras abertas, propostas enviadas e itens ignorados, para melhorar ranking sem depender so de cadastro manual.
- Exibir explicacoes discretas no front, por exemplo `Combina com um item seu` ou `Procura proxima de voce`.
- Criar notificacoes controladas para novas procuras compativeis, com limite por periodo para evitar spam.
- Migrar a parte textual para busca dedicada quando a base crescer: Atlas Search, OpenSearch, Meilisearch ou Typesense. O contrato `InterestSearchGateway` ja deixa essa troca isolada.

## Cache, indices e busca

A aplicacao usa cache server-side apenas para dados publicos e reconstruiveis:

- `content`: catalogo publico de textos publicados (`GET /api/content/public`)
- `catalog`: categorias e produtos publicados do CRM operacional
- `marketplace`: listagens publicas de interesses com `openOnly=true`
- `address`: resultado de consulta de CEP

Dados privados, admin, sessoes, dashboard, notificacoes, conversas, creditos e pagamentos nao entram em cache compartilhado. As respostas da API recebem `Cache-Control: no-store`; o ganho de performance vem do cache interno do backend, nao do navegador.

Configuracao por ambiente:

```bash
APP_PUBLIC_CACHE_ENABLED=true
APP_PUBLIC_CACHE_MAX_ENTRIES=2000
APP_PUBLIC_CACHE_CONTENT_TTL_SECONDS=300
APP_PUBLIC_CACHE_CATALOG_TTL_SECONDS=300
APP_PUBLIC_CACHE_MARKETPLACE_TTL_SECONDS=60
APP_PUBLIC_CACHE_ADDRESS_TTL_SECONDS=2592000
```

Invalidacao automatica:

- publicar ou arquivar conteudo invalida `content`
- salvar catalogo operacional invalida `catalog`
- criar, editar, renovar, desativar, ativar, excluir, moderar, denunciar ou impulsionar interesse invalida `marketplace`

Invalidacao manual:

```bash
GET  /api/admin/cache
POST /api/admin/cache/invalidate?scope=all
POST /api/admin/cache/invalidate?scope=content
POST /api/admin/cache/invalidate?scope=catalog
POST /api/admin/cache/invalidate?scope=marketplace
POST /api/admin/cache/invalidate?scope=address
```

O botao `Limpar cache` fica no CRM de conteudo e chama a invalidacao global. O acesso usa o mesmo controle admin de `APP_ADMIN_ALLOWED_EMAILS`.

O store atual e `LOCAL_MEMORY`, suficiente para desenvolvimento, HML e uma instancia simples. Para varias instancias, o desenho ja esta isolado em `PublicCacheService`; o proximo passo natural e trocar esse store por Redis mantendo as mesmas chaves, TTLs e namespaces.

O Mongo cria indices automaticamente via `spring.data.mongodb.auto-index-creation=true`. Hoje existem indices para:

- vitrine de interesses por `status`, `location.city`, `category`, `boostedUntil`, `createdAt` e expiracao
- interesses por dono
- itens do vendedor por dono, status ativo, categoria e cidade
- conteudo publicado por `locale`, `status` e `key`
- text indexes em titulo, descricao e tags de interesses/itens

A busca publica continua usando Mongo, mas a regra de busca ficou atras do contrato `InterestSearchGateway`. Para uma busca dedicada no futuro, crie outro adapter para esse contrato, por exemplo Atlas Search ou OpenSearch, sem alterar o caso de uso do marketplace.

## Logs de auditoria e integracoes externas

A aplicacao grava dois tipos de registro operacional no MongoDB:

- `audit_events`: eventos internos de negocio e administracao, como cadastro, login, logout, criacao/edicao de interesse, envio de oferta e invalidacao manual de cache.
- `external_integration_logs`: chamadas para servicos externos, com request/response agrupados para facilitar inspecao no Mongo.

Os logs externos usam uma estrutura intencionalmente simples:

```json
{
  "createdAt": "2026-05-06T20:00:00Z",
  "operation": "OPEN_AI_MODERATION",
  "correlationId": "interest-123",
  "request": {
    "method": "POST",
    "url": "/v1/moderations",
    "headers": {
      "Authorization": "***"
    },
    "body": {
      "model": "omni-moderation-latest",
      "input": []
    }
  },
  "response": {
    "status": 200,
    "body": {}
  },
  "durationMs": 123,
  "success": true,
  "errorMessage": null
}
```

Operacoes registradas atualmente:

- `OPEN_AI_MODERATION`: chamada de moderacao para a OpenAI.
- `VIA_CEP`: consulta de endereco por CEP.
- `MERCADO_PAGO_CREATE_CHECKOUT_PREFERENCE`: criacao de preferencia no Checkout Pro.
- `MERCADO_PAGO_FIND_PAYMENT`: consulta de pagamento no Mercado Pago.

Headers sensiveis sao mascarados antes de salvar (`Authorization`, tokens, API keys, cookies, passwords e secrets). Bodies grandes sao truncados por seguranca e controle de volume.

Configuracao:

```bash
APP_AUDIT_TTL_SECONDS=604800
APP_EXTERNAL_LOG_TTL_SECONDS=604800
APP_EXTERNAL_LOG_BODY_MAX_LENGTH=4000
```

Por padrao, os registros expiram em 7 dias por indice TTL. Esse prazo e adequado para testes, HML e investigacao curta; para producao, ajuste conforme necessidade juridica/operacional.

Observacao importante sobre OpenAI: o log `OPEN_AI_MODERATION` so aparece quando a chamada real para a OpenAI acontece. No fluxo atual, a criacao de interesse publica um evento de moderacao no RabbitMQ; se o consumidor nao estiver rodando, se o RabbitMQ estiver indisponivel, se `APP_OPENAI_MODERATION_ENABLED=false` ou se `OPENAI_API_KEY` estiver ausente/invalida, nao havera request externo para registrar nessa collection.

## Monetizacao e pagamentos

O MVP ja possui produtos de monetizacao configuraveis por ambiente:

- pacotes de creditos para vendedores enviarem propostas
- plano vendedor Pro com propostas liberadas enquanto estiver ativo
- boost de 3 ou 7 dias para destacar interesses na busca e na home

Por padrao geral, a disponibilidade comercial sobe desligada pelo CRM operacional, mesmo que o provedor de pagamento esteja configurado. O pagamento usa o provedor `LOCAL_MOCK`: a API simula aprovacao imediata para permitir testar produto, tela e regras de negocio sem dinheiro real quando uma modalidade for habilitada.

No profile `local`, o provedor padrao e `LOCAL_CHECKOUT_MOCK`. Ele cria um pedido pendente, redireciona para um checkout fake local e aprova o pedido automaticamente antes de voltar ao frontend. Assim da para testar o fluxo ponta-a-ponta sem ngrok e sem webhook real:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Com esse profile, ao comprar creditos no frontend:

1. A API cria um pedido em `payment_orders`.
2. O frontend redireciona para `/api/monetization/local-checkout/approve/{paymentOrderId}`.
3. A API aprova o pedido localmente.
4. A API redireciona de volta para `http://localhost:5173?payment=success`.
5. Os creditos aparecem para o usuario.

Para usar o Checkout Pro do Mercado Pago:

```bash
APP_MONETIZATION_PROVIDER=MERCADO_PAGO_CHECKOUT_PRO
MERCADO_PAGO_ACCESS_TOKEN=TEST-...
MERCADO_PAGO_SANDBOX=true
MERCADO_PAGO_SUCCESS_URL=https://seu-front.vercel.app?payment=success
MERCADO_PAGO_FAILURE_URL=https://seu-front.vercel.app?payment=failure
MERCADO_PAGO_PENDING_URL=https://seu-front.vercel.app?payment=pending
MERCADO_PAGO_NOTIFICATION_URL=https://sua-api.onrender.com/api/monetization/mercado-pago/webhook
MERCADO_PAGO_WEBHOOK_SECRET=assinatura-secreta-do-webhook
MERCADO_PAGO_WEBHOOK_SIGNATURE_REQUIRED=true
```

Para forcar o mock local mesmo fora do profile `local`:

```bash
APP_MONETIZATION_PROVIDER=LOCAL_CHECKOUT_MOCK
LOCAL_CHECKOUT_BASE_URL=http://localhost:8080/api/monetization/local-checkout/approve
LOCAL_CHECKOUT_SUCCESS_URL=http://localhost:5173?payment=success
```

Fluxo do Checkout Pro:

1. O usuario escolhe Pix ou cartao no frontend.
2. O backend cria uma preferencia de pagamento no Mercado Pago.
3. O usuario paga no gateway.
4. O Mercado Pago chama o webhook da API.
5. A API consulta o pagamento no Mercado Pago usando o `payment_id`.
6. Somente apos status `approved`, a API libera creditos ou plano.

O endpoint publico do webhook e:

```bash
POST /api/monetization/mercado-pago/webhook
```

Mesmo sendo publico, o webhook valida a autenticidade da chamada com os headers `x-signature` e `x-request-id`, usando a assinatura secreta configurada no painel do Mercado Pago. Sem uma assinatura valida, a API responde `401` e nao consulta nem libera pagamento.

### Precos configuraveis

Precos, quantidades, planos, boosts e promocoes sao gerenciados pelo CRM operacional, nao por texto hardcoded no frontend. O catalogo padrao e semeado pelo backend para a primeira execucao, mas depois passa a ser editavel em runtime pela aba admin.

## Seguranca para producao

- Cookies de sessao HTTP-only com configuracao por ambiente
- Sessao deslizante: usuarios ativos renovam a expiracao quando a sessao entra na janela final configurada
- `Strict-Transport-Security`, `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy` e `Permissions-Policy`
- `CORS` limitado por `APP_CORS_ALLOWED_ORIGINS`
- Rate limit para login, cadastro, reset de senha e envio de mensagens
- `server.forward-headers-strategy=framework` para funcionar bem atras de proxy/Load Balancer

Variaveis importantes para producao:

```bash
SPRING_PROFILES_ACTIVE=prod
APP_CORS_ALLOWED_ORIGINS=https://app.seudominio.com
APP_AUTH_COOKIE_SECURE=true
APP_AUTH_COOKIE_SAME_SITE=Lax
APP_AUTH_COOKIE_DOMAIN=.seudominio.com
APP_AUTH_EXPOSE_RESET_PREVIEW=false
APP_AUTH_EXPOSE_SESSION_TOKEN=false
APP_AUTH_SESSION_HOURS=168
APP_AUTH_SESSION_RENEWAL_THRESHOLD_HOURS=24
APP_RESET_BASE_URL=https://app.seudominio.com
```

`APP_AUTH_SESSION_HOURS` define o tempo maximo de inatividade. `APP_AUTH_SESSION_RENEWAL_THRESHOLD_HOURS` define quando renovar: com os defaults, uma sessao dura 7 dias e, se o usuario continuar ativo quando faltar menos de 24h, o backend renova por mais 7 dias.

## Rate limit

O rate limit e aplicado automaticamente nas rotas sensiveis:

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `POST /api/offers/:offerId/messages`

O padrao e **25 requisicoes por janela de 5 minutos por IP**. A contagem usa o IP real do cliente, lido do header `CF-Connecting-IP` quando a requisicao passa pelo Cloudflare.

Para ajustar os limites, use variaveis de ambiente (no Render ou no `.env.local`):

```bash
APP_RATE_LIMIT_ENABLED=true
APP_RATE_LIMIT_MAX_REQUESTS=25        # numero maximo de requisicoes por janela
APP_RATE_LIMIT_WINDOW_SECONDS=300     # tamanho da janela em segundos (300 = 5 minutos)
APP_RATE_LIMIT_MAX_TRACKED_KEYS=10000 # maximo de IPs monitorados simultaneamente
```

Os defaults ficam em `backend/src/main/resources/application.yml` na chave `application.security.rate-limit`. A implementacao esta em `backend/src/main/java/com/euprocuro/api/entrypoints/rest/security/RateLimitInterceptor.java`.

## Configuracao de RabbitMQ

O backend ja sobe com publisher pronto para RabbitMQ. Os principais eventos publicados hoje sao:

- `user.registered`
- `auth.login`
- `auth.logout`
- `auth.password-reset-requested`
- `auth.password-reset-completed`
- `interest.created`
- `interest.updated`
- `interest.boosted`
- `offer.created`
- `monetization.purchase.created`
- `monetization.purchase.completed`

Exchange e filas podem ser ajustadas por ambiente:

```bash
APP_RABBIT_EXCHANGE=euprocuro.exchange
APP_RABBIT_INTEREST_CREATED_QUEUE=euprocuro.interest.created
APP_RABBIT_OFFER_CREATED_QUEUE=euprocuro.offer.created
APP_RABBIT_AUTH_QUEUE=euprocuro.auth.events
```

## Documentacao da API com Swagger/OpenAPI

A API possui documentacao interativa gerada automaticamente com Swagger (Springdoc OpenAPI).

### Acessar o Swagger

Com o backend rodando em `http://localhost:8080`, acesse:

- **UI Interativa**: `http://localhost:8080/swagger-ui.html`
- **JSON OpenAPI**: `http://localhost:8080/v3/api-docs`
- **YAML OpenAPI**: `http://localhost:8080/v3/api-docs.yaml`

### No Swagger voce consegue:

- Visualizar todos os endpoints da API
- Ver os parametros, request bodies e responses de cada endpoint
- Testar endpoints diretamente pelo navegador (Try it out)
- Consultar os schemas dos DTOs
- Filtrar endpoints por tag (Auth, Interests, Offers, etc.)

### Configuracao

A dependencia `springdoc-openapi-starter-webmvc-ui` ja esta adicionada no `pom.xml`. A configuracao padrao esta em `application.yml`:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operations-sorter: method
    tags-sorter: alpha
    doc-expansion: none
```

### Anotacoes nos Controllers

Para melhorar a documentacao, os controllers usam anotacoes Swagger:

```java
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Endpoints relacionados a autenticacao...")
public class AuthController {

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuario", description = "Realiza login...")
    @ApiResponse(responseCode = "200", description = "Login realizado com sucesso")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        // ...
    }
}
```

As anotacoes sao importadas de:

```java
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
```

## Endpoints principais

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `GET /api/dashboard`
- `GET /api/categories`
- `GET /api/interests`
- `GET /api/interests/{id}`
- `POST /api/interests`
- `PUT /api/interests/{id}`
- `GET /api/interests/{id}/offers`
- `POST /api/interests/{id}/offers`
- `GET /api/offers/{id}/conversation`
- `GET /api/offers/{id}/messages`
- `POST /api/offers/{id}/messages`
- `GET /api/monetization/products`
- `GET /api/monetization/account`
- `POST /api/monetization/purchase`
- `POST /api/monetization/interests/{interestId}/boost`
- `POST /api/monetization/mercado-pago/webhook`
- `POST /api/ouvidoria`
- `GET /api/admin/ouvidoria`
- `POST /api/admin/ouvidoria/{id}/response`
- `PATCH /api/admin/ouvidoria/{id}/status`
- `GET /api/content/public`
- `GET /api/admin/content`
- `POST /api/admin/content`
- `PUT /api/admin/content/{id}`
- `POST /api/admin/content/{id}/publish`
- `POST /api/admin/content/{id}/archive`
- `POST /api/admin/content/{id}/apply-default`
- `POST /api/admin/content/{id}/dismiss-default`
- `GET /api/admin/catalog`
- `PUT /api/admin/catalog`
- `GET /api/admin/cache`
- `POST /api/admin/cache/invalidate?scope=all`

## Ouvidoria

A Ouvidoria e um canal formal e simples para reclamacoes, contestacoes de moderacao, problemas com pagamento e sugestoes.

- O link publico fica no footer e abre `/ouvidoria`.
- O envio publico usa `POST /api/ouvidoria` e grava na collection `ombudsman_requests`.
- Cada manifestacao recebe um protocolo no formato `OUV-AAAA-XXXXXXXX`.
- O painel administrativo exibe as manifestacoes dentro da area `Admin`.
- Administradores podem filtrar por status, mudar status e enviar uma resposta.
- Ao enviar uma manifestacao ou resposta, o sistema tenta enviar e-mail ao usuario. Falha de e-mail nao impede a gravacao da manifestacao.

Status disponiveis:

- `OPEN`
- `IN_REVIEW`
- `ANSWERED`
- `CLOSED`

## Testes e cobertura

Na pasta `backend`:

```bash
mvn clean verify
```

Esse comando:

- executa os testes unitarios
- gera o relatorio JaCoCo em `backend/target/site/jacoco`
- falha o build se a cobertura do core do backend ficar abaixo de `90%`

## Pipeline GitHub

O workflow esta em [.github/workflows/ci.yml](/C:/projetos/euprocuro/.github/workflows/ci.yml) e faz:

- backend: `mvn -B clean verify`
- frontend: `npm ci` + `npm run build`

Ele roda em `push` para `main`, `hml`, `master` e `codex/**`, em `pull_request` para `main` e `hml`, e tambem pode ser disparado manualmente.

## Deploy

- Backend containerizado em [backend/Dockerfile](/C:/projetos/euprocuro/backend/Dockerfile)
- Exemplo de blueprint do Render em [render.yaml](/C:/projetos/euprocuro/render.yaml)
- Exemplo de variaveis em [.env.example](/C:/projetos/euprocuro/.env.example)
- Guia de HML em [docs/hml-deploy.md](/C:/projetos/euprocuro/docs/hml-deploy.md)
- Guia de PRD com Render Starter, Cloudflare e Vercel em [docs/prd-deploy.md](/C:/projetos/euprocuro/docs/prd-deploy.md)

## IntelliJ

1. Abra `C:\projetos\euprocuro`.
2. Recarregue o Maven pelo `pom.xml` da raiz.
3. Confirme o SDK do projeto como Java 11.
4. Use uma das configuracoes salvas:
   - `Eu Procuro Backend Local`
   - `Eu Procuro Backend Atlas`
   - `Eu Procuro Frontend`

Use `Eu Procuro Backend Local` para trabalhar com o MongoDB do Rancher/Desktop.
Use `Eu Procuro Backend Atlas` para trabalhar com o banco remoto no MongoDB Atlas.

Se o IntelliJ nao localizar o Node automaticamente, configure manualmente:

- `Node runtime`: `C:\Program Files\nodejs\node.exe`
- `Package manager`: `C:\Program Files\nodejs\npm.cmd`
