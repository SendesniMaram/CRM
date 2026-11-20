# Compatible with Windows PowerShell 5.1. Never enable HTTP tracing/transcription.
[CmdletBinding()]
param([switch]$DryRun, [string]$DataPath)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($DataPath)) { $DataPath = Join-Path $PSScriptRoot 'technova.json' }
$script:Gateway = 'http://localhost:8080'
$script:Headers = @{}
$script:Paths = [ordered]@{
    departments = '/department/api/departments'; employees = '/employee/api/employees'
    customers = '/customer/api/customers'; leaves = '/hr/api/leaves'
    invoices = '/invoice/api/invoices'; fees = '/fees/api/fees'; payrolls = '/payroll/api/payrolls'
}

function Assert-Demo($Condition, [string]$Message) {
    if (-not $Condition) { Stop-Demo $Message }
}

function Stop-Demo([string]$Message) {
    $errorObject = New-Object System.InvalidOperationException($Message)
    $errorObject.Data['DemoSafe'] = $true
    throw $errorObject
}

function Read-DemoPlan([string]$Path) {
    Assert-Demo (Test-Path -LiteralPath $Path -PathType Leaf) 'Fichier de donnees absent.'
    $plan = Get-Content -LiteralPath $Path -Raw -Encoding UTF8 | ConvertFrom-Json
    $counts = @{ departments=3; employees=6; customers=5; leaves=6; invoices=8; fees=6; payrolls=6 }
    foreach ($module in $script:Paths.Keys) {
        $rows = @($plan.$module)
        Assert-Demo ($rows.Count -eq $counts[$module]) "Nombre invalide : $module."
        Assert-Demo (@($rows.ref | Select-Object -Unique).Count -eq $rows.Count) "References dupliquees : $module."
        foreach ($row in $rows) {
            Assert-Demo ($row.ref -match '^[A-Z][A-Z0-9]*$') 'Reference locale invalide.'
            foreach ($relation in @(@('departmentRef','departments'), @('employeeRef','employees'), @('customerRef','customers'))) {
                if ($row.PSObject.Properties.Name -contains $relation[0]) {
                    Assert-Demo (@($plan.($relation[1]) | Where-Object ref -CEQ $row.($relation[0])).Count -eq 1) "Relation invalide : $module."
                }
            }
        }
    }
    foreach ($row in $plan.departments) { Assert-Demo ($row.payload.code -like 'DEMO-TNC-*' -and $row.payload.description.Length -le 255) 'Departement invalide.' }
    foreach ($row in $plan.employees) {
        Assert-Demo ($row.payload.employeeCode -like 'DEMO-TNC-*' -and $row.payload.email -like '*@example.invalid' -and [decimal]$row.payload.salary -ge 0) 'Employe invalide.'
        Assert-Demo ([datetime]$row.payload.hireDate -le [datetime]::Today) 'Embauche future interdite.'
    }
    foreach ($row in $plan.customers) { Assert-Demo ($row.payload.name -like '[[]D*' -and $row.payload.email -like '*@example.invalid' -and $row.payload.phone -match '^\+120255501\d{2}$') 'Client invalide.' }
    foreach ($row in $plan.leaves) {
        Assert-Demo ($row.targetStatus -cin @('PENDING','APPROVED','REJECTED')) 'Statut de conge invalide.'
        Assert-Demo ([datetime]$row.payload.startDate -ge [datetime]::Today -and [datetime]$row.payload.endDate -ge [datetime]$row.payload.startDate) 'Dates de conge expirees ou invalides : revoir le plan avant insertion.'
        Assert-Demo ($row.payload.reason -like '*DEMO-TNC-*') 'Marqueur de conge absent.'
    }
    foreach ($row in $plan.invoices) {
        Assert-Demo ($row.payload.invoiceNumber -like 'DEMO-TNC-*' -and $row.payload.currency -ceq 'TND' -and [decimal]$row.payload.subtotal -gt 0 -and [decimal]$row.payload.tax -ge 0) 'Facture invalide.'
        Assert-Demo ([datetime]$row.payload.dueDate -ge [datetime]$row.payload.issueDate) 'Echeance invalide.'
    }
    foreach ($row in $plan.fees) { Assert-Demo ($row.payload.serviceName -like 'DEMO-TNC-*' -and [decimal]$row.payload.hourlyRate -gt 0 -and [decimal]$row.payload.workedHours -gt 0 -and $row.payload.description.Length -le 500) 'Honoraire invalide.' }
    foreach ($row in $plan.payrolls) { Assert-Demo ($row.payload.status -ceq 'DEMO' -and $row.payload.currency -ceq 'TND' -and $row.payload.payPeriod -ceq '2026-09' -and [decimal]$row.payload.baseSalary -gt 0) 'Paie invalide.' }
    $total = [decimal]0
    foreach ($row in $plan.invoices) { $total += [decimal]$row.payload.subtotal + [decimal]$row.payload.tax }
    Assert-Demo ($total -eq 30583) 'Total des huit factures different de 30583 TND.'
    Assert-Demo (@($plan.leaves | Where-Object targetStatus -EQ APPROVED).Count -eq 2 -and @($plan.leaves | Where-Object targetStatus -EQ REJECTED).Count -eq 2) 'Transitions attendues : deux approbations et deux refus.'
    return $plan
}

