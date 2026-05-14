# Prompt para gerar imagens do novo layout do Eu Procuro

Use este prompt apenas para criar imagens, mockups visuais ou conceitos de interface do novo frontend do Eu Procuro. Não gere código. Não altere backend. Não invente novas funcionalidades. O objetivo é visualizar direções de layout para depois implementar em Next.js.

## Prompt principal

Crie imagens de layout para o novo frontend do **Eu Procuro**, um marketplace reverso brasileiro.

No Eu Procuro, o comprador publica o que está procurando e recebe propostas de vendedores ou prestadores. O vendedor encontra pessoas que já declararam uma necessidade real e pode responder com uma proposta. A plataforma também permite cadastrar itens/serviços disponíveis, encontrar procuras compatíveis, conversar com compradores, comprar créditos, assinar plano Pro, impulsionar procuras com boost, acessar páginas legais, enviar manifestações para ouvidoria e gerenciar moderação/admin.

O layout deve comunicar claramente:

- "O que você procura hoje?"
- "Encontre pessoas procurando o que você oferece."
- Marketplace reverso: primeiro vem a necessidade do comprador, depois a proposta do vendedor.
- Confiança, simplicidade, clareza e foco em conversão.
- Boa indexação no Google por páginas de categoria.

## Identidade visual

- Cor principal obrigatória: **azul**.
- Criar versões em **tema claro** e **tema escuro**.
- O azul deve aparecer em botões principais, links importantes, estados ativos, destaques, badges e elementos de marca.
- Evite uma interface monocromática. Combine azul com neutros, superfícies limpas, estados de sucesso/alerta/erro e contraste acessível.
- Aparência moderna, confiável, brasileira, de produto digital sério.
- Não usar visual infantil, exageradamente futurista ou com excesso de gradientes.
- Interface responsiva, com versões desktop e mobile.
- Usar cards apenas quando fizer sentido para itens, propostas, modais e painéis; evitar telas confusas com cards demais.

## Estilo desejado

Visual de SaaS/marketplace moderno, limpo e organizado.

Direção:

- Header claro, com logo, navegação principal, login/cadastro, alternância de tema e, quando logado, notificações/créditos/perfil.
- Home pública com proposta de valor forte e imediata.
- Busca e filtros bem visíveis para procuras: texto, categoria, cidade e orçamento.
- Cards de procuras com título, categoria, cidade, valor estimado, tags, status de boost e imagem opcional.
- Detalhe da procura com descrição, localização, categoria, tags, botão de proposta e ações de compartilhamento.
- Páginas de categoria com título SEO, texto introdutório curto, filtros e lista de procuras daquela categoria.
- Áreas privadas mais densas e operacionais, priorizando escaneabilidade.
- Admin com layout de painel, seco e funcional.

## Telas que devem ser geradas

Gere mockups para as seguintes telas:

1. **Home pública desktop**
   - Hero explicando o marketplace reverso.
   - CTA principal: publicar procura.
   - CTA secundário: entrar/cadastrar como vendedor.
   - Busca/filtros.
   - Lista de procuras recentes.
   - Área de detalhe da procura selecionada.

2. **Home pública mobile**
   - Hero compacto.
   - Busca acessível.
   - Cards empilhádos.
   - Detalhe abrindo abaixo ou como painel dedicado.

3. **Página de categoria desktop**
   - Exemplo: "Serviços".
   - Título indexável.
   - Texto explicando que pessoas estão procurando serviços e vendedores podem responder.
   - Filtros por cidade, texto e orçamento.
   - Lista de procuras da categoria.
   - CTA para publicar uma procura nessa categoria.

4. **Página de detalhe de procura**
   - Título da procura.
   - Categoria, cidade/UF, tags, imagem opcional.
   - Descrição clara.
   - CTA para enviar proposta.
   - Estado para visitante com login/cadastro.
   - Ações de compartilhar.

