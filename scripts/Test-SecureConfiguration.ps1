<# Validates configuration only. Never starts containers or reads the active .env. #>
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$passed = 0

function Assert-Configuration([bool]$condition, [string]$message) {
    if (-not $condition) { throw $message }
    $script:passed++
    Write-Output "PASS: $message"
}

$compose = Get-Content -LiteralPath (Join-Path $root 'docker-compose.yml') -Raw
foreach ($name in @('MYSQL_ROOT_PASSWORD', 'APP_JWT_SECRET')) {
    $references = [regex]::Matches($compose, ('\$\{' + $name + '([^}]*)\}'))
    Assert-Configuration ($references.Count -gt 0) "$name is referenced in Compose"
    Assert-Configuration (@($references | Where-Object { -not $_.Groups[1].Value.StartsWith(':?') }).Count -eq 0) "$name is required, with no fallback"
}
$example = Get-Content -LiteralPath (Join-Path $root '.env.example')
foreach ($name in @('MYSQL_ROOT_PASSWORD', 'APP_JWT_SECRET', 'SPRING_DATASOURCE_PASSWORD')) {
    Assert-Configuration (@($example | Where-Object { $_ -match ('^' + $name + '=$') }).Count -eq 1) "$name is empty in the example"
}
$backend = Get-Content -LiteralPath (Join-Path $root 'backend/src/main/resources/application.properties') -Raw
Assert-Configuration ($backend.Contains('spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}')) 'Legacy DB password is supplied externally'
Assert-Configuration ($backend.Contains('jwt.secret=${APP_JWT_SECRET}')) 'Legacy JWT secret is supplied externally'
$jwt = Get-Content -LiteralPath (Join-Path $root 'backend/src/main/java/com/crm/security/jwt/JwtService.java') -Raw
Assert-Configuration ($jwt.Contains('@Value("${jwt.secret}")') -and -not $jwt.Contains('${jwt.secret:')) 'Legacy JWT has no code fallback'

foreach ($path in @('.env', '.env.production', '.env.production.local', 'frontend/.env.local')) {
    & git -C $root check-ignore --quiet -- $path
    Assert-Configuration ($LASTEXITCODE -eq 0) "$path is ignored"
}
& git -C $root check-ignore --quiet -- .env.example
Assert-Configuration ($LASTEXITCODE -eq 1) '.env.example can be tracked'

$saved = @{}
$names = @('MYSQL_ROOT_PASSWORD', 'APP_JWT_SECRET')
$emptyEnvironment = [IO.Path]::GetTempFileName()
function Invoke-ComposeValidation([string[]]$composeArguments) {
    # Windows PowerShell otherwise turns expected native stderr into a stop error.
    $ErrorActionPreference = 'Continue'
    $captured = & docker @composeArguments 2>&1
    return @{ Code = $LASTEXITCODE; Text = ($captured | Out-String) }
}
try {
    foreach ($name in $names) {
        $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
        [Environment]::SetEnvironmentVariable($name, 'configuration-test-only-value-2026-never-use-as-a-secret', 'Process')
    }
    # All output is captured. --quiet never prints the interpolated configuration.
    $arguments = @('compose', '--project-directory', $root, '--env-file', $emptyEnvironment, '-f', (Join-Path $root 'docker-compose.yml'), 'config', '--quiet')
    $result = Invoke-ComposeValidation $arguments
    Assert-Configuration ($result.Code -eq 0) 'Compose syntax accepts dummy credentials'
    foreach ($name in $names) {
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
        $result = Invoke-ComposeValidation $arguments
        Assert-Configuration ($result.Code -ne 0) "Compose rejects missing $name"
        Assert-Configuration ($result.Text.Contains($name)) "Compose identifies missing $name"
        [Environment]::SetEnvironmentVariable($name, 'configuration-test-only-value-2026-never-use-as-a-secret', 'Process')
    }
} finally {
    foreach ($name in $names) {
        if ($saved.ContainsKey($name)) {
            [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process')
        }
    }
    Remove-Item -LiteralPath $emptyEnvironment
}
Write-Output "Configuration checks passed: $passed"
