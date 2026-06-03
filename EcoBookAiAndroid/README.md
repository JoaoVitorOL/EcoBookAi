# EcoBookAiAndroid

Aplicativo Android nativo do EcoBook AI.

## O Que O App Entrega Hoje

- Login, cadastro, restauração de sessão e logout com `JWT`
- Onboarding com WhatsApp, CPF obrigatório do adulto responsável, cidade, bairro, instituição e consentimento de IA
- Doação com imagem frontal obrigatória, imagem traseira opcional, preview IA, necessidade acadêmica manual e publicação final
- Busca de materiais com filtros, paginação e diálogo de detalhes
- `Minhas solicitações` para o estudante
- `Pedidos recebidos` para o doador, incluindo aprovar, recusar, revogar e concluir
- Inbox local de notificações, sininho de não lidas e ações de abertura apenas quando existe um destino útil
- Edição de perfil com foto, gerenciamento de consentimentos e exclusão da conta

## Aviso De Uso

O app foi desenhado para ser operado por pais, mães e responsáveis legais dos alunos que precisam dos materiais.

O EcoBook não define ponto de encontro dentro da plataforma. Quando uma solicitação é aprovada, doador e responsável combinam conversa, entrega e retirada exclusivamente pelo WhatsApp.

## Requisitos

- Java 17 ou Java 21
- Android Studio
- Android SDK Platform 34
- Android Build-Tools
- Platform-Tools
- Emulador ou dispositivo físico

## Configuração Inicial

1. Abra `local.properties.example`.
2. Crie ou ajuste `local.properties`.
3. Configure `sdk.dir`.
4. Configure `backend.url`.

Exemplo:

```properties
sdk.dir=C\:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
```

Se o backend estiver no WSL e `10.0.2.2` não funcionar, use o IP atual do Linux:

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
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:compileDebugKotlin
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:assembleDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:lintDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

O wrapper ASCII agora é o caminho recomendado para os comandos principais do Gradle nesta workspace com espaços/acentos no caminho do Windows. Em `2026-06-03`, ele passou a serializar a execução inteira do Gradle para evitar corridas entre aliases temporários.

Validacao Firebase em emulador/dispositivo:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.fcm.FirebaseRealDeviceValidationTest'
```

## Rodando O App

1. Abra a pasta `EcoBookAiAndroid` no Android Studio.
2. Aguarde o sync do Gradle.
3. Inicie um emulador API 34 ou conecte um dispositivo físico.
4. Clique em `Run`.

## Firebase

`google-services.json` é opcional para compile e para os fluxos principais de autenticação, materiais e solicitações.

Ele só é necessário quando você quer validar:

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

Para push real, prefira dispositivo físico ou emulador com Google Play services.

## Validacao Executada

Validado em `2026-05-21`:

- `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:compileDebugKotlin`
- `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:assembleDebug`
- `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`

Validado em `2026-05-23`:

- `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:assembleDebug`
- `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:lintDebug`
- `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`
- `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.fcm.FirebaseRealDeviceValidationTest'`
- fluxo real de `token FCM -> sync backend -> solicitação criada -> notificação persistida -> recebimento no app` confirmado em `Pixel_6` AVD com Google Play services

Validado em `2026-06-03`:

- `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:lintDebug`
- `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`
- revisão do texto legal exibido no app para mantê-lo fiel ao fluxo atual de privacidade

Lint/UI baseline atual:

- `0` erros
- `20` avisos não bloqueantes
- detalhes da revisão em `../docs/android-ui-review.md`

## Troubleshooting

- O app não abre no Android Studio: confirme que a pasta aberta é `EcoBookAiAndroid`, não a raiz do repositório.
- O emulador não encontra o backend: revise `backend.url` e, se preciso, troque `10.0.2.2` pelo IP atual do WSL.
- O push não chega: verifique `google-services.json`, permissão de notificação, backend com credencial Admin SDK válida e rode a validação `FirebaseRealDeviceValidationTest` para isolar se a falha está no token, no backend ou no recebimento em app.