5. **Dashboard logado**
   - Cards de estatísticas: minhas procuras, propostas enviadas, propostas recebidas.
   - Navegação privada: explorar, publicar procura, minhas procuras, propostas enviadas, propostas recebidas, meus itens.
   - Feed ou workspace com área principal e painel lateral.

6. **Formulário de publicar procura**
   - Campos: título, descrição, categoria, tags, orçamento mínimo/máximo, CEP, cidade, UF, bairro, condição preferida, modo de contato, imagem de referência.
   - Indicadores de limite de caracteres.
   - Aviso de que links não são permitidos.
   - Nota de expiração/renovação.

7. **Tela de propostas e conversa**
   - Lista de propostas enviadas/recebidas.
   - Modal ou painel de conversa.
   - Dados do contato quando permitido.
   - Campo de mensagem.

8. **Meus itens / matches**
   - Lista de itens cadastrados pelo vendedor.
   - Contador de pessoas procurando algo parecido.
   - Cards de matches.
   - Formulário curto para compartilhar item como proposta.

9. **Créditos e plano**
   - Saldo atual.
   - Pacotes de créditos.
   - Plano Pro.
   - Histórico de pagamentos.
   - Botão Mercado Pago.

10. **Admin**
    - Fila de moderação.
    - Regras de moderação.
    - Denúncias.
    - Conteúdo/CRM.
    - Catálogo operacional.
    - Ouvidoria.

## Estados importantes para mostrar

Inclua variações ou pequenos detalhes visuais para:

- Usuário visitante.
- Usuário logado sem créditos.
- Usuário logado com créditos.
- Plano Pro ativo.
- Procura com boost ativo.
- Procura pendente de moderação.
- Procura rejeitada.
- Procura expiring sóon, faltando poucos dias.
- Lista vazia com empty staté.
- Tema claro.
- Tema escuro.

## Regras visuais de conteúdo

Não use textos genericos demais como "Lorem ipsum" nas áreas principais. Use exemplos realistas em portugues.

Exemplos de procuras:

- "Procuro eletricista para instalação residencial"
- "Procuro notebook usado para estudar"
- "Procuro professór particular de violão"
- "Procuro apartamento para alugar em Campinas"
- "Procuro mecânico para revisão de carro"

Exemplos de categorias:

- Automoveis
- Imoveis
- Serviços
- Eletrônicos
- Instrumentos
- Outros

Exemplos de CTAs:

- "Publicar uma procura"
- "Responder procuras"
- "Enviar proposta"
- "Cadastrar item disponível"
- "Comprar créditos"

## Instrucoes negativas

Não criar landing page puramente institucional sem a experiência do produto.
Não esconder a lista de procuras.
Não fazer visual de é-commerce tradicional onde vendedores publicam produtos primeiro.
Não criar novas funcionalidades fora do escopo.
Não usar paleta principal roxa, laranjá, bege ou verde. O azul deve ser dominante.
Não criar telas apenas conceituais sem componentes reais de produto.
Não usar excesso de cards decorativos.
Não usar textos explicando como a UI funciona dentro da própria UI.
Não colocar admin com visual de marketing; admin deve ser funcional e denso.

## Formato das imagens desejádas

Gerar imagens em alta fidelidade visual, formato de mockup de produto digital.

Para cada tela, gerar:

- Desktop 1440x1024.
- Mobile 390x844 quando fizer sentido.
- Uma versão em tema claro.
- Uma versão em tema escuro.

As imagens devem parecer prontas para orientar um designer ou desenvolvedor frontend na implementação em Next.js.

## Prompt curto alternativo

Crie mockups de alta fidelidade para o novo frontend do Eu Procuro, um marketplace reverso brasileiro onde compradores publicam o que procuram e vendedores enviam propostas. A cor principal deve ser azul, com tema claro e escuro. Mostre home pública, páginas de categoria para SEO, detalhe da procura, dashboard logado, formulário de publicar procura, propostas/conversa, meus itens/matches, créditos/plano e admin. O visual deve ser moderno, confiável, responsivo, claro e orientado a produto real, sem parecer é-commerce tradicional. Não gere código; gere apenas imagens do layout.





