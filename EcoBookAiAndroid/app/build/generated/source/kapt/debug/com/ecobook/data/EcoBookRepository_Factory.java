package com.ecobook.data;

import com.ecobook.api.EcoBookApiClient;
import com.ecobook.utils.SecureStorage;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class EcoBookRepository_Factory implements Factory<EcoBookRepository> {
  private final Provider<EcoBookApiClient> apiClientProvider;

  private final Provider<SecureStorage> secureStorageProvider;

  public EcoBookRepository_Factory(Provider<EcoBookApiClient> apiClientProvider,
      Provider<SecureStorage> secureStorageProvider) {
    this.apiClientProvider = apiClientProvider;
    this.secureStorageProvider = secureStorageProvider;
  }

  @Override
  public EcoBookRepository get() {
    return newInstance(apiClientProvider.get(), secureStorageProvider.get());
  }

  public static EcoBookRepository_Factory create(Provider<EcoBookApiClient> apiClientProvider,
      Provider<SecureStorage> secureStorageProvider) {
    return new EcoBookRepository_Factory(apiClientProvider, secureStorageProvider);
  }

  public static EcoBookRepository newInstance(EcoBookApiClient apiClient,
      SecureStorage secureStorage) {
    return new EcoBookRepository(apiClient, secureStorage);
  }
}
