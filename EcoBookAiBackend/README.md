# EcoBookAiBackend

Backend Spring Boot do EcoBook AI.

## O Que Este Módulo Entrega

- Autenticação com `email + senha + JWT`
- Perfil do usuário com onboarding, `perfil_completo`, CPF obrigatório para conclusão e foto de perfil opcional
- Atualização de perfil com troca opcional de e-mail, preservação de consentimentos/dados opcionais omitidos e reautenticação quando a identidade principal muda
- Preview IA de material com capa frontal obrigatória e capa traseira opcional
- Publicação, listagem, edição e exclusão de materiais do doador, incluindo necessidade acadêmica por item
- Discovery com filtros, ranking geográfico, necessidade acadêmica do material e paginação offset/cursor
- Fluxo de solicitações com aprovar, recusar, cancelar, concluir e expiração automática
- Registro de token FCM, retry persistente e inbox de notificações com rotas de abertura mais consistentes
- Consentimentos, exclusão/anonimização de conta, acesso autenticado a imagens e trilha de auditoria
- Observabilidade com `Micrometer`, endpoint `/actuator/prometheus`, smoke suite, cache para leituras autenticadas, catálogo público/cacheado em `/api/v1/reference-data/material-options` e cursor `after_id` na busca de materiais

## Requisitos

- Java 21 ou superior
- Maven 3.9+
- Docker Desktop opcional

## Modos De Execução

### Perfil `local` (recomendado para primeiro boot)

Usa H2 em arquivo, não depende de Docker e sobe com preview mock do `Gemini` apenas quando `GEMINI_API_KEY` não estiver presente.
Se a chave existir no processo, o perfil `local` usa o Gemini real automaticamente.
Para forçar o mock mesmo com a chave configurada, defina `GEMINI_MOCK_FORCE=true`.

Da raiz do repositório, o caminho mais simples é:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Run-BackendLocal.ps1 -JavaHome "C:\Program Files\Java\jdk-26"
```

Equivalente manual dentro de `EcoBookAiBackend`:

```bash
cd EcoBookAiBackend
mvn -q -DskipTests compile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

No Windows PowerShell, use esta forma para evitar o parsing errado do `-D...`:

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-21-ou-superior'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn --% spring-boot:run -Dspring-boot.run.profiles=local
```

Valide:

```bash
curl http://127.0.0.1:8080/api/v1/health
```

### Perfil default (`PostgreSQL + Flyway`)

Na raiz do repositório:

```powershell
docker compose up -d postgres
```

Depois:

```bash
cd EcoBookAiBackend
mvn spring-boot:run
```

Esse perfil espera:

- banco em `jdbc:postgresql://localhost:5432/ecobook`
- usuário `ecobook`
- senha vinda de `DB_PASSWORD` ou default `dev_password_123`

Use esse modo quando o processo do backend realmente consegue alcançar o mesmo `localhost:5432` onde o PostgreSQL está exposto. Se você estiver no WSL e o banco estiver publicado em outro host/rede, ajuste `SPRING_DATASOURCE_URL` para um endereço alcançável pelo backend.

## Variáveis Importantes

- `DB_PASSWORD`
- `JWT_SECRET`
- `SERVER_PORT`
- `GEMINI_API_KEY`
- `FIREBASE_SERVICE_ACCOUNT_PATH`
- `FIREBASE_DATABASE_URL`
- `STORAGE_UPLOAD_DIR`

## Testes Automatizados

O backend agora tenta três caminhos, nesta ordem, quando você roda `mvn test`:

1. `Testcontainers` com PostgreSQL
2. PostgreSQL externo em `localhost:5432`
3. H2 em memória com `LocalH2Dialect`

Com isso, a suíte não depende mais de Docker para rodar em ambientes locais mais restritos.

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-21-ou-superior'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn test
```

Na validação mais recente de `2026-06-03`, o backend ficou verde com `229` testes e `3` skips controlados: `LoadValidationTest` como gate manual e `2` cenários snapshot-only em `MigrationRollbackValidationTest`.

## Swagger / OpenAPI

Com o `server.servlet.context-path=/api`, a documentação publicada do backend fica em:

- `http://localhost:8080/api/swagger-ui.html`
- `http://localhost:8080/api/v3/api-docs`

