param(
    [string]$JavaHome = "C:\Program Files\Java\jdk-26"
)

$env:JAVA_HOME = $JavaHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Push-Location "c:\Users\jvol2\OneDrive\Área de Trabalho\projIntegrador\EcoBookAi\EcoBookAiBackend"
try {
    mvn -Dtest=MigrationRollbackValidationTest test
} finally {
    Pop-Location
}
