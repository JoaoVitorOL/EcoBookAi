package com.ecobook.ui;

import com.ecobook.data.EcoBookRepository;
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
public final class EcoBookViewModel_Factory implements Factory<EcoBookViewModel> {
  private final Provider<EcoBookRepository> repositoryProvider;

  public EcoBookViewModel_Factory(Provider<EcoBookRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public EcoBookViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static EcoBookViewModel_Factory create(Provider<EcoBookRepository> repositoryProvider) {
    return new EcoBookViewModel_Factory(repositoryProvider);
  }

  public static EcoBookViewModel newInstance(EcoBookRepository repository) {
    return new EcoBookViewModel(repository);
  }
}
