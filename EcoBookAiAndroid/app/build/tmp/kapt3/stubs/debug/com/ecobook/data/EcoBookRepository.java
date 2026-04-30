package com.ecobook.data;

import com.ecobook.api.EcoBookApiClient;
import com.ecobook.model.AIPreviewField;
import com.ecobook.model.AiAssistStatus;
import com.ecobook.model.BackendConnectionState;
import com.ecobook.model.BackendStatus;
import com.ecobook.model.Disciplina;
import com.ecobook.model.DonationPreview;
import com.ecobook.model.DonationStep;
import com.ecobook.model.EstadoConservacao;
import com.ecobook.model.MaterialHighlight;
import com.ecobook.model.NivelEnsino;
import com.ecobook.model.ProjectInsight;
import com.ecobook.model.SistemaEnsino;
import com.ecobook.model.UserProfileDraft;
import com.ecobook.ui.EcoBookUiState;
import com.ecobook.utils.SecureStorage;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0006\u0010\u0007\u001a\u00020\bJ\u000e\u0010\t\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u000bJ\u000e\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\rH\u0002J\b\u0010\u000f\u001a\u00020\u0010H\u0002J\u000e\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00120\rH\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/ecobook/data/EcoBookRepository;", "", "apiClient", "Lcom/ecobook/api/EcoBookApiClient;", "secureStorage", "Lcom/ecobook/utils/SecureStorage;", "(Lcom/ecobook/api/EcoBookApiClient;Lcom/ecobook/utils/SecureStorage;)V", "initialState", "Lcom/ecobook/ui/EcoBookUiState;", "loadBackendStatus", "Lcom/ecobook/model/BackendStatus;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sampleCatalog", "", "Lcom/ecobook/model/MaterialHighlight;", "sampleDonationPreview", "Lcom/ecobook/model/DonationPreview;", "sampleInsights", "Lcom/ecobook/model/ProjectInsight;", "app_debug"})
public final class EcoBookRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.ecobook.api.EcoBookApiClient apiClient = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecobook.utils.SecureStorage secureStorage = null;
    
    @javax.inject.Inject()
    public EcoBookRepository(@org.jetbrains.annotations.NotNull()
    com.ecobook.api.EcoBookApiClient apiClient, @org.jetbrains.annotations.NotNull()
    com.ecobook.utils.SecureStorage secureStorage) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.ui.EcoBookUiState initialState() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object loadBackendStatus(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.ecobook.model.BackendStatus> $completion) {
        return null;
    }
    
    private final java.util.List<com.ecobook.model.MaterialHighlight> sampleCatalog() {
        return null;
    }
    
    private final java.util.List<com.ecobook.model.ProjectInsight> sampleInsights() {
        return null;
    }
    
    private final com.ecobook.model.DonationPreview sampleDonationPreview() {
        return null;
    }
}