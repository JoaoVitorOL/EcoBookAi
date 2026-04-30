# OAuth2 & Firebase Setup Guide

**Purpose**: Step-by-step setup for Google OAuth2 and Firebase integration  
**Reference**: user-api.md (authentication), notification-schema.md (FCM)  
**Date**: 2026-04-17

---

## Table of Contents

1. [Google OAuth2 Setup](#google-oauth2-setup)
2. [Firebase Setup](#firebase-setup)
3. [Backend Configuration](#backend-configuration)
4. [Android Configuration](#android-configuration)
5. [Testing & Verification](#testing--verification)

---

## Google OAuth2 Setup

### Overview

Google OAuth2 provides secure authentication without storing passwords. Users sign in with their Google account, and we receive an ID token to establish a session.

**Flow**:
1. User clicks "Sign in with Google" on Android app
2. AppAuth library (Android) handles OAuth2 flow
3. Google returns authorization code
4. Backend exchanges code for ID token
5. Backend creates JWT session token
6. Frontend uses JWT for subsequent API calls

### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click "Select a Project" → "New Project"
3. Enter name: `EcoBook IA`
4. Click "Create"
5. Wait for project creation (1-2 minutes)

### Step 2: Enable OAuth2 API

1. In Google Cloud Console, go to **APIs & Services** → **Library**
2. Search for "Google+ API"
3. Click "Google+ API"
4. Click "Enable"

### Step 3: Create OAuth2 Credentials

#### For Backend (Spring Boot)

1. Go to **APIs & Services** → **Credentials**
2. Click "Create Credentials" → "OAuth client ID"
3. Choose "Web application"
4. Fill in:
   - Name: `EcoBook Backend`
   - Authorized JavaScript origins: `http://localhost:8080`, `https://api.ecobook.com`
   - Authorized redirect URIs: `http://localhost:8080/api/v1/auth/callback`, `https://api.ecobook.com/api/v1/auth/callback`
5. Click "Create"
6. Copy **Client ID** and **Client Secret**
7. Save to `.env`:
   ```properties
   OAUTH2_GOOGLE_CLIENT_ID=<your_client_id>.apps.googleusercontent.com
   OAUTH2_GOOGLE_CLIENT_SECRET=<your_client_secret>
   ```

#### For Android App

1. Go to **APIs & Services** → **Credentials**
2. Click "Create Credentials" → "OAuth client ID"
3. Choose "Android"
4. Fill in:
   - Package name: `com.ecobook.ia` (or your package name)
   - SHA-1 certificate fingerprint: See [Get Android SHA-1](#get-android-sha-1)
5. Click "Create"
6. Copy **Client ID**
7. Save to `.env`:
   ```properties
   ANDROID_OAUTH2_CLIENT_ID=<your_android_client_id>.apps.googleusercontent.com
   ```

#### Get Android SHA-1

```bash
# Generate debug keystore SHA-1
./gradlew signingReport

# Or manually:
keytool -list -v -keystore ~/.EcoBookAiAndroid/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the SHA-1 value and use in Android OAuth2 credentials setup.

### Step 4: Configure Redirect URIs

Add these to both backend and Android credentials:

**Backend**:
- Dev: `http://localhost:8080/api/v1/auth/callback`
- Staging: `https://staging-api.ecobook.com/api/v1/auth/callback`
- Prod: `https://api.ecobook.com/api/v1/auth/callback`

**Android**:
- `ecobook://auth/callback` (custom redirect scheme)

---

## Firebase Setup

### Overview

Firebase Cloud Messaging (FCM) handles push notifications. We use Firebase to:
- Send notifications when requests are received/approved
- Track notification delivery
- Handle retry logic and dead-letter queue

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter name: `EcoBook IA`
4. Disable Google Analytics (optional)
5. Click "Create project"
6. Wait for project creation (1-2 minutes)

### Step 2: Add Android App to Firebase

1. In Firebase Console, click the Android icon
2. Fill in:
   - Android package name: `com.ecobook.ia`
   - App nickname: `EcoBook IA Mobile`
   - SHA-1 certificate fingerprint: (from previous section)
3. Click "Register app"
4. Download `google-services.json`
5. Place file in Android project: `app/google-services.json`

### Step 3: Create Service Account (for Backend)

1. In Firebase Console, go to **Project Settings** (gear icon)
2. Click **Service Accounts** tab
3. Click "Generate New Private Key"
4. Save file as `firebase-service-account.json`
5. Store securely (e.g., in `.env` or secrets manager)

### Step 4: Enable Firebase Messaging API

1. In Firebase Console, go to **Cloud Messaging** tab
2. Verify "Cloud Messaging API" is enabled
3. If not, click "Enable" and wait for activation

### Step 5: Get Firebase Configuration

1. In Firebase Console, go to **Project Settings** → **General**
2. Copy these values to `.env`:
   ```properties
   FIREBASE_PROJECT_ID=<your_project_id>
   FIREBASE_SERVICE_ACCOUNT_JSON=/path/to/firebase-service-account.json
   ANDROID_FIREBASE_PROJECT_ID=<your_project_id>
   ANDROID_FIREBASE_SENDER_ID=<your_sender_id>
   ANDROID_FIREBASE_API_KEY=<your_api_key>
   ```

---

## Backend Configuration

### Spring Boot OAuth2 Configuration

**File**: `src/main/resources/application.yml` or `application.properties`

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${OAUTH2_GOOGLE_CLIENT_ID}
            client-secret: ${OAUTH2_GOOGLE_CLIENT_SECRET}
            scope: openid,email,profile
            redirect-uri: "{baseUrl}/api/v1/auth/callback"
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://openidconnect.googleapis.com/v1/userinfo
```

### JWT Token Configuration

**File**: `src/main/java/com/ecobook/config/JwtConfig.java`

```java
@Configuration
public class JwtConfig {
    
    @Value("${JWT_SECRET}")
    private String jwtSecret;
    
    @Value("${JWT_EXPIRY_MS:604800000}")  // 7 days default
    private long jwtExpiryMs;
    
    public String generateToken(String userId, String email) {
        return Jwts.builder()
            .setSubject(userId)
            .claim("email", email)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiryMs))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public String getUserIdFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
}
```

### OAuth2 Login Endpoint

**File**: `src/main/java/com/ecobook/controller/AuthController.java`

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    @PostMapping("/callback")
    public ResponseEntity<LoginResponse> handleOAuthCallback(
            @RequestParam String code,
            @RequestParam String state) {
        
        // 1. Exchange code for ID token (via Google)
        String idToken = exchangeCodeForToken(code);
        
        // 2. Verify and decode ID token
        Claims claims = verifyIdToken(idToken);
        String email = claims.get("email", String.class);
        String googleId = claims.getSubject();
        
        // 3. Find or create user
        Usuario usuario = usuarioRepository.findByGoogleId(googleId)
            .orElseGet(() -> {
                Usuario newUser = new Usuario();
                newUser.setGoogleId(googleId);
                newUser.setEmail(email);
                newUser.setNome(claims.get("name", String.class));
                return usuarioRepository.save(newUser);
            });
        
        // 4. Generate JWT
        String jwtToken = jwtConfig.generateToken(usuario.getId(), usuario.getEmail());
        
        // 5. Return token to client
        return ResponseEntity.ok(new LoginResponse(jwtToken, usuario));
    }
    
    private String exchangeCodeForToken(String code) {
        // Call Google token endpoint
        // POST https://oauth2.googleapis.com/token
        // Parameters: code, client_id, client_secret, grant_type=authorization_code
        // Returns: { "id_token": "...", "access_token": "...", ... }
    }
    
    private Claims verifyIdToken(String idToken) {
        return Jwts.parser()
            .setSigningKey(/*Google public key*/)
            .parseClaimsJws(idToken)
            .getBody();
    }
}
```

### Firebase Messaging Configuration

**File**: `src/main/java/com/ecobook/config/FirebaseConfig.java`

```java
@Configuration
public class FirebaseConfig {
    
    @Value("${FIREBASE_SERVICE_ACCOUNT_JSON}")
    private String firebaseServiceAccountJson;
    
    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        FileInputStream serviceAccount = new FileInputStream(firebaseServiceAccountJson);
        
        FirebaseOptions options = new FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build();
        
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
        
        return FirebaseApp.getInstance();
    }
}
```

### FCM Notification Service

**File**: `src/main/java/com/ecobook/service/FCMNotificationService.java`

```java
@Service
public class FCMNotificationService {
    
    public void sendNotification(String userId, String tipo, Map<String, String> payload) {
        try {
            String fcmToken = usuarioRepository.findById(userId)
                .map(Usuario::getFcmToken)
                .orElseThrow(() -> new NotFoundException("User FCM token not found"));
            
            Message message = Message.builder()
                .setToken(fcmToken)
                .putAllData(payload)
                .setNotification(new Notification(
                    payload.get("title"),
                    payload.get("body")
                ))
                .build();
            
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("FCM message sent: {}", response);
            
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send FCM notification: {}", e.getMessage());
            // Implement retry logic (see Q7 requirements in ai-response.md)
        }
    }
}
```

---

## Android Configuration

### Add Google Play Services

**File**: `app/build.gradle`

```gradle
dependencies {
    // OAuth2 / AppAuth
    implementation 'net.openid:appauth:0.11.0'
    
    // Firebase
    implementation 'com.google.firebase:firebase-messaging:23.2.1'
    implementation 'com.google.firebase:firebase-analytics:21.5.0'
    
    // HTTP Client (Retrofit)
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    
    // Other dependencies...
}

apply plugin: 'com.google.gms.google-services'
```

### OAuth2 Login Activity

**File**: `app/src/main/java/com/ecobook/ui/LoginActivity.kt`

```kotlin
class LoginActivity : AppCompatActivity() {
    
    private lateinit var authService: AuthorizationService
    private val config = AuthorizationServiceConfiguration(
        Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
        Uri.parse("https://oauth2.googleapis.com/token")
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        findViewById<Button>(R.id.btn_sign_in_google).setOnClickListener {
            performOAuth2Login()
        }
    }
    
    private fun performOAuth2Login() {
        val request = AuthorizationRequest.Builder(
            config,
            BuildConfig.ANDROID_OAUTH2_CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse("ecobook://auth/callback")
        )
            .setScopes("openid", "email", "profile")
            .setState("random_state_12345")
            .build()
        
        authService.performAuthorizationRequest(
            request,
            PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0),
            PendingIntent.getActivity(this, 1, Intent(this, LoginActivity::class.java), 0)
        )
    }
    
    override fun onResume() {
        super.onResume()
        
        val intent = intent
        if (intent?.action == Intent.ACTION_VIEW) {
            val response = AuthorizationResponse.fromIntent(intent)
            val error = AuthorizationException.fromIntent(intent)
            
            if (response != null && error == null) {
                // Exchange code for token
                exchangeCodeForToken(response.authorizationCode!!)
            }
        }
    }
    
    private fun exchangeCodeForToken(code: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost:8080/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val apiService = retrofit.create(ApiService::class.java)
        apiService.login(LoginRequest(code)).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val jwtToken = response.body()?.token
                    // Save JWT to SharedPreferences or DataStore
                    saveJWTToken(jwtToken)
                    // Navigate to main app
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                }
            }
            
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("LoginActivity", "OAuth login failed", t)
            }
        })
    }
}
```

### Firebase Messaging Service

**File**: `app/src/main/java/com/ecobook/service/FCMService.kt`

```kotlin
class FCMService : FirebaseMessagingService() {
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val type = remoteMessage.data["type"]
        val titulo = remoteMessage.data["material_titulo"] ?: "EcoBook IA"
        val mensagem = remoteMessage.data["message"] ?: "Nova notificação"
        
        // Create notification
        val notificationId = System.currentTimeMillis().toInt()
        val notification = NotificationCompat.Builder(this, "ecobook_notifications")
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()
        
        // Send notification
        NotificationManagerCompat.from(this).notify(notificationId, notification)
        
        // Handle deep link
        val intent = when (type) {
            "SOLICITACAO_RECEBIDA" -> Intent(this, SolicitacoesActivity::class.java)
                .putExtra("solicitacao_id", remoteMessage.data["solicitacao_id"])
            "SOLICITACAO_APROVADA" -> Intent(this, MaterialDetailActivity::class.java)
                .putExtra("material_id", remoteMessage.data["material_id"])
            else -> Intent(this, MainActivity::class.java)
        }
        
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        notification.contentIntent = pendingIntent
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Send token to backend for storage
        val jwtToken = loadJWTToken()
        val apiService = createApiService()
        apiService.updateFCMToken(UpdateFCMTokenRequest(token))
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    Log.d("FCMService", "FCM token updated on backend")
                }
                
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("FCMService", "Failed to update FCM token", t)
                }
            })
    }
}
```

### Update User Profile with FCM Token

After OAuth login, update user profile with FCM token:

```kotlin
// In MainActivity or after login
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val fcmToken = task.result
        
        val apiService = createApiService()
        val updateRequest = UpdateUsuarioRequest(
            nome = user.nome,
            whatsapp = user.whatsapp,
            cidade = user.cidade,
            bairro = user.bairro,
            fcmToken = fcmToken
        )
        
        apiService.updatePerfil(user.id, updateRequest)
            .enqueue(object : Callback<UsuarioResponse> {
                override fun onResponse(call: Call<UsuarioResponse>, response: Response<UsuarioResponse>) {
                    Log.d("MainActivity", "Profile updated with FCM token")
                }
                
                override fun onFailure(call: Call<UsuarioResponse>, t: Throwable) {
                    Log.e("MainActivity", "Failed to update profile", t)
                }
            })
    }
}
```

---

## Testing & Verification

### Test OAuth2 Flow

1. **Backend**: Start Spring Boot server
   ```bash
   mvn spring-boot:run
   ```

2. **Android**: Run app on emulator
   ```bash
   ./gradlew installDebug
   adb shell am start -n com.ecobook.ia/.LoginActivity
   ```

3. **Login**: Click "Sign in with Google" button

4. **Verify**:
   - Check backend logs for "User created" or "User logged in"
   - Check Android logs for JWT token received
   - Check database for new usuario record

### Test FCM Notification

1. **Trigger event**: Create request in app (POST /solicitacoes)

2. **Check backend**: Verify FCM notification sent in logs

3. **Check Android**: Notification should appear in notification center

4. **Test deep link**: Click notification → should navigate to correct screen

### Test Production Credentials

Before deploying to production:

1. ✅ Update OAuth2 redirect URIs in Google Console
2. ✅ Update Firebase credentials in environment
3. ✅ Test with production database
4. ✅ Verify FCM certificates are valid
5. ✅ Test with Google Play Services (not emulator)

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Invalid OAuth2 client" | Verify Client ID and Client Secret match Google Console |
| "Redirect URI mismatch" | Check redirect URI in Google Console matches `OAUTH2_GOOGLE_REDIRECT_URI` |
| "Firebase token invalid" | Verify `firebase-service-account.json` path is correct |
| "FCM token not received on Android" | Check Firebase project ID matches; verify google-services.json is in app/ |
| "Google Play Services not available" | Use physical device (not emulator) for production testing |
| "JWT token expired" | Verify JWT_SECRET matches between backend and frontend |

---

## References

- [Google OAuth2 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)
- [AppAuth for Android](https://openid.github.io/AppAuth-Android/)
- [Spring Boot OAuth2](https://spring.io/projects/spring-security-oauth)
