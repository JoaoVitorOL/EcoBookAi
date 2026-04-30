package com.ecobook.di;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class NetworkModule_ProvideBaseUrlFactory implements Factory<String> {
  private final Provider<Context> contextProvider;

  public NetworkModule_ProvideBaseUrlFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public String get() {
    return provideBaseUrl(contextProvider.get());
  }

  public static NetworkModule_ProvideBaseUrlFactory create(Provider<Context> contextProvider) {
    return new NetworkModule_ProvideBaseUrlFactory(contextProvider);
  }

  public static String provideBaseUrl(Context context) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideBaseUrl(context));
  }
}
