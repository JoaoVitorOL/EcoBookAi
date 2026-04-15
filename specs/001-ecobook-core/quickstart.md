# Quickstart Guide: EcoBook IA Development

**Phase**: 1 (Design & Contracts)  
**Date**: 2026-04-15  
**Purpose**: Setup local development environment and run first integration test

---

## Prerequisites

### Backend (Spring Boot)
- **Java**: JDK 17+ (LTS recommended)
  - Verify: `java -version`
  - Install: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://jdk.java.net)

- **PostgreSQL**: 14+ (database)
  - Verify: `psql --version`
  - Install: [PostgreSQL Downloads](https://www.postgresql.org/download/)
  - OR use Docker (see below)

- **Maven** or **Gradle**: Build tool
  - Maven 3.8+: `mvn --version`
  - Gradle 7.x+: `gradle --version`

- **Docker** (optional, recommended for PostgreSQL):
  - Verify: `docker --version`
  - Install: [Docker Desktop](https://www.docker.com/products/docker-desktop/)

- **Git**: Version control
  - Verify: `git --version`

### Android (Kotlin/Jetpack Compose)
- **Android Studio**: 2023.1 or newer
  - Download: [Android Studio](https://developer.android.com/studio)
  - Install: Follow wizard, accept all SDK components

- **Android SDK**: minSdk=26 (Android 8.0), targetSdk=34
  - Via Android Studio: Settings → SDK Manager → ensure API 26, 34 installed

- **Emulator or Physical Device**: For testing
  - Emulator: Android Studio → Device Manager → Create device (Pixel 6, API 34)
  - Physical: Enable USB debugging, connect via USB

---

## Project Setup

### 1. Clone Repository

```bash
cd ~/projects
git clone https://github.com/yourepo/ecobook-ia.git
cd ecobook-ia
git checkout 001-ecobook-core
```

### 2. Backend Environment Setup

#### Option A: Docker (Recommended)

```bash
# Create docker-compose.yml in project root
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: ecobook-db
    environment:
      POSTGRES_DB: ecobook
      POSTGRES_USER: ecobook
      POSTGRES_PASSWORD: dev_password_123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ecobook"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
EOF

# Start PostgreSQL
docker-compose up -d postgres

# Verify connection
sleep 5
psql -h localhost -U ecobook -d ecobook -c "SELECT 1;"
```

#### Option B: Local PostgreSQL Installation

```bash
# macOS (Homebrew)
brew install postgresql@15
brew services start postgresql@15

# Linux (Ubuntu)
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql

# Windows
# Use PostgreSQL installer or Windows Subsystem for Linux (WSL)

# Create database and user
psql postgres
CREATE DATABASE ecobook;
CREATE USER ecobook WITH PASSWORD 'dev_password_123';
ALTER ROLE ecobook WITH CREATEDB;
\q
```

#### 3. Backend Configuration

```bash
cd backend

# Create .env file with secrets
cat > .env << 'EOF'
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/ecobook
DATABASE_USER=ecobook
DATABASE_PASSWORD=dev_password_123

# Google OAuth2
GOOGLE_CLIENT_ID=<your-google-client-id>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<your-google-client-secret>

# Google Gemini API
GEMINI_API_KEY=<your-gemini-api-key>

# Firebase Cloud Messaging
FCM_PROJECT_ID=<your-firebase-project-id>
FCM_SERVICE_ACCOUNT_JSON=$(cat /path/to/firebase-service-account.json)

# JWT Secret
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters-long
EOF

# Setup Google OAuth2 credentials
# 1. Go to https://console.cloud.google.com/
# 2. Create new project "EcoBook IA"
# 3. APIs & Services → Credentials → Create OAuth 2.0 Client ID
# 4. Application type: Desktop (for testing)
# 5. Copy Client ID and Secret to .env

# Setup Gemini API
# 1. Go to https://makersuite.google.com/app/apikey
# 2. Create new API key
# 3. Copy to .env

# Setup Firebase
# 1. Go to https://console.firebase.google.com/
# 2. Create new project "EcoBook IA"
# 3. Enable Cloud Messaging (Messaging tab)
# 4. Service Accounts tab → Generate service account key (JSON)
# 5. Save JSON file, copy path to .env
```

#### 4. Build & Run Backend

```bash
# Navigate to backend directory
cd backend

# Using Maven
mvn clean install
mvn spring-boot:run

# OR using Gradle
gradle clean build
gradle bootRun

# Verify backend is running
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

**If error**: Check logs for database connection issues
```bash
# Debug database connection
psql -h localhost -U ecobook -d ecobook -c "SELECT NOW();"
```

---

### 5. Android Environment Setup

#### Google Services JSON

```bash
cd android/app

# Download google-services.json from Firebase Console
# Project Settings → Your Apps → Android → Download google-services.json
# Place in: android/app/

ls -la google-services.json  # Verify file exists
```

#### Local Build Properties

```bash
cd android

# Create local.properties
cat > local.properties << 'EOF'
sdk.dir=/Users/yourname/Library/Android/sdk
EOF

# On Linux:
cat > local.properties << 'EOF'
sdk.dir=/home/yourname/Android/Sdk
EOF

# On Windows:
cat > local.properties << 'EOF'
sdk.dir=C:\\Users\\yourname\\AppData\\Local\\Android\\Sdk
EOF
```

#### API Configuration

```bash
cat > app/src/main/res/values/api_config.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="backend_url">http://10.0.2.2:8080</string>
    <!-- Note: 10.0.2.2 is the special alias for localhost from Android emulator -->
</resources>
EOF
```

#### Build & Run Android App

```bash
cd android

# Build debug APK
./gradlew assembleDebug

# Start emulator
emulator -avd Pixel6_API34 &  # Replace with your emulator name

# Install and run app
./gradlew installDebugAndroidTest
adb shell am instrument -w com.ecobook.test/androidx.test.runner.AndroidJUnitRunner
```

**Or use Android Studio**:
1. Open Android Studio
2. File → Open → select `ecobook-ia/android`
3. Click green "Run" button
4. Select emulator or device
5. App will build, install, and launch

---

## First Integration Test: Material Upload & Search

### Test Scenario
1. Register user via OAuth2
2. Upload a textbook image
3. View AI classification
4. Confirm upload
5. Search for material
6. Verify ranking

### Step-by-Step

#### Step 1: Start Backend & Database

```bash
# Terminal 1: PostgreSQL (if not already running)
docker-compose up postgres

# Terminal 2: Spring Boot backend
cd backend
mvn spring-boot:run
# Wait for: "Started EcoBookApplication in X seconds"
```

#### Step 2: Create Test Data

```bash
# Create test user (via API call)
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "nome": "Test User",
    "whatsapp": "+5548999999999",
    "role": "AMBOS"
  }'

# Response:
# {
#   "id": "user-uuid",
#   "email": "test@example.com",
#   "perfil_completo": false,
#   "token": "eyJhbGc..."
# }

# Complete profile
BEARER_TOKEN="eyJhbGc..."  # Copy from response above
curl -X PATCH http://localhost:8080/usuarios/user-uuid \
  -H "Authorization: Bearer $BEARER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cidade": "Florianópolis",
    "bairro": "Centro",
    "consentimento_ia": true
  }'
```

#### Step 3: Upload Material Image

```bash
# Prepare test image (or download a sample textbook cover)
# For testing, use: https://example.com/sample-math-textbook.jpg

curl -X POST http://localhost:8080/materiais/preview \
  -H "Authorization: Bearer $BEARER_TOKEN" \
  -F "image=@sample-math-textbook.jpg" \
  -F "cidade=Florianópolis" \
  -F "bairro=Centro"

# Response:
# {
#   "status_ia": "SUCCESS",
#   "upload_id": "temp-upload-abc123",
#   "best_prediction": {
#     "disciplina": { "value": "MATEMATICA", "confidence": 0.95 },
#     "nivel_ensino": { "value": "FUNDAMENTAL", "confidence": 0.88 },
#     "ano": { "value": 7, "confidence": 0.75 },
#     "sistema_ensino": { "value": "ANGLO", "confidence": 0.92 },
#     "estado_conservacao": { "value": "BOM", "confidence": 0.81 }
#   }
# }
```

#### Step 4: Confirm & Persist Material

```bash
curl -X POST http://localhost:8080/materiais \
  -H "Authorization: Bearer $BEARER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Matemática 7º Ano - Anglo",
    "descricao": "Livro usado, bem conservado",
    "disciplina": "MATEMATICA",
    "nivel_ensino": "FUNDAMENTAL",
    "ano": 7,
    "sistema_ensino": "ANGLO",
    "estado_conservacao": "BOM",
    "cidade": "Florianópolis",
    "bairro": "Centro",
    "upload_id": "temp-upload-abc123"
  }'

