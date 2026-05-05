package com.ecobook.ui;

import com.ecobook.auth.SessionManager;
import com.ecobook.data.AuthRepository;
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

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public EcoBookViewModel_Factory(Provider<EcoBookRepository> repositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.repositoryProvider = repositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public EcoBookViewModel get() {
    return newInstance(repositoryProvider.get(), authRepositoryProvider.get(), sessionManagerProvider.get());
  }

  public static EcoBookViewModel_Factory create(Provider<EcoBookRepository> repositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new EcoBookViewModel_Factory(repositoryProvider, authRepositoryProvider, sessionManagerProvider);
  }

  public static EcoBookViewModel newInstance(EcoBookRepository repository,
      AuthRepository authRepository, SessionManager sessionManager) {
    return new EcoBookViewModel(repository, authRepository, sessionManager);
  }
}
