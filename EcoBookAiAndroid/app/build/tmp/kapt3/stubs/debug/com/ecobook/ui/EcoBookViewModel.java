package com.ecobook.ui;

import androidx.lifecycle.ViewModel;
import com.ecobook.data.EcoBookRepository;
import com.ecobook.model.BackendStatus;
import com.ecobook.model.Disciplina;
import com.ecobook.model.NivelEnsino;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import kotlinx.coroutines.flow.StateFlow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\f\u001a\u00020\rJ\u0006\u0010\u000e\u001a\u00020\rJ\u0006\u0010\u000f\u001a\u00020\rJ\u000e\u0010\u0010\u001a\u00020\r2\u0006\u0010\u0011\u001a\u00020\u0012J\u000e\u0010\u0013\u001a\u00020\r2\u0006\u0010\u0014\u001a\u00020\u0015J\u000e\u0010\u0016\u001a\u00020\r2\u0006\u0010\u0017\u001a\u00020\u0018J\u000e\u0010\u0019\u001a\u00020\r2\u0006\u0010\u0017\u001a\u00020\u0018J\u000e\u0010\u001a\u001a\u00020\r2\u0006\u0010\u0017\u001a\u00020\u0018J\u000e\u0010\u001b\u001a\u00020\r2\u0006\u0010\u0017\u001a\u00020\u0018J\u000e\u0010\u001c\u001a\u00020\r2\u0006\u0010\u0017\u001a\u00020\u0018J\u000e\u0010\u001d\u001a\u00020\r2\u0006\u0010\u001e\u001a\u00020\u0018J\u000e\u0010\u001f\u001a\u00020\r2\u0006\u0010\u0017\u001a\u00020\u0018R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006 "}, d2 = {"Lcom/ecobook/ui/EcoBookViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/ecobook/data/EcoBookRepository;", "(Lcom/ecobook/data/EcoBookRepository;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/ecobook/ui/EcoBookUiState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "clearDiscoveryFilters", "", "refreshBackendStatus", "toggleConsentimentoIa", "toggleDisciplina", "disciplina", "Lcom/ecobook/model/Disciplina;", "toggleNivelEnsino", "nivelEnsino", "Lcom/ecobook/model/NivelEnsino;", "updateBairro", "value", "", "updateCidade", "updateEmail", "updateInstituicao", "updateNome", "updateSearchQuery", "query", "updateWhatsapp", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class EcoBookViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ecobook.data.EcoBookRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.ecobook.ui.EcoBookUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ecobook.ui.EcoBookUiState> uiState = null;
    
    @javax.inject.Inject()
    public EcoBookViewModel(@org.jetbrains.annotations.NotNull()
    com.ecobook.data.EcoBookRepository repository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.ecobook.ui.EcoBookUiState> getUiState() {
        return null;
    }
    
    public final void refreshBackendStatus() {
    }
    
    public final void updateSearchQuery(@org.jetbrains.annotations.NotNull()
    java.lang.String query) {
    }
    
    public final void toggleDisciplina(@org.jetbrains.annotations.NotNull()
    com.ecobook.model.Disciplina disciplina) {
    }
    
    public final void toggleNivelEnsino(@org.jetbrains.annotations.NotNull()
    com.ecobook.model.NivelEnsino nivelEnsino) {
    }
    
    public final void clearDiscoveryFilters() {
    }
    
    public final void updateNome(@org.jetbrains.annotations.NotNull()
    java.lang.String value) {
    }
    
    public final void updateEmail(@org.jetbrains.annotations.NotNull()
    java.lang.String value) {
    }
    
    public final void updateWhatsapp(@org.jetbrains.annotations.NotNull()
    java.lang.String value) {
    }
    
    public final void updateCidade(@org.jetbrains.annotations.NotNull()
    java.lang.String value) {
    }
    
    public final void updateBairro(@org.jetbrains.annotations.NotNull()
    java.lang.String value) {
    }
    
    public final void updateInstituicao(@org.jetbrains.annotations.NotNull()
    java.lang.String value) {
    }
    
    public final void toggleConsentimentoIa() {
    }
}