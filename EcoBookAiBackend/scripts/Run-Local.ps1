[CmdletBinding()]
param(
    [string]$JavaHome,

    [int]$Port = 8080
)

$scriptDir = Split-Path -Parent $PSCommandPath
$backendDir = Split-Path -Parent $scriptDir

if ($JavaHome) {
    $resolvedJavaHome = (Resolve-Path -LiteralPath $JavaHome).Path
    $env:JAVA_HOME = $resolvedJavaHome
    $env:Path = "$resolvedJavaHome\bin;$env:Path"
}

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "Iniciando backend local em http://127.0.0.1:$Port/api"

if ([string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) {
    Write-Host "GEMINI_API_KEY ausente: o backend local mantera o preview mock do Gemini."
} else {
    Write-Host "GEMINI_API_KEY detectada: o backend local usara o Gemini real. Para forcar mock, defina GEMINI_MOCK_FORCE=true."
}

Set-Location $backendDir
mvn "spring-boot:run" "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--server.port=$Port"
