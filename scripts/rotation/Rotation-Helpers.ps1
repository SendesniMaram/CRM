<# Definitions only: dot-sourcing does not contact Docker or change any file.
   Mutations require -Authorized and the separate runbook authorization.
   Never enable tracing/transcription while using credentials. Windows PowerShell 5.1+.
#>
function Invoke-CrmDocker {
    param([Parameter(Mandatory)][string[]]$Arguments, [string]$InputText = '', [switch]$IncludeStandardError)
    $quoted = foreach ($argument in $Arguments) {
        '"' + [regex]::Replace([regex]::Replace($argument, '(\\*)"', '$1$1\"'), '(\\+)$', '$1$1') + '"'
    }
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = 'docker'
    $start.Arguments = $quoted -join ' '
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardInput = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.StandardOutputEncoding = New-Object Text.UTF8Encoding($false)
    $start.StandardErrorEncoding = New-Object Text.UTF8Encoding($false)
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $start
    try {
        [void]$process.Start()
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        # Preserve accented DEMO-TNC text even under Windows legacy console encodings.
        $inputBytes = [Text.Encoding]::UTF8.GetBytes($InputText)
        $process.StandardInput.BaseStream.Write($inputBytes, 0, $inputBytes.Length)
        $process.StandardInput.BaseStream.Flush()
        $process.StandardInput.Close()
        $process.WaitForExit()
        $text = $stdout.GetAwaiter().GetResult()
        $errorText = $stderr.GetAwaiter().GetResult()
        if ($process.ExitCode -ne 0) { throw 'Docker command failed; private output withheld. Stop and diagnose without logging credentials.' }
        if ($IncludeStandardError) { $text += $errorText }
        return $text
    } finally { $process.Dispose() }
}

function ConvertFrom-CrmSecureString([Security.SecureString]$Value) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

function New-CrmSecret([int]$Bytes = 64) {
    $buffer = New-Object byte[] $Bytes
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($buffer); return [Convert]::ToBase64String($buffer) }
    finally { $generator.Dispose(); [Array]::Clear($buffer, 0, $buffer.Length) }
}

function Invoke-CrmMysql {
    param([Parameter(Mandatory)][Security.SecureString]$Password,
          [string]$Sql, [switch]$Dump, [switch]$AllowWrite, [switch]$Authorized)
    if (($Dump -or $AllowWrite) -and -not $Authorized) { throw 'Separate execution authorization required.' }
    if (-not $Dump -and -not $AllowWrite -and $Sql -notmatch '^\s*(SELECT|SHOW|CHECKSUM|START TRANSACTION READ ONLY)\b') {
        throw 'Non-read-only SQL requires -AllowWrite -Authorized.'
    }
    if (-not $Dump -and -not $AllowWrite -and $Sql -match '(?i)\b(CREATE|ALTER|UPDATE|DELETE|INSERT|DROP|GRANT|REVOKE|TRUNCATE|REPLACE|CALL|LOAD)\b') {
        throw 'Write statements require explicit execution authorization.'
    }
    $plain = ConvertFrom-CrmSecureString $Password
    $config = "[client]`nuser=root`npassword=`"" + $plain.Replace('\', '\\').Replace('"', '\"') + "`"`n"
    $config64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($config))
    $plain = $null; $config = $null
    if ($Dump) {
        $command = 'mysqldump --defaults-extra-file="$config" --single-transaction --quick --default-character-set=utf8mb4 --hex-blob --routines --events --triggers --no-tablespaces --set-gtid-purged=OFF --column-statistics=0 --databases crm_db identity_db employee_db department_db hr_db customer_db invoice_db fees_db payroll_db'
        $sql64 = ''
    } else {
        $command = 'printf %s "$sql64" | base64 -d | mysql --defaults-extra-file="$config" --default-character-set=utf8mb4 --batch --skip-column-names'
        $sql64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Sql))
    }
    # Credentials and SQL travel only through stdin, never process arguments.
    # A private option file in the container is removed even when MySQL fails.
    $shell = @'
set -eu
umask 077
config=$(mktemp)
trap 'rm -f "$config"' EXIT HUP INT TERM
printf %s '__CONFIG__' | base64 -d > "$config"
sql64='__SQL__'
__COMMAND__
'@
    $shell = $shell.Replace('__CONFIG__', $config64).Replace('__SQL__', $sql64).Replace('__COMMAND__', $command)
    return Invoke-CrmDocker -Arguments @('exec', '-i', 'crm-mysql-1', 'sh') -InputText ($shell.Replace("`r", '') + "`n")
}

