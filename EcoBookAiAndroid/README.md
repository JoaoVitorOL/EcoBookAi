# EcoBookAiAndroid

Aplicativo Android nativo do EcoBook AI.

## Stack

- Kotlin
- Jetpack Compose
- Credential Manager + Sign in with Google
- Hilt
- Retrofit + OkHttp
- Firebase Messaging

## O que este app entrega agora

- Navegacao com quatro areas: `Painel`, `Buscar`, `Doar` e `Perfil`
- Verificacao real do backend via `GET /api/v1/health`
- Catalogo inicial de materiais para validar a experiencia de matching
- Fluxo visual de doacao alinhado com os contratos do projeto
- Base pronta para integrar auth, perfil, materiais e solicitacoes

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

Exemplo:

```properties
sdk.dir=C\:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
google.oauth.clientId=SEU_GOOGLE_WEB_CLIENT_ID
```

`google.oauth.clientId` deve receber o **Web client ID** do Google, que sera usado pelo Credential Manager para solicitar o Google ID token entregue ao backend.
Nao use aqui o client ID do tipo `Android`. O client Android continua necessario no Google Cloud para validar `package name + SHA-1`, mas o valor usado em `local.properties` e no backend deve ser o client do tipo `Web application`.

## Como abrir no Android Studio

1. Abra o Android Studio.
2. Clique em `Open`.
3. Selecione a pasta `EcoBookAiAndroid`.
4. Aguarde o sync do Gradle.

## Como rodar via terminal

Na pasta `EcoBookAiAndroid`:

```powershell
.\gradlew.bat assembleDebug
```

Validacao atual nesta maquina:

```powershell
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

Esses comandos passam com o SDK configurado em `local.properties`.

## Como rodar pelo Android Studio

1. Crie um emulador com Android 14 / API 34 ou conecte um celular com depuracao USB.
2. Clique em `Run`.
3. Escolha o dispositivo.

## Backend local no emulador

O app aponta por padrao para:

```text
http://10.0.2.2:8080/api
```

Isso funciona para emulador Android quando o backend roda na mesma maquina host.

## Celular fisico

Se for testar em um dispositivo fisico, ajuste `backend_url_physical` em:

`app/src/main/res/values/api_config.xml`

Use o IP local da sua maquina na mesma rede Wi-Fi, por exemplo:

```text
http://192.168.0.10:8080/api
```

## Observacao importante

Se o app nao abrir no Android Studio, confirme que a pasta aberta e `EcoBookAiAndroid`, nao a raiz do repositorio nem a antiga pasta `android`.
