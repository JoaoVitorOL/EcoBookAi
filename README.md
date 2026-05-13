# EcoBook AI

O repositorio esta organizado em duas bases explicitas:

- `EcoBookAiBackend`: backend Spring Boot, PostgreSQL, Flyway, auth local por email e senha, e seguranca JWT.
- `EcoBookAiAndroid`: aplicativo Android nativo em Kotlin + Jetpack Compose + Hilt + Retrofit.

## O que existe hoje

- O backend ja tem auth local com `email + senha + JWT`, perfil de usuario, migracoes Flyway, `GET /api/v1/health`, `POST /api/v1/materiais/preview`, `POST /api/v1/materiais` e `GET /api/v1/materiais`.
- O Android ja tem fluxo de login/cadastro, onboarding, logout, persistencia segura do JWT, fluxo real de doacao com galeria/camera, preview IA, revisao manual, publicacao final e discovery real conectada ao backend.
- Solicitacoes e notificacoes continuam como os proximos modulos do MVP.

## Estado de validacao

- `EcoBookAiAndroid`: `gradlew.bat app:compileDebugKotlin` e `.\EcoBookAiAndroid\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest app:lintDebug app:compileDebugAndroidTestKotlin` executaram com sucesso nesta maquina.
- `EcoBookAiAndroid`: o fluxo instrumentado de auth continua disponivel via `.\EcoBookAiAndroid\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.auth.AuthFlowE2ETest` quando houver um emulador ativo.
- `EcoBookAiBackend`: `mvn test` executou com sucesso nesta maquina usando Java 21 no WSL, com PostgreSQL real de teste.

## Proximos passos

- Para rodar o Android Studio: veja `EcoBookAiAndroid/README.md`.
- Para subir o backend local: veja `EcoBookAiBackend/README.md`.
- Para entender os contratos e a visao de produto: veja `specs/001-ecobook-core/`.
