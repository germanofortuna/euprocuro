# E2E Smoke Tests

Playwright smoke tests para validar páginas públicas e endpoints públicos após deploy em HML e PRD.

## Rodando localmente

```bash
cd e2e
npm install
npx playwright install --with-deps chromium

# contra ambiente local (backend em :8080 e frontend em :5173)
npm run test:smoke

# contra HML
E2E_FRONTEND_URL=https://hml.euprocuro.com \
E2E_API_URL=https://api-hml.euprocuro.com \
npm run test:smoke

# contra PRD
E2E_FRONTEND_URL=https://euprocuro.com \
E2E_API_URL=https://api.euprocuro.com \
npm run test:smoke

# abrir o relatório
npm run report
```

## CI

- Push em `hml` dispara [`.github/workflows/e2e-hml.yml`](../.github/workflows/e2e-hml.yml).
- Push em `main` dispara [`.github/workflows/e2e-prd.yml`](../.github/workflows/e2e-prd.yml).

Ambos esperam o Render terminar o deploy (polling no healthcheck), rodam os testes e fazem upload do relatório como artifact. Se algum teste falhar, é disparado um e-mail via MailerSend.

## Secrets necessários no GitHub

Em `Settings → Secrets and variables → Actions` do repositório:

| Nome | Descrição |
| --- | --- |
| `MAILERSEND_API_KEY` | Mesma chave usada pelo backend (já existente em PRD) |
| `E2E_ALERT_TO_EMAIL` | E-mail que recebe o alerta de falha |
| `E2E_ALERT_FROM_EMAIL` | Remetente (ex.: `no-reply@euprocuro.com`) |
| `E2E_FRONTEND_URL_HML` | Ex.: `https://hml.euprocuro.com` |
| `E2E_API_URL_HML` | Ex.: `https://api-hml.euprocuro.com` |
| `E2E_FRONTEND_URL_PRD` | Ex.: `https://euprocuro.com` |
| `E2E_API_URL_PRD` | Ex.: `https://api.euprocuro.com` |

## O que é testado

### Páginas públicas (frontend SSR)
- Home `/` renderiza com brand e conteúdo
- `/categorias`, `/como-funciona`, `/ouvidoria`
- `/legal/termos-de-uso`
- `/robots.txt`, `/sitemap.xml`
- `/interesses/<id-inválido>` mostra "não encontrada" sem crashar
- Listagem **não** contém links `sample-N` (regressão do bug de fallback mock)

### API pública (backend)
- `/actuator/health` retorna UP
- `/api/categories` retorna categorias sem auth
- `/api/operational/public` retorna 200 sem auth (regressão do 401)
- `/api/interests` retorna array sem auth
- `/api/interests/<bogus>` retorna 404
- `/api/dashboard` rejeita anônimo com 401

## Roadmap (Fase 2)

Próximo passo é adicionar testes de fluxo completo só em HML (`e2e/flows/`):
- Registro → login → cadastrar procura → listar
- Pré-requisito: `APP_AUTH_EMAIL_VERIFICATION_REQUIRED=false` no HML e captcha desligado.