function Invoke-DemoApi([string]$Method, [string]$Path, $Body = $null) {
    # Never include server response bodies, request bodies or exception details in errors.
    try {
        $arguments = @{ Uri=($script:Gateway + $Path); Method=$Method; Headers=$script:Headers; TimeoutSec=30; MaximumRedirection=0; ErrorAction='Stop'; Verbose=$false; Debug=$false }
        if ($null -ne $Body) {
            $arguments.ContentType = 'application/json; charset=utf-8'
            $arguments.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 12 -Compress))
        }
        # Windows PowerShell 5.1 may decode JSON without charset as Latin-1.
        # Spring sends UTF-8 application/json; decode the original bytes explicitly.
        $arguments.UseBasicParsing = $true
        $response = Invoke-WebRequest @arguments
        $stream = $response.RawContentStream
        if ($stream.CanSeek) { $stream.Position = 0 }
        $utf8 = New-Object System.Text.UTF8Encoding($false, $true)
        $reader = New-Object System.IO.StreamReader($stream, $utf8, $true)
        try { $json = $reader.ReadToEnd() } finally { $reader.Dispose() }
        return ConvertFrom-Json -InputObject $json
    } catch {
        $status = 'reseau ou reponse invalide'
        if ($_.Exception.PSObject.Properties.Name -contains 'Response' -and $null -ne $_.Exception.Response) { $status = [string][int]$_.Exception.Response.StatusCode }
        Stop-Demo "API $Method $Path : echec ($status). Arret sans nouvelle tentative."
    }
}

function Get-DemoRows([string]$Module) {
    $all = @(); $page = 0; $expected = $null
    do {
        $result = Invoke-DemoApi GET ($script:Paths[$Module] + "?page=$page&size=100&sortBy=id&direction=asc")
        Assert-Demo ($null -ne $result -and $result.PSObject.Properties.Name -contains 'content') "Reponse paginee invalide : $Module."
        if ($null -eq $expected) { $expected = [long]$result.totalElements }
        Assert-Demo ($expected -ge 0 -and $result.totalElements -eq $expected -and $result.number -eq $page -and $result.size -eq 100) "Pagination instable : $Module."
        Assert-Demo ($result.totalPages -eq [math]::Ceiling($expected / 100.0)) "Nombre de pages incoherent : $Module."
        $rows = @($result.content)
        Assert-Demo ($rows.Count -eq [math]::Min(100, [math]::Max(0, $expected - $page * 100))) "Page incomplete : $Module."
        $all += $rows; $page++
        Assert-Demo ($page -le 1000) 'Limite de lecture atteinte : aucune insertion supplementaire.'
    } while ($all.Count -lt $expected)
    Assert-Demo (@($all | ForEach-Object { $_.id } | Select-Object -Unique).Count -eq $all.Count) "Identifiants dupliques : $Module."
    return $all
}

