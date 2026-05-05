package com.ecobook.api;

import com.ecobook.auth.SessionManager;
import com.ecobook.utils.SecureStorage;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class AuthInterceptor_Factory implements Factory<AuthInterceptor> {
  private final Provider<SecureStorage> secureStorageProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public AuthInterceptor_Factory(Provider<SecureStorage> secureStorageProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.secureStorageProvider = secureStorageProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public AuthInterceptor get() {
    return newInstance(secureStorageProvider.get(), sessionManagerProvider.get());
  }

  public static AuthInterceptor_Factory create(Provider<SecureStorage> secureStorageProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new AuthInterceptor_Factory(secureStorageProvider, sessionManagerProvider);
  }

  public static AuthInterceptor newInstance(SecureStorage secureStorage,
      SessionManager sessionManager) {
    return new AuthInterceptor(secureStorage, sessionManager);
  }
}
