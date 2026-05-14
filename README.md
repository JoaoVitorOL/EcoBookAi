# EcoBook AI

O repositorio esta organizado em duas bases explicitas:

- `EcoBookAiBackend`: backend Spring Boot, PostgreSQL, Flyway, auth local por email e senha, e seguranca JWT.
- `EcoBookAiAndroid`: aplicativo Android nativo em Kotlin + Jetpack Compose + Hilt + Retrofit.

## O que existe hoje

- O backend ja tem auth local com `email + senha + JWT`, perfil de usuario, migracoes Flyway, `GET /api/v1/health`, fluxo completo de materiais (`preview`, `create`, `search`, `list me`, `update`, `delete`) e fluxo transacional de solicitacoes (`create`, `list`, `approve`, `decline`, `cancel`, `complete`) com expiracao automatica de reservas.
- O Android ja tem fluxo de login/cadastro, onboarding, logout, persistencia segura do JWT, fluxo real de doacao com galeria/camera, preview IA, revisao manual, publicacao final, discovery real conectada ao backend, solicitacao real de materiais, inbox do estudante, inbox do doador, historico do doador com itens doados e liberacao de contato apos aprovacao.
- A base de notificacoes agora cobre o ciclo principal nos dois lados: sincronizacao de token FCM, endpoint backend para registrar token, disparo pos-commit com payload padronizado, fila persistida de retry no backend, notificacoes locais com IDs estaveis, inbox de avisos no Android, sininho com variacao visual por leitura nas telas principais e roteamento de toque para as areas corretas quando o Firebase estiver configurado.
- O fluxo de doacao agora aceita capa da frente obrigatoria e capa de tras opcional no cadastro de materiais; a imagem frontal segue como base da analise inicial da IA e a imagem traseira fica vinculada ao item publicado.
- Estado atual das fases: Fases 1 a 4 concluidas, Fase 5 entregue em runtime e Fase 6 entregue em runtime. O principal pendente antes dos modulos transversais e a validacao ponta a ponta com Firebase real em dispositivo/emulador com Google Play services.

## Diretriz Android

- Toda evolucao do app Android deve seguir referencias e padroes oficiais publicados em `developer.android.com`.
- Arquitetura, gerenciamento de estado, navegacao, permissoes, notificacoes e testes devem priorizar as recomendacoes oficiais do Android antes de convencoes locais.
- Referencias principais:
  - [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
  - [Guide to app architecture](https://developer.android.com/topic/architecture)
  - [UI layer and state holders](https://developer.android.com/topic/architecture/ui-layer/stateholders)
  - [Notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)

## Estado de validacao

- `EcoBookAiAndroid`: `gradlew.bat app:compileDebugKotlin` e `.\EcoBookAiAndroid\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest app:lintDebug app:compileDebugAndroidTestKotlin` executaram com sucesso nesta maquina.
- `EcoBookAiAndroid`: o fluxo instrumentado de auth continua disponivel via `.\EcoBookAiAndroid\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.auth.AuthFlowE2ETest` quando houver um emulador ativo.
- `EcoBookAiBackend`: `mvn -q -DskipTests compile` passou no host e `mvn -q -Dtest=FcmServiceTest test` passou no WSL com Java 21. A suite completa continua dependente de Docker/Testcontainers ou de um PostgreSQL externo de teste.
- Push real nao depende de uma fase futura: a Fase 6 ja cobre o fluxo. Quando ele nao chega ao aparelho, a causa mais comum no ambiente local e backend sem `FIREBASE_SERVICE_ACCOUNT_PATH` valido ou teste em emulador sem Google Play services.

## Proximos passos

- Para rodar o Android Studio: veja `EcoBookAiAndroid/README.md`.
- Para subir o backend local: veja `EcoBookAiBackend/README.md`.
- Para entender os contratos e a visao de produto: veja `specs/001-ecobook-core/`.
