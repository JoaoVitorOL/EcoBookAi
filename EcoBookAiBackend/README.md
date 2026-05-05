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

- Estrutura principal de entidades, DTOs e repositorios
- Configuracao de seguranca
- Migracao inicial do banco
- Endpoint de saude em `GET /api/v1/health`
- Base para auth, usuarios, materiais e solicitacoes

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

## Como validar a saude

Com o backend rodando:

```text
http://localhost:8080/api/v1/health
```

## Variaveis importantes

- `DB_PASSWORD`
- `JWT_SECRET`
- `SERVER_PORT`
- `GEMINI_API_KEY`
- `FIREBASE_SERVICE_ACCOUNT_PATH`

## Regra atual de autenticacao

- Cadastro e login usam `email + senha`.
- A senha nunca deve ser salva em texto puro; o backend deve persistir apenas `password_hash`.
- O backend emite JWT apos `register` e `login`.
- O app deve limpar a sessao local ao receber `401`.

## Observacao importante

Neste ambiente de trabalho a validacao local do backend foi executada com `mvn clean test` usando um JDK mais novo do que o Java 17 padrao da sessao. Para rodar os testes de infraestrutura com Docker real via Testcontainers, ainda sera necessario ter um runtime de containers disponivel na maquina.
