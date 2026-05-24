[CmdletBinding()]
param(
    [string]$JavaHome,

    [int]$Port = 8080
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "EcoBookAiBackend"

if (-not (Test-Path -LiteralPath $backendDir -PathType Container)) {
    throw "Pasta do backend nao encontrada: $backendDir"
}

if ($JavaHome) {
    $resolvedJavaHome = (Resolve-Path -LiteralPath $JavaHome).Path
    $env:JAVA_HOME = $resolvedJavaHome
    $env:Path = "$resolvedJavaHome\bin;$env:Path"
}

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "Iniciando backend local em http://127.0.0.1:$Port/api"

Set-Location $backendDir
mvn "spring-boot:run" "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--server.port=$Port"
