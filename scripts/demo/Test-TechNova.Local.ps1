# Offline tests only: the API function is replaced by an in-memory implementation.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Import-TechNova.ps1')
$realApi = ${function:Invoke-DemoApi}
$plan = Read-DemoPlan (Join-Path $PSScriptRoot 'technova.json')
$demoPeople = @($plan.employees) + @($plan.customers)
Assert-Demo (@($demoPeople | Where-Object { $_.payload.phone -notmatch '^\+120255501\d{2}$' }).Count -eq 0) 'Demo phones must use reserved fictitious numbers.'
Assert-Demo (@($demoPeople.payload.phone | Select-Object -Unique).Count -eq 11) 'Demo phones must remain unique.'
Assert-Demo (@($demoPeople | Where-Object { $_.payload.address -notlike 'ADRESSE FICTIVE *' }).Count -eq 0) 'Demo addresses must be explicitly fictitious.'
Assert-Demo (@($plan.employees | Where-Object { $_.payload.firstName -notlike 'PrenomDemo*' -or $_.payload.lastName -notlike 'EmployeFictif*' }).Count -eq 0) 'Demo employee names must be synthetic.'
Assert-Demo (@($plan.customers | Where-Object { $_.payload.name -notlike '[[]DEMO SYNTHETIQUE]*' -or $_.payload.company -notlike 'ENTREPRISE FICTIVE *' }).Count -eq 0) 'Demo customers must be synthetic.'
$script:Store = @{}; $script:Writes = 0; $script:Patches = 0; $script:Reads = 0
foreach ($module in $script:Paths.Keys) { $script:Store[$module] = @() }
function Invoke-DemoApi([string]$Method, [string]$Path, $Body = $null) {
    $module = @($script:Paths.Keys | Where-Object { $Path.StartsWith($script:Paths[$_]) })[0]
    if ($Method -eq 'GET') {
        $script:Reads++
        if ($Path.Contains('?')) {
            $page = [int]([regex]::Match($Path, 'page=(\d+)').Groups[1].Value)
            $rows = @($script:Store[$module]); $content = @($rows | Select-Object -Skip ($page*100) -First 100)
            return [pscustomobject]@{content=$content;number=$page;size=100;totalElements=$rows.Count;totalPages=[int][math]::Ceiling($rows.Count/100.0)}
        }
        $id = [long]($Path.Split('/')[-1])
        return @($script:Store[$module] | Where-Object id -EQ $id)[0]
    }
    if ($Method -eq 'POST') {
        $script:Writes++
        $copy = [ordered]@{id=($script:Store[$module].Count+1)}
        foreach ($key in $Body.Keys) { $copy[$key]=$Body[$key] }
        if ($module -eq 'leaves') { $copy['status']='PENDING' }
        if ($module -eq 'invoices') { $copy['totalAmount']=[decimal]$Body.subtotal+[decimal]$Body.tax }
        if ($module -eq 'fees') { $copy['amount']=[decimal]$Body.hourlyRate*[decimal]$Body.workedHours }
        if ($module -eq 'payrolls') { $copy['netSalary']=[decimal]$Body.baseSalary+[decimal]$Body.bonuses-[decimal]$Body.deductions-[decimal]$Body.taxAmount }
        $record=[pscustomobject]$copy; $script:Store[$module]+=$record; return $record
    }
    Assert-Demo ($Method -eq 'PATCH' -and $module -eq 'leaves') 'Unexpected local write.'
    $script:Patches++; $id=[long]($Path.Split('/')[-2]); $row=@($script:Store.leaves | Where-Object id -EQ $id)[0]
    $row.status=if($Path.EndsWith('/approve')){'APPROVED'}else{'REJECTED'}; return $row
}
# Reproduce the real partial import: DEV id=1 is already correctly stored in UTF-8.
$dev = $plan.departments[0]
$bytes = [byte[]](0x44,0xC3,0xA9,0x76,0x65,0x6C,0x6F,0x70,0x70,0x65,0x6D,0x65,0x6E,0x74,0x20,0x69,0x6E,0x66,0x6F,0x72,0x6D,0x61,0x74,0x69,0x71,0x75,0x65)
$storedName = [Text.Encoding]::UTF8.GetString($bytes)
Assert-Demo ($storedName -ceq $dev.payload.name) 'Stored DEV bytes differ from plan.'
$legacy = [pscustomobject]@{id=1;code=$dev.payload.code;name=[Text.Encoding]::GetEncoding(28591).GetString($bytes);description=$dev.payload.description}
$failed=$false
try { Assert-DemoRecord departments $dev (Get-DemoPayload $dev @{}) $legacy } catch {
    $failed=$_.Exception.Message -ceq 'Conflit de contenu : departments/DEV/name. Aucun ecrasement autorise.'
}
Assert-Demo $failed 'Exact legacy encoding conflict not reproduced.'
$legacy.name=$storedName; $script:Store.departments=@($legacy)
Start-DemoImport $plan
Assert-Demo ($script:Writes -eq 39 -and $script:Patches -eq 4 -and $script:Reads -gt 44) 'Offline partial resume failed.'
$script:Writes=0; $script:Patches=0
Start-DemoImport $plan
Assert-Demo ($script:Writes -eq 0 -and $script:Patches -eq 0) 'Offline idempotence failed.'
$script:Store.employees[0].email='collision@example.invalid'
$failed=$false
try { Start-DemoImport $plan } catch { $failed=$true }
Assert-Demo ($failed -and $script:Writes -eq 0 -and $script:Patches -eq 0) 'Conflict did not stop before writes.'
$script:Store.employees[0].email=$plan.employees[0].payload.email
$script:Store.leaves[0].status='PENDING'
$failed=$false
try { Start-DemoImport $plan } catch { $failed=$true }
Assert-Demo ($failed -and $script:Patches -eq 0) 'Unsafe transition of previous record.'
# More than one page must be scanned, including a last partial page.
$script:Store.departments=@(1..205 | ForEach-Object {[pscustomobject]@{id=$_}})
Assert-Demo (@(Get-DemoRows departments).Count -eq 205) 'Pagination test failed.'
# Authentication contracts are mocked too: no real credentials, no signed session.
function Get-Credential {
    $object=[pscustomobject]@{UserName=$script:Identifier}
    $object | Add-Member ScriptMethod GetNetworkCredential { [pscustomobject]@{Password=$script:TestPassword} }
    return $object
}
$script:AuthRole='SUPER_ADMIN'
Set-Item Function:Invoke-DemoApi $realApi
# Exercise the real response decoder, with UTF-8 bytes and no HTTP charset.
$script:UnicodeResponse = $storedName
function Invoke-WebRequest {
    $json=[pscustomobject]@{id=1;name=$script:UnicodeResponse;code=$dev.payload.code;description=$dev.payload.description}|ConvertTo-Json -Compress
    return [pscustomobject]@{RawContentStream=(New-Object IO.MemoryStream(,[Text.Encoding]::UTF8.GetBytes($json)))}
}
$decoded=Invoke-DemoApi GET '/department/api/departments/1'
Assert-DemoRecord departments $dev (Get-DemoPayload $dev @{}) $decoded
$decoded.name='Different real name'; $failed=$false
try { Assert-DemoRecord departments $dev (Get-DemoPayload $dev @{}) $decoded } catch { $failed=$true }
Assert-Demo $failed 'Real name mismatch was ignored.'
$script:LoginCalls=0
# Use the real API wrapper and JSON serialization; replace only the HTTP transport.
function Invoke-WebRequest {
    param($Uri,$Method,$Headers,$TimeoutSec,$MaximumRedirection,$ErrorAction,$Verbose,$Debug,$ContentType,$Body,$UseBasicParsing)
    $script:LoginCalls++
    Assert-Demo ($Method -eq 'POST' -and $Uri -ceq 'http://localhost:8080/identity/api/auth/login') 'Unexpected login URL or method.'
    Assert-Demo ($ContentType -ceq 'application/json; charset=utf-8' -and $Body -is [byte[]]) 'Incorrect JSON transport.'
    $json=[Text.Encoding]::UTF8.GetString($Body)|ConvertFrom-Json
    Assert-Demo (@($json.PSObject.Properties).Count -eq 3 -and $json.password -ceq $script:TestPassword) 'Password or JSON fields changed.'
    if($script:Identifier.Contains('@')) {
        Assert-Demo ($null -eq $json.username -and $json.email -ceq $script:Identifier) 'Email sent as username.'
    } else { Assert-Demo ($null -eq $json.email -and $json.username -ceq $script:Identifier) 'Incorrect username payload.' }
    $claims=@{roles=@($script:AuthRole);exp=([DateTimeOffset]::UtcNow.ToUnixTimeSeconds()+600)}|ConvertTo-Json -Compress
    $part=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($claims)).TrimEnd('=').Replace('+','-').Replace('/','_')
    $script:ExpectedToken='offline.'+$part+'.unsigned'
    $jsonResponse=[pscustomobject]@{enabled=$true;roles=@($script:AuthRole);token=$script:ExpectedToken}|ConvertTo-Json -Compress
    return [pscustomobject]@{RawContentStream=(New-Object IO.MemoryStream(,[Text.Encoding]::UTF8.GetBytes($jsonResponse)))}
}
# Spaces, quotes, backslash and Unicode must survive the UTF-8 JSON round trip.
$script:TestPassword=' local "quoted" \ '+[char]0x00e9+' password '
foreach($script:Identifier in @('offline-user','offline-user@example.invalid')) {
    Connect-Demo
    Assert-Demo ($script:Headers.Authorization -ceq ('Bearer '+$script:ExpectedToken)) 'JWT response field token not used.'
}
Assert-Demo ($script:LoginCalls -eq 2) 'Login retried unexpectedly.'
$script:Headers=@{}; $script:AuthRole='EMPLOYEE'; $failed=$false
try { Connect-Demo } catch { $failed=$true }
Assert-Demo ($failed -and $script:Headers.Count -eq 0) 'Non-admin mock accepted.'
Assert-Demo ($script:LoginCalls -eq 3) 'Non-admin login retried.'
Set-Item Function:Invoke-DemoApi $realApi
function Invoke-WebRequest { $script:LoginCalls++; throw 'OFFLINE-SENSITIVE-CANARY' }
$failed=$false
try { Invoke-DemoApi GET '/offline' } catch {
    $failed=$true
    Assert-Demo (-not $_.Exception.Message.Contains('OFFLINE-SENSITIVE-CANARY')) 'Raw API error leaked.'
}
Assert-Demo $failed 'API failure did not stop.'
$before=$script:LoginCalls; $failed=$false
try { Connect-Demo } catch { $failed=$true }
Assert-Demo ($failed -and $script:LoginCalls -eq ($before+1) -and $script:Headers.Count -eq 0) 'Failed login retried or retained a session.'
function Invoke-WebRequest { throw 'NETWORK MUST NOT BE CALLED IN DRY RUN' }
& (Join-Path $PSScriptRoot 'Import-TechNova.ps1') -DryRun
Write-Host 'LOCAL TESTS PASS: exact DEV encoding conflict, explicit UTF-8 decoding, partial resume (1 existing + 39 mock creations), 4 mock transitions, idempotence, real conflicts, pagination, login contract and network-free DryRun. No real API called.'
