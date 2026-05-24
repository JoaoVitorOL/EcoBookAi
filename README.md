# EcoBook AI

Plataforma Android + backend Spring Boot para doacao e solicitacao de materiais de estudo, com classificacao assistida por IA, matching geografico, notificacoes e controles de consentimento/LGPD.

## Status Atual

- Fases `1` a `10` estao implementadas e validadas no estado atual do repositorio.
- Backend: auth `email + senha + JWT`, onboarding, preview IA, materiais, discovery, solicitacoes, notificacoes, moderacao/admin, consentimentos, exclusao de conta, exportacao de dados, OpenAPI e observabilidade.
- Android: login/cadastro, onboarding, doacao, busca, pedidos do estudante, pedidos do doador, central de notificacoes, edicao de perfil, consentimentos e exclusao de conta.
- Em `2026-05-23`, o app Android tambem passou por uma revisao de UI/codigo baseada em referencias oficiais do Compose para acessibilidade, state hoisting e layouts adaptativos.

## O Jeito Mais Seguro De Rodar Tudo

Se voce quer subir o projeto inteiro sem cair em armadilhas de ambiente, siga exatamente esta ordem:

1. Abra um PowerShell na raiz do repositorio `EcoBookAi`.
2. Suba o backend no perfil `local` com H2.
3. Valide o `health` da API.
4. Configure `EcoBookAiAndroid/local.properties`.
5. Rode `assembleDebug`, `lintDebug` e `testDebugUnitTest` no Android, em sequencia.
6. Abra `EcoBookAiAndroid` no Android Studio e execute o app.

Essa ordem foi revalidada nesta maquina em `2026-05-23`.

## Requisitos

- Windows PowerShell ou terminal equivalente
- Java `21+` para o backend
- Maven `3.9+`
- Android Studio
- Android SDK Platform `34`
- Emulador Android ou dispositivo fisico
- Docker Desktop opcional, apenas para o perfil PostgreSQL

## Estrutura Do Repositorio

```text
.
|-- EcoBookAiBackend/        # API REST e regras de negocio
|-- EcoBookAiAndroid/        # App Android nativo
|-- specs/001-ecobook-core/ # Spec kit, contratos e backlog historico
|-- docs/                    # Guias, revisoes e runbooks
|-- scripts/                 # Scripts auxiliares
`-- docker-compose.yml       # PostgreSQL local para o perfil default
```

## Passo A Passo Validado

### 1. Subir o backend local

Na raiz do repositorio:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Run-BackendLocal.ps1 -JavaHome "C:\Program Files\Java\jdk-26"
```

O perfil `local`:

- usa H2 em arquivo
- nao depende de Docker
- sobe o backend em `http://127.0.0.1:8080/api`
- deixa o Gemini em modo mock

Se a porta `8080` estiver ocupada:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Run-BackendLocal.ps1 -JavaHome "C:\Program Files\Java\jdk-26" -Port 8081
```

### 2. Validar o health da API

Em outro terminal:

```powershell
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/api/v1/health
```

Resposta esperada:

```json
{"status":200,"message":"Backend online", ...}
```

Se voce usou outra porta no passo anterior, ajuste aqui e tambem no `backend.url` do Android.

### 3. Configurar o Android

Crie ou ajuste `EcoBookAiAndroid/local.properties` com base em `EcoBookAiAndroid/local.properties.example`.

Exemplo para emulador Android:

```properties
sdk.dir=C\:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
```

Se o backend estiver no WSL ou se voce estiver usando um aparelho fisico, troque `10.0.2.2` pelo IP real da maquina host.

Para descobrir o IP do WSL:

```powershell
wsl.exe -e bash -lc "hostname -I"
```

Exemplo:

```properties
backend.url=http://172.22.160.34:8080/api
```

`google-services.json` nao e obrigatorio para compilar ou testar os fluxos principais. Ele so e necessario quando voce quer validar push real do Firebase.

### 4. Build, lint e testes do Android

Entre na pasta do app:

```powershell
cd .\EcoBookAiAndroid
```

Rode os comandos abaixo em sequencia:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:assembleDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:lintDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

Por que usar esse wrapper:

- a workspace atual tem espacos e acentos no caminho do Windows
- o script cria um alias ASCII temporario para o Gradle
- em `2026-05-23`, o wrapper tambem foi endurecido contra corrida entre aliases temporarios

Baseline validado nesta rodada:

- `app:assembleDebug` verde
- `app:lintDebug` verde com `0` erros
- `app:testDebugUnitTest` verde

### 5. Rodar o app

1. Abra a pasta `EcoBookAiAndroid` no Android Studio.
2. Aguarde o sync do Gradle.
3. Inicie um emulador API `34` ou conecte um dispositivo fisico.
4. Execute o app em `debug`.
5. Use login/cadastro normal no app.

## Validacoes Uteis

Backend:

```powershell
cd .\EcoBookAiBackend
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn test
```

Android:

```powershell
cd .\EcoBookAiAndroid
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:compileDebugKotlin
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:lintDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