# Response:
# {
#   "id": "material-uuid-1",
#   "titulo": "Matemática 7º Ano - Anglo",
#   "status": "DISPONIVEL",
#   "imagem_url": "/uploads/u12345678/m87654321.jpg",
#   "doador_id": "user-uuid",
#   ...
# }
```

#### Step 5: Search Materials

```bash
# Create student profile (another user)
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@example.com",
    "nome": "Student User",
    "whatsapp": "+5548988888888",
    "role": "AMBOS"
  }'

# Update student profile
STUDENT_TOKEN="eyJhbGc..."
curl -X PATCH http://localhost:8080/usuarios/student-uuid \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -d '{
    "cidade": "Florianópolis",
    "bairro": "Centro"
  }'

# Search materials (matching student needs)
curl -X GET "http://localhost:8080/materiais?disciplina=MATEMATICA&nivel_ensino=FUNDAMENTAL&ano=7&sistema_ensino=ANGLO&cidade=Florianópolis&bairro=Centro" \
  -H "Authorization: Bearer $STUDENT_TOKEN"

# Response:
# {
#   "content": [
#     {
#       "id": "material-uuid-1",
#       "titulo": "Matemática 7º Ano - Anglo",
#       "status": "DISPONIVEL",
#       "doador": {
#         "nome": "Test User",
#         "whatsapp": "+5548999999999",
#         "cidade": "FLORIANOPOLIS",
#         "bairro": "CENTRO"
#       }
#     }
#   ],
#   "totalElements": 1
# }
```

#### Step 6: Submit Request

```bash
curl -X POST http://localhost:8080/solicitacoes \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "material_id": "material-uuid-1",
    "estudante_id": "student-uuid"
  }'

