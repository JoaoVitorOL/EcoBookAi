package com.ecobook.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b$\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001Be\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u0012\u0006\u0010\u000e\u001a\u00020\u0003\u0012\u0006\u0010\u000f\u001a\u00020\u0003\u0012\u0006\u0010\u0010\u001a\u00020\u0003\u0012\u0006\u0010\u0011\u001a\u00020\u0012\u0012\u0006\u0010\u0013\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0014J\t\u0010)\u001a\u00020\u0003H\u00c6\u0003J\t\u0010*\u001a\u00020\u0003H\u00c6\u0003J\t\u0010+\u001a\u00020\u0012H\u00c6\u0003J\t\u0010,\u001a\u00020\u0003H\u00c6\u0003J\t\u0010-\u001a\u00020\u0003H\u00c6\u0003J\t\u0010.\u001a\u00020\u0003H\u00c6\u0003J\t\u0010/\u001a\u00020\u0007H\u00c6\u0003J\t\u00100\u001a\u00020\tH\u00c6\u0003J\t\u00101\u001a\u00020\u000bH\u00c6\u0003J\t\u00102\u001a\u00020\rH\u00c6\u0003J\t\u00103\u001a\u00020\u0003H\u00c6\u0003J\t\u00104\u001a\u00020\u0003H\u00c6\u0003J\u0081\u0001\u00105\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\u000b2\b\b\u0002\u0010\f\u001a\u00020\r2\b\b\u0002\u0010\u000e\u001a\u00020\u00032\b\b\u0002\u0010\u000f\u001a\u00020\u00032\b\b\u0002\u0010\u0010\u001a\u00020\u00032\b\b\u0002\u0010\u0011\u001a\u00020\u00122\b\b\u0002\u0010\u0013\u001a\u00020\u0003H\u00c6\u0001J\u0013\u00106\u001a\u0002072\b\u00108\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00109\u001a\u00020\u0012H\u00d6\u0001J\t\u0010:\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0010\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\f\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0016R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001dR\u0011\u0010\u001e\u001a\u00020\u00038F\u00a2\u0006\u0006\u001a\u0004\b\u001f\u0010\u0016R\u0011\u0010\u0013\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u0016R\u0011\u0010\u000f\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\u0016R\u0011\u0010\u0011\u001a\u00020\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010#R\u0011\u0010\u000e\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u0016R\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u0016R\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\'R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010\u0016\u00a8\u0006;"}, d2 = {"Lcom/ecobook/model/MaterialHighlight;", "", "id", "", "title", "summary", "discipline", "Lcom/ecobook/model/Disciplina;", "level", "Lcom/ecobook/model/NivelEnsino;", "teachingSystem", "Lcom/ecobook/model/SistemaEnsino;", "conservationState", "Lcom/ecobook/model/EstadoConservacao;", "schoolYear", "neighborhood", "city", "publicationYear", "", "matchNote", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/ecobook/model/Disciplina;Lcom/ecobook/model/NivelEnsino;Lcom/ecobook/model/SistemaEnsino;Lcom/ecobook/model/EstadoConservacao;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V", "getCity", "()Ljava/lang/String;", "getConservationState", "()Lcom/ecobook/model/EstadoConservacao;", "getDiscipline", "()Lcom/ecobook/model/Disciplina;", "getId", "getLevel", "()Lcom/ecobook/model/NivelEnsino;", "locationLabel", "getLocationLabel", "getMatchNote", "getNeighborhood", "getPublicationYear", "()I", "getSchoolYear", "getSummary", "getTeachingSystem", "()Lcom/ecobook/model/SistemaEnsino;", "getTitle", "component1", "component10", "component11", "component12", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
public final class MaterialHighlight {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String id = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String title = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String summary = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecobook.model.Disciplina discipline = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecobook.model.NivelEnsino level = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecobook.model.SistemaEnsino teachingSystem = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecobook.model.EstadoConservacao conservationState = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String schoolYear = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String neighborhood = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String city = null;
    private final int publicationYear = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String matchNote = null;
    
    public MaterialHighlight(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String summary, @org.jetbrains.annotations.NotNull()
    com.ecobook.model.Disciplina discipline, @org.jetbrains.annotations.NotNull()
    com.ecobook.model.NivelEnsino level, @org.jetbrains.annotations.NotNull()
    com.ecobook.model.SistemaEnsino teachingSystem, @org.jetbrains.annotations.NotNull()
    com.ecobook.model.EstadoConservacao conservationState, @org.jetbrains.annotations.NotNull()
    java.lang.String schoolYear, @org.jetbrains.annotations.NotNull()
    java.lang.String neighborhood, @org.jetbrains.annotations.NotNull()
    java.lang.String city, int publicationYear, @org.jetbrains.annotations.NotNull()
    java.lang.String matchNote) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getTitle() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSummary() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.Disciplina getDiscipline() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.NivelEnsino getLevel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.SistemaEnsino getTeachingSystem() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.EstadoConservacao getConservationState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSchoolYear() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getNeighborhood() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getCity() {
        return null;
    }
    
    public final int getPublicationYear() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getMatchNote() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLocationLabel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component10() {
        return null;
    }
    
    public final int component11() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component12() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.Disciplina component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.NivelEnsino component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.SistemaEnsino component6() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.EstadoConservacao component7() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component8() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecobook.model.MaterialHighlight copy(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String summary, @org.jetbrains.annotations.NotNull()
    com.ecobook.model.Disciplina discipline, @org.jetbrains.annotations.NotNull()
    com.ecobook.model.NivelEnsino level, @org.jetbrains.annotations.NotNull()
    com.ecobook.model.SistemaEnsino teachingSystem, @org.jetbrains.annotations.NotNull()
    com.ecobook.model.EstadoConservacao conservationState, @org.jetbrains.annotations.NotNull()
    java.lang.String schoolYear, @org.jetbrains.annotations.NotNull()
    java.lang.String neighborhood, @org.jetbrains.annotations.NotNull()
    java.lang.String city, int publicationYear, @org.jetbrains.annotations.NotNull()
    java.lang.String matchNote) {
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