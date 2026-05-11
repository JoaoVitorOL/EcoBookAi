# EcoBookAiAndroid

Aplicativo Android nativo do EcoBook AI.

## Stack

- Kotlin
- Jetpack Compose
- Email/password + JWT
- Hilt
- Retrofit + OkHttp
- Firebase Messaging

## O que este app entrega agora

- Navegacao com quatro areas: `Painel`, `Buscar`, `Doar` e `Perfil`
- Verificacao real do backend via `GET /api/v1/health`
- Fluxo real de `login`, `cadastro`, `logout` e onboarding com `email + senha + JWT`
- Fluxo real de doacao com galeria/camera, `POST /materiais/preview`, revisao manual e `POST /materiais`
- Base pronta para evoluir matching, solicitacoes e notificacoes de negocio

## Requisitos

- Java 17 ou Java 21 instalado
- Android Studio
- Android SDK Platform 34
- Android Build-Tools
- Platform-Tools
- Emulator ou dispositivo fisico

## Configuracao inicial

1. Instale o Android Studio.
2. Abra o `SDK Manager` e instale:
   - Android SDK Platform 34
   - Android SDK Build-Tools
   - Android SDK Platform-Tools
   - Android Emulator
3. Crie o arquivo `local.properties` na raiz de `EcoBookAiAndroid`.
4. Use `local.properties.example` como base.
5. Para ativar Firebase Cloud Messaging real, baixe `google-services.json` do Firebase Console e coloque em `app/google-services.json`.

Exemplo:

```properties
sdk.dir=C\:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
```

`backend.url` em `local.properties` tem prioridade sobre `app/src/main/res/values/api_config.xml`. Isso e util quando voce roda o backend em outra porta, como `8081`.

Se o backend estiver rodando dentro do WSL, `10.0.2.2` pode nao enxergar o relay corretamente. Nesse caso, use o IP atual do Linux, por exemplo:

```properties
backend.url=http://172.22.160.34:8080/api
```

Voce pode descobrir esse IP com:

```powershell
wsl.exe -e bash -lc "hostname -I"
```

## Como abrir no Android Studio

1. Abra o Android Studio.
2. Clique em `Open`.
3. Selecione a pasta `EcoBookAiAndroid`.
4. Aguarde o sync do Gradle.

Se quiser executar `testDebugUnitTest` pelo Android Studio nesta mesma workspace Windows, abra o projeto por um alias ASCII temporario:

```powershell
subst X: "C:\caminho\real\para\EcoBookAiAndroid"
```

Depois abra `X:\` no Android Studio. Quando terminar, remova o alias:

```powershell
subst X: /d
```

## Como rodar via terminal

Na pasta `EcoBookAiAndroid`:

```powershell
.\gradlew.bat assembleDebug
```

Se a pasta local do projeto estiver em um caminho Windows com espacos/acentos, como esta workspace do OneDrive, o runner de `testDebugUnitTest` pode falhar com `ClassNotFoundException` apesar de a classe ter compilado corretamente. Nessa situacao, execute o Gradle pelo wrapper ASCII:

```powershell
.\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

Validacao atual nesta maquina:

```powershell
.\gradlew.bat app:compileDebugKotlin
.\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest app:lintDebug app:compileDebugAndroidTestKotlin
```

Esses comandos passam com o SDK configurado em `local.properties`.

Com um emulador Android ja iniciado, o fluxo auth E2E tambem pode ser validado por instrumentacao:

```powershell
.\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.auth.AuthFlowE2ETest
```

Sem `app/google-services.json`, o app continua compilando para o fluxo atual de autenticacao, catalogo, upload e publicacao de materiais. O plugin `google-services` so e aplicado automaticamente quando esse arquivo existe, o que evita quebrar o build antes da configuracao real do Firebase.

## Como rodar pelo Android Studio

1. Crie um emulador com Android 14 / API 34 ou conecte um celular com depuracao USB.
2. Clique em `Run`.
3. Escolha o dispositivo.

## Managed device do Gradle

O projeto tambem inclui o managed device `pixel6Api34` em `app/build.gradle.kts` para smoke tests automatizados.

- Perfil: `Pixel 6`
- API: `34`
- System image: `aosp-atd`

O Android Gradle Plugin gerencia automaticamente a criacao e a persistencia desse AVD no workspace de managed devices. A DSL oficial do `ManagedVirtualDevice` nao expoe campos dedicados para RAM manual ou caminho customizado de storage; por isso a configuracao usa o hardware profile do Pixel 6 como baseline acima de 1 GB de memoria.

Para listar ou executar as tasks relacionadas, use:

```powershell
.\gradlew.bat tasks --all
```

Comandos confirmados nesta workspace:

```powershell
.\gradlew.bat app:pixel6Api34DebugAndroidTest
.\gradlew.bat app:smokeGroupDebugAndroidTest
```

Para testar FCM real, prefira um dispositivo fisico Android ou um emulador com Google Play services. O managed device `aosp-atd` continua util para smoke tests gerais, mas nao e o alvo ideal para push real.

## Backend local no emulador

O app aponta por padrao para:

```text
http://10.0.2.2:8080/api
```

Isso funciona para emulador Android quando o backend roda na mesma maquina host.

Se o backend estiver no WSL e o app continuar mostrando `Servidor indisponivel`, a causa mais comum e esta:
- Windows consegue abrir `http://localhost:8080`, mas nao `http://127.0.0.1:8080`
- nesse caso o backend esta acessivel apenas por IPv6/relay local e o emulador nao consegue usar `10.0.2.2`
- a correcao pratica e rodar o backend no host Windows ou apontar `backend.url` para o IP atual do WSL

## Regra atual de autenticacao

- O fluxo alvo de entrada e `login` e `cadastro` com email e senha.
- A API devolve JWT apos autenticacao bem-sucedida.
- O JWT deve ficar armazenado de forma segura no dispositivo.
- Se a API responder `401`, o app encerra a sessao local e volta para a tela de login.

## Observacao sobre o estado atual do codigo

O fluxo legado de autenticacao com Google foi removido do app. O foco atual do modulo Android e consolidar o que ja esta integrado em auth/perfil/materiais e avancar nos proximos endpoints de matching, solicitacoes e notificacoes.

## Celular fisico

Se for testar em um dispositivo fisico, ajuste `backend_url_physical` em:

`app/src/main/res/values/api_config.xml`

Use o IP local da sua maquina na mesma rede Wi-Fi, por exemplo:

```text
http://192.168.0.10:8080/api
```

## Observacao importante

Se o app nao abrir no Android Studio, confirme que a pasta aberta e `EcoBookAiAndroid`, nao a raiz do repositorio nem a antiga pasta `android`.