function Get-DemoPayload($Row, $Ids) {
    $payload = [ordered]@{}
    foreach ($property in $Row.payload.PSObject.Properties) { $payload[$property.Name] = $property.Value }
    foreach ($relation in @(@('departmentRef','departmentId'), @('employeeRef','employeeId'), @('customerRef','customerId'))) {
        if ($Row.PSObject.Properties.Name -contains $relation[0]) {
            $ref = $Row.($relation[0]); Assert-Demo ($Ids.ContainsKey($ref)) 'Relation non resolue.'
            $payload[$relation[1]] = if ($relation[1] -eq 'departmentId') { [long]$Ids[$ref] } else { [string]$Ids[$ref] }
        }
    }
    return $payload
}

function Find-DemoExisting([string]$Module, $Row, $Payload, $Rows) {
    $matches = @($Rows | Where-Object {
        $candidate = $_
        switch ($Module) {
            departments { $candidate.code -ceq $Payload.code -or $candidate.name -ceq $Payload.name }
            employees { $candidate.employeeCode -ceq $Payload.employeeCode -or $candidate.email -ceq $Payload.email }
            customers { $candidate.email -ceq $Payload.email -or $candidate.name -ceq $Payload.name }
            leaves { $candidate.reason -ceq $Payload.reason -or ([string]$candidate.employeeId -ceq [string]$Payload.employeeId -and $candidate.startDate -le $Payload.endDate -and $candidate.endDate -ge $Payload.startDate) }
            invoices { $candidate.invoiceNumber -ceq $Payload.invoiceNumber }
            fees { $candidate.serviceName -ceq $Payload.serviceName }
            payrolls { [string]$candidate.employeeId -ceq [string]$Payload.employeeId -and $candidate.payPeriod -ceq $Payload.payPeriod }
        }
    })
    Assert-Demo ($matches.Count -le 1) "Conflit multiple : $Module/$($Row.ref)."
    if ($matches.Count -eq 1) { return $matches[0] }
    return $null
}

function Assert-DemoRecord([string]$Module, $Row, $Payload, $Record) {
    Assert-Demo ($null -ne $Record -and [long]$Record.id -gt 0) "GET sans identifiant : $Module/$($Row.ref)."
    foreach ($key in $Payload.Keys) {
        $property = $Record.PSObject.Properties[$key]
        Assert-Demo ($null -ne $property) "Champ absent : $Module/$($Row.ref)/$key."
        $expected = $Payload[$key]; $actual = $property.Value
        $numeric = $expected -is [ValueType] -and $expected -isnot [bool]
        $equal = if ($numeric) { [decimal]$actual -eq [decimal]$expected } else { [string]$actual -ceq [string]$expected }
        Assert-Demo $equal "Conflit de contenu : $Module/$($Row.ref)/$key. Aucun ecrasement autorise."
    }
    if ($Module -eq 'invoices') { Assert-Demo ([decimal]$Record.totalAmount -eq ([decimal]$Payload.subtotal + [decimal]$Payload.tax)) 'Calcul facture incorrect.' }
    if ($Module -eq 'fees') { Assert-Demo ([decimal]$Record.amount -eq ([decimal]$Payload.hourlyRate * [decimal]$Payload.workedHours)) 'Calcul honoraire incorrect.' }
    if ($Module -eq 'payrolls') { Assert-Demo ([decimal]$Record.netSalary -eq ([decimal]$Payload.baseSalary + [decimal]$Payload.bonuses - [decimal]$Payload.deductions - [decimal]$Payload.taxAmount)) 'Calcul net incorrect.' }
}

