# Estudo: CRM/CMS para textos parametrizados em runtime

## Objetivo

Permitir que textos exibidos ao usuário sejam alterados em tempo de execução, sem novo build/deploy, com controle de versão, auditoria, revisão e publicação. Isso inclui labels, CTAs, mensagens de validação, textos institucionais, documentos legais, e-mails e conteúdos de telas.

O primeiro passo já foi dado nos documentos legais: o conteúdo saiu de código JavaScript e foi concentrado em `frontend/src/content/legal-pages.json`. Esse JSON ainda é empacotado no build, então resolve manutenção local, mas não resolve alteração em runtime. Para runtime, o conteúdo precisa sair do bundle e passar por API + persistência + cache.

## Moderação não é o lugar ideal

A aba de moderação atual deve continuar focada em conteúdo criado por usuários, denúncias e aprovação/reprovação de publicações. Misturar isso com textos oficiais da plataforma cria risco operacional:

- permissões diferentes: moderador não necessariamente pode alterar Termos de Uso;
- ciclos diferentes: denúncia exige fila e decisão; texto institucional exige rascunho, revisão e publicação;
- impacto diferente: uma alteração ruim em texto legal, botão de cadastro ou mensagem de erro afeta toda a plataforma;
- auditoria diferente: documentos legais precisam de versão, data de vigência e histórico de aceite.

Recomendação: criar uma área administrativa separada, por exemplo `Admin > Conteúdo`, com permissões próprias.

## Conceito parecido com Oracle Endeca/BCC

O paralelo com Endeca/BCC faz sentido principalmente em quatro ideias:

- chaves de conteúdo: cada texto tem uma chave estável, como `auth.register.terms.acceptance`;
- slots/placements: blocos de tela podem buscar conteúdos por posição, como `home.hero.title` ou `footer.legal.links`;
- publicação: alterações passam por rascunho, revisão, preview e publicação;
- versionamento: cada publicação gera uma versão rastreável e reversível.

Não é necessário recriar um Endeca completo agora. O ideal é começar com um CMS interno pequeno, orientado a chave/valor versionado, e evoluir para regras, segmentação e preview quando houver necessidade real.

## Modelo de domínio sugerido

Novo contexto: `Content Management`.

Entidade principal: `content_entries`.

Campos recomendados:

- `id`
- `key`: identificador estável, exemplo `auth.register.submit`
- `type`: `TEXT`, `RICH_TEXT`, `LEGAL_DOCUMENT`, `LABEL`, `CTA`, `ERROR_MESSAGE`, `EMAIL_TEMPLATE`
- `locale`: exemplo `pt-BR`
- `status`: `DRAFT`, `PUBLISHED`, `ARCHIVED`
- `version`
- `value`: texto simples, Markdown ou JSON estruturado
- `description`: ajuda interna para editores
- `screen`: tela ou área, exemplo `auth`, `home`, `footer`, `legal`
- `startsAt` e `endsAt`: vigência opcional
- `updatedBy`, `updatedAt`, `publishedBy`, `publishedAt`

Entidade de histórico: `content_revisions`.

Campos recomendados:

- `contentEntryId`
- `version`
- `snapshot`
- `author`
- `changeReason`
- `createdAt`

Para documentos legais, adicionar metadados:

- `legalSlug`: `termos-de-uso`, `privacidade`, `conteudo-proibido`, `denuncia-remocao`
- `effectiveFrom`
- `requiresUserAcceptance`: usado em mudanças relevantes de Termos de Uso

## APIs sugeridas

Públicas:

- `GET /api/content/public?locale=pt-BR&keys=auth.register.submit,footer.terms`
- `GET /api/content/legal/{slug}`
- `GET /api/content/version`

Administrativas:

- `GET /api/admin/content`
- `POST /api/admin/content`
- `PUT /api/admin/content/{id}`
- `POST /api/admin/content/{id}/publish`
- `POST /api/admin/content/{id}/archive`
- `GET /api/admin/content/{id}/revisions`

