# EcoBookAiBackend

Backend Spring Boot do EcoBook AI.

## O Que Este Modulo Entrega

- Autenticacao com `email + senha + JWT`
- Perfil do usuario com onboarding e `perfil_completo`
- Preview IA de material com capa frontal obrigatoria e capa traseira opcional
- Publicacao, listagem, edicao e exclusao de materiais do doador
- Discovery com filtros, ranking geografico e paginacao
- Fluxo de solicitacoes com aprovar, recusar, cancelar, concluir e expiracao automatica
- Registro de token FCM, retry persistente e inbox de notificacoes

## Requisitos

- Java 21
- Maven 3.9+
- Docker Desktop opcional

## Modos De Execucao

### Perfil `local` (recomendado para primeiro boot)

Usa H2 em arquivo, nao depende de Docker e sobe com `Gemini` em modo mock.

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

Na raiz do repositorio:

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
- usuario `ecobook`
- senha vinda de `DB_PASSWORD` ou default `dev_password_123`

Use esse modo quando o processo do backend realmente consegue alcancar o mesmo `localhost:5432` onde o PostgreSQL esta exposto. Se voce estiver no WSL e o banco estiver publicado em outro host/rede, ajuste `SPRING_DATASOURCE_URL` para um endereco alcancavel pelo backend.

## Variaveis Importantes

- `DB_PASSWORD`
- `JWT_SECRET`
- `SERVER_PORT`
- `GEMINI_API_KEY`
- `FIREBASE_SERVICE_ACCOUNT_PATH`
- `FIREBASE_DATABASE_URL`
- `STORAGE_UPLOAD_DIR`

## Smoke Test Validado

O fluxo abaixo foi revalidado no perfil `local` em `2026-05-21`:

1. `POST /api/v1/auth/register`
2. `GET /api/v1/usuarios/me`
3. `PUT /api/v1/usuarios/me`
4. `GET /api/v1/materiais?page=0&size=5`
5. `POST /api/v1/auth/login`

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

Se voce repetir esse smoke test no mesmo banco local, troque o email do exemplo ou limpe os arquivos `EcoBookAiBackend/data/ecobook-local*`.

Exemplo de onboarding:

```bash
curl -X PUT http://127.0.0.1:8080/api/v1/usuarios/me \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Usuario Teste",
    "whatsapp": "+5511999999999",
    "cidade": "Florianopolis",
    "bairro": "Centro",
    "consentimento_ia": true,
    "necessidades_academicas": ["TEXTBOOKS", "WORKBOOKS"]
  }'
```

## Firebase Para Push Real

O backend so envia push real quando existe uma credencial valida do Firebase Admin SDK.

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

Sem essa configuracao, o app continua usando inbox local e a fila persistida de tentativas, mas o envio real fica dormente.

## Contratos E Fontes De Verdade

- Contratos HTTP: `specs/001-ecobook-core/contracts/`
- Migrations: `src/main/resources/db/migration/`
- Perfil local H2: `src/main/resources/application-local.yml`

## Troubleshooting

- `release version 21 not supported`: seu terminal esta usando Java 17 ou inferior; troque para Java 21.
- Emulador nao alcanca o backend no WSL: descubra o IP do Linux com `wsl.exe -e bash -lc "hostname -I"` e use esse IP no app Android.
- Push nao chega ao aparelho: confirme `FIREBASE_SERVICE_ACCOUNT_PATH` e teste em dispositivo/emulador com Google Play services.
