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
- Fluxo de materiais com `preview`, `create`, `search`, `list me`, `update` e `delete`, incluindo capa frontal obrigatoria e capa traseira opcional no cadastro
- Fluxo de solicitacoes com `create`, `list minhas`, `list pendentes`, `list aprovadas`, `approve`, `decline`, `cancel` e `complete`
- Expiracao automatica de reservas aprovadas
- Registro de token FCM em `POST /api/v1/fcm/tokens`, disparo de notificacoes pos-commit com payload de navegacao para Android e fila persistida de retry para falhas transientes quando o Firebase estiver configurado
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
- `FIREBASE_DATABASE_URL` (opcional)

## Fonte de verdade dos endpoints

Para o shape atual das respostas e dos endpoints entregues, use estes contratos:

- `specs/001-ecobook-core/contracts/user-api.md`
- `specs/001-ecobook-core/contracts/material-api.md`
- `specs/001-ecobook-core/contracts/solicitacao-api.md`

## Firebase para push real

O backend so envia push real quando existe um JSON valido da conta de servico do Firebase Admin SDK.

O app Android deste repositorio esta ligado ao projeto Firebase `ecobook-148f2` via `EcoBookAiAndroid/app/google-services.json`. Gere a chave de service account nesse mesmo projeto.

Passo a passo:

1. Abra o Firebase Console no projeto `ecobook-148f2`.
2. Va em `Project settings` > `Service accounts`.
3. Clique em `Generate new private key`.
4. Salve o JSON com seguranca. Uma opcao local segura para este repo e `EcoBookAiBackend/credentials/ecobook-adminsdk.json`.

O backend aceita duas variaveis de ambiente:

- `FIREBASE_SERVICE_ACCOUNT_PATH`
- `GOOGLE_APPLICATION_CREDENTIALS` (fallback alinhado ao padrao oficial do Firebase Admin SDK)

Sem essa configuracao, a Fase 6 continua compilada e o app ainda registra token/local inbox/deep link, mas o envio backend fica dormente. Agora, quando isso acontecer, o servidor tambem registra a tentativa na fila persistida de retry com motivo explicito.

### Subir com PowerShell

```powershell
.\scripts\Run-BackendWithFirebase.ps1 -ServiceAccountPath .\EcoBookAiBackend\credentials\ecobook-adminsdk.json
```

### Subir com WSL

```bash
chmod +x ./scripts/run-backend-with-firebase.sh
./scripts/run-backend-with-firebase.sh /mnt/c/Users/jvol2/OneDrive/Area\ de\ Trabalho/projIntegrador/EcoBookAi/EcoBookAiBackend/credentials/ecobook-adminsdk.json
```

### Sem script

Windows:

```powershell
$env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\caminho\firebase-adminsdk.json"
$env:GOOGLE_APPLICATION_CREDENTIALS=$env:FIREBASE_SERVICE_ACCOUNT_PATH
mvn spring-boot:run
```

WSL:

```bash
export FIREBASE_SERVICE_ACCOUNT_PATH="/mnt/c/caminho/firebase-adminsdk.json"
export GOOGLE_APPLICATION_CREDENTIALS="$FIREBASE_SERVICE_ACCOUNT_PATH"
mvn spring-boot:run
```

Importante:

- reinicie o backend depois de definir a variavel
- `google-services.json` do Android nao substitui a credencial Admin SDK do servidor
- nao versione o JSON; a pasta `EcoBookAiBackend/credentials/` foi preparada para uso local e o `.gitignore` ja ignora `*.json`

## Flyway via Maven

O `pom.xml` agora deixa `flyway:info`, `flyway:validate` e `flyway:repair` apontando por padrao para o banco local de desenvolvimento:

- URL: `jdbc:postgresql://localhost:5432/ecobook`
- Usuario: `ecobook`
- Senha default local: `dev_password_123`

Exemplos:

```bash
mvn flyway:info
mvn flyway:repair
```

Se o seu ambiente usar outra credencial, voce pode sobrescrever na linha de comando:

```bash
mvn -Dflyway.password=sua_senha flyway:repair
```

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
