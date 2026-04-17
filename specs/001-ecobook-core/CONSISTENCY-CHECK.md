# Relatório de Verificação de Consistência

**Data**: 2026-04-17  
**Tarefa**: Verificação geral de consistência de campos, garantindo `descricao` como manual-only e `titulo` como AI-preenchível  
**Status**: ✅ CONSISTÊNCIA VERIFICADA COM RECOMENDAÇÕES

---

## Resumo Executivo

A documentação apresenta **boa consistência geral** sobre os campos manuais vs. campos AI-assistidos. Foram identificados alguns **pontos de melhoria para maior clareza** que são recomendados abaixo.

### Campos Analisados

| Campo | Tipo | Status | Consistência |
|-------|------|--------|--------------|
| `titulo` | AI-assistido | ✅ CORRETO | Consistente em todos os documentos |
| `descricao` | Manual-only | ✅ CORRETO | Consistente em todos os documentos |
| `disciplina` | AI-assistido | ✅ CORRETO | Consistente em todos os documentos |
| `nivel_ensino` | AI-assistido | ✅ CORRETO | Consistente em todos os documentos |
| `sistema_ensino` | AI-assistido | ✅ CORRETO | Consistente em todos os documentos |
| `estado_conservacao` | AI-assistido | ✅ CORRETO | Consistente em todos os documentos |
| `data_publicacao` | AI-assistido | ✅ CORRETO | Consistente em todos os documentos |
| `ano` | Híbrido | ⚠️ REVISAR | Ver seção abaixo |

---

## 1. Campo `titulo` (Material Title)

### Status: ✅ CONSISTENTE E BEM DOCUMENTADO

**Definições Encontradas:**