function Connect-Demo {
    $script:Headers = @{}
    $credential = Get-Credential -Message 'Nom utilisateur ou email SUPER_ADMIN - aucun identifiant ne sera enregistre'
    Assert-Demo ($null -ne $credential) 'Authentification annulee.'
    $plain = $null; $response = $null; $request = $null
    try {
        $plain = $credential.GetNetworkCredential().Password
        # Match Angular: an email belongs in email, not in username. Never trim the password.
        $identifier = $credential.UserName
        $request = @{username=$null;email=$null;password=$plain}
        if ($identifier.Contains('@')) { $request.email=$identifier } else { $request.username=$identifier }
        $response = Invoke-DemoApi POST '/identity/api/auth/login' $request
        Assert-Demo ($response.enabled -eq $true -and @($response.roles | Where-Object { $_ -cin @('SUPER_ADMIN','ROLE_SUPER_ADMIN') }).Count -gt 0) 'Compte sans role SUPER_ADMIN.'
        $segments = $response.token.Split('.')
        Assert-Demo ($segments.Count -eq 3) 'Jeton de session invalide.'
        $encoded = $segments[1].Replace('-','+').Replace('_','/'); $encoded += '=' * ((4 - $encoded.Length % 4) % 4)
        $claims = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encoded)) | ConvertFrom-Json
        Assert-Demo (@($claims.roles | Where-Object { $_ -cin @('SUPER_ADMIN','ROLE_SUPER_ADMIN') }).Count -gt 0 -and [long]$claims.exp -gt [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()) 'Role ou expiration JWT invalide.'
        # Claims are inspected, not independently signature-verified here. APIs verify the signature.
        $script:Headers = @{Authorization=('Bearer ' + $response.token)}
    } finally { $plain=$null; $credential=$null; $response=$null; $request=$null }
}