# Response:
# {
#   "id": "solicitacao-uuid-1",
#   "material_id": "material-uuid-1",
#   "estudante_id": "student-uuid",
#   "status": "PENDENTE",
#   "created_at": "2026-04-15T10:30:00Z"
# }
```

#### Step 7: Approve Request (as Donor)

```bash
curl -X PATCH http://localhost:8080/solicitacoes/solicitacao-uuid-1 \
  -H "Authorization: Bearer $BEARER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "APROVADA"}'

# Response:
# {
#   "id": "solicitacao-uuid-1",
#   "status": "APROVADA",
#   "contato_doador": {
#     "nome": "Test User",
#     "whatsapp": "+5548999999999"
#   },
#   "approved_at": "2026-04-15T10:35:00Z",
#   "expires_at": "2026-04-29T10:35:00Z"
# }
```

#### Verify Success

```bash
# Check material status changed to RESERVADO
curl -X GET http://localhost:8080/materiais/material-uuid-1 \
  -H "Authorization: Bearer $BEARER_TOKEN"

# Should show: "status": "RESERVADO"

# Check request status is APROVADA
curl -X GET http://localhost:8080/solicitacoes/solicitacao-uuid-1 \
  -H "Authorization: Bearer $STUDENT_TOKEN"

# Should show: "status": "APROVADA", "contato_doador": { ... }
```

---

## Troubleshooting

### Backend Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `Connection refused: 5432` | PostgreSQL not running | Run `docker-compose up postgres` or check local PostgreSQL service |
| `Invalid API key for Gemini` | Missing or incorrect API key in .env | Verify GEMINI_API_KEY in .env; regenerate if needed |
| `Port 8080 already in use` | Another service using port 8080 | Kill existing process: `lsof -i :8080` + `kill -9 <PID>` |
| `JWT validation failed` | Expired token or wrong secret | Tokens expire in 7 days; create new one via POST /auth/register |

### Android Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `Failed to resolve: com.google.firebase:firebase-messaging` | Dependency not in gradle | Ensure `google-services.json` in `app/` and gradle synced |
| `Emulator can't reach localhost:8080` | Emulator network isolation | Use `10.0.2.2` instead of `localhost` in api_config.xml |
| `OAuth2 login fails` | Invalid Google OAuth2 credentials | Verify CLIENT_ID and CLIENT_SECRET in Google Cloud Console |

### Database Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `ERROR: database "ecobook" does not exist` | Database not created | Run: `createdb -U ecobook ecobook` |
| `FATAL: Ident authentication failed` | PostgreSQL auth method | Edit `pg_hba.conf`: change `ident` to `md5` or `trust` for dev |
| `Tables not created` | Migration not run | Backend will auto-create on startup with Flyway; check logs |

---

## Development Workflow

### Making Changes

**Backend**:
```bash
cd backend
# Edit src/main/java or src/main/resources

# Rebuild
mvn compile

# Restart (Ctrl+C to stop, then):
mvn spring-boot:run

# Or enable hot-reload: add spring-boot-devtools to pom.xml
```

**Android**:
```bash
cd android
# Edit app/src/main/kotlin or app/src/main/res

# Rebuild and reinstall on emulator/device
./gradlew installDebug
adb shell am start -n com.ecobook/.MainActivity
```

### Running Tests

**Backend**:
```bash
cd backend
mvn test
# Or in IDE: right-click test file → Run
```

**Android**:
```bash
cd android
./gradlew connectedAndroidTest
# Or in Android Studio: Run → Run Tests
```

---

## Next Steps

After completing this quickstart:

1. **Phase 2**: Implement full API endpoints and UI screens
2. **Phase 3**: Collect test images, validate Gemini integration
3. **Phase 4**: Complete all features, integration testing
4. **Phase 5**: Deploy MVP, monitor in production

---

## Useful Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Google Gemini API Documentation](https://ai.google.dev/)
- [Firebase Cloud Messaging Documentation](https://firebase.google.com/docs/cloud-messaging)

