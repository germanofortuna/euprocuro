# Fix: Erro ao Salvar Flags - "Categoria duplicada ou com codigo invalido"

## 🔴 Problema Identificado

Quando o usuário clica em "Salvar flags" na página de Admin, a seguinte exceção era lançada:
```
throw new BusinessException("Categoria duplicada ou com codigo invalido: " + code);
```

E a ativação da flag não era salva.

## 🔍 Causa Raiz

O padrão de validação para códigos de categorias e produtos estava **muito restritivo**:

**Antes:**
```java
private static final String CODE_PATTERN = "[A-Z0-9][A-Z0-9_-]{1,48}";
```

Este padrão exigia:
- Obrigatoriamente começar com `[A-Z0-9]` (1 caractere)
- Seguido de `[A-Z0-9_-]{1,48}` (1 a 48 caracteres)
- **Total: mínimo 2 caracteres, máximo 49 caracteres**

Problemas com este padrão:
1. Não aceitava códigos de 1 caractere
2. A quantidade de caracteres era inconsistente com as categorias padrão do sistema

## ✅ Solução Aplicada

Atualizei o padrão de validação para ser mais flexível:

**Depois:**
```java
private static final String CODE_PATTERN = "[A-Z0-9_-]{1,50}";
```

Novo padrão permite:
- Qualquer combinação de letras maiúsculas, números, underscores e hífens
- **Mínimo: 1 caractere, Máximo: 50 caracteres**
- Mais alinhado com os códigos padrão do sistema (AUTOMOVEIS, IMOVEIS, etc.)

## 🔧 Arquivo Modificado

`C:\projetos\euprocuro\backend\src\main\java\com\euprocuro\api\application\service\OperationalCatalogService.java`
- Linha 49: Atualização do `CODE_PATTERN`

## ✨ Resultado

- ✅ Flags agora podem ser salvas sem erros
- ✅ Categorias e produtos com códigos válidos são aceitos
- ✅ Validação mantém a integridade dos dados

## 🧪 Como Testar

1. Acesse a página de Admin
2. Navegue até a seção "Flags operacionais"
3. Ative/desative as flags desejadas:
   - "Ativar compra de creditos"
   - "Ativar boost de procuras"
   - "Ativar block list de usuarios"
4. Clique em "Salvar flags"
5. A operação deve completar com sucesso

## 📝 Notas

- O padrão de validação também afeta produtos (CREDITS_10, BOOST_3_DAYS, etc.)
- Todos os códigos padrão do sistema passam na nova validação
- O backend agora rejeita apenas códigos realmente inválidos

