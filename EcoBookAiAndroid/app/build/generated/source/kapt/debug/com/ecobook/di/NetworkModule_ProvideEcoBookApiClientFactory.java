package com.ecobook.di;

import com.ecobook.api.EcoBookApiClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideEcoBookApiClientFactory implements Factory<EcoBookApiClient> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideEcoBookApiClientFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public EcoBookApiClient get() {
    return provideEcoBookApiClient(retrofitProvider.get());
  }

  public static NetworkModule_ProvideEcoBookApiClientFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideEcoBookApiClientFactory(retrofitProvider);
  }

  public static EcoBookApiClient provideEcoBookApiClient(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideEcoBookApiClient(retrofit));
  }
}
