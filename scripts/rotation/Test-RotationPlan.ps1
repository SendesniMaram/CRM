<# Preparation checks only: dummy config, syntax and authorization gates.
   No backup, SQL, live .env access, image build or container lifecycle operations.
#>
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
. (Join-Path $PSScriptRoot 'Rotation-Helpers.ps1')
$count = 0
function Assert-Plan([bool]$Condition, [string]$Description) {
    if (-not $Condition) { throw $Description }
    $script:count++
    Write-Output "PASS: $Description"
}

$tokens = $null; $parseErrors = $null
[void][Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'Rotation-Helpers.ps1'), [ref]$tokens, [ref]$parseErrors)
Assert-Plan ($parseErrors.Count -eq 0) 'Helper PowerShell syntax valid'
$doc = Get-Content -LiteralPath (Join-Path $root 'docs/ROTATION_EXECUTION.md') -Raw -Encoding UTF8
$blocks = [regex]::Matches($doc, '(?s)```powershell\r?\n(.*?)```')
$code = ($blocks | ForEach-Object { $_.Groups[1].Value }) -join "`n"
[void][Management.Automation.Language.Parser]::ParseInput($code, [ref]$tokens, [ref]$parseErrors)
Assert-Plan ($blocks.Count -gt 0 -and $parseErrors.Count -eq 0) 'Runbook PowerShell blocks parse without execution'

# These calls must fail at authorization gates BEFORE any I/O.
foreach ($attempt in @(
    { Backup-CrmDatabases }, { Test-CrmBackup },
    { Save-CrmProtectedState }, { Set-CrmEnvironment }
)) {
    $rejected = $false
    try { & $attempt } catch { $rejected = $_.Exception.Message -match 'authorization required' }
    Assert-Plan $rejected 'Execution rejected without explicit authorization switch'
}
$dummyPassword = ConvertTo-SecureString 'dummy-plan-only' -AsPlainText -Force
foreach ($attempt in @(
    { Invoke-CrmMysql -Password $dummyPassword -Sql 'CREATE USER dummy;' -AllowWrite },
    { Invoke-CrmMysql -Password $dummyPassword -Sql 'SELECT 1; DELETE FROM dummy;' }
)) {
    $rejected = $false
    try { & $attempt } catch { $rejected = $_.Exception.Message -match 'authorization required|execution authorization' }
    Assert-Plan $rejected 'SQL write attempt rejected before any Docker call'
}

$values = @{ MYSQL_ROOT_PASSWORD='rotation-plan-only-not-an-active-password'; APP_JWT_SECRET='rotation-plan-only-not-an-active-jwt-secret-2026' }
$prefixes = @('IDENTITY','EMPLOYEE','DEPARTMENT','CUSTOMER','HR','INVOICE','FEES','PAYROLL')
foreach ($prefix in $prefixes) {
    $values[$prefix + '_DB_USERNAME'] = 'crm_' + $prefix.ToLowerInvariant()
    $values[$prefix + '_DB_PASSWORD'] = 'rotation-plan-only-not-an-active-password'
    $values[$prefix + '_ROTATION_IMAGE'] = 'crm/rotation-plan:dummy'
}
$values['MYSQL_ROTATION_IMAGE'] = 'crm/rotation-plan:dummy'
$values['GATEWAY_ROTATION_IMAGE'] = 'crm/rotation-plan:dummy'
$saved = @{}
$empty = [IO.Path]::GetTempFileName()
try {
    foreach ($key in $values.Keys) {
        $saved[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
        [Environment]::SetEnvironmentVariable($key, $values[$key], 'Process')
    }
    $arguments = @('compose','--project-directory',$root,'--project-name','crm',
        '--env-file',$empty,'-f',(Join-Path $root 'docker-compose.yml'),
        '-f',(Join-Path $root 'docker-compose.rotation.yml'),'config')
    [void](Invoke-CrmDocker -Arguments ($arguments + @('--quiet')))
    Assert-Plan $true 'Merged Compose syntax valid using only dummy variables'
    $config = Invoke-CrmDocker -Arguments ($arguments + @('--format','json')) | ConvertFrom-Json
    foreach ($prefix in $prefixes) {
        $service = $prefix.ToLowerInvariant() + '-service'
        $settings = $config.services.$service.environment
        Assert-Plan ($settings.SPRING_DATASOURCE_USERNAME -eq ('crm_' + $prefix.ToLowerInvariant())) "$service uses its dedicated account"
        Assert-Plan ($settings.SPRING_JPA_HIBERNATE_DDL_AUTO -eq 'validate' -and $settings.SPRING_SQL_INIT_MODE -eq 'never') "$service cannot initialize the schema"
        Assert-Plan ($config.services.$service.image -eq 'crm/rotation-plan:dummy') "$service image is explicitly pinned"
    }
    Assert-Plan (($config.volumes.PSObject.Properties.Name -contains 'mysql-data') -and $config.volumes.'mysql-data'.name -eq 'crm_mysql-data') 'Existing MySQL volume name preserved'
    Assert-Plan ($config.services.mysql.healthcheck.test[-1].Contains('SELECT 1')) 'MySQL healthcheck authenticates a real query'
    Assert-Plan (-not ($config.services.'gateway-service'.environment.PSObject.Properties.Name -contains 'APP_JWT_SECRET')) 'Gateway has no JWT rotation dependency'
    [Environment]::SetEnvironmentVariable('IDENTITY_DB_PASSWORD', $null, 'Process')
    $rejected = $false
    try { [void](Invoke-CrmDocker -Arguments ($arguments + @('--quiet'))) } catch { $rejected = $true }
    Assert-Plan $rejected 'Merged configuration refuses a missing dedicated password'
} finally {
    foreach ($key in $saved.Keys) { [Environment]::SetEnvironmentVariable($key, $saved[$key], 'Process') }
    Remove-Item -LiteralPath $empty
}
Write-Output "Rotation preparation checks passed: $count"
