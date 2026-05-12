# Ambiente PRD

Este guia sobe a primeira producao do Eu Procuro com custo baixo e caminho simples de escala.

## Arquitetura

```text
Cloudflare DNS/WAF
  -> Vercel (React/Vite)
  -> Render Starter (Spring Boot)
  -> MongoDB Atlas
  -> LavinMQ/RabbitMQ
  -> Mercado Pago, MailerSend, OpenAI, ViaCEP
```

Dominios:

- `euprocuro.com`: frontend PRD.
- `www.euprocuro.com`: alias do frontend PRD.
- `api.euprocuro.com`: backend PRD.
- `hml.euprocuro.com`: frontend HML.
- `api-hml.euprocuro.com`: backend HML.

Branches:

- `main`: producao.
- `hml`: homologacao.

## Cloudflare e GoDaddy

1. Adicione `euprocuro.com` na Cloudflare.
2. Copie os nameservers informados pela Cloudflare.
3. Na GoDaddy, altere os nameservers do dominio para os nameservers da Cloudflare.
4. Aguarde a Cloudflare marcar a zona como ativa.
5. Configure SSL/TLS como `Full`.
6. Ative HTTPS automatico e redirect canonico:
   - escolha `euprocuro.com` como dominio principal;
   - redirecione `www.euprocuro.com` para `https://euprocuro.com`, ou mantenha os dois apontando para a Vercel se preferir preservar `www`.

Crie os DNS records depois de adicionar os dominios nas plataformas, usando os alvos exibidos por Vercel e Render:

| Nome | Tipo | Destino | Proxy |
| --- | --- | --- | --- |
| `@` | A ou CNAME conforme Vercel | valor mostrado pela Vercel | DNS only inicialmente |
| `www` | CNAME | valor mostrado pela Vercel | DNS only inicialmente |
| `hml` | CNAME | valor mostrado pela Vercel | DNS only inicialmente |
| `api` | CNAME | valor mostrado pelo Render | DNS only inicialmente |
| `api-hml` | CNAME | valor mostrado pelo Render | DNS only inicialmente |

Depois que Vercel e Render validarem os certificados, avalie ligar proxy da Cloudflare nos subdominios HTTP. Se houver problema com WebSocket ou validacao de certificado, volte temporariamente para `DNS only`.

Protecao recomendada:

- WAF managed rules ativado.
- Rate limiting de borda para `/api/auth/login`, `/api/auth/register`, `/api/auth/forgot-password` e `/api/auth/reset-password`, se o plano da Cloudflare permitir.
- Cloudflare Access em `hml.euprocuro.com` e `api-hml.euprocuro.com`, ou Vercel Deployment Protection para o frontend HML.

## Vercel

Projeto frontend:

- Root directory: `frontend`
- Framework: Vite
- Build command: `npm run build`
- Output directory: `dist`
- Production branch: `main`

Variaveis de producao:

```env
VITE_API_BASE=https://api.euprocuro.com/api
VITE_WS_BASE=wss://api.euprocuro.com/ws/chat
VITE_AUTH_SESSION_MODE=cookie
VITE_GA_MEASUREMENT_ID=
```

Variaveis de HML:

```env
VITE_API_BASE=https://api-hml.euprocuro.com/api
VITE_WS_BASE=wss://api-hml.euprocuro.com/ws/chat
VITE_AUTH_SESSION_MODE=cookie
VITE_GA_MEASUREMENT_ID=
```

Adicione os dominios:

- PRD: `euprocuro.com` e `www.euprocuro.com`.
- HML: `hml.euprocuro.com`.

## Render

O arquivo `render.yaml` define dois Web Services:

- `eu-procuro-api-hml`: branch `hml`, plano free, profile `prod,hml`.
- `eu-procuro-api-prd`: branch `main`, plano `starter`, profile `prod`.

Crie ou sincronize o Blueprint no Render a partir do repositorio `germanofortuna/euprocuro`.

Variaveis PRD que devem ser preenchidas manualmente no Render:

```env
MONGO_URI=
APP_ADMIN_ALLOWED_EMAILS=
APP_SECURITY_DOCUMENT_HASH_PEPPER=
MERCADO_PAGO_ACCESS_TOKEN=
MERCADO_PAGO_WEBHOOK_SECRET=
MAILERSEND_API_KEY=
MAILERSEND_TEMPLATE_ID_DEFAULT=
MAILERSEND_TEMPLATE_ID_EMAIL_VERIFICATION=
MAILERSEND_TEMPLATE_ID_PASSWORD_RESET=
MAILERSEND_TEMPLATE_ID_OFFER_RECEIVED=
MAILERSEND_TEMPLATE_ID_CONVERSATION_MESSAGE=
MAILERSEND_TEMPLATE_ID_PURCHASE_CONFIRMATION=
MAILERSEND_TEMPLATE_ID_BOOST_ACTIVATED=
OPENAI_API_KEY=
RABBITMQ_HOST=
RABBITMQ_PORT=
RABBITMQ_USERNAME=
RABBITMQ_PASSWORD=
RABBITMQ_VIRTUAL_HOST=
APP_TRUSTED_PROXIES=
```

