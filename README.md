# EcoBook AI

Plataforma Android + backend Spring Boot para doacao e solicitacao de materiais de estudo, com classificacao assistida por IA, matching geografico e fluxo transacional entre doador e estudante.

## Status Atual

- Fases 1 a 8 estao implementadas em runtime e a fase 9 esta em andamento.
- O backend cobre autenticacao por `email + senha + JWT`, onboarding, preview IA, publicacao de materiais, discovery, solicitacoes, conclusao de doacao, inbox de notificacoes, moderacao/admin, consentimento LGPD, exclusao de conta, exportacao de dados e acesso autenticado a imagens.
- O app Android cobre login/cadastro, onboarding, doacao, busca, pedidos do estudante, pedidos do doador, central de avisos, controles de consentimento, leitura visivel de termos/privacidade antes do aceite, edicao de perfil (`nome`, `email`, `telefone`, `cidade`, `bairro`, `instituicao`) e fluxo de exclusao de conta com confirmacao destrutiva.
- A fase 9 ja cobre compressao de resposta, metricas Prometheus/Micrometer, smoke tests de backend, cache para leituras autenticadas, catalogo publico/cacheado de dados de referencia, paginacao cursor/keyset na discovery e cleanup transacional no fluxo de material.
- O principal fechamento externo pendente segue sendo a validacao ponta a ponta do push real com Firebase em dispositivo/emulador com Google Play services, junto dos itens finais de hardening de fase 9/10.

## Stack

- Backend: Java 21, Spring Boot 3.2, Spring Security, JWT, JPA/Hibernate, Flyway
- Banco principal: PostgreSQL 15
- Perfil rapido local: H2 em arquivo
- Android: Kotlin, Jetpack Compose, Hilt, Retrofit, Firebase Messaging

## Estrutura Do Repositorio

```text
.
|-- EcoBookAiBackend/        # API REST e regras de negocio
|-- EcoBookAiAndroid/        # App Android nativo
|-- specs/001-ecobook-core/ # Spec kit, contratos e backlog
|-- scripts/                # Scripts auxiliares de backend/Firebase
`-- docker-compose.yml      # PostgreSQL local para o perfil default
```

## Requisitos

- Java 21 ou superior para o backend
- Maven 3.9+
- Android Studio + Android SDK Platform 34
- Android Emulator ou dispositivo fisico para rodar o app
- Docker Desktop opcional para usar o perfil PostgreSQL

## Instalacao Rapida

### 1. Backend

O caminho mais rapido e validado hoje usa o perfil `local`, que sobe o backend com H2 e `Gemini` em modo mock. Ele nao precisa de Docker.

Em um terminal com Java 21:

```bash
cd EcoBookAiBackend
mvn -q -DskipTests compile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Se voce estiver usando Windows PowerShell, prefira esta variante:

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-21-ou-superior'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn --% spring-boot:run -Dspring-boot.run.profiles=local
```

Com o backend no ar, valide:

```bash
curl http://127.0.0.1:8080/api/v1/health
```

Resposta esperada:

```json
{
  "status": 200,
  "message": "Backend online"
}
```

Para a suite automatizada do backend, o caminho atual tambem foi endurecido para funcionar sem Docker: `mvn test` tenta `Testcontainers`, depois PostgreSQL externo, e cai para H2 em memoria quando necessario. O unico prerequisito obrigatorio continua sendo Java 21 ou superior.

### 2. Android

Na pasta `EcoBookAiAndroid`, use `local.properties.example` como base para o seu `local.properties`.

Exemplo:

```properties
sdk.dir=C\:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
```

Depois:

```powershell
cd EcoBookAiAndroid
.\gradlew.bat assembleDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

Se o backend estiver rodando dentro do WSL e o emulador não enxergar `10.0.2.2`, descubra o IP atual do Linux:

```powershell
wsl.exe -e bash -lc "hostname -I"
```

