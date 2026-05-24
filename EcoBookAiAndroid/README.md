# EcoBookAiAndroid

Aplicativo Android nativo do EcoBook AI.

## O Que O App Entrega Hoje

- Login, cadastro, restauracao de sessao e logout com `JWT`
- Onboarding com WhatsApp, cidade, bairro, instituicao, necessidades e consentimento de IA
- Doacao com imagem frontal obrigatoria, imagem traseira opcional, preview IA e publicacao final
- Busca de materiais com filtros, paginacao e dialogo de detalhes
- `Minhas solicitacoes` para o estudante
- `Pedidos recebidos` para o doador, incluindo aprovar, recusar, revogar e concluir
- Inbox local de notificacoes, sininho de nao lidas e deep links para as areas corretas

## Requisitos

- Java 17 ou Java 21
- Android Studio
- Android SDK Platform 34
- Android Build-Tools
- Platform-Tools
- Emulador ou dispositivo fisico

## Configuracao Inicial

1. Abra `local.properties.example`.
2. Crie ou ajuste `local.properties`.
3. Configure `sdk.dir`.
4. Configure `backend.url`.

Exemplo:

```properties
sdk.dir=C\:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
```

Se o backend estiver no WSL e `10.0.2.2` nao funcionar, use o IP atual do Linux:

```powershell
wsl.exe -e bash -lc "hostname -I"
```

e ajuste, por exemplo:

```properties
backend.url=http://172.22.160.34:8080/api
```

## Build E Testes

Na pasta `EcoBookAiAndroid`:

```powershell
.\gradlew.bat app:compileDebugKotlin
.\gradlew.bat assembleDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

O wrapper ASCII continua sendo o caminho mais confiavel para `testDebugUnitTest` nesta workspace com espacos/acentos no caminho do Windows.

Validacao Firebase em emulador/dispositivo:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.fcm.FirebaseRealDeviceValidationTest'
```

## Rodando O App

1. Abra a pasta `EcoBookAiAndroid` no Android Studio.
2. Aguarde o sync do Gradle.
3. Inicie um emulador API 34 ou conecte um dispositivo fisico.
4. Clique em `Run`.

## Firebase

`google-services.json` e opcional para compile e para os fluxos principais de autenticacao, materiais e solicitacoes.

Ele so e necessario quando voce quer validar:

- FCM real
- registro real de token Firebase
- recebimento real de push no aparelho

Coloque o arquivo em:

```text
app/google-services.json
```

## Managed Device

O projeto inclui o managed device `pixel6Api34` para smoke tests automatizados. Exemplos:

```powershell
.\gradlew.bat app:pixel6Api34DebugAndroidTest
.\gradlew.bat app:smokeGroupDebugAndroidTest
```

Para push real, prefira dispositivo fisico ou emulador com Google Play services.

## Validacao Executada

Validado em `2026-05-21`:

- `.\gradlew.bat app:compileDebugKotlin`
- `.\gradlew.bat assembleDebug`
- `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`

Validado em `2026-05-23`:

- `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`
- `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.fcm.FirebaseRealDeviceValidationTest'`
- fluxo real de `token FCM -> sync backend -> solicitacao criada -> notificacao persistida -> recebimento no app` confirmado em `Pixel_6` AVD com Google Play services

## Troubleshooting

- O app nao abre no Android Studio: confirme que a pasta aberta e `EcoBookAiAndroid`, nao a raiz do repositorio.
- O emulador nao encontra o backend: revise `backend.url` e, se preciso, troque `10.0.2.2` pelo IP atual do WSL.
- O push nao chega: verifique `google-services.json`, permissao de notificacao, backend com credencial Admin SDK valida e rode a validacao `FirebaseRealDeviceValidationTest` para isolar se a falha esta no token, no backend ou no recebimento em app.
