# Ambiente HML

Este guia prepara um ambiente de homologacao para validar a aplicacao como um site normal antes de pensar em producao.

## Arquitetura sugerida

```text
Vercel (React)
  -> Render (Spring Boot)
  -> MongoDB Atlas (database euprocuro_hml)
```

## Banco no MongoDB Atlas

Use o mesmo cluster do Atlas, mas com outro database:

```text
euprocuro_hml
```

No MongoDB, o database aparece quando a primeira collection/documento e criada. Entao basta usar a URI com `/euprocuro_hml` no backend.

Exemplo sem credenciais reais:

```text
mongodb+srv://usuario:senha@cluster.mongodb.net/euprocuro_hml?retryWrites=true&w=majority&appName=euprocuro
```

Se a senha tiver caracteres especiais, codifique na URI. Exemplo: `@` vira `%40`.

No Atlas, libere acesso de rede:

1. Abra o projeto no Atlas.
2. Va em `Security` > `Network Access`.
3. Clique em `Add IP Address`.
4. Para HML simples, use `0.0.0.0/0` temporariamente ou libere o IP de saida fixo do provedor.
5. Use um usuario exclusivo para HML, com permissao apenas no database `euprocuro_hml`.

## Backend no Render

O arquivo [render.yaml](/C:/projetos/euprocuro/render.yaml) esta configurado para um Web Service HML.

Configuracao esperada:

- Root directory: `backend`
- Runtime: Docker
- Branch: `hml`
- Health check path: `/actuator/health`
- Auto deploy: habilitado

Variaveis obrigatorias no Render:

```env
SPRING_PROFILES_ACTIVE=prod,hml
MONGO_ATLAS_HML_URI=mongodb+srv://usuario:senha@cluster.mongodb.net/euprocuro_hml?retryWrites=true&w=majority&appName=euprocuro
APP_CORS_ALLOWED_ORIGINS=https://sua-url-hml.vercel.app
APP_RESET_BASE_URL=https://sua-url-hml.vercel.app
APP_AUTH_COOKIE_SECURE=true
APP_AUTH_COOKIE_SAME_SITE=None
APP_AUTH_EXPOSE_RESET_PREVIEW=false
APP_AUTH_EXPOSE_SESSION_TOKEN=true
APP_MONETIZATION_PROVIDER=LOCAL_MOCK
APP_HML_ACCESS_ENABLED=true
APP_HML_ALLOWED_EMAILS=voce@email.com,pessoa@email.com
```

`APP_AUTH_EXPOSE_SESSION_TOKEN=true` facilita o HML quando frontend e backend ficam em dominios diferentes, como Vercel e Render.

## Acesso restrito por e-mail

O HML pode ficar protegido por uma allowlist de e-mails. Com `APP_HML_ACCESS_ENABLED=true`, apenas os e-mails informados em `APP_HML_ALLOWED_EMAILS` conseguem criar conta ou fazer login.

Exemplo:

```env
APP_HML_ACCESS_ENABLED=true
APP_HML_ALLOWED_EMAILS=seuemail@gmail.com,avaliador@gmail.com
```

Se a allowlist estiver ativa e vazia, ninguem consegue entrar. Isso ajuda a evitar que o ambiente de homologacao seja usado por pessoas nao liberadas.

Importante: essa protecao restringe o uso autenticado da aplicacao e da API. A pagina publica da Vercel ainda pode abrir; para bloquear a visualizacao do frontend inteiro antes de carregar a aplicacao, use uma camada externa como Vercel Deployment Protection, Cloudflare Access ou outra protecao no dominio.

## Frontend na Vercel

Configure o projeto apontando para a pasta `frontend`.

- Framework: Vite
- Root directory: `frontend`
- Build command: `npm run build`
- Output directory: `dist`
- Branch: `hml`

Variaveis obrigatorias na Vercel:

```env
VITE_API_BASE=https://sua-api-hml.onrender.com/api
VITE_WS_BASE=wss://sua-api-hml.onrender.com/ws/chat
```

Depois que a URL final da Vercel existir, volte no Render e ajuste `APP_CORS_ALLOWED_ORIGINS` e `APP_RESET_BASE_URL` para essa URL.

## Deploy automatico

Fluxo recomendado:

```text
git push origin hml
  -> Render publica backend
  -> Vercel publica frontend
```

## Checklist de validacao

- Cadastro de usuario
- Login
- Criacao de interesse
- Edicao/desativacao/exclusao de interesse
- Criacao de item pessoal
- Contagem de possiveis interessados
- Envio de oferta
- Chat entre usuarios
- Compra simulada de creditos
- Boost de interesse

## Cuidados

- Nao use o mesmo database de producao.
- Nao versione URI real do Atlas.
- Nao reutilize usuario/senha de producao no HML.
- Se liberar `0.0.0.0/0` no Atlas, use apenas para HML e com usuario restrito.
