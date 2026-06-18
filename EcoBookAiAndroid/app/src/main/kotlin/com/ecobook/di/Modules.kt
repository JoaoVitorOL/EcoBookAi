package com.ecobook.di

import android.content.Context
import com.ecobook.BuildConfig
import com.ecobook.R
import com.ecobook.api.AuthInterceptor
import com.ecobook.api.AuthApiService
import com.ecobook.api.EcoBookApiClient
import com.ecobook.api.FcmApiService
import com.ecobook.api.MaterialApiService
import com.ecobook.api.NotificationApiService
import com.ecobook.api.ReferenceDataApiService
import com.ecobook.api.RequestApiService
import com.ecobook.api.RuntimeBackendUrlOverride
import com.ecobook.fcm.NotificationInboxStore
import com.ecobook.fcm.SecureNotificationInboxStore
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideBaseUrl(@ApplicationContext context: Context): String {
        val runtimeOverride = RuntimeBackendUrlOverride.current(context)
        val overrideUrl = if (!runtimeOverride.isNullOrBlank()) {
            runtimeOverride
        } else {
            BuildConfig.BACKEND_URL_OVERRIDE.trim()
        }
        val configuredUrl = if (overrideUrl.isNotEmpty()) {
            overrideUrl
        } else {
            context.getString(R.string.backend_url)
        }
        return configuredUrl.trimEnd('/') + "/"
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val connectTimeoutSeconds = 8L
        val readTimeoutSeconds = 20L
        val writeTimeoutSeconds = 30L
        val callTimeoutSeconds = 20L

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            // The app talks to a local/dev backend most of the time, so failing
            // fast is better UX than hanging for 30s when the server is offline.
            .retryOnConnectionFailure(false)
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .callTimeout(callTimeoutSeconds, TimeUnit.SECONDS)
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
    fun provideRequestApiService(retrofit: Retrofit): RequestApiService {
        return retrofit.create(RequestApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideFcmApiService(retrofit: Retrofit): FcmApiService {
        return retrofit.create(FcmApiService::class.java)
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
