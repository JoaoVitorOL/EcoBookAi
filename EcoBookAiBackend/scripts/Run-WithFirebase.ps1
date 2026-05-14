[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ServiceAccountPath,

    [int]$Port = 8080
)

$scriptDir = Split-Path -Parent $PSCommandPath
$repoRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$rootScript = Join-Path $repoRoot "scripts\Run-BackendWithFirebase.ps1"

& $rootScript -ServiceAccountPath $ServiceAccountPath -Port $Port
