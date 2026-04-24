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

Ou:

```bash
mvn clean package
```

Para subir com configuracoes de producao localmente:

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

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

## Ambiente local esperado

- Java 11
- Maven
- Node.js + npm
- Rancher Desktop

## Configuracao de e-mail

Para envio real de e-mail, configure variaveis como:

```bash
MAIL_HOST=smtp.seuprovedor.com
MAIL_PORT=587
MAIL_USERNAME=seu-usuario
MAIL_PASSWORD=sua-senha
APP_EMAIL_FROM=no-reply@seudominio.com
APP_RESET_BASE_URL=http://localhost:5173
```

Sem SMTP configurado, a API responde com um `previewResetLink` para facilitar o teste local do fluxo.
No profile `prod`, esse preview fica desabilitado por padrao.

## Seguranca para producao

- Cookies de sessao HTTP-only com configuracao por ambiente
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
APP_RESET_BASE_URL=https://app.seudominio.com
```

## Configuracao de RabbitMQ

O backend ja sobe com publisher pronto para RabbitMQ. Os principais eventos publicados hoje sao:

- `user.registered`
- `auth.login`
- `auth.logout`
- `auth.password-reset-requested`
- `auth.password-reset-completed`
- `interest.created`
- `offer.created`

Exchange e filas podem ser ajustadas por ambiente:

```bash
APP_RABBIT_EXCHANGE=euprocuro.exchange
APP_RABBIT_INTEREST_CREATED_QUEUE=euprocuro.interest.created
APP_RABBIT_OFFER_CREATED_QUEUE=euprocuro.offer.created
APP_RABBIT_AUTH_QUEUE=euprocuro.auth.events
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

Ele roda em `push` e `pull_request`.

## Deploy

- Backend containerizado em [backend/Dockerfile](/C:/projetos/euprocuro/backend/Dockerfile)
- Exemplo de blueprint do Render em [render.yaml](/C:/projetos/euprocuro/render.yaml)
- Exemplo de variaveis em [.env.example](/C:/projetos/euprocuro/.env.example)

## IntelliJ

1. Abra `C:\projetos\euprocuro`.
2. Recarregue o Maven pelo `pom.xml` da raiz.
3. Confirme o SDK do projeto como Java 11.
4. Use as configuracoes salvas `Eu Procuro Backend` e `Eu Procuro Frontend`.

Se o IntelliJ nao localizar o Node automaticamente, configure manualmente:

- `Node runtime`: `C:\Program Files\nodejs\node.exe`
- `Package manager`: `C:\Program Files\nodejs\npm.cmd`
