# Levantamento Tecnologico do EcoBook

Data da auditoria: `2026-06-04`

## 1. Resposta curta

O projeto EcoBook usa:

- Android nativo com `Kotlin + Jetpack Compose`
- Backend REST com `Java 21 + Spring Boot`
- Banco principal `PostgreSQL`
- Banco alternativo/local `H2`
- Autenticacao com `email/senha + JWT`
- IA com `Google Gemini`, chamada pelo backend via `OkHttp`
- Notificacoes push com `Firebase Cloud Messaging (FCM)`

Resposta objetiva sobre banco local Android:

- **Nao, o projeto nao usa `RoomDatabase`.**
- Tambem **nao usa SQLite manual, `SQLiteOpenHelper`, `Realm`, `ObjectBox` ou `DataStore`**.
- No Android, a persistencia local atual e feita com `EncryptedSharedPreferences`, principalmente para sessao, token JWT, dados basicos do usuario, token FCM e inbox local de notificacoes.

## 2. Como esta auditoria foi feita

O levantamento foi baseado em:

- dependencias e plugins em `EcoBookAiAndroid/app/build.gradle.kts`
- dependencias Maven em `EcoBookAiBackend/pom.xml`
- configuracoes em `EcoBookAiBackend/src/main/resources/application.yml`, `application-local.yml` e `application-test.yml`
- infraestrutura local em `docker-compose.yml`
- uso real no codigo-fonte Android e backend
- buscas globais no codigo para confirmar o que existe e o que nao existe

Buscas sem ocorrencias no codigo-fonte atual:

- `RoomDatabase`
- `androidx.room`
- `@Database`
- `Room.databaseBuilder`
- `SQLiteOpenHelper`
- `DataStore`
- `WorkManager`
- `Paging`
- `Firestore`
- `FirebaseAuth`
- `GoogleSignIn`
- `Realm`
- `ObjectBox`
- `Supabase`

## 3. Tecnologias em uso no Android

| Tecnologia | Onde aparece | Finalidade | Como foi usada | Justificativa |
|---|---|---|---|---|
| `Kotlin` | `EcoBookAiAndroid/app/build.gradle.kts:5-9` | Linguagem principal do app Android | Toda a aplicacao Android foi escrita em Kotlin | Boa integracao com Android moderno, coroutines e Jetpack |
| `Jetpack Compose` | `EcoBookAiAndroid/app/build.gradle.kts:86-93`, `134-148`; `EcoBookAiAndroid/app/src/main/kotlin/com/ecobook/MainActivity.kt:66-77` | UI nativa declarativa | As telas sao renderizadas via `setContent`, componentes Compose e Material 3 | Reduz boilerplate e facilita manutencao de UI moderna |
| `Material 3` | `EcoBookAiAndroid/app/build.gradle.kts:140`; `EcoBookAiAndroid/app/src/main/kotlin/com/ecobook/navigation/NavGraph.kt:17-23` | Design system da interface | Navegacao, scaffold, top bars, bottom bars e componentes visuais | Padrao atual do Android, consistencia visual e acessibilidade |
| `Navigation Compose` | `EcoBookAiAndroid/app/build.gradle.kts:143-145`; `EcoBookAiAndroid/app/src/main/kotlin/com/ecobook/navigation/NavGraph.kt:38-40`, `149-257` | Navegacao entre telas | Rotas para login, onboarding, busca, doacao, pedidos, perfil e notificacoes | Navegacao declarativa integrada ao Compose |
| `Lifecycle + ViewModel + StateFlow` | `EcoBookAiAndroid/app/build.gradle.kts:129-130`, `147-148`; `MainActivity.kt:67-69`; `SessionManager.kt:19-20`; `DiscoveryViewModel.kt:34-40` | Gerenciamento de estado de tela e sessao | ViewModels expoem `StateFlow` e telas observam com `collectAsStateWithLifecycle()` | Mantem estado reativo e seguro ao ciclo de vida |
| `Coroutines` | `EcoBookAiAndroid/app/build.gradle.kts:171-173`; varios `suspend fun` em `api/*` e `data/*` | Programacao assincrona | Chamadas HTTP, sincronizacao e fluxo de UI | Simples, idiomatico em Kotlin e adequado para IO |
| `Hilt / Dagger` | `EcoBookAiAndroid/app/build.gradle.kts:159-162`; `EcoBookAiAndroid/app/src/main/kotlin/com/ecobook/EcoBookApp.kt:12`; `MainActivity.kt:36`; `di/Modules.kt:32-144` | Injecao de dependencia | Injeta repositorios, clientes HTTP, gerenciadores de sessao e FCM | Facilita organizacao, teste e desacoplamento |
| `Retrofit` | `EcoBookAiAndroid/app/build.gradle.kts:153-156`; `di/Modules.kt:83-92`; `api/AuthApiService.kt:24-52` | Cliente HTTP tipado para a API REST | Interfaces de API para auth, materiais, pedidos, notificacoes, FCM e dados de referencia | Simplifica integracao com o backend REST |
| `OkHttp` | `EcoBookAiAndroid/app/build.gradle.kts:156-157`; `di/Modules.kt:55-70`; `api/AuthInterceptor.kt:14-29` | Camada HTTP e interceptacao | Adiciona JWT automaticamente e trata `401` limpando a sessao | Controle fino da rede e integracao natural com Retrofit |
| `Gson` | `EcoBookAiAndroid/app/build.gradle.kts:155`, `176`; `di/Modules.kt:17-19`, `75-79`; `NotificationInboxRepository.kt:3-4`, `92-110` | Serializacao JSON | Converte payloads da API e persiste inbox de notificacoes em JSON local | Leve e suficiente para o modelo atual do app |
| `AndroidX Security Crypto` | `EcoBookAiAndroid/app/build.gradle.kts:150-151`; `utils/SecureStorage.kt:4-5`, `229-237` | Persistencia local segura | Guarda JWT, dados do usuario, token FCM, preferencias e payload local de notificacoes em `EncryptedSharedPreferences` | Protege dados sensiveis sem precisar de banco relacional local |
| `Firebase Cloud Messaging` | `EcoBookAiAndroid/app/build.gradle.kts:164-167`; `EcoBookAiAndroid/app/src/main/kotlin/com/ecobook/fcm/EcoBookMessagingService.kt:8-15`, `26-76` | Recebimento de push notifications | O app recebe mensagens, grava inbox local e exibe notificacoes do sistema | Necessario para avisos de pedidos, aprovacoes e eventos do app |
| `Coil` | `EcoBookAiAndroid/app/build.gradle.kts:168-169`; `discovery/MaterialListItem.kt:31-32`, `184-191` | Carregamento de imagens | Renderiza capas dos materiais e outras imagens remotas | Biblioteca moderna e otimizada para Compose |
| `Timber` | `EcoBookAiAndroid/app/build.gradle.kts:178-179`; `EcoBookApp.kt:24-27`; `EcoBookMessagingService.kt:12`, `27`, `44-47` | Logging no app | Logs de debug, notificacoes, falhas de storage e fluxo FCM | Logging mais limpo do que `Log.*` |