Variaveis PRD ja versionadas no Blueprint:

```env
SPRING_PROFILES_ACTIVE=prod
APP_CORS_ALLOWED_ORIGINS=https://euprocuro.com,https://www.euprocuro.com
APP_RESET_BASE_URL=https://euprocuro.com
APP_AUTH_COOKIE_DOMAIN=.euprocuro.com
APP_AUTH_COOKIE_SECURE=true
APP_AUTH_COOKIE_SAME_SITE=Lax
APP_AUTH_EXPOSE_RESET_PREVIEW=false
APP_AUTH_EXPOSE_SESSION_TOKEN=false
APP_MONETIZATION_PROVIDER=MERCADO_PAGO_CHECKOUT_PRO
MERCADO_PAGO_SANDBOX=false
APP_EMAIL_PROVIDER=MAILERSEND_API
APP_EMAIL_FROM=no-reply@euprocuro.com
APP_EMAIL_FROM_NAME=Eu Procuro
APP_EMAIL_APP_URL=https://euprocuro.com
APP_EMAIL_TERMS_URL=https://euprocuro.com/legal/termos-de-uso
APP_EMAIL_PRIVACY_URL=https://euprocuro.com/legal/politica-de-privacidade
APP_EMAIL_SUPPORT_URL=mailto:suporte@euprocuro.com
APP_OPENAI_MODERATION_ENABLED=true
APP_OPENAI_MODERATION_MODEL=omni-moderation-latest
```

Adicione o custom domain `api.euprocuro.com` no servico PRD e `api-hml.euprocuro.com` no servico HML. Use os valores de DNS exibidos pelo Render na Cloudflare.

## Integracoes

MongoDB Atlas:

- Crie usuario exclusivo de PRD.
- Use database separado, por exemplo `euprocuro`.
- Ative Cloud Backups antes de abrir para usuarios reais.
- Faca um teste de restore para outro database/projeto antes do lancamento publico.

LavinMQ/RabbitMQ:

- Use vhost e usuario exclusivos para PRD.
- Configure filas duraveis.
- Nao reutilize credenciais de HML.

Mercado Pago:

- Use credenciais de producao.
- Configure `MERCADO_PAGO_SANDBOX=false`.
- Configure webhook para `https://api.euprocuro.com/api/monetization/mercado-pago/webhook`.
- Mantenha `MERCADO_PAGO_WEBHOOK_SIGNATURE_REQUIRED=true`.

MailerSend:

- Valide o dominio `euprocuro.com`.
- Configure SPF, DKIM, Return-Path e DMARC no DNS.
- Use `no-reply@euprocuro.com` como remetente.

OpenAI:

- Use chave separada de HML.
- Mantenha `APP_OPENAI_MODERATION_ENABLED=true`.
- Monitore custo e erros de moderacao nos logs de integracao.

## Checklist de release

Antes do deploy:

```bash
cd backend
mvn -B test

cd ../frontend
npm run build
npm audit --omit=dev
```

Smoke test em HML:

- Cadastro com e-mail permitido.
- Login.
- Criacao de interesse.
- Oferta.
- Chat WebSocket.
- Reset/verificacao por e-mail.
- Pagamento sandbox ou mock.

Smoke test em PRD:

- `https://api.euprocuro.com/actuator/health` responde `UP`.
- Frontend carrega em `https://euprocuro.com`.
- Cadastro e login criam cookie HTTP-only seguro.
- CORS aceita apenas `https://euprocuro.com` e `https://www.euprocuro.com`.
- E-mail chega com dominio autenticado.
- Webhook do Mercado Pago valida assinatura.
- Backup Atlas esta ativo.

## Monitoramento inicial

Render Starter deve ser acompanhado nos primeiros dias:

- memoria usada;
- tempo de boot;
- latencia p95;
- erros 5xx;
- falhas de webhook;
- falhas de envio de e-mail;
- fila Rabbit acumulada.

Se a API ficar apertada em 512 MB, a primeira acao e subir o plano do servico Render. Se o custo ficar ruim, migre apenas o backend para DigitalOcean App Platform, Fly.io ou VPS com Coolify, mantendo Cloudflare, Vercel e Atlas.
