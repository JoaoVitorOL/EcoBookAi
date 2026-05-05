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
- Catalogo local e fluxo visual de doacao para validar UX antes da integracao dos modulos de materiais e matching
- Base pronta para integrar materiais, preview com IA e solicitacoes

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

O fluxo legado de autenticacao com Google foi removido do app. O foco atual do modulo Android e estabilizar auth/perfil e integrar os proximos endpoints de materiais, preview com IA e solicitacoes.

## Celular fisico

Se for testar em um dispositivo fisico, ajuste `backend_url_physical` em:

`app/src/main/res/values/api_config.xml`

Use o IP local da sua maquina na mesma rede Wi-Fi, por exemplo:

```text
http://192.168.0.10:8080/api
```

## Observacao importante

Se o app nao abrir no Android Studio, confirme que a pasta aberta e `EcoBookAiAndroid`, nao a raiz do repositorio nem a antiga pasta `android`.
