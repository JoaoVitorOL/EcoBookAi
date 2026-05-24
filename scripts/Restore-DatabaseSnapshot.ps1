param(
    [string]$DbHost = "localhost",
    [int]$DbPort = 5432,
    [Parameter(Mandatory = $true)]
    [string]$Database,
    [Parameter(Mandatory = $true)]
    [string]$Username,
    [Parameter(Mandatory = $true)]
    [string]$InputFile,
    [string]$Password = ""
)

$pgRestore = Get-Command pg_restore -ErrorAction SilentlyContinue
if (-not $pgRestore) {
    throw "pg_restore nao encontrado no PATH. Instale o cliente PostgreSQL antes de restaurar snapshots."
}

if (-not (Test-Path -LiteralPath $InputFile)) {
    throw "Snapshot nao encontrado em '$InputFile'."
}

$previousPassword = $env:PGPASSWORD
if ($Password) {
    $env:PGPASSWORD = $Password
}

try {
    & $pgRestore.Source `
        --clean `
        --if-exists `
        --no-owner `
        --host=$DbHost `
        --port=$DbPort `
        --username=$Username `
        --dbname=$Database `
        $InputFile

    if ($LASTEXITCODE -ne 0) {
        throw "pg_restore retornou codigo $LASTEXITCODE ao restaurar o snapshot."
    }
} finally {
    $env:PGPASSWORD = $previousPassword
}
