param(
    [string]$DbHost = "localhost",
    [int]$DbPort = 5432,
    [Parameter(Mandatory = $true)]
    [string]$Database,
    [Parameter(Mandatory = $true)]
    [string]$Username,
    [Parameter(Mandatory = $true)]
    [string]$OutputFile,
    [string]$Password = ""
)

$pgDump = Get-Command pg_dump -ErrorAction SilentlyContinue
if (-not $pgDump) {
    throw "pg_dump nao encontrado no PATH. Instale o cliente PostgreSQL antes de criar snapshots de rollback."
}

$resolvedOutput = Resolve-Path -LiteralPath (Split-Path -Path $OutputFile -Parent) -ErrorAction SilentlyContinue
if (-not $resolvedOutput) {
    New-Item -ItemType Directory -Path (Split-Path -Path $OutputFile -Parent) -Force | Out-Null
}

$previousPassword = $env:PGPASSWORD
if ($Password) {
    $env:PGPASSWORD = $Password
}

try {
    & $pgDump.Source `
        --format=custom `
        --file=$OutputFile `
        --host=$DbHost `
        --port=$DbPort `
        --username=$Username `
        $Database

    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump retornou codigo $LASTEXITCODE ao criar o snapshot."
    }
} finally {
    $env:PGPASSWORD = $previousPassword
}
