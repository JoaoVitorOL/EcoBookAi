[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ServiceAccountPath,

    [string]$JavaHome,

    [int]$Port = 8080
)

$scriptDir = Split-Path -Parent $PSCommandPath
$backendDir = Split-Path -Parent $scriptDir
$resolvedServiceAccountPath = (Resolve-Path -LiteralPath $ServiceAccountPath).Path

if (-not (Test-Path -LiteralPath $resolvedServiceAccountPath -PathType Leaf)) {
    throw "Arquivo de credencial nao encontrado: $ServiceAccountPath"
}

if ($JavaHome) {
    $resolvedJavaHome = (Resolve-Path -LiteralPath $JavaHome).Path
    $env:JAVA_HOME = $resolvedJavaHome
    $env:Path = "$resolvedJavaHome\bin;$env:Path"
}

$env:FIREBASE_SERVICE_ACCOUNT_PATH = $resolvedServiceAccountPath
$env:GOOGLE_APPLICATION_CREDENTIALS = $resolvedServiceAccountPath

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "FIREBASE_SERVICE_ACCOUNT_PATH=$resolvedServiceAccountPath"
Write-Host "GOOGLE_APPLICATION_CREDENTIALS=$resolvedServiceAccountPath"
Write-Host "Iniciando backend em http://127.0.0.1:$Port/api"

Set-Location $backendDir
mvn "spring-boot:run" "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--server.port=$Port"
