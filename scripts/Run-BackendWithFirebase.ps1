[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ServiceAccountPath,

    [int]$Port = 8080
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "EcoBookAiBackend"
$resolvedServiceAccountPath = (Resolve-Path -LiteralPath $ServiceAccountPath).Path

if (-not (Test-Path -LiteralPath $resolvedServiceAccountPath -PathType Leaf)) {
    throw "Arquivo de credencial nao encontrado: $ServiceAccountPath"
}

if (-not (Test-Path -LiteralPath $backendDir -PathType Container)) {
    throw "Pasta do backend nao encontrada: $backendDir"
}

$env:FIREBASE_SERVICE_ACCOUNT_PATH = $resolvedServiceAccountPath
$env:GOOGLE_APPLICATION_CREDENTIALS = $resolvedServiceAccountPath

Write-Host "FIREBASE_SERVICE_ACCOUNT_PATH=$resolvedServiceAccountPath"
Write-Host "GOOGLE_APPLICATION_CREDENTIALS=$resolvedServiceAccountPath"
Write-Host "Iniciando backend em http://localhost:$Port/api"

Set-Location $backendDir
mvn "spring-boot:run" "-Dspring-boot.run.arguments=--server.port=$Port"
