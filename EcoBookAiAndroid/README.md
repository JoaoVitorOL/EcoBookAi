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
- Catalogo inicial de materiais para validar a experiencia de matching
- Fluxo visual de doacao alinhado com os contratos do projeto
- Base pronta para integrar auth local, perfil, materiais e solicitacoes

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
```

`backend.url` em `local.properties` tem prioridade sobre `app/src/main/res/values/api_config.xml`. Isso e util quando voce roda o backend em outra porta, como `8081`.

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

## Regra atual de autenticacao

- O fluxo alvo de entrada e `login` e `cadastro` com email e senha.
- A API devolve JWT apos autenticacao bem-sucedida.
- O JWT deve ficar armazenado de forma segura no dispositivo.
- Se a API responder `401`, o app encerra a sessao local e volta para a tela de login.

## Observacao sobre o estado atual do codigo

Parte da implementacao ainda reflete o fluxo anterior com Google. A documentacao oficial do projeto, no entanto, ja considera **email + senha + JWT** como a regra de auth a seguir nas proximas etapas.

## Celular fisico

Se for testar em um dispositivo fisico, ajuste `backend_url_physical` em:

`app/src/main/res/values/api_config.xml`

Use o IP local da sua maquina na mesma rede Wi-Fi, por exemplo:

```text
http://192.168.0.10:8080/api
```

## Observacao importante

Se o app nao abrir no Android Studio, confirme que a pasta aberta e `EcoBookAiAndroid`, nao a raiz do repositorio nem a antiga pasta `android`.