Ultimo baseline conhecido:

- Backend: `mvn test` verde com `218` testes, `0` falhas, `0` erros e `3` skips controlados
- Backend: JaCoCo em `85.23%`
- Android lint: `0` erros e `20` warnings nao bloqueantes
- Android JVM: `app:testDebugUnitTest` verde

## Modos Opcionais

### PostgreSQL + Flyway

Se voce quiser validar o caminho mais proximo do banco principal:

```powershell
docker compose up -d postgres
cd .\EcoBookAiBackend
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

Esse modo espera:

- `localhost:5432`
- banco `ecobook`
- usuario `ecobook`
- senha `dev_password_123` ou `DB_PASSWORD`

### Firebase real

Para push real no Android, alem do backend local voce precisa:

- `EcoBookAiAndroid/app/google-services.json`
- credencial Admin SDK valida no backend
- emulador com Google Play services ou dispositivo fisico

Backend com Firebase:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Run-BackendWithFirebase.ps1 -ServiceAccountPath .\EcoBookAiBackend\credentials\ecobook-adminsdk.json
```

Teste instrumentado:

```powershell
cd .\EcoBookAiAndroid
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.fcm.FirebaseRealDeviceValidationTest'
```

## Revisao Geral Mais Recente

Lacunas/falhas corrigidas nesta passada:

- `NavGraph` deixou de usar o padrao inseguro de `getBackStackEntry()` durante composicao.
- A UI Android agora usa um conteiner adaptativo com largura maxima nas telas principais.
- Linhas de consentimento passaram a ser inteiramente clicaveis, nao so o `Switch`/`Checkbox`.
- Chips customizados passaram a expor melhor semantica de selecao.
- `SharedPreferences.commit()` foi trocado por `apply()` no override de backend usado em runtime/testes.
- O servico FCM Android deixou de carregar uma checagem obsoleta de `SDK_INT`.
- `Run-BackendLocal.ps1` agora expande corretamente `-Port`, em vez de repassar `server.port=$Port` literal para o Spring Boot.
- O wrapper `Invoke-GradleAsciiPath.ps1` passou a sincronizar a criacao do drive ASCII temporario, evitando falhas silenciosas em execucoes concorrentes.
- Dois recursos Android nao usados foram removidos.

Sugestoes para a proxima rodada:

- subir `targetSdk` e revisar o bloco de dependencias Android
- expandir validacao visual em tablet/foldable
- fazer uma passada dedicada de edge-to-edge/insets
- mover mais strings visiveis para `res/values/strings.xml` se localizacao entrar no curto prazo
- revisar `SecureStorage` porque `EncryptedSharedPreferences`/`MasterKey` hoje geram warnings de deprecacao no assemble validado

## Limites Conhecidos

- O texto de termos/privacidade do MVP existe e esta visivel no app, mas ainda precisa de revisao juridica antes de publicacao real.
- O `OWASP Dependency Check` ainda aponta risco residual documentado em `docs/security-scan.md`.
- O Android lint ainda lista warnings de dependencia desatualizada e `targetSdk = 34`, mas sem erro funcional bloqueante nesta rodada.

## Documentacao

- Backend: [EcoBookAiBackend/README.md](EcoBookAiBackend/README.md)
- Android: [EcoBookAiAndroid/README.md](EcoBookAiAndroid/README.md)
- Quickstart tecnico: [specs/001-ecobook-core/quickstart.md](specs/001-ecobook-core/quickstart.md)
- Contratos HTTP: [specs/001-ecobook-core/contracts/README.md](specs/001-ecobook-core/contracts/README.md)
- Plano consolidado: [specs/001-ecobook-core/PLAN-SUMMARY.md](specs/001-ecobook-core/PLAN-SUMMARY.md)
- Backlog historico: [specs/001-ecobook-core/TASKS.md](specs/001-ecobook-core/TASKS.md)
- Guia de testes: [docs/testing.md](docs/testing.md)
- Revisao Android UI: [docs/android-ui-review.md](docs/android-ui-review.md)
- Revisao de codigo: [docs/code-review.md](docs/code-review.md)
- Analise estatica: [docs/static-analysis.md](docs/static-analysis.md)
- Seguranca de dependencias: [docs/security-scan.md](docs/security-scan.md)
- Rollback de migrations: [docs/migration-rollbacks.md](docs/migration-rollbacks.md)
- Arquitetura: [docs/architecture.md](docs/architecture.md)
- Troubleshooting: [docs/troubleshooting.md](docs/troubleshooting.md)
- Deploy: [docs/deployment.md](docs/deployment.md)
- Termos e privacidade do MVP: [docs/legal/termos-e-privacidade.md](docs/legal/termos-e-privacidade.md)
