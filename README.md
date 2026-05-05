# EcoBook AI

O repositorio esta organizado em duas bases explicitas:

- `EcoBookAiBackend`: backend Spring Boot, PostgreSQL, Flyway, auth local por email e senha, e seguranca JWT.
- `EcoBookAiAndroid`: aplicativo Android nativo em Kotlin + Jetpack Compose + Hilt + Retrofit.

## O que existe hoje

- O backend ja tem auth local com `email + senha + JWT`, perfil de usuario, migracoes Flyway e `GET /api/v1/health`.
- O Android ja tem fluxo de login/cadastro, onboarding, logout, persistencia segura do JWT e integracao com o health check do backend.
- Materiais, preview com IA, matching e solicitacoes continuam como os proximos modulos do MVP.

## Estado de validacao

- `EcoBookAiAndroid`: `gradlew.bat lintDebug`, `gradlew.bat assembleDebug` e `gradlew.bat testDebugUnitTest` executaram com sucesso nesta maquina.
- `EcoBookAiAndroid`: a validacao em emulador nao foi concluida porque o AVD local ficou offline no ADB.
- `EcoBookAiBackend`: `mvn clean test` executou com sucesso nesta maquina usando um JDK mais novo do que o Java padrao da sessao.

## Proximos passos

- Para rodar o Android Studio: veja `EcoBookAiAndroid/README.md`.
- Para subir o backend local: veja `EcoBookAiBackend/README.md`.
- Para entender os contratos e a visao de produto: veja `specs/001-ecobook-core/`.