Os controladores principais agora estão anotados com `@Operation`, `@Parameter`, `@ApiResponse` e `@SecurityRequirement`, e a smoke suite valida que a UI e o JSON OpenAPI seguem publicamente acessíveis sem quebrar os endpoints autenticados.

Na mesma rodada, a cobertura JaCoCo do backend ficou em `84.72%`. Essa baseline segue útil como referência, mas não deve ser apresentada como cumprimento da meta histórica de `85%`.

O fechamento da phase 10 também incluiu a passada uniforme de JavaDoc nos métodos públicos detectados do backend, eliminando o último bloqueio formal de documentação.

Runbook operacional:

- o `README.md` raiz agora documenta a ordem revalidada `backend local -> health -> local.properties -> assemble/lint/tests Android -> Android Studio`
- esse fluxo foi revisto novamente em `2026-06-03` para evitar instruções que dependessem de ajustes manuais escondidos

Load/performance manual (`T217`):

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-21-ou-superior'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn --% -Decobook.runLoadTest=true -Dtest=LoadValidationTest test
```

Esse comando executa `20` uploads concorrentes e `30` buscas concorrentes contra o backend em `RANDOM_PORT`, coleta métricas do Prometheus/Hikari e grava os artefatos em `target/load-reports/`.

Na rodada validada em `2026-06-03`, o resultado local foi:

- `0%` de erro
- `562 ms` de p95 para busca
- `616 ms` de p95 para upload
- `5` conexões Hikari máximas observadas e `0` pendentes

## Smoke Test Validado

O fluxo abaixo foi revalidado no perfil `local` em `2026-06-03`:

1. `POST /api/v1/auth/register`
2. `GET /api/v1/usuarios/me`
3. `PUT /api/v1/usuarios/me`
4. `GET /api/v1/materiais?page=0&size=5`
5. `POST /api/v1/auth/login`

O backend também passou a ter uma smoke suite automatizada (`SmokeTests`) que valida:

1. `GET /api/v1/health`
2. `GET /actuator/health`
3. `POST /api/v1/auth/register` + `PUT /api/v1/usuarios/me` + `GET /api/v1/materiais`
4. `POST /api/v1/materiais/preview`
5. `GET /actuator/prometheus`

E agora também conta com uma suíte consolidada de cenários fim a fim em `src/test/java/com/ecobook/E2ETests.java`, cobrindo 20 fluxos de API para happy path, gates, edge cases, expiry, reautenticação e exclusão de conta.

## Análise Estática E Segurança

Checkstyle baseline:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn --% org.apache.maven.plugins:maven-checkstyle-plugin:3.5.0:check -Dcheckstyle.config.location=config/checkstyle/checkstyle.xml -Dcheckstyle.consoleOutput=true
```

Resultado atual:

- `0` violações

SpotBugs baseline:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn --% com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:spotbugs -DskipTests -Denforcer.skip=true
```

Resultado atual:

- `0` findings de prioridade 1
- `191` findings de prioridade 2, majoritariamente `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` em DTOs mutáveis gerados com Lombok

OWASP Dependency Check:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn --% org.owasp:dependency-check-maven:12.1.0:check -DskipTests -Denforcer.skip=true -Dformats=HTML,JSON -DautoUpdate=false -DknownExploitedEnabled=false -DassemblyAnalyzerEnabled=false -DossindexAnalyzerEnabled=false -DossIndexAnalyzerEnabled=false -DnodeAuditAnalyzerEnabled=false -DnodePackageAnalyzerEnabled=false
```

Resultado atual:

- relatórios em `target/dependency-check-report.html` e `target/dependency-check-report.json`
- baseline reduzido para `13` dependências vulneráveis / `41` findings
- o risco residual aceito desta rodada está detalhado em `../docs/security-scan.md`