As rotas públicas devem retornar apenas conteúdo publicado e vigente. As rotas administrativas exigem autenticação e autorização específica.

## Frontend sugerido

Criar um `ContentProvider` que carrega um dicionário de textos no início da aplicação:

- busca `/api/content/public`;
- mantém cache em memória;
- opcionalmente usa `localStorage` como fallback;
- expõe `t("auth.register.submit")`;
- usa conteúdo padrão local se a API estiver fora.

Exemplo de uso futuro:

```jsx
const { t } = useContent();

<button>{t("auth.register.submit", "Criar conta")}</button>
```

Para textos ricos e legais:

- preferir Markdown ou JSON estruturado;
- renderizar com componente controlado;
- bloquear HTML arbitrário para evitar XSS;
- manter fallback local para Termos de Uso, porque cadastro não pode quebrar se a API de conteúdo falhar.

## Chaves iniciais candidatas

Autenticação:

- `auth.login.title`
- `auth.register.title`
- `auth.register.terms.acceptance`
- `auth.register.terms.helper.closed`
- `auth.register.terms.helper.opened`
- `auth.register.submit`
- `auth.password.validation.short`
- `auth.password.validation.invalid`
- `auth.password.validation.valid`

Footer e páginas legais:

- `footer.legal.terms`
- `footer.legal.privacy`
- `footer.legal.prohibitedContent`
- `footer.legal.reportRemoval`
- `legal.terms.document`
- `legal.privacy.document`
- `legal.prohibitedContent.document`
- `legal.reportRemoval.document`

Mensagens de sistema:

- `errors.generic`
- `errors.auth.invalidCredentials`
- `errors.register.termsRequired`
- `feedback.success.created`
- `feedback.error.requiredFields`

## Backend e aceite de termos

O aceite de termos não deve depender apenas do texto no frontend. O backend já deve persistir:

- se o usuário aceitou;
- data/hora do aceite;
- versão aceita.

Quando a plataforma publicar uma versão relevante de Termos de Uso, o conteúdo publicado deve ter uma nova versão. No futuro, o login pode verificar se o usuário aceitou a versão vigente e solicitar novo aceite.

## Cache e publicação

Fluxo recomendado:

1. Editor altera conteúdo como rascunho.
2. Revisor aprova/publica.
3. Backend incrementa versão global de conteúdo.
4. Frontend detecta a nova versão por `ETag`, `contentVersion` ou resposta de `/api/content/version`.
5. Cache local é atualizado sem redeploy.

Para produção, usar cache curto nas rotas públicas e invalidação por versão.

## Segurança e governança

Requisitos mínimos:

- papéis separados: `CONTENT_EDITOR`, `CONTENT_PUBLISHER`, `LEGAL_ADMIN`;
- auditoria de quem alterou e publicou;
- histórico e rollback;
- preview antes da publicação;
- sanitização de Markdown/conteúdo rico;
- logs de alteração em documentos legais;
- impedir exclusão física de documentos legais já aceitos por usuários.

## Plano de implementação incremental

1. Consolidar textos legais fora de JS/Java em JSON versionado local. Status: iniciado.
2. Mapear textos hard-coded do frontend e backend em uma planilha ou JSON de inventário.
3. Criar modelo de conteúdo no backend e endpoints públicos somente leitura.
4. Criar `ContentProvider` no frontend com fallback local.
5. Migrar telas de autenticação, footer e mensagens comuns para `t(key)`.
6. Criar `Admin > Conteúdo` com busca, edição, rascunho e publicação.
7. Adicionar versionamento, preview e workflow de aprovação.
8. Migrar e-mails, mensagens de erro e documentos legais para o módulo.

## Decisão recomendada agora

Não colocar esse gerenciamento dentro da aba de moderação. Criar um módulo administrativo próprio de Conteúdo é mais limpo e reduz risco em produção. Para a próxima etapa técnica, o melhor avanço é implementar o backend de conteúdo publicado + `ContentProvider` com fallback local, começando pelos textos de cadastro, login, footer e páginas legais.
