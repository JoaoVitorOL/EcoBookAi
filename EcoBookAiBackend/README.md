# EcoBookAiBackend

Backend Spring Boot do EcoBook AI.

## Stack

- Java 21+
- Spring Boot 3.2
- PostgreSQL
- Flyway
- Spring Security + JWT
- Auth local por email e senha com hash forte no servidor

## O que existe hoje

- Auth local com `email + senha + JWT`, hashing forte no servidor e endpoints de perfil do usuario
- Fluxo de materiais com `preview`, `create`, `search`, `list me`, `update` e `delete`
- Fluxo de solicitacoes com `create`, `list minhas`, `list pendentes`, `list aprovadas`, `approve`, `decline`, `cancel` e `complete`
- Expiracao automatica de reservas aprovadas
- Registro de token FCM em `POST /api/v1/fcm/tokens` e disparo de notificacoes com payload de navegacao para Android quando o Firebase estiver configurado
- Contratos de runtime atualizados em `specs/001-ecobook-core/contracts/`

## Requisitos

- Java 21+
- Maven 3.9+ instalado no sistema
- Docker Desktop opcional, mas recomendado para o PostgreSQL

## Banco de dados

Na raiz do repositorio:

```powershell
docker compose up -d postgres
```

## Como rodar

Na pasta `EcoBookAiBackend`:

```powershell
mvn spring-boot:run
```

Para testes com o emulador Android usando `http://10.0.2.2:8080/api`, prefira iniciar o backend no host Windows. Quando o Spring Boot roda dentro do WSL, o relay de localhost pode ficar visivel apenas em `::1`, e o emulador passa a enxergar o servidor como indisponivel.

## Como validar a saude

Com o backend rodando:

```text
http://localhost:8080/api/v1/health
```

Se voce quiser confirmar que o backend tambem esta acessivel por IPv4 para o emulador, valide:

```text
http://127.0.0.1:8080/api/v1/health
```

## Variaveis importantes

- `DB_PASSWORD`
- `JWT_SECRET`
- `SERVER_PORT`
- `GEMINI_API_KEY`
- `FIREBASE_SERVICE_ACCOUNT_PATH`

## Fonte de verdade dos endpoints

Para o shape atual das respostas e dos endpoints entregues, use estes contratos:

- `specs/001-ecobook-core/contracts/user-api.md`
- `specs/001-ecobook-core/contracts/material-api.md`
- `specs/001-ecobook-core/contracts/solicitacao-api.md`

## Gemini no ambiente local

O backend so habilita a classificacao assistida quando `GEMINI_API_KEY` existe no processo que inicia o Spring Boot.

Se voce rodar no Windows host:

```powershell
$env:GEMINI_API_KEY="sua-chave"
mvn spring-boot:run
```

Se voce rodar no WSL:

```bash
export GEMINI_API_KEY="sua-chave"
mvn spring-boot:run
```

Importante:

- definir `GEMINI_API_KEY=...` sem `export` no shell do WSL nao basta; o Maven e o Spring nao herdam essa variavel
- uma chave configurada no WSL nao aparece automaticamente no `PowerShell` do Windows
- o modelo padrao `gemini-2.5-flash` nao aceita `google_search` combinado com `responseMimeType=application/json` nesse fluxo; por isso o backend passa a desligar grounding automaticamente fora dos modelos `gemini-3*` para evitar `HTTP 400` da API

## Regra atual de autenticacao

- Cadastro e login usam `email + senha`.
- A senha nunca deve ser salva em texto puro; o backend deve persistir apenas `password_hash`.
- O backend emite JWT apos `register` e `login`.
- O app deve limpar a sessao local ao receber `401`.

## Observacao importante

Neste ciclo, a suite do backend nao foi revalidada nesta maquina.

Detalhe importante sobre a infraestrutura de teste atual:

- a suite tenta usar Testcontainers PostgreSQL primeiro
- se o Docker engine local estiver novo demais para a versao atual do cliente Testcontainers do projeto, o bootstrap cai automaticamente para um banco `ecobook_test` criado no PostgreSQL do `docker compose`
- por isso, antes de `mvn test`, mantenha `docker compose up -d postgres` ativo na raiz do repositorio, ou configure explicitamente `ECOBOOK_TEST_DB_*`
- se o Maven estiver rodando com Java 17 no host Windows, a suite atual pode falhar porque o backend esta alinhado com Java 21
