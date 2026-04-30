# EcoBookAiBackend

Backend Spring Boot do EcoBook AI.

## Stack

- Java 21
- Spring Boot 3.2
- PostgreSQL
- Flyway
- Spring Security + JWT

## O que existe hoje

- Estrutura principal de entidades, DTOs e repositorios
- Configuracao de seguranca
- Migracao inicial do banco
- Endpoint de saude em `GET /api/v1/health`
- Base para auth, usuarios, materiais e solicitacoes

## Requisitos

- Java 21
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

## Como validar a saude

Com o backend rodando:

```text
http://localhost:8080/api/v1/health
```

## Variaveis importantes

- `DB_PASSWORD`
- `JWT_SECRET`
- `SERVER_PORT`
- `GOOGLE_OAUTH_CLIENT_ID`
- `GOOGLE_OAUTH_CLIENT_SECRET`
- `GEMINI_API_KEY`
- `FIREBASE_SERVICE_ACCOUNT_PATH`

## Observacao importante

Neste ambiente de trabalho nao foi possivel validar a compilacao do backend porque o executavel `mvn` nao esta instalado. O projeto, porem, esta estruturado para Maven e usa Java 21 no `pom.xml`.