## 4. Tecnologias em uso no backend

| Tecnologia | Onde aparece | Finalidade | Como foi usada | Justificativa |
|---|---|---|---|---|
| `Java 21` | `EcoBookAiBackend/pom.xml:26-28`, `328-329`, `354-356` | Linguagem e runtime do backend | Todo o backend Spring Boot roda em Java 21+ | LTS moderno, bom suporte para Spring Boot 3.x |
| `Spring Boot` | `EcoBookAiBackend/pom.xml:16-21`, `47-97` | Base do backend | Estrutura REST, configuracao, seguranca, cache, actuator e testes | Framework maduro e produtivo para APIs Java |
| `Spring Web` | `EcoBookAiBackend/pom.xml:47-51`; `controller/AuthController.java:22-80` e demais controllers | API REST | Controllers HTTP para auth, usuario, materiais, pedidos, notificacoes, FCM e relatorios | Boa separacao de camadas e integracao com validacao e seguranca |
| `Spring Data JPA` | `EcoBookAiBackend/pom.xml:53-56`; `model/Usuario.java:46-62`; `model/Material.java:45-67`; `repository/UsuarioRepository.java:20-40`; `repository/MaterialRepository.java:19-33` | Persistencia relacional | Mapeamento por entidades e repositorios JPA | Reduz SQL manual e acelera o desenvolvimento do dominio |
| `Hibernate` | transitivo de `spring-boot-starter-data-jpa`; uso em entidades com `@Entity`, `@JdbcTypeCode`, `@SQLRestriction` | ORM | Mapeia enums, relacionamentos e entidades do dominio para o banco | Integracao natural com Spring Data JPA |
| `Spring Security` | `EcoBookAiBackend/pom.xml:58-61`; `config/SecurityConfig.java:27-115`; `security/JwtAuthenticationFilter.java:21-80` | Protecao dos endpoints | Cadeia stateless com filtro JWT e regras de autorizacao | Necessario para API autenticada segura |
| `BCrypt` | `config/SecurityConfig.java:14-15`, `49-52`; `service/AuthService.java:68-80` | Hash de senha | Senhas sao armazenadas como hash e verificadas no login | Muito mais seguro do que guardar senha em texto puro |
| `JWT / JJWT` | `EcoBookAiBackend/pom.xml:123-142`; `security/JwtTokenProvider.java:22-160`; `service/AuthService.java:88-115` | Sessao autenticada | O backend emite e valida tokens JWT com claims de role, usuario e perfil | Solucao leve e adequada para API stateless |
| `PostgreSQL` | `application.yml:5-9`; `docker-compose.yml:2-18`; `pom.xml:99-105` | Banco principal do sistema | Persiste usuarios, materiais, solicitacoes, notificacoes, consentimentos, auditoria e afins | Banco relacional robusto, confiavel e alinhado a JPA/Flyway |
| `H2` | `pom.xml:107-111`; `application-local.yml:2-15`; `application-test.yml:1-19` | Banco alternativo local e fallback de teste | Perfil `local` usa H2 em arquivo; testes podem cair para H2 | Facilita desenvolvimento/teste rapido sem depender sempre de Postgres |
| `Flyway` | `pom.xml:113-116`, `297-307`; `application.yml:35-39`; migracoes em `src/main/resources/db/migration` | Versionamento do schema | O schema e evoluido por migracoes versionadas (`V1` a `V19`) | Controle historico do banco e reproducao do ambiente |
| `HikariCP` | `pom.xml:118-121`; `config/DataSourceConfig.java:3-4`, `30-36`; `application.yml:10-17` | Pool de conexoes | Configura o datasource com pool para performance e estabilidade | Padrao eficiente para APIs Java com banco relacional |
| `OkHttp` | `pom.xml:180-185`; `service/GeminiService.java:12-15`, `280-287`, `355-359` | Cliente HTTP do backend | O backend chama a API do Gemini via HTTP manual | Simples, direto e suficiente para integracao externa |
| `Google Gemini` | `application.yml:73-83`; `service/GeminiService.java:37-40`, `98-131`, `283-337` | Classificacao assistida por IA de materiais | Backend envia imagens e interpreta resposta do Gemini | Evita OCR/classificacao propria e acelera o MVP |
| `Firebase Admin SDK` | `pom.xml:153-178`; `service/FcmService.java:8-15`, `37-40`, `232-259` | Envio de push notifications | Backend envia notificacoes FCM para os dispositivos | Integracao padrao para push no Android |
| `Spring Validation` | `pom.xml:83-86`; `controller/AuthController.java:14`, `47`, `73`; `model/Usuario.java:22-25` | Validacao de entrada e dominio | DTOs e entidades validam email, CPF, WhatsApp e payloads | Reduz dados invalidos e melhora retorno ao cliente |
| `Spring Cache + Caffeine` | `pom.xml:68-81`; `config/CacheConfig.java:17-47`; `service/ReferenceDataService.java:11`, `25-33` | Cache de leituras quentes | Cache para perfil, consentimento, auth-context e dados de referencia | Diminui leituras repetidas e melhora tempo de resposta |
| `Micrometer + Prometheus + Actuator` | `pom.xml:63-76`; `application.yml:101-121`; `event/RuntimeMetricsListener.java:16-57` | Observabilidade | Exposicao de health, metrics e contadores de eventos de negocio | Ajuda monitoramento e diagnostico do backend |
| `OpenAPI / Swagger (springdoc)` | `pom.xml:93-97`; `config/OpenApiConfig.java:17-55`; `application.yml:138-142` | Documentacao da API | Gera `/api/v3/api-docs` e `/api/swagger-ui.html` | Facilita teste, onboarding e apresentacao tecnica |
| `Jackson` | `pom.xml:195-204`; `config/SecurityConfig.java:4`, `33`, `127-128`; `GeminiService.java:7-9`, `64` | Serializacao JSON no backend | Serializa respostas, erros e parseia payloads externos | Integracao padrao do ecossistema Spring |
| `Lombok` | `pom.xml:187-193`; uso em entidades, services e DTOs | Reducao de boilerplate | `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, etc. | Aumenta produtividade e legibilidade |

## 5. Banco de dados e persistencia: o que esta sendo usado de fato

### 5.1 Banco principal do sistema

O banco principal do projeto e `PostgreSQL`.

Evidencias:

- `EcoBookAiBackend/src/main/resources/application.yml:5-9`
- `docker-compose.yml:2-18`
- `EcoBookAiBackend/pom.xml:99-105`

Isso significa que os dados oficiais do sistema ficam no backend, em banco relacional. Exemplos:

- usuarios
- materiais
- solicitacoes
- consentimentos
- notificacoes
- fila de notificacoes com falha
- relatorios de nao recebimento
- logs de auditoria
- tokens JWT revogados

### 5.2 Banco alternativo/local

O projeto tambem usa `H2`, mas **nao como banco principal de producao**.

Uso do H2:

- desenvolvimento rapido local sem Postgres: `application-local.yml:2-15`
- testes e fallback: `application-test.yml:1-19`

Ou seja:

- `PostgreSQL` = banco principal real
- `H2` = banco auxiliar para desenvolvimento/testes

### 5.3 Migracao de schema

O schema do banco e controlado por `Flyway`.

Evidencias:

- `EcoBookAiBackend/pom.xml:113-116`, `297-307`
- `EcoBookAiBackend/src/main/resources/application.yml:35-39`
- migracoes versionadas em `EcoBookAiBackend/src/main/resources/db/migration`

Isso mostra que o banco nao esta sendo tratado "na mao"; existe evolucao versionada do schema.

### 5.4 Persistencia local no Android

No Android, **nao existe banco local relacional**.

Em vez disso, o app usa `EncryptedSharedPreferences` via `SecureStorage`.

Evidencias:

- `EcoBookAiAndroid/app/src/main/kotlin/com/ecobook/utils/SecureStorage.kt:19-29`
- `SecureStorage.kt:229-237`
- `SessionManager.kt:24-39`, `84-93`
- `SecureNotificationInboxStore.kt:8-16`
- `NotificationInboxRepository.kt:21-22`, `86-110`

O que e salvo localmente:

- token JWT
- id, nome, email e role do usuario
- flag de perfil completo
- dados de perfil basicos
- consentimento de IA
- token FCM descoberto/sincronizado
- override de tema
- inbox local de notificacoes em JSON

Conclusao tecnica:

- **Nao usa `RoomDatabase`**
- **Nao usa banco SQL local no Android**
- **Usa chave-valor criptografado**

## 6. Tecnologias de teste, build e operacao

| Tecnologia | Onde aparece | Para que serve | Situacao |
|---|---|---|---|
| `Gradle Kotlin DSL` | `EcoBookAiAndroid/app/build.gradle.kts` | Build do app Android | Em uso |
| `Maven` | `EcoBookAiBackend/pom.xml` | Build, testes e plugins do backend | Em uso |
| `Docker Compose` | `docker-compose.yml` | Subir Postgres localmente | Em uso de desenvolvimento |
| `JUnit 4` | `EcoBookAiAndroid/app/build.gradle.kts:181-184` | Testes unitarios Android | Em uso |
| `MockK` | `EcoBookAiAndroid/app/build.gradle.kts:183` | Mock em testes Kotlin/Android | Em uso |
| `kotlinx-coroutines-test` | `EcoBookAiAndroid/app/build.gradle.kts:184` | Testar coroutines | Em uso |
| `Espresso` | `EcoBookAiAndroid/app/build.gradle.kts:187-195` | Testes Android instrumentados | Em uso |
| `Compose UI Test` | `EcoBookAiAndroid/app/build.gradle.kts:193-196` | Testes de UI Compose | Em uso |
| `Hilt Android Testing` | `EcoBookAiAndroid/app/build.gradle.kts:190-192` | Testes com injecao de dependencia | Em uso |
| `MockWebServer` | `EcoBookAiAndroid/app/build.gradle.kts:191` | Mock HTTP em testes Android | Em uso |
| `Spring Boot Test` | `EcoBookAiBackend/pom.xml:213-223` | Testes do backend | Em uso |
| `Spring Security Test` | `EcoBookAiBackend/pom.xml:225-229` | Testes de seguranca | Em uso |
| `Mockito` | `EcoBookAiBackend/pom.xml:261-272` | Mocks nos testes Java | Em uso |
| `JUnit 5` | `EcoBookAiBackend/pom.xml:274-291` | Suite moderna de testes backend | Em uso |
| `REST Assured` | `EcoBookAiBackend/pom.xml:253-259` | Testes de API HTTP | Em uso |
| `Testcontainers` | `EcoBookAiBackend/pom.xml:231-251` | Testes com infraestrutura efemera | Em uso de teste |
| `JaCoCo` | `EcoBookAiBackend/pom.xml:364-405`; workflow CI | Cobertura de testes | Em uso |

## 7. O que esta presente apenas como apoio, opcional ou fallback

| Tecnologia / item | Situacao real no projeto | Observacao |
|---|---|---|
| `H2` | Em uso, mas nao como banco principal | Serve para `local` e fallback de testes |
| `google-services.json` | Opcional para o core do app | Necessario para validar push real do Firebase, nao para os fluxos principais |
| `Firebase` no Android | Recurso funcional, mas concentrado em FCM | Nao e a base de autenticacao nem de persistencia do app |
| `firebase.database-url` no backend | Parametro opcional do `FirebaseOptions` | O codigo nao implementa Firestore ou Realtime Database como banco do produto |
| `Gemini mock mode` | Em uso em local/teste | Permite continuar o desenvolvimento sem depender sempre da API real |

## 8. O que NAO esta sendo usado no projeto atual

### 8.1 Nao usado no Android

| Tecnologia | Status | Motivo da conclusao |
|---|---|---|
| `RoomDatabase / Android Room` | **Nao usado** | Nao existe dependencia `androidx.room` em `EcoBookAiAndroid/app/build.gradle.kts` e nao ha ocorrencias de `RoomDatabase`, `@Database` ou `Room.databaseBuilder` no codigo |
| `SQLiteOpenHelper` | **Nao usado** | Busca global sem ocorrencias |
| `DataStore` | **Nao usado** | Busca global sem ocorrencias; persistencia atual e via `EncryptedSharedPreferences` |
| `WorkManager` | **Nao usado** | Busca global sem ocorrencias |
| `Paging 3` | **Nao usado** | Busca global sem ocorrencias de `Pager`, `PagingSource` ou `RemoteMediator` |
| `Realm` | **Nao usado** | Busca global sem ocorrencias |
| `ObjectBox` | **Nao usado** | Busca global sem ocorrencias |
| `Supabase` | **Nao usado** | Busca global sem ocorrencias |

### 8.2 Nao usado como autenticacao atual

| Tecnologia | Status | Motivo da conclusao |
|---|---|---|
| `Google OAuth2` | **Nao usado no runtime atual** | A autenticacao real atual esta em `AuthService.java:24-128` com email/senha + JWT |
| `Firebase Auth` | **Nao usado** | Busca global sem ocorrencias no codigo-fonte atual |
| `Google Sign-In` | **Nao usado** | Busca global sem ocorrencias no codigo-fonte atual |

### 8.3 Nao usado como banco principal ou backend adicional

| Tecnologia | Status | Motivo da conclusao |
|---|---|---|
| `Firestore` | **Nao usado** | Nao ha repositorios/servicos/queries dessa tecnologia |
| `Firebase Realtime Database` | **Nao usado como persistencia do produto** | Existe apenas o campo opcional `firebase.database-url`; o projeto nao implementa leitura/escrita nesse banco |
| `Redis` | **Nao usado** | Busca global sem ocorrencias e cache atual e `Caffeine` em memoria |
| `RabbitMQ` | **Nao usado** | Busca global sem ocorrencias |
| `Kafka` | **Nao usado** | Busca global sem ocorrencias |
| `GraphQL` | **Nao usado** | A interface do backend e REST com controllers Spring |
| `WebSocket` | **Nao usado** | Busca global sem ocorrencias |

### 8.4 Nao usado para Gemini

| Tecnologia | Status | Motivo da conclusao |
|---|---|---|
| SDK oficial `google-generativeai` | **Nao usado** | A dependencia esta comentada em `EcoBookAiBackend/pom.xml:144-151`; a integracao real usa `OkHttp` em `GeminiService.java:280-287` |

## 9. Resposta pronta para a pergunta "esta usando RoomDatabase?"

Pode responder assim:

> Nao. O app Android nao usa `RoomDatabase`. A persistencia local atual e feita com `EncryptedSharedPreferences`, usada para guardar JWT, sessao, dados basicos do usuario, token FCM e a inbox local de notificacoes em JSON. O banco principal do sistema fica no backend e e `PostgreSQL`. Para desenvolvimento local e alguns cenarios de teste, o backend tambem suporta `H2`.

## 10. Conclusao final

O EcoBook tem uma arquitetura bem definida:

- `Android nativo` para experiencia do usuario
- `API REST Spring Boot` para regras de negocio
- `PostgreSQL` como banco principal
- `JWT` para autenticacao stateless
- `Gemini` para classificacao assistida de materiais
- `FCM` para notificacoes push

Do ponto de vista da tua pergunta principal, a resposta mais importante e:

- **o projeto usa PostgreSQL no backend**
- **usa H2 como apoio local/teste**
- **nao usa RoomDatabase no Android**
- **o Android persiste localmente apenas via armazenamento criptografado por chave-valor**