function Start-DemoImport($Plan) {
    $ids = @{}; $baseline = @{}; $created = @{}; $patched = 0
    foreach ($module in $script:Paths.Keys) { $baseline[$module] = @(Get-DemoRows $module); $created[$module] = 0 }
    # Full preflight before the first business write. Negative IDs are symbolic new references.
    $previewIds = @{}; $temporary = -1
    foreach ($module in $script:Paths.Keys) {
        foreach ($row in $Plan.$module) {
            $payload = Get-DemoPayload $row $previewIds
            $existing = Find-DemoExisting $module $row $payload $baseline[$module]
            if ($null -ne $existing) {
                Assert-DemoRecord $module $row $payload $existing
                if ($module -eq 'leaves') { Assert-Demo ($existing.status -ceq $row.targetStatus) 'Conge existant avec statut different : arret avant insertion, aucune transition automatique sur un ancien enregistrement.' }
                $previewIds[$row.ref] = [long]$existing.id
            } else { $previewIds[$row.ref] = $temporary; $temporary-- }
        }
    }
    foreach ($module in $script:Paths.Keys) {
        foreach ($row in $Plan.$module) {
            $payload = Get-DemoPayload $row $ids
            $existing = Find-DemoExisting $module $row $payload @(Get-DemoRows $module)
            $isNew = $null -eq $existing
            if ($isNew) {
                $existing = Invoke-DemoApi POST $script:Paths[$module] $payload
                Assert-Demo ($null -ne $existing -and [long]$existing.id -gt 0) 'Creation sans identifiant confirme : arret.'
                $created[$module]++
            }
            $record = Invoke-DemoApi GET ($script:Paths[$module] + '/' + $existing.id)
            Assert-DemoRecord $module $row $payload $record
            $ids[$row.ref] = [long]$record.id
            if ($module -eq 'leaves') {
                if ($isNew) {
                    Assert-Demo ($record.status -ceq 'PENDING') 'Nouveau conge non PENDING.'
                    if ($row.targetStatus -cne 'PENDING') {
                        $action = if ($row.targetStatus -ceq 'APPROVED') { 'approve' } else { 'reject' }
                        $null = Invoke-DemoApi PATCH ($script:Paths[$module] + '/' + $record.id + '/' + $action)
                        $record = Invoke-DemoApi GET ($script:Paths[$module] + '/' + $record.id)
                        Assert-DemoRecord $module $row $payload $record
                        Assert-Demo ($record.status -ceq $row.targetStatus) 'Transition non confirmee par GET.'
                        $patched++
                    }
                } else { Assert-Demo ($record.status -ceq $row.targetStatus) 'Conge existant : statut incompatible, aucune modification.' }
            }
            Write-Host "$module | $($row.ref) | id=$($record.id) | GET confirme | nouveau=$isNew"
        }
    }
    # Re-read every page: scoped demo totals AND current global Dashboard totals.
    $demoTotal = [decimal]0; $currencies = @{}; $leaveStates = @{}; $global = @{}
    foreach ($module in $script:Paths.Keys) {
        $rows = @(Get-DemoRows $module); $global[$module] = $rows
        foreach ($row in $Plan.$module) {
            $payload = Get-DemoPayload $row $ids
            $found = @($rows | Where-Object { [long]$_.id -eq $ids[$row.ref] })
            Assert-Demo ($found.Count -eq 1) 'Enregistrement absent au controle final.'
            Assert-DemoRecord $module $row $payload $found[0]
            if ($module -eq 'invoices') { $demoTotal += [decimal]$found[0].totalAmount }
            if ($module -eq 'leaves') {
                Assert-Demo ($found[0].status -ceq $row.targetStatus) 'Statut final divergent.'
                $status = [string]$found[0].status
                if (-not $leaveStates.ContainsKey($status)) { $leaveStates[$status]=0 }; $leaveStates[$status]++
            }
        }
        Write-Host "$module | creations=$($created[$module]) | DEMO verifie=$(@($Plan.$module).Count) | total API=$($rows.Count)"
    }
    Assert-Demo ($demoTotal -eq 30583 -and $leaveStates['APPROVED'] -eq 2 -and $leaveStates['PENDING'] -eq 2 -and $leaveStates['REJECTED'] -eq 2) 'Totaux DEMO incoherents.'
    foreach ($invoice in $global.invoices) {
        $currency = [string]$invoice.currency
        if ([string]::IsNullOrWhiteSpace($currency)) { $currency = 'DEVISE_ABSENTE' }
        if (-not $currencies.ContainsKey($currency)) { $currencies[$currency]=[decimal]0 }
        $currencies[$currency] += [decimal]$invoice.totalAmount
    }
    Write-Host "DEMO factures : 30583 TND | transitions effectuees=$patched"
    foreach ($currency in $currencies.Keys) { Write-Host "Facturation globale API | $currency | $($currencies[$currency])" }
    $pending = @($global.leaves | Where-Object status -CEQ 'PENDING').Count
    Write-Host "Dashboard attendu : employes=$($global.employees.Count), clients=$($global.customers.Count), conges=$($global.leaves.Count), en attente=$pending, factures=$($global.invoices.Count), honoraires=$($global.fees.Count), paies=$($global.payrolls.Count)."
    Write-Host 'Honoraires : devise absente du contrat ; total monetaire Dashboard indisponible. Verification visuelle non effectuee.'
}

function Invoke-DemoMain {
    try {
        $plan = Read-DemoPlan $DataPath
        Write-Host 'Plan valide : 40 creations maximum, 4 transitions maximum ; factures 30583 TND.'
        if ($DryRun) { Write-Host 'DRY RUN : validation locale uniquement ; aucun reseau, aucune authentification, aucune ecriture.'; return }
        $confirmation = Read-Host 'Saisir exactement INSERER DEMO-TNC pour autoriser le login puis les creations'
        Assert-Demo ($confirmation -ceq 'INSERER DEMO-TNC') 'Execution annulee avant authentification.'
        Connect-Demo
        Start-DemoImport $plan
    } catch {
        # Only our controlled errors may be displayed. Raw .NET errors can contain requests.
        if ($_.Exception.Data.Contains('DemoSafe')) {
            Write-Host ('ARRET : ' + $_.Exception.Message)
        } else { Write-Host 'ARRET : erreur inattendue. Aucun detail sensible affiche ; verifier les contrats et les GET avant reprise.' }
        exit 1
    } finally { $script:Headers=@{} }
}

if ($MyInvocation.InvocationName -ne '.') { Invoke-DemoMain }
