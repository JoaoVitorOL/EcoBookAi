package com.ecobook.auth

import android.Manifest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.ecobook.MainActivity
import com.ecobook.api.MockEcoBookBackendRule
import com.ecobook.api.AuthApiService
import com.ecobook.api.AuthInterceptor
import com.ecobook.api.EcoBookApiClient
import com.ecobook.api.FcmApiService
import com.ecobook.api.MaterialApiService
import com.ecobook.api.NotificationApiService
import com.ecobook.api.ReferenceDataApiService
import com.ecobook.api.RequestApiService
import com.ecobook.di.NetworkModule
import com.ecobook.fcm.NotificationInboxStore
import com.ecobook.fcm.SecureNotificationInboxStore
import com.ecobook.ui.ComposeTestUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@UninstallModules(NetworkModule::class)
class AuthFlowE2ETest {

    @Module
    @InstallIn(SingletonComponent::class)
    object TestNetworkModule {

        @Provides
        @Singleton
        fun provideBaseUrl(): String {
            return "http://127.0.0.1:${MockEcoBookBackendRule.PORT}/api/"
        }

        @Provides
        @Singleton
        fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            return OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        fun provideGson(): Gson {
            return GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
        }

        @Provides
        @Singleton
        fun provideRetrofit(
            baseUrl: String,
            okHttpClient: OkHttpClient,
            gson: Gson
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }

        @Provides
        @Singleton
        fun provideEcoBookApiClient(retrofit: Retrofit): EcoBookApiClient {
            return retrofit.create(EcoBookApiClient::class.java)
        }

        @Provides
        @Singleton
        fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
            return retrofit.create(AuthApiService::class.java)
        }

        @Provides
        @Singleton
        fun provideMaterialApiService(retrofit: Retrofit): MaterialApiService {
            return retrofit.create(MaterialApiService::class.java)
        }

        @Provides
        @Singleton
        fun provideFcmApiService(retrofit: Retrofit): FcmApiService {
            return retrofit.create(FcmApiService::class.java)
        }

        @Provides
        @Singleton
        fun provideRequestApiService(retrofit: Retrofit): RequestApiService {
            return retrofit.create(RequestApiService::class.java)
        }

        @Provides
        @Singleton
        fun provideNotificationApiService(retrofit: Retrofit): NotificationApiService {
            return retrofit.create(NotificationApiService::class.java)
        }

        @Provides
        @Singleton
        fun provideReferenceDataApiService(retrofit: Retrofit): ReferenceDataApiService {
            return retrofit.create(ReferenceDataApiService::class.java)
        }

        @Provides
        @Singleton
        fun provideNotificationInboxStore(
            secureNotificationInboxStore: SecureNotificationInboxStore
        ): NotificationInboxStore {
            return secureNotificationInboxStore
        }
    }

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val backendRule = MockEcoBookBackendRule()

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 3)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun registerShouldReachOnboardingWithAuthenticatedSession() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Entrar no EcoBook").fetchSemanticsNodes().isNotEmpty()
        }
        ComposeTestUtils.assertAuthenticationVisible(composeRule)

        composeRule.onNodeWithText("Ainda nao tem conta? Criar conta").performClick()
        ComposeTestUtils.enterTextInField(composeRule, "Nome completo", "Teste E2E")
        ComposeTestUtils.enterTextInField(composeRule, "Email", "e2e@example.com")
        ComposeTestUtils.enterTextInField(composeRule, "Senha", "SenhaSegura123")
        ComposeTestUtils.enterTextInField(composeRule, "Confirmar senha", "SenhaSegura123")
        composeRule.onAllNodesWithText("Criar conta")[1].performClick()

        val registerRequest = backendRule.awaitRequest("/api/v1/auth/register")
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Boas-vindas ao EcoBook").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Boas-vindas ao EcoBook").assertIsDisplayed()
        composeRule.onNodeWithText("Começar cadastro").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Completar cadastro").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Completar cadastro").assertIsDisplayed()
        composeRule.onNodeWithText("Etapa 1 de 3").assertIsDisplayed()

        val registerBody = registerRequest.body.readUtf8()
        assertTrue(registerBody.contains("\"email\":\"e2e@example.com\""))
        assertTrue(registerBody.contains("\"nome\":\"Teste E2E\""))
        assertEquals("/api/v1/auth/register", registerRequest.path?.substringBefore('?'))
    }
}
