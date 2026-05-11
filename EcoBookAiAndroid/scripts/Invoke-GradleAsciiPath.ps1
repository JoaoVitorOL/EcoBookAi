[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not $GradleArgs -or $GradleArgs.Count -eq 0) {
    $GradleArgs = @("tasks", "--all")
}

$projectDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$driveCandidates = @("X:", "W:", "V:", "U:", "T:")
$mappedDrive = $null
$enteredLocation = $false
$exitCode = 1

try {
    foreach ($candidate in $driveCandidates) {
        if (-not (Test-Path "$candidate\")) {
            $mappedDrive = $candidate
            break
        }
    }

    if (-not $mappedDrive) {
        throw "Nao foi encontrado um drive livre para criar o alias ASCII temporario."
    }

    & subst $mappedDrive $projectDir | Out-Null

    if (-not (Test-Path "$mappedDrive\")) {
        throw "Falha ao criar o alias temporario $mappedDrive para $projectDir."
    }

    Push-Location "$mappedDrive\"
    $enteredLocation = $true

    & ".\\gradlew.bat" @GradleArgs
    $exitCode = $LASTEXITCODE
}
finally {
    if ($enteredLocation) {
        Pop-Location
    }

    if ($mappedDrive) {
        & subst $mappedDrive /d | Out-Null
    }
}

exit $exitCode
