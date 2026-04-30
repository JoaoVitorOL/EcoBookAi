# EcoBook AI

O repositorio agora esta organizado em duas bases explicitas:

- `EcoBookAiBackend`: backend Spring Boot, PostgreSQL, Flyway e seguranca JWT.
- `EcoBookAiAndroid`: aplicativo Android nativo em Kotlin + Jetpack Compose + Hilt + Retrofit.

## O que existe hoje

- O backend ja tem estrutura de dominio, seguranca, DTOs, migracao inicial e `GET /api/v1/health`.
- O Android que existia antes era apenas um placeholder. Agora ele virou uma base navegavel com telas de painel, descoberta, doacao e perfil, conectada ao health check real do backend.
- O wrapper do Gradle para Windows foi adicionado em `EcoBookAiAndroid`, entao o projeto pode ser aberto e executado pelo Android Studio com menos atrito.

## Estado de validacao

- `EcoBookAiAndroid`: `gradlew.bat lintDebug`, `gradlew.bat assembleDebug` e `gradlew.bat testDebugUnitTest` executaram com sucesso nesta maquina.
- `EcoBookAiAndroid`: a validacao em emulador nao foi concluida porque o AVD local ficou offline no ADB.
- `EcoBookAiBackend`: a compilacao nao foi validada aqui porque o comando `mvn` nao esta instalado no ambiente atual.

## Proximos passos

- Para rodar o Android Studio: veja `EcoBookAiAndroid/README.md`.
- Para subir o backend local: veja `EcoBookAiBackend/README.md`.
- Para entender os contratos e a visao de produto: veja `specs/001-ecobook-core/`.
