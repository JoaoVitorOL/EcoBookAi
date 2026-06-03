# EcoBook AI

Plataforma Android + backend Spring Boot para doação e solicitação de materiais de estudo, com classificação assistida por IA, matching geográfico, filtros por necessidade acadêmica do material, notificações e controles de consentimento/LGPD.

O uso previsto do MVP é por adultos, pais e responsáveis legais dos alunos. O app não intermedeia a conversa nem o ponto de encontro: depois da aprovação, contato e entrega são combinados exclusivamente via WhatsApp entre as partes.

## Status Atual

- Fases `1` a `10` estão implementadas e validadas no estado atual do repositório; o que resta para lançamento real é revisão jurídica do texto legal, monitoramento contínuo de dependências e revalidação do ambiente alvo.
- Backend: auth `email + senha + JWT`, onboarding, preview IA, materiais, discovery, solicitações, notificações, moderação/admin, consentimentos, exclusão de conta, OpenAPI e observabilidade.
- Android: login/cadastro, onboarding, doação, busca, pedidos do estudante, pedidos do doador, central de notificações, edição de perfil com CPF obrigatório e foto de perfil, consentimentos e exclusão de conta.
- Em `2026-05-23`, o app Android também passou por uma revisão de UI/código baseada em referências oficiais do Compose para acessibilidade, state hoisting e layouts adaptativos.

## O Jeito Mais Seguro De Rodar Tudo

Se você quer subir o projeto inteiro sem cair em armadilhas de ambiente, siga exatamente esta ordem:

1. Abra um PowerShell na raiz do repositório `EcoBookAi`.
2. Suba o backend no perfil `local` com H2.
3. Valide o `health` da API.
4. Configure `EcoBookAiAndroid/local.properties`.
5. Rode `assembleDebug`, `lintDebug` e `testDebugUnitTest` no Android, em sequência.
6. Abra `EcoBookAiAndroid` no Android Studio e execute o app.

Essa ordem foi revalidada nesta máquina em `2026-06-03`.

## Requisitos

- Windows PowerShell ou terminal equivalente
- Java `21+` para o backend
- Maven `3.9+`
- Android Studio
- Android SDK Platform `34`
- Emulador Android ou dispositivo físico
- Docker Desktop opcional, apenas para o perfil PostgreSQL

## Estrutura Do Repositório

```text
.
|-- EcoBookAiBackend/        # API REST e regras de negócio
|-- EcoBookAiAndroid/        # App Android nativo
|-- specs/001-ecobook-core/ # Spec kit, contratos e backlog histórico
|-- docs/                    # Guias, revisoes e runbooks
|-- scripts/                 # Scripts auxiliares
`-- docker-compose.yml       # PostgreSQL local para o perfil default
```

## Passo A Passo Validado

### 1. Subir o backend local

Na raiz do repositório:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Run-BackendLocal.ps1 -JavaHome "C:\Program Files\Java\jdk-26"
```

O perfil `local`:

- usa H2 em arquivo
- não depende de Docker
- sobe o backend em `http://127.0.0.1:8080/api`
- usa preview mock do Gemini apenas quando `GEMINI_API_KEY` não estiver presente no processo

Se `GEMINI_API_KEY` estiver configurada, o backend local passa a usar o Gemini real automaticamente.
Se você quiser forçar o preview mock mesmo com chave presente, defina:

```powershell
$env:GEMINI_MOCK_FORCE = "true"
```

Antes de subir outra instância local, vale checar se já existe uma API respondendo em `8080`:

```powershell
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/api/v1/health
```

Se esse comando já responder `200`, o backend já está rodando e você não precisa abrir uma segunda instância. Isso evita conflito de porta, disputa pelo H2 local e ruído no `logs/ecobook.log`.

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

Se você usou outra porta no passo anterior, ajuste aqui e também no `backend.url` do Android.

Se o `health` já estava respondendo antes do passo 1, apenas reaproveite essa instância e siga para a configuração do Android.

### 3. Configurar o Android

