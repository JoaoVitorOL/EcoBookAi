# EcoBook AI

Plataforma Android + backend Spring Boot para doacao e solicitacao de materiais de estudo, com classificacao assistida por IA, matching geografico e fluxo transacional entre doador e estudante.

## Status Atual

- Fases `1` a `10` estao implementadas e validadas no estado atual do repositorio.
- Backend: auth `email + senha + JWT`, onboarding, preview IA, materiais, discovery, solicitacoes, notificacoes, moderacao/admin, consentimento LGPD, exclusao de conta, exportacao de dados e acesso autenticado a imagens.
- Android: login/cadastro, onboarding, doacao, busca, pedidos do estudante, pedidos do doador, central de avisos, edicao de perfil, consentimentos e exclusao de conta.
- A rodada mais recente tambem incluiu uma revisao geral da interface Android com base em guias oficiais do Compose para acessibilidade e adaptive layout.

## Caminho Recomendado

Se voce quer rodar o projeto inteiro sem bater em configuracoes opcionais, siga este fluxo:

1. Suba o backend no perfil `local` com H2.
2. Valide o `health` da API.
3. Configure `EcoBookAiAndroid/local.properties` apontando para esse backend.
4. Gere o app debug, rode `lint` e rode os testes JVM do Android.
5. Abra `EcoBookAiAndroid` no Android Studio e execute em emulador/dispositivo.

Esse foi o caminho revalidado nesta maquina em `2026-05-23`.

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
|-- specs/001-ecobook-core/ # Spec kit, contratos e backlog
|-- docs/                   # Guias, revisoes e runbooks
|-- scripts/                # Scripts auxiliares
`-- docker-compose.yml      # PostgreSQL local para o perfil default
```

## Passo A Passo Validado

### 1. Backend local

Abra um PowerShell na raiz do repositorio e ajuste o Java do backend:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Opcao recomendada, usando o script local:

```powershell
.\scripts\Run-BackendLocal.ps1
```

Opcao manual equivalente:

```powershell
cd .\EcoBookAiBackend
mvn -q -DskipTests compile
mvn --% spring-boot:run -Dspring-boot.run.profiles=local
```

O perfil `local`:

- usa H2 em arquivo
- nao depende de Docker
- sobe o backend em `http://127.0.0.1:8080/api`
- deixa o Gemini em modo mock

### 2. Validar o backend

Em outro terminal:

```powershell
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/api/v1/health
```

Resposta esperada:

```json
{"status":200,"message":"Backend online", ...}
```

Se a porta `8080` ja estiver ocupada, pare a instancia antiga ou rode:

```powershell
.\scripts\Run-BackendLocal.ps1 -Port 8081
```

e depois ajuste o `backend.url` do Android para `http://10.0.2.2:8081/api`.

### 3. Configurar o Android

Crie ou ajuste o arquivo `EcoBookAiAndroid/local.properties` com base em `EcoBookAiAndroid/local.properties.example`.

Exemplo para emulador Android:

```properties
sdk.dir=C\:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
```

Se o backend estiver rodando dentro do WSL ou se voce estiver usando um aparelho fisico, troque `10.0.2.2` pelo IP real da maquina host.

Para descobrir o IP do WSL:

```powershell
wsl.exe -e bash -lc "hostname -I"
```

Exemplo:

```properties
backend.url=http://172.22.160.34:8080/api
```

### 4. Build, lint e testes do Android

Na pratica, o wrapper PowerShell com drive ASCII e o caminho mais estavel nesta workspace com espacos/acentos.

```powershell
cd .\EcoBookAiAndroid
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:assembleDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:lintDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

Estado validado nesta rodada:

- `app:assembleDebug` verde
- `app:lintDebug` verde com `0` erros
- `app:testDebugUnitTest` verde

### 5. Rodar o app

1. Abra a pasta `EcoBookAiAndroid` no Android Studio.
2. Aguarde o sync do Gradle.
3. Inicie um emulador API `34` ou conecte um dispositivo fisico.
4. Execute o app em `debug`.
5. Use login/cadastro normal no app.

## Comandos De Validacao

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

Se voce quiser validar migrations e o caminho mais proximo do banco principal:

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

- `app/google-services.json`
- credencial Admin SDK valida no backend
- emulador com Google Play services ou dispositivo fisico

Backend com Firebase:

```powershell
.\scripts\Run-BackendWithFirebase.ps1 -ServiceAccountPath .\EcoBookAiBackend\credentials\ecobook-adminsdk.json
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
- Dois recursos Android nao usados foram removidos.

Sugestoes que ficaram para uma proxima rodada:

- subir `targetSdk` e revisar o bloco de dependencias Android
- expandir validacao visual em tablet/foldable
- fazer uma passada dedicada de edge-to-edge/insets

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
