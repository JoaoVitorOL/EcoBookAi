# EcoBook AI

O repositorio esta organizado em duas bases explicitas:

- `EcoBookAiBackend`: backend Spring Boot, PostgreSQL, Flyway, auth local por email e senha, e seguranca JWT.
- `EcoBookAiAndroid`: aplicativo Android nativo em Kotlin + Jetpack Compose + Hilt + Retrofit.

## O que existe hoje

- O backend ja tem estrutura de dominio, seguranca, DTOs, migracao inicial e `GET /api/v1/health`.
- O Android ja tem base navegavel, telas principais e integracao com o health check do backend.
- O projeto esta em transicao documental de auth: a direcao oficial agora e **email + senha + JWT**, substituindo o fluxo anterior com Google.

## Estado de validacao

- `EcoBookAiAndroid`: `gradlew.bat lintDebug`, `gradlew.bat assembleDebug` e `gradlew.bat testDebugUnitTest` executaram com sucesso nesta maquina.
- `EcoBookAiAndroid`: a validacao em emulador nao foi concluida porque o AVD local ficou offline no ADB.
- `EcoBookAiBackend`: `mvn clean test` executou com sucesso nesta maquina usando um JDK mais novo do que o Java padrao da sessao.

## Proximos passos

- Para rodar o Android Studio: veja `EcoBookAiAndroid/README.md`.
- Para subir o backend local: veja `EcoBookAiBackend/README.md`.
- Para entender os contratos e a visao de produto: veja `specs/001-ecobook-core/`.