function Save-CrmProtectedState {
    param([string]$Path, $Value, [switch]$Authorized)
    if (-not $Authorized) { throw 'Separate execution authorization required.' }
    if (Test-Path -LiteralPath $Path) { throw 'Refusing to overwrite protected state.' }
    Add-Type -AssemblyName System.Security
    $bytes = [Text.Encoding]::UTF8.GetBytes(($Value | ConvertTo-Json -Depth 30 -Compress))
    try {
        $encrypted = [Security.Cryptography.ProtectedData]::Protect($bytes, $null, [Security.Cryptography.DataProtectionScope]::CurrentUser)
        [IO.File]::WriteAllBytes($Path, $encrypted)
    } finally { [Array]::Clear($bytes, 0, $bytes.Length) }
}

function Read-CrmProtectedState([string]$Path) {
    Add-Type -AssemblyName System.Security
    $bytes = [Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($Path), $null, [Security.Cryptography.DataProtectionScope]::CurrentUser)
    try { return ([Text.Encoding]::UTF8.GetString($bytes) | ConvertFrom-Json) }
    finally { [Array]::Clear($bytes, 0, $bytes.Length) }
}

function Backup-CrmDatabases {
    param([string]$BackupPath, [Security.SecureString]$Password, [switch]$Authorized)
    if (-not $Authorized) { throw 'Separate execution authorization required.' }
    $root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
    $full = [IO.Path]::GetFullPath($BackupPath)
    if ($full.StartsWith($root.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Backups must be outside the repository.' }
    if (-not (Test-Path -LiteralPath (Split-Path $full))) { throw 'Create a protected external backup directory first.' }
    $mysql = (Invoke-CrmDocker -Arguments @('inspect', 'crm-mysql-1') | ConvertFrom-Json)[0]
    if ($mysql.State.Health.Status -ne 'healthy') { throw 'MySQL is not healthy.' }
    $inventory = Invoke-CrmMysql -Password $Password -Sql "SELECT TABLE_SCHEMA,TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA IN ('crm_db','identity_db','employee_db','department_db','hr_db','customer_db','invoice_db','fees_db','payroll_db') AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_SCHEMA,TABLE_NAME;"
    if (-not $inventory.Trim()) { throw 'No application tables found.' }
    $tables = foreach ($line in $inventory.Trim().Split("`n")) { $parts = $line.Trim().Split("`t"); '`' + $parts[0] + '`.`' + $parts[1] + '`' }
    $checksumSql = 'CHECKSUM TABLE ' + ($tables -join ',') + ' EXTENDED;'
    $before = Invoke-CrmMysql -Password $Password -Sql $checksumSql
    if ($before -match '\bNULL\b') { throw 'Table checksum unavailable.' }
    $dump = Invoke-CrmMysql -Password $Password -Dump -Authorized
    if (-not $dump.Contains('-- Dump completed on') -or $dump.Length -gt 64MB) { throw 'Incomplete dump or preparation memory limit exceeded (64 MiB).' }
    $after = Invoke-CrmMysql -Password $Password -Sql $checksumSql
    if ($before -cne $after) { throw 'Data changed during backup. Freeze writers and retry; do not proceed.' }
    $containers = Invoke-CrmDocker -Arguments @('ps', '-aq', '--filter', 'label=com.docker.compose.project=crm')
    $snapshots = Invoke-CrmDocker -Arguments (@('inspect') + @($containers.Trim().Split("`n") | ForEach-Object { $_.Trim() })) | ConvertFrom-Json
    $bundle = @{ Sql = $dump; Checksums = $before; ChecksumSql = $checksumSql; MysqlImage = $mysql.Image;
        Env = [IO.File]::ReadAllText((Join-Path $root '.env')); Containers = $snapshots; CreatedUtc = [DateTime]::UtcNow.ToString('o') }
    Save-CrmProtectedState -Path $full -Value $bundle -Authorized
    Write-Output 'Encrypted backup created. It is NOT verified until the isolated restore succeeds.'
}

function Test-CrmBackup {
    param([string]$BackupPath, [switch]$Authorized)
    if (-not $Authorized) { throw 'Isolated restore execution authorization required.' }
    $bundle = Read-CrmProtectedState $BackupPath
    $name = 'crm-rotation-restore-' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
    $cleanupToken = [Guid]::NewGuid().ToString('N')
    $containerId = $null
    $created = $false
    try {
        $containerId = (Invoke-CrmDocker -Arguments @('run', '-d', '--pull=never', '--name', $name, '--label', ('crm.rotation.restore-test=' + $cleanupToken), '--network', 'none', '--tmpfs', '/var/lib/mysql:rw', '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes', $bundle.MysqlImage, '--event-scheduler=OFF')).Trim()
        if ($containerId -notmatch '^[a-f0-9]{64}$') { throw 'Temporary container ID not established. No resource will be deleted.' }
        $created = $true; $ready = $false
        for ($attempt = 0; $attempt -lt 60; $attempt++) {
            try { [void](Invoke-CrmDocker -Arguments @('exec', $name, 'mysql', '-uroot', '-Nse', 'SELECT 1')); $ready = $true; break }
            catch { Start-Sleep -Seconds 2 }
        }
        if (-not $ready) { throw 'Isolated MySQL restore instance did not become ready.' }
        [void](Invoke-CrmDocker -Arguments @('exec', '-i', $name, 'mysql', '-uroot', '--default-character-set=utf8mb4') -InputText $bundle.Sql)
        $actual = Invoke-CrmDocker -Arguments @('exec', '-i', $name, 'mysql', '-uroot', '--batch', '--skip-column-names') -InputText $bundle.ChecksumSql
        if ($actual -cne $bundle.Checksums) { throw 'Restore checksums differ. Stop before any rotation.' }
        # A receipt binds the verified restore to this exact encrypted backup.
        $receipt = @{ BackupSha256 = (Get-FileHash -LiteralPath $BackupPath -Algorithm SHA256).Hash; VerifiedUtc = [DateTime]::UtcNow.ToString('o') }
        [IO.File]::WriteAllText(($BackupPath + '.verified.json'), ($receipt | ConvertTo-Json))
        Write-Output 'Isolated restore and all table checksums verified.'
    } finally {
        if ($created) {
            # Fail closed: never delete by name, or without checking every invariant.
            try {
                $inspected = @(Invoke-CrmDocker -Arguments @('inspect', $containerId) | ConvertFrom-Json)
                if ($inspected.Count -ne 1) { throw 'Unexpected inspection result.' }
                $temporary = $inspected[0]
                $mounts = @($temporary.Mounts)
                $label = $temporary.Config.Labels.'crm.rotation.restore-test'
                $valid = $temporary.Id -ceq $containerId -and
                    $temporary.Image -ceq $bundle.MysqlImage -and
                    $temporary.Name -ceq ('/' + $name) -and
                    $label -ceq $cleanupToken -and
                    $temporary.HostConfig.NetworkMode -ceq 'none' -and
                    @($temporary.HostConfig.Binds | Where-Object { $null -ne $_ }).Count -eq 0 -and
                    @($temporary.HostConfig.VolumesFrom | Where-Object { $null -ne $_ }).Count -eq 0 -and
                    $mounts.Count -eq 1 -and
                    $mounts[0].Type -ceq 'tmpfs' -and
                    $mounts[0].Destination -ceq '/var/lib/mysql' -and
                    $temporary.HostConfig.Tmpfs.'/var/lib/mysql' -ceq 'rw'
                if (-not $valid) { throw 'Temporary container identity or mounts differ.' }
            } catch {
                throw 'Cleanup refused: exact temporary container identity, image and isolation could not be verified. No resource deleted.'
            }
            [void](Invoke-CrmDocker -Arguments @('rm', '-f', $containerId))
        }
    }
}

function Set-CrmEnvironment {
    param([hashtable]$Updates, [switch]$Authorized)
    if (-not $Authorized) { throw 'Active .env modification authorization required.' }
    $root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
    $path = Join-Path $root '.env'
    $text = [IO.File]::ReadAllText($path)
    foreach ($key in $Updates.Keys) {
        if ($key -notmatch '^[A-Z][A-Z0-9_]+$' -or $Updates[$key] -notmatch '^[A-Za-z0-9_+/:=.-]+$') { throw 'Unsafe environment entry.' }
        $pattern = '(?m)^' + [regex]::Escape($key) + '=[^\r\n]*'
        if ([regex]::Matches($text, $pattern).Count -gt 1) { throw 'Duplicate environment variable.' }
        $entry = $key + '=' + $Updates[$key]
        if ($text -match $pattern) { $text = [regex]::Replace($text, $pattern, $entry) }
        else { $text = $text.TrimEnd() + "`n" + $entry + "`n" }
    }
    $temporary = $path + '.rotation.tmp'
    if (Test-Path -LiteralPath $temporary) { throw 'Existing staged environment file; investigate before continuing.' }
    try {
        [IO.File]::WriteAllText($temporary, $text, (New-Object Text.UTF8Encoding($false)))
        [IO.File]::Replace($temporary, $path, $null)
    } finally {
        if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary }
    }
}

function Invoke-CrmHttp {
    param([string]$Uri, [ValidateSet('GET','POST')][string]$Method = 'GET',
          [hashtable]$Headers = @{}, [hashtable]$Body)
    try {
        $parameters = @{ Uri=$Uri; Method=$Method; Headers=$Headers; UseBasicParsing=$true; TimeoutSec=10; ErrorAction='Stop' }
        if ($null -ne $Body) {
            $parameters.ContentType = 'application/json; charset=utf-8'
            $parameters.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Compress))
        }
        $response = Invoke-WebRequest @parameters
        $content = $response.Content
        if ($content -is [byte[]]) { $content = [Text.Encoding]::UTF8.GetString($content) }
        return @{ Status=[int]$response.StatusCode; Json=($content | ConvertFrom-Json) }
    } catch {
        if ($null -ne $_.Exception.Response) {
            return @{ Status=[int]$_.Exception.Response.StatusCode; Json=$null }
        }
        throw 'HTTP transport or JSON decoding failed; response withheld.'
    }
}