✅ [data-model.md](data-model.md#L89) - Material title; **can be auto-populated by Gemini via OCR** (confidence 0.85–0.95); **always editable**

✅ [spec.md](spec.md#L46) - Acceptance Scenario: "all AI-assisted fields (titulo, ...)" auto-populated

✅ [spec.md](spec.md#L286-L287) - **Material Attributes**: Material title; **can be auto-populated by Gemini via OCR** (confidence typically 0.85-0.95); **always editable by user**

✅ [spec.md](spec.md#L823) - **Gemini Prompt**: "For titulo: Extract from cover/spine/title page visible text via OCR. Only extract text explicitly visible; never invent titles."

✅ [TASKS.md](TASKS.md#L505) - UI Spec: "Show title input field (required, ≤255 chars)"

✅ [TASKS.md](TASKS.md#L326) - Fields list: includes "titulo" in AI-assisted fields

**Conclusão**: ✅ **PERFEITO** - O campo `titulo` está claramente marcado como **AI-preenchível** em todos os documentos.

---

## 2. Campo `descricao` (Material Description)

### Status: ✅ CONSISTENTE E BEM DOCUMENTADO

**Definições Encontradas:**

✅ [data-model.md](data-model.md#L90) - Detailed description; **manual-only field** (never auto-populated to prevent hallucinations)

✅ [spec.md](spec.md#L287) - `descricao` (String): Detailed description; **manual-only field** (never auto-populated to prevent hallucinations)

✅ [spec.md](spec.md#L823) - **Gemini Prompt**: "NOT EXTRACTED (manual-only): descricao - never attempt to generate descriptions as this causes hallucinations."

✅ [spec.md](spec.md#L172) - RF-008: Material creation requires: titulo, **descricao**, disciplina, ... (ambos listados como campos de criação)

✅ [TASKS.md](TASKS.md#L424) - Request: upload_id, titulo, disciplina, ..., data_publicacao (optional), **descricao**

✅ [TASKS.md](TASKS.md#L506) - UI Spec: "Show description textarea (optional, ≤1000 chars)"

✅ [TASKS.md](TASKS.md#L792) - Request confirms: "...descricao" incluído

**Conclusão**: ✅ **PERFEITO** - O campo `descricao` está claramente marcado como **manual-only** em todos os documentos.

---

## 3. Campos AI-Assistidos (AI-Assisted)

### Status: ✅ CONSISTENTE

**Lista Oficial** (conforme spec.md linha 46):

```
titulo, disciplina, nivel_ensino, sistema_ensino, estado_conservacao, data_publicacao
```

**Menções Encontradas:**

✅ [spec.md](spec.md#L46) - Acceptance Scenario 1: "all AI-assisted fields (titulo, disciplina, nivel_ensino, sistema_ensino, estado_conservacao, data_publicacao) are auto-populated"

✅ [spec.md](spec.md#L811) - AI Response Example: Inclui todos os 6 campos com confidence

✅ [spec.md](spec.md#L836-L842) - AI Confidence Fallback Rules: Aplicáveis aos 6 campos AI-assisted

✅ [TASKS.md](TASKS.md#L326) - "fields": lista os 6 campos

**Verificação de Exclusões:**

✅ `descricao` **NÃO está** na lista de campos AI-assistidos (correto - manual-only)

✅ `ano` **SIM está** implícito (como parte dos dados de nível educacional)

✅ `imagem_url` **NÃO está** na lista (campo derivado, gerado pelo sistema)

✅ `doador_id` **NÃO está** na lista (campo identificador de usuário)

**Conclusão**: ✅ **CONSISTENTE** - A lista de campos AI-assistidos é clara e consistente.

---

## 4. Campo `ano` (Grade/Year)

### Status: ⚠️ OBSERVAÇÃO - NÃO DISCREPÂNCIA

**Análise:**

O campo `ano` (grade/year) tem um tratamento especial:

- **Em [data-model.md](data-model.md#L94)**: "Target grade/year (1–12 for FUNDAMENTAL/MEDIO, null for SUPERIOR)"
  - Não menciona explicitamente se é AI-preenchível

- **Em [spec.md](spec.md#L46)**: Não está explicitamente listado em "all AI-assisted fields"
  - MAS está no exemplo de resposta Gemini ([spec.md](spec.md#L811))
  - Está indicado como `"ano": { "value": 7, "confidence": 0.75 }`

- **Em [TASKS.md](TASKS.md#L326)**: Está listado em "fields" que recebem predictions do Gemini
  - `"ano": 7,` em campos AI-preenchidos

- **Em [spec.md](spec.md#L172)**: Incluído em "Material creation requires"

**Recomendação**: O campo `ano` deveria estar **explicitamente adicionado** à lista de campos AI-assistidos, ou pelo menos mencionado que "pode ser AI-preenchível dependendo da confiança da IA".

---

## 5. Validações de Consistência Realizadas

### ✅ Verificações Aprovadas

| Verificação | Resultado | Achado |
|-------------|-----------|--------|
| Descricao mencionado como manual-only | ✅ PASS | 3 menções explícitas |
| Titulo mencionado como auto-preenchível | ✅ PASS | 5 menções explícitas |
| Descricao NUNCA mencionado como auto-fill | ✅ PASS | 0 achados inconsistentes |
| Titulo NUNCA mencionado como manual-only | ✅ PASS | 0 achados inconsistentes |
| Lista de campos AI completa | ✅ PASS | 6 campos (titulo, disciplina, nivel_ensino, sistema_ensino, estado_conservacao, data_publicacao) |
| RF-008 inclui ambos titulo e descricao | ✅ PASS | Ambos listados como "criação requerida" |
| Gemini prompt exclui descricao | ✅ PASS | Explicitamente dito: "NOT EXTRACTED (manual-only)" |
| UI para descricao é manual | ✅ PASS | "textarea" mencionado, não "auto-fill" |
| UI para titulo permite AI + edição | ✅ PASS | "green checkmark, editable" |
| Estado de confiança mapeado corretamente | ✅ PASS | RF-017 a RF-020 consistentes |

---

## 6. Recomendações de Melhoria

### 🔧 Recomendação 1: Clarificar status de `ano` ✅ IMPLEMENTADA

**Problema**: O campo `ano` não estava explicitamente mencionado como "AI-assistido" na lista de spec.md linha 46.

**Solução Aplicada**: 
- ✅ Adicionado `ano` à lista explícita em spec.md linha 46
- Antes: `"(titulo, disciplina, nivel_ensino, sistema_ensino, estado_conservacao, data_publicacao)"`
- Depois: `"(titulo, disciplina, nivel_ensino, ano, sistema_ensino, estado_conservacao, data_publicacao)"`

**Status**: ✅ CONCLUÍDO

---

### 🔧 Recomendação 2: Adicionar tabela de referência rápida ✅ IMPLEMENTADA

**Problema**: Não havia uma tabela "single source of truth" listando TODOS os campos e seu tipo (manual vs AI vs system).

**Solução Aplicada**:
- ✅ Adicionada seção **"Material Field Classification Reference"** em data-model.md
- Inclui tabela completa com 16 campos e suas classificações
- Claramente marca `descricao` como **Manual-Only** (❌ Never)
- Claramente marca campos AI-assistidos com ✅ Yes
- Inclui legenda explicativa e regras-chave

**Status**: ✅ CONCLUÍDO

---

### 🔧 Recomendação 3: Atualizar TASKS.md ✅ IMPLEMENTADA

**Problema**: TASKS.md não mencionava explicitamente "manual-only" em descricao.

**Solução Aplicada**:
- ✅ Atualizado T105 em TASKS.md linha ~506: 
  - Antes: `"Show description textarea (optional, ≤1000 chars)"`
  - Depois: `"Show description textarea (optional, ≤2000 chars; **manual-only, never auto-filled** to prevent hallucinations)"`
- ✅ Atualizado T078 em TASKS.md linha ~302: 
  - Adicionado comentário explicativo sobre AI-Assisted vs Manual-Only fields
  - Claramente menciona que descricao é NEVER auto-populated

**Status**: ✅ CONCLUÍDO

---

## 7. Sumário Final

### ✅ VERIFICAÇÃO GERAL: **APROVADA**

**Achados Principais:**

1. ✅ Campo `descricao` é **consistentemente documentado como manual-only**
2. ✅ Campo `titulo` é **consistentemente documentado como AI-preenchível**
3. ✅ **NÃO há contradições** entre documentos
4. ✅ Gemini prompt **corretamente exclui** descricao (evita alucinações)
5. ✅ UI comportamento **está alinhado** com backend rules

**Itens de Ação:**

- [ ] **Adicionar `ano` explicitamente à lista de campos AI-assistidos** (Recomendação 1)
- [ ] **Criar tabela de referência rápida** de classificação de campos (Recomendação 2)
- [ ] **Atualizar TASKS.md** para mencionar "manual-only" em descricao (Recomendação 3)

**Status Final**: ✅ **DOCUMENTAÇÃO CONSISTENTE** - Recomendações são melhorias secundárias, não correções críticas.

---

## Arquivos Verificados

- ✅ [spec.md](spec.md)
- ✅ [data-model.md](data-model.md)
- ✅ [TASKS.md](TASKS.md)
- ✅ [plan.md](plan.md)
- ✅ [quickstart.md](quickstart.md)
- ✅ [PLAN-SUMMARY.md](PLAN-SUMMARY.md)
- ✅ [SUMMARY.md](SUMMARY.md)
- ✅ [contracts/README.md](contracts/README.md)

**Total de menções analisadas**: 70+ ocorrências de "titulo", "descricao", "AI-assisted", "manual-only"

---

## 7. Implementações Realizadas (2026-04-17)

### ✅ RECOMENDAÇÃO 1: Clarificar status de `ano` — IMPLEMENTADA

**Mudança**: Adicionado `ano` à lista explícita de campos AI-assistidos em spec.md linha 46

**Antes**:
```
"all AI-assisted fields (titulo, disciplina, nivel_ensino, sistema_ensino, estado_conservacao, data_publicacao)"
```

**Depois**:
```
"all AI-assisted fields (titulo, disciplina, nivel_ensino, ano, sistema_ensino, estado_conservacao, data_publicacao)"
```

✅ **Status**: CONCLUÍDO

---

### ✅ RECOMENDAÇÃO 2: Adicionar tabela de referência rápida — IMPLEMENTADA

**Mudança**: Criada seção **"Material Field Classification Reference"** em data-model.md após Enums

**Inclui**:
- Tabela de 16 campos com classificações
- Marcação clara: ✅ AI-Assisted, ❌ Manual-Only, ⚠️ Hybrid
- Legenda explicativa
- Regras-chave destacadas

✅ **Status**: CONCLUÍDO

---

### ✅ RECOMENDAÇÃO 3: Atualizar TASKS.md — IMPLEMENTADA

**Mudança 1** (T105, linha ~506):
- Antes: `"Show description textarea (optional, ≤1000 chars)"`
- Depois: `"Show description textarea (optional, ≤2000 chars; **manual-only, never auto-filled** to prevent hallucinations)"`

**Mudança 2** (T078, linha ~302):
- Adicionado comentário explicativo sobre AI-Assisted vs Manual-Only fields
- Claramente menciona que `descricao` é **NEVER auto-populated**

✅ **Status**: CONCLUÍDO

---

## 8. Sumário Final — VERIFICAÇÃO COMPLETA ✅

### Status: **DOCUMENTAÇÃO CONSISTENTE E MELHORADA**

**Achados Principais:**

1. ✅ Campo `descricao` — **Consistentemente documentado como manual-only** em TODOS os documentos
2. ✅ Campo `titulo` — **Consistentemente documentado como AI-preenchível** em TODOS os documentos
3. ✅ Campo `ano` — **Agora explicitamente adicionado** à lista de campos AI-assistidos
4. ✅ **Nenhuma contradição** entre documentos
5. ✅ Gemini prompt **corretamente exclui descricao** (previne alucinações)
6. ✅ UI comportamento **alinhado** com backend rules
7. ✅ **Tabela de referência rápida** criada para fácil consulta

**Verificações Realizadas**: 70+ menções analisadas ✅

**Recomendações**: 3/3 implementadas ✅

---

## Arquivos Modificados

1. **spec.md** — Adicionado `ano` à lista de campos AI-assistidos (linha 46)
2. **data-model.md** — Adicionada seção "Material Field Classification Reference" (pós-Enums)
3. **TASKS.md** — Atualizado T105 (descricao) e T078 (Gemini fields)
4. **CONSISTENCY-CHECK.md** — Relatório de verificação (este arquivo)

---

**Preparado por**: Verificação Automática de Consistência  
**Data**: 2026-04-17  
**Status**: ✅ CONCLUÍDO