Crie ou ajuste `EcoBookAiAndroid/local.properties` com base em `EcoBookAiAndroid/local.properties.example`.

Exemplo para emulador Android:

```properties
sdk.dir=C\:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
```

Se o backend estiver no WSL ou se você estiver usando um aparelho físico, troque `10.0.2.2` pelo IP real da máquina host.

Para descobrir o IP do WSL:

```powershell
wsl.exe -e bash -lc "hostname -I"
```

Exemplo:

```properties
backend.url=http://172.22.160.34:8080/api
```

`google-services.json` não é obrigatório para compilar ou testar os fluxos principais. Ele só é necessário quando você quer validar push real do Firebase.

## Onde Fica A Chave Do Gemini

A chave do Gemini deve ficar **somente no backend**, nunca no Android.

Hoje o código do backend lê a chave por variável de ambiente em:

- `EcoBookAiBackend/src/main/resources/application.yml` -> `gemini.api-key: ${GEMINI_API_KEY:}`
- `EcoBookAiBackend/src/main/java/com/ecobook/service/GeminiService.java`

Para ambiente local ou produção, esconda a chave no processo do backend:

```powershell
$env:GEMINI_API_KEY = "sua-chave-aqui"
```

Depois suba o backend normalmente.

O que não fazer:

- não colocar a chave em `EcoBookAiAndroid/local.properties`
- não colocar a chave no app Android, `BuildConfig`, Compose UI ou frontend
- não commitar a chave em `application.yml`, `application-local.yml`, `README.md` ou qualquer arquivo versionado

No perfil `local` documentado acima, o backend sobe com preview mock apenas quando `GEMINI_API_KEY` não estiver configurada. Se a chave estiver presente, o preview usa o Gemini real automaticamente.

### 4. Build, lint e testes do Android

Entre na pasta do app:

```powershell
cd .\EcoBookAiAndroid
```

Rode os comandos abaixo em sequência:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:assembleDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:lintDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

Por que usar esse wrapper:

- a workspace atual tem espaços e acentos no caminho do Windows
- o script cria um alias ASCII temporário para o Gradle
- em `2026-06-03`, o wrapper passou a serializar a execução inteira do Gradle nessa workspace, evitando corridas entre aliases temporários

Baseline validado nesta rodada:

- `app:assembleDebug` verde
- `app:lintDebug` verde com `0` erros
- `app:testDebugUnitTest` verde

### 5. Rodar o app

1. Abra a pasta `EcoBookAiAndroid` no Android Studio.
2. Aguarde o sync do Gradle.
3. Inicie um emulador API `34` ou conecte um dispositivo físico.
4. Execute o app em `debug`.
5. Use login/cadastro normal no app.

## Validações Úteis

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

Último baseline conhecido em `2026-06-03`:

- Backend: `mvn test` verde com `229` testes, `0` falhas, `0` erros e `3` skips controlados
- Backend: JaCoCo em `84.72%`
- Android lint: `0` erros e `20` avisos não bloqueantes
- Android JVM: `app:testDebugUnitTest` verde

## Modos Opcionais

### PostgreSQL + Flyway

Se você quiser validar o caminho mais próximo do banco principal:

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
- usuário `ecobook`
- senha `dev_password_123` ou `DB_PASSWORD`

### Firebase real

Para push real no Android, além do backend local você precisa:

- `EcoBookAiAndroid/app/google-services.json`
- credencial Admin SDK válida no backend
- emulador com Google Play services ou dispositivo físico