e ajuste `backend.url`, por exemplo:

```properties
backend.url=http://172.22.160.34:8080/api
```

## Como Usar

### Smoke Test Da API

Com o backend `local` rodando, o fluxo abaixo foi validado:

1. `POST /api/v1/auth/register`
2. `GET /api/v1/usuarios/me`
3. `PUT /api/v1/usuarios/me`
4. `GET /api/v1/materiais?page=0&size=5`
5. `POST /api/v1/auth/login`

Endpoints base:

- API: `http://127.0.0.1:8080/api/v1`
- Health: `http://127.0.0.1:8080/api/v1/health`

### Perfil PostgreSQL

O perfil default continua sendo o caminho mais proximo do schema principal com Flyway. Ele espera um PostgreSQL acessivel para o mesmo processo do backend em `localhost:5432/ecobook`.

Na raiz do repositorio:

```powershell
docker compose up -d postgres
```

Depois, em `EcoBookAiBackend`:

```bash
mvn spring-boot:run
```

Esse perfil e indicado quando voce precisa validar migrations e comportamento proximo do ambiente principal. Para o primeiro boot, prefira o perfil `local`.

Observacao importante:

- nesta workspace, o caminho totalmente revalidado foi o perfil `local`
- se voce rodar o backend dentro do WSL enquanto o PostgreSQL estiver exposto em outro host/rede, ajuste `SPRING_DATASOURCE_URL` para um endereco realmente alcancavel pelo processo do backend

## Validacao Executada

Validado em `2026-05-21`:

- Backend: `mvn -q -DskipTests compile`
- Backend: `mvn spring-boot:run -Dspring-boot.run.profiles=local`
- Backend: `mvn test` com fallback automatico para H2 em memoria quando Docker/PostgreSQL nao estao disponiveis
- Backend: fluxo `health -> register -> get me -> onboarding -> search -> login`
- Infra: `docker compose up -d postgres` deixa o container `ecobook-db` saudavel
- Android: `.\gradlew.bat app:compileDebugKotlin`
- Android: `.\gradlew.bat assembleDebug`
- Android: `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`

Observacao importante:

- Nesta maquina, o backend foi revalidado com Java 21+ tambem no Windows, ajustando `JAVA_HOME` antes de rodar Maven.
- O Android foi validado em compilacao e teste JVM local; execucao visual ainda depende de emulador/dispositivo configurado no Android Studio.

Validado em `2026-05-23`:

- Backend: `mvn test` verde com `140` testes
- Android: `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`
- Backend: metricas e smoke gate revalidados em runtime por `/actuator/prometheus`, `/actuator/health` e `SmokeTests`

## Limites Conhecidos

- Push real com Firebase ainda precisa de validacao final em dispositivo/emulador com Google Play services.
- O texto de termos/privacidade do MVP agora existe e esta visivel no app, mas ainda precisa de revisao juridica antes de qualquer publicacao real.
- As frentes restantes de fase 9/10 agora se concentram em carga/performance, edge cases finais, Swagger/OpenAPI e os quality gates finais.

## Documentacao

- Backend: [EcoBookAiBackend/README.md](EcoBookAiBackend/README.md)
- Android: [EcoBookAiAndroid/README.md](EcoBookAiAndroid/README.md)
- Quickstart tecnico: [specs/001-ecobook-core/quickstart.md](specs/001-ecobook-core/quickstart.md)
- Contratos de runtime: [specs/001-ecobook-core/contracts/README.md](specs/001-ecobook-core/contracts/README.md)
- Termos e privacidade do MVP: [docs/legal/termos-e-privacidade.md](docs/legal/termos-e-privacidade.md)
- Estado do plano: [specs/001-ecobook-core/PLAN-SUMMARY.md](specs/001-ecobook-core/PLAN-SUMMARY.md)
- Backlog por fase: [specs/001-ecobook-core/TASKS.md](specs/001-ecobook-core/TASKS.md)
