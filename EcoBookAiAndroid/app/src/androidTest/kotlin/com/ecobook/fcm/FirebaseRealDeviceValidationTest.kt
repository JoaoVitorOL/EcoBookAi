package com.ecobook.fcm

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.rule.GrantPermissionRule
import com.ecobook.MainActivity
import com.ecobook.auth.SessionManager
import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.AuthResponseDTO
import com.ecobook.dto.CreateMaterialRequestDTO
import com.ecobook.dto.MaterialDTO
import com.ecobook.dto.RegisterRequestDTO
import com.ecobook.dto.SolicitacaoDTO
import com.ecobook.dto.UpdateProfileRequestDTO
import com.ecobook.dto.UserNotificationDTO
import com.ecobook.dto.UsuarioDTO
import com.ecobook.utils.SecureStorage
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.ecobook.api.AuthApiService
import com.ecobook.api.MaterialApiService
import com.ecobook.api.NotificationApiService
import com.ecobook.api.RequestApiService

@HiltAndroidTest
class FirebaseRealDeviceValidationTest {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var secureStorage: SecureStorage

    @Inject
    lateinit var fcmTokenSyncManager: FcmTokenSyncManager

    @Inject
    lateinit var notificationInboxRepository: NotificationInboxRepository

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        secureStorage.clear()
        notificationInboxRepository.markAllAsRead()
    }

    @Test
    fun donorShouldReceiveRealFirebaseNotificationForIncomingRequest() {
        waitForBackendHealth()

        val uniqueId = System.currentTimeMillis().toString()
        val donorEmail = "firebase-donor-$uniqueId@example.com"
        val studentEmail = "firebase-student-$uniqueId@example.com"

        val donorAuth = registerUser(donorEmail, "Doador Firebase $uniqueId")
        val studentAuth = registerUser(studentEmail, "Estudante Firebase $uniqueId")

        val donorProfile = completeProfile(
            auth = donorAuth,
            whatsapp = "+5511991234501",
            cpf = "52998224725",
            cidade = "Florianopolis",
            bairro = "Centro"
        )
        completeProfile(
            auth = studentAuth,
            whatsapp = "+5511991234502",
            cpf = "11144477735",
            cidade = "Florianopolis",
            bairro = "Trindade"
        )

        sessionManager.onAuthSuccess(donorAuth)
        sessionManager.onUserLoaded(donorProfile)
        notificationInboxRepository.markAllAsRead()

        val firebaseToken = Tasks.await(FirebaseMessaging.getInstance().token, 60, TimeUnit.SECONDS)
        assertTrue("Firebase token should not be blank", firebaseToken.isNotBlank())
        Log.i(TAG, "Firebase token obtained with length=${firebaseToken.length}")

        fcmTokenSyncManager.syncTokenAsync(firebaseToken)
        eventually(timeoutMillis = 30_000, failureMessage = "FCM token was not synced to the backend session.") {
            secureStorage.getLastSyncedFcmToken() == firebaseToken
        }

        val uploadId = previewUploadId(donorAuth.token, "firebase-$uniqueId")
        val material = createMaterial(donorAuth.token, uploadId, uniqueId)
        val request = createRequest(studentAuth.token, material.id)

        eventually(timeoutMillis = 30_000, failureMessage = "Backend notification was not persisted for the donor.") {
            listNotifications(donorAuth.token).any {
                it.notificationType == "SOLICITACAO_RECEBIDA" && it.requestId == request.id
            }
        }

        composeRule.waitUntil(timeoutMillis = 120_000) {
            notificationInboxRepository.notifications.value.any {
                it.notificationType == "SOLICITACAO_RECEBIDA" && it.requestId == request.id
            }
        }

        val receivedNotification = notificationInboxRepository.notifications.value.first {
            it.notificationType == "SOLICITACAO_RECEBIDA" && it.requestId == request.id
        }

        Log.i(
            TAG,
            "Real FCM notification received. materialId=${receivedNotification.materialId}, requestId=${receivedNotification.requestId}"
        )
        assertEquals(request.id, receivedNotification.requestId)
        assertEquals(material.id, receivedNotification.materialId)
        assertTrue(receivedNotification.title.isNotBlank())
        assertTrue(receivedNotification.body.isNotBlank())
    }

    private fun waitForBackendHealth() {
        eventually(timeoutMillis = 60_000, failureMessage = "Backend health endpoint did not become available.") {
            runCatching {
                retrofitWithoutSuffix()
                    .create(HealthApi::class.java)
                    .health()
                    .execute()
                    .isSuccessful
            }.getOrDefault(false)
        }
    }

    private fun registerUser(email: String, nome: String): AuthResponseDTO {
        val response = runBlocking {
            retrofit()
                .create(AuthApiService::class.java)
                .register(
                    RegisterRequestDTO(
                        email = email,
                        password = PASSWORD,
                        nome = nome
                    )
                )
        }
        return requireData(response, "register user $email")
    }

    private fun completeProfile(
        auth: AuthResponseDTO,
        whatsapp: String,
        cpf: String,
        cidade: String,
        bairro: String
    ): UsuarioDTO {
        val response = runBlocking {
            retrofit(auth.token)
                .create(AuthApiService::class.java)
                .updateProfile(
                    UpdateProfileRequestDTO(
                        nome = auth.nome,
                        whatsapp = whatsapp,
                        cpf = cpf,
                        cidade = cidade,
                        bairro = bairro,
                        instituicao = "UFSC",
                        consentimentoIa = true,
                        necessidadesAcademicas = setOf("TEXTBOOKS", "WORKBOOKS")
                    )
                )
        }
        return requireData(response, "complete profile for ${auth.email}")
    }

    private fun previewUploadId(token: String, label: String): String {
        val imageBytes = createPngBytes(label.hashCode())
        val response = runBlocking {
            retrofit(token)
                .create(MaterialApiService::class.java)
                .previewMaterial(
                    fileFront = MultipartBody.Part.createFormData(
                        "file_front",
                        "$label-front.png",
                        imageBytes.toRequestBody("image/png".toMediaType())
                    )
                )
        }

        val payload = requireData(response, "preview material $label")
        assertTrue("Upload id should not be blank", payload.uploadId.isNotBlank())
        return payload.uploadId
    }

    private fun createMaterial(token: String, uploadId: String, suffix: String): MaterialDTO {
        val response = runBlocking {
            retrofit(token)
                .create(MaterialApiService::class.java)
                .createMaterial(
                    CreateMaterialRequestDTO(
                        uploadId = uploadId,
                        titulo = "Colecao Firebase $suffix",
                        autor = "Equipe EcoBook",
                        editora = "EcoBook Press",
                        descricao = "Material criado para validar FCM em dispositivo real.",
                        disciplina = "MATEMATICA",
                        nivelEnsino = "FUNDAMENTAL",
                        ano = 7,
                        sistemaEnsino = "ANGLO",
                        estadoConservacao = "BOM",
                        dataPublicacao = 2024
                    )
                )
        }
        return requireData(response, "create material $suffix")
    }

    private fun createRequest(token: String, materialId: String): SolicitacaoDTO {
        val response = runBlocking {
            retrofit(token)
                .create(RequestApiService::class.java)
                .createRequest(materialId)
        }
        return requireData(response, "create request for material $materialId")
    }

    private fun listNotifications(token: String): List<UserNotificationDTO> {
        val response = runBlocking {
            retrofit(token)
                .create(NotificationApiService::class.java)
                .listNotifications()
        }
        return requireData(response, "list donor notifications")
    }

    private fun <T> requireData(response: Response<ApiEnvelopeDTO<T>>, operation: String): T {
        if (!response.isSuccessful) {
            error("$operation failed with HTTP ${response.code()} and body=${response.errorBody()?.string()}")
        }

        val envelope = response.body()
        assertNotNull("Expected a response envelope for $operation", envelope)
        return envelope?.data ?: error("Expected non-null data for $operation, message=${envelope?.message}")
    }

    private fun retrofit(token: String? = null): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
        if (!token.isNullOrBlank()) {
            clientBuilder.addInterceptor(
                Interceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("Authorization", "Bearer $token")
                            .build()
                    )
                }
            )
        }

        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

        return Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun retrofitWithoutSuffix(): Retrofit {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

        return Retrofit.Builder()
            .baseUrl(HOST_BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun createPngBytes(seed: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val color = Color.rgb(
            (seed shr 16) and 0xFF,
            (seed shr 8) and 0xFF,
            seed and 0xFF
        )
        bitmap.eraseColor(color)

        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
    }

    private fun eventually(
        timeoutMillis: Long,
        pollIntervalMillis: Long = 1_000,
        failureMessage: String,
        condition: () -> Boolean
    ) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) {
                return
            }
            Thread.sleep(pollIntervalMillis)
        }
        error(failureMessage)
    }

    private interface HealthApi {
        @retrofit2.http.GET("api/v1/health")
        fun health(): retrofit2.Call<ApiEnvelopeDTO<Map<String, Any>>>
    }

    private companion object {
        const val TAG = "FirebaseValidation"
        const val PASSWORD = "SenhaSegura123"
        const val HOST_BASE_URL = "http://10.0.2.2:8080/"
        const val API_BASE_URL = "http://10.0.2.2:8080/api/"
    }
}