Rollback validation:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -Dtest=MigrationRollbackValidationTest test
```

Resultado atual:

- cada migration em `src/main/resources/db/migration` agora possui um artefato pareado em `src/main/resources/db/rollback`
- a suíte valida `apply -> rollback -> re-apply` em PostgreSQL para o subconjunto SQL-backed
- `V4` e `V12` agora falham explicitamente para o fluxo de snapshot/restore, documentado em `../docs/migration-rollbacks.md`

Exemplo de cadastro:

```bash
curl -X POST http://127.0.0.1:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teste@example.com",
    "password": "SenhaSegura123",
    "nome": "Usuario Teste"
  }'
```

Se você repetir esse smoke test no mesmo banco local, troque o e-mail do exemplo ou limpe os arquivos `EcoBookAiBackend/data/ecobook-local*`.

Exemplo de onboarding:

```bash
curl -X PUT http://127.0.0.1:8080/api/v1/usuarios/me \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Usuario Teste",
    "whatsapp": "+5511999999999",
    "cpf": "52998224725",
    "cidade": "Florianopolis",
    "bairro": "Centro",
    "consentimento_ia": true
  }'
```

Observação operacional:

- o uso previsto do app é por adultos responsáveis
- o backend não define ponto de encontro ou conversa dentro da plataforma
- após a aprovação, entrega e retirada são combinadas entre doador e responsável via WhatsApp

## Firebase Para Push Real

O backend só envia push real quando existe uma credencial válida do Firebase Admin SDK.

Opcoes aceitas:

- `FIREBASE_SERVICE_ACCOUNT_PATH`
- `GOOGLE_APPLICATION_CREDENTIALS`

Script PowerShell:

```powershell
cd ..
.\scripts\Run-BackendWithFirebase.ps1 -ServiceAccountPath .\EcoBookAiBackend\credentials\ecobook-adminsdk.json
```

Script WSL:

```bash
cd ..
chmod +x ./scripts/run-backend-with-firebase.sh
./scripts/run-backend-with-firebase.sh /mnt/c/caminho/ecobook-adminsdk.json
```

Sem essa configuração, o app continua usando inbox local e a fila persistida de tentativas, mas o envio real fica dormente.

Validação executada em `2026-05-23`:

- backend `local` iniciado com `Run-BackendWithFirebase.ps1`
- `Pixel_6` AVD com Google Play services e `google-services.json` real no app
- `FirebaseRealDeviceValidationTest` confirmou `registro -> onboarding -> sync do token -> persistência da notificação -> envio FCM -> recebimento no app`

## Observabilidade E Cache

- `GET /actuator/prometheus` expõe métricas HTTP, JVM, HikariCP e contadores de domínio do runtime
- Leituras autenticadas de perfil, status de consentimento e contexto de segurança agora usam cache de 30 minutos
- `GET /api/v1/materiais` segue aceitando `page`/`size`, mas agora também devolve `next_after_id` e aceita `after_id` para continuar a discovery via keyset sem custo crescente de `OFFSET`
- O fluxo de criação de material agora remove arquivos promovidos se a persistência falhar, evitando arquivos órfãos silenciosos

## Contratos E Fontes De Verdade

- Contratos HTTP: `specs/001-ecobook-core/contracts/`
- Migrations: `src/main/resources/db/migration/`
- Code review: `../docs/code-review.md`
- Análise estática: `../docs/static-analysis.md`
- Segurança de dependências: `../docs/security-scan.md`
- Inventário de rollback: `../docs/migration-rollbacks.md`
- Perfil local H2: `src/main/resources/application-local.yml`

## Troubleshooting

- `release version 21 not supported` ou falha do Maven antes dos testes: seu terminal está usando Java 17 ou inferior; troque para Java 21+.
- Emulador não alcança o backend no WSL: descubra o IP do Linux com `wsl.exe -e bash -lc "hostname -I"` e use esse IP no app Android.
- O perfil `local` falha logo no primeiro boot com H2: apague `EcoBookAiBackend/data/ecobook-local*` se estiver carregando um banco legado; o bootstrap local atual foi endurecido para banco vazio, mas arquivos antigos podem manter schema defasado.
- Push não chega ao aparelho: confirme `FIREBASE_SERVICE_ACCOUNT_PATH`, `google-services.json`, permissão de notificação e repita `FirebaseRealDeviceValidationTest` em dispositivo/emulador com Google Play services.