function Test-CrmDemoData {
    param([Security.SecureString]$Password)
    $sql = @'
START TRANSACTION READ ONLY;
SELECT 'departments',COUNT(*) FROM department_db.departments WHERE code LIKE 'DEMO-TNC-%';
SELECT 'employees',COUNT(*) FROM employee_db.employees WHERE employee_code LIKE 'DEMO-TNC-%';
SELECT 'customers',COUNT(*) FROM customer_db.customers WHERE email LIKE 'demo.tnc.c%@example.invalid';
SELECT 'leaves',COUNT(*) FROM hr_db.leave_requests WHERE reason LIKE '%DEMO-TNC-%';
SELECT 'invoices',COUNT(*) FROM invoice_db.invoices WHERE invoice_number LIKE 'DEMO-TNC-%';
SELECT 'fees',COUNT(*) FROM fees_db.fees WHERE service_name LIKE 'DEMO-TNC-%';
SELECT 'payrolls',COUNT(*) FROM payroll_db.payrolls p JOIN employee_db.employees e ON e.id=p.employee_id WHERE e.employee_code LIKE 'DEMO-TNC-%';
SELECT 'invalid_employee',COUNT(*) FROM employee_db.employees e LEFT JOIN department_db.departments d ON d.id=e.department_id WHERE e.employee_code LIKE 'DEMO-TNC-%' AND e.department_id IS NOT NULL AND d.id IS NULL;
SELECT 'invalid_leave',COUNT(*) FROM hr_db.leave_requests l LEFT JOIN employee_db.employees e ON e.id=l.employee_id WHERE l.reason LIKE '%DEMO-TNC-%' AND (e.id IS NULL OR l.end_date<l.start_date);
SELECT 'invalid_invoice',COUNT(*) FROM invoice_db.invoices i LEFT JOIN customer_db.customers c ON c.id=i.customer_id WHERE i.invoice_number LIKE 'DEMO-TNC-%' AND (c.id IS NULL OR i.total_amount<>i.subtotal+i.tax OR i.due_date<i.issue_date);
SELECT 'invalid_fee',COUNT(*) FROM fees_db.fees f LEFT JOIN employee_db.employees e ON e.id=f.employee_id LEFT JOIN customer_db.customers c ON c.id=f.customer_id WHERE f.service_name LIKE 'DEMO-TNC-%' AND (e.id IS NULL OR c.id IS NULL OR f.amount<>ROUND(f.hourly_rate*f.worked_hours,2));
SELECT 'invalid_payroll',COUNT(*) FROM payroll_db.payrolls p JOIN employee_db.employees e ON e.id=p.employee_id WHERE e.employee_code LIKE 'DEMO-TNC-%' AND p.net_salary<>p.base_salary+p.bonuses-p.deductions-p.tax_amount;
COMMIT;
'@
    $actual = Invoke-CrmMysql -Password $Password -Sql $sql
    $expected = @{ departments=3; employees=6; customers=5; leaves=6; invoices=8; fees=6; payrolls=6;
        invalid_employee=0; invalid_leave=0; invalid_invoice=0; invalid_fee=0; invalid_payroll=0 }
    $seen = @{}
    foreach ($line in $actual.Trim().Split("`n")) {
        $columns = $line.Trim().Split("`t")
        if ($columns.Length -ne 2 -or -not $expected.ContainsKey($columns[0]) -or [int]$columns[1] -ne $expected[$columns[0]]) {
            throw 'DEMO-TNC counts or invariants differ. Keep maintenance active.'
        }
        $seen[$columns[0]] = $true
    }
    if ($seen.Count -ne $expected.Count) { throw 'Incomplete DEMO-TNC verification.' }
    Write-Output 'DEMO-TNC: 7 expected counts and 5 invariants verified.'
}
