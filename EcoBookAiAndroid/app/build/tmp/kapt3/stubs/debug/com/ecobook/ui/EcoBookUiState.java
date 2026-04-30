package com.ecobook.ui;

import com.ecobook.model.BackendStatus;
import com.ecobook.model.Disciplina;
import com.ecobook.model.DonationPreview;
import com.ecobook.model.MaterialHighlight;
import com.ecobook.model.NivelEnsino;
import com.ecobook.model.ProjectInsight;
import com.ecobook.model.UserProfileDraft;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u001c\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001Be\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u0012\u000e\b\u0002\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u0007\u0012\b\b\u0002\u0010\u000b\u001a\u00020\f\u0012\b\b\u0002\u0010\r\u001a\u00020\u000e\u0012\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u0010\u0012\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0012\u00a2\u0006\u0002\u0010\u0013J\t\u0010%\u001a\u00020\u0003H\u00c6\u0003J\t\u0010&\u001a\u00020\u0005H\u00c6\u0003J\u000f\u0010\'\u001a\b\u0012\u0004\u0012\u00020\b0\u0007H\u00c6\u0003J\u000f\u0010(\u001a\b\u0012\u0004\u0012\u00020\n0\u0007H\u00c6\u0003J\t\u0010)\u001a\u00020\fH\u00c6\u0003J\t\u0010*\u001a\u00020\u000eH\u00c6\u0003J\u000b\u0010+\u001a\u0004\u0018\u00010\u0010H\u00c6\u0003J\u000b\u0010,\u001a\u0004\u0018\u00010\u0012H\u00c6\u0003Ji\u0010-\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u00072\u000e\b\u0002\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u00072\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\u000e2\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u00102\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u00c6\u0001J\u0013\u0010.\u001a\u00020/2\b\u00100\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00101\u001a\u000202H\u00d6\u0001J\t\u00103\u001a\u00020\u000eH\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0017\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\b0\u00078F\u00a2\u0006\u0006\u001a\u0004\b\u001b\u0010\u0017R\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0017R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR\u0011\u0010\r\u001a\u00020\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u0013\u0010\u000f\u001a\u0004\u0018\u00010\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\"R\u0013\u0010\u0011\u001a\u0004\u0018\u00010\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010$\u00a8\u00064"}, d2 = {"Lcom/ecobook/ui/EcoBookUiState;", "", "backendStatus", "Lcom/ecobook/model/BackendStatus;", "profile", "Lcom/ecobook/model/UserProfileDraft;", "catalog", "", "Lcom/ecobook/model/MaterialHighlight;", "insights", "Lcom/ecobook/model/ProjectInsight;", "donationPreview", "Lcom/ecobook/model/DonationPreview;", "searchQuery", "", "selectedDisciplina", "Lcom/ecobook/model/Disciplina;", "selectedNivelEnsino", "Lcom/ecobook/model/NivelEnsino;", "(Lcom/ecobook/model/BackendStatus;Lcom/ecobook/model/UserProfileDraft;Ljava/util/List;Ljava/util/List;Lcom/ecobook/model/DonationPreview;Ljava/lang/String;Lcom/ecobook/model/Disciplina;Lcom/ecobook/model/NivelEnsino;)V", "getBackendStatus", "()Lcom/ecobook/model/BackendStatus;", "getCatalog", "()Ljava/util/List;", "getDonationPreview", "()Lcom/ecobook/model/DonationPreview;", "filteredMaterials", "getFilteredMaterials", "getInsights", "getProfile", "()Lcom/ecobook/model/UserProfileDraft;", "getSearchQuery", "()Ljava/lang/String;", "getSelectedDisciplina", "()Lcom/ecobook/model/Disciplina;", "getSelectedNivelEnsino", "()Lcom/ecobook/model/NivelEnsino;", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
public final class EcoBookUiState {
    @org.jetbrains.annotations.NotNull()
    private final com.ecobook.model.BackendStatus backendStatus = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecobook.model.UserProfileDraft profile = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.ecobook.model.MaterialHighlight> catalog = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.ecobook.model.ProjectInsight> insights = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecobook.model.DonationPreview donationPreview = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String searchQuery = null;
    @org.jetbrains.annotations.Nullable()
    private final com.ecobook.model.Disciplina selectedDisciplina = null;
    @org.jetbrains.annotations.Nullable()
    private final com.ecobook.model.NivelEnsino selectedNivelEnsino = null;
    
    public EcoBookUiState(@org.jetbrains.annotations.NotNull()
    com.ecobook.model.BackendStatus backendStatus, @org.jetbrains.annotations.NotNull()
    com.ecobook.model.UserProfileDraft profile, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ecobook.model.MaterialHighlight> catalog, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ecobook.model.ProjectInsight> insights, @org.jetbrains.annotations.NotNull()
    com.ecobook.model.DonationPreview donationPreview, @org.jetbrains.annotations.NotNull()
    java.lang.String searchQuery, @org.jetbrains.annotations.Nullable()
    com.ecobook.model.Disciplina selectedDisciplina, @org.jetbrains.annotations.Nullable()
    com.ecobook.model.NivelEnsino selectedNivelEnsino) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.BackendStatus getBackendStatus() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.UserProfileDraft getProfile() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ecobook.model.MaterialHighlight> getCatalog() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ecobook.model.ProjectInsight> getInsights() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.DonationPreview getDonationPreview() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSearchQuery() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ecobook.model.Disciplina getSelectedDisciplina() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ecobook.model.NivelEnsino getSelectedNivelEnsino() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ecobook.model.MaterialHighlight> getFilteredMaterials() {
        return null;
    }
    
    public EcoBookUiState() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.BackendStatus component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.UserProfileDraft component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ecobook.model.MaterialHighlight> component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ecobook.model.ProjectInsight> component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.DonationPreview component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component6() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ecobook.model.Disciplina component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ecobook.model.NivelEnsino component8() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.ui.EcoBookUiState copy(@org.jetbrains.annotations.NotNull()
    com.ecobook.model.BackendStatus backendStatus, @org.jetbrains.annotations.NotNull()
    com.ecobook.model.UserProfileDraft profile, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ecobook.model.MaterialHighlight> catalog, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ecobook.model.ProjectInsight> insights, @org.jetbrains.annotations.NotNull()
    com.ecobook.model.DonationPreview donationPreview, @org.jetbrains.annotations.NotNull()
    java.lang.String searchQuery, @org.jetbrains.annotations.Nullable()
    com.ecobook.model.Disciplina selectedDisciplina, @org.jetbrains.annotations.Nullable()
    com.ecobook.model.NivelEnsino selectedNivelEnsino) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}