Backend com Firebase:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Run-BackendWithFirebase.ps1 -ServiceAccountPath .\EcoBookAiBackend\credentials\ecobook-adminsdk.json
```

Teste instrumentado:

```powershell
cd .\EcoBookAiAndroid
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.fcm.FirebaseRealDeviceValidationTest'
```

## Revisão Geral Mais Recente

Lacunas/falhas corrigidas nesta passada:

- As suítes e fixtures agora estão alinhadas ao CPF obrigatório do perfil, ao campo obrigatório `necessidade_academica` dos materiais e ao `WebMvcTest` do `UsuarioController`.
- A exclusão de conta no backend deixou de engolir falhas de remoção de arquivos e agora registra a limpeza executada e qualquer erro residual no detalhe operacional.
- O bootstrap do Firebase Admin SDK voltou a aceitar novas tentativas de inicialização depois de uma falha de credencial, sem exigir restart do backend.
- `NavGraph` deixou de usar o padrão inseguro de `getBackStackEntry()` durante composição.
- A UI Android agora usa um contêiner adaptativo com largura máxima nas telas principais.
- Linhas de consentimento passaram a ser inteiramente clicáveis, não só o `Switch`/`Checkbox`.
- Chips customizados passaram a expor melhor semântica de seleção.
- `SharedPreferences.commit()` foi trocado por `apply()` no override de backend usado em runtime/testes.
- O serviço FCM Android deixou de carregar uma checagem obsoleta de `SDK_INT`.
- `Run-BackendLocal.ps1` agora expande corretamente `-Port`, em vez de repassar `server.port=$Port` literal para o Spring Boot.
- A expiração automática de reservas agora também cobre o instante-limite exato (`expires_at == now`), em linha com os testes e com a regra de negócio documentada.
- O wrapper `Invoke-GradleAsciiPath.ps1` passou a sincronizar a criação do drive ASCII temporário, evitando falhas silenciosas em execuções concorrentes.
- O Android deixou de carregar a antiga `HomeScreen` de demonstração e os modelos/sample states que já não participavam da navegação real.

Sugestões para a próxima rodada:

- subir `targetSdk` e revisar o bloco de dependências Android
- expandir validação visual em tablet/foldable
- fazer uma passada dedicada de edge-to-edge/insets
- mover mais strings visíveis para `res/values/strings.xml` se localização entrar no curto prazo
- revisar `SecureStorage` porque `EncryptedSharedPreferences`/`MasterKey` hoje geram avisos de deprecação no assemble validado

## Limites Conhecidos

- O texto de termos/privacidade do MVP existe e está visível no app, mas ainda precisa de revisão jurídica antes de publicação real.
- O `OWASP Dependency Check` ainda aponta risco residual documentado em `docs/security-scan.md`.
- O Android lint ainda lista avisos de dependência desatualizada e `targetSdk = 34`, mas sem erro funcional bloqueante nesta rodada.

## Documentação

- Backend: [EcoBookAiBackend/README.md](EcoBookAiBackend/README.md)
- Android: [EcoBookAiAndroid/README.md](EcoBookAiAndroid/README.md)
- Quickstart técnico: [specs/001-ecobook-core/quickstart.md](specs/001-ecobook-core/quickstart.md)
- Contratos HTTP: [specs/001-ecobook-core/contracts/README.md](specs/001-ecobook-core/contracts/README.md)
- Plano consolidado: [specs/001-ecobook-core/PLAN-SUMMARY.md](specs/001-ecobook-core/PLAN-SUMMARY.md)
- Backlog histórico: [specs/001-ecobook-core/TASKS.md](specs/001-ecobook-core/TASKS.md)
- Guia de testes: [docs/testing.md](docs/testing.md)
- Revisão Android UI: [docs/android-ui-review.md](docs/android-ui-review.md)
- Revisão de código: [docs/code-review.md](docs/code-review.md)
- Análise estática: [docs/static-analysis.md](docs/static-analysis.md)
- Segurança de dependências: [docs/security-scan.md](docs/security-scan.md)
- Rollback de migrations: [docs/migration-rollbacks.md](docs/migration-rollbacks.md)
- Arquitetura: [docs/architecture.md](docs/architecture.md)
- Troubleshooting: [docs/troubleshooting.md](docs/troubleshooting.md)
- Deploy: [docs/deployment.md](docs/deployment.md)
- Termos e privacidade do MVP: [docs/legal/termos-e-privacidade.md](docs/legal/termos-e-privacidade.md)
