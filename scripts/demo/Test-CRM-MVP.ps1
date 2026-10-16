# Windows PowerShell 5.1. Real authentication is started only by a manual invocation.
[CmdletBinding()]
param([switch]$LocalCheck)

$checkOffline = $LocalCheck
. (Join-Path $PSScriptRoot 'Import-TechNova.ps1')
$script:SharedApi = ${function:Invoke-DemoApi}
$script:LoginAllowed = $false
$script:Results = @()

# Defense in depth: the checker cannot issue a business write through this wrapper.
function Invoke-DemoApi([string]$Method, [string]$Path, $Body = $null) {
    $login = $script:LoginAllowed -and $Method -ceq 'POST' -and $Path -ceq '/identity/api/auth/login'
    Assert-Demo ($Method -ceq 'GET' -or $login) 'Test lecture seule : methode interdite.'
    if ($login) { $script:LoginAllowed = $false } # Never retry authentication automatically.
    & $script:SharedApi $Method $Path $Body
}

function Confirm-CRM([string]$Id, [string]$Description, [bool]$Success) {
    Assert-Demo $Success "FAIL $Id : $Description"
    $script:Results += [pscustomobject]@{Id=$Id;Status='PASS';Description=$Description}
    Write-Host "$script:TestMode | PASS | $Id | $Description"
}

function Test-CRMPage([string]$Module, [int]$Count, [string]$Keyword = '') {
    $expected = @(Get-DemoRows $Module)
    if ($Keyword) {
        # Caller supplies the expected filtered subset separately through the count.
        $suffix = '&keyword=' + [uri]::EscapeDataString($Keyword)
    } else { $suffix = '' }
    $collected = @(); $page=0
    do {
        $response=Invoke-DemoApi GET ($script:Paths[$Module]+"?page=$page&size=2&sortBy=id&direction=asc"+$suffix)
        Assert-Demo ($response.totalElements -eq $Count -and $response.number -eq $page -and $response.size -eq 2 -and $response.totalPages -eq [math]::Ceiling($Count/2.0)) "Pagination ou recherche incorrecte : $Module."
        $items=@($response.content)
        Assert-Demo ($items.Count -eq [math]::Min(2,[math]::Max(0,$Count-$page*2))) 'Contenu de page incomplet.'
        $collected += $items; $page++
    } while ($collected.Count -lt $Count)
    $ids=@($collected|ForEach-Object {[long]$_.id})
    Assert-Demo (@($ids|Select-Object -Unique).Count -eq $Count) 'Doublon entre pages.'
    for($i=1;$i -lt $ids.Count;$i++){Assert-Demo ($ids[$i] -gt $ids[$i-1]) 'Tri id asc incoherent.'}
    if (-not $Keyword) {
        Assert-Demo ((@($expected|ForEach-Object {[string]$_.id}) -join ',') -ceq (@($ids|ForEach-Object {[string]$_}) -join ',')) 'Pagination differente de la lecture complete.'
    } else {
        $allIds=@($expected|ForEach-Object {[long]$_.id})
        foreach($item in $collected){Assert-Demo ([long]$item.id -in $allIds) 'Recherche retournant un ID inconnu.'}
        foreach($item in $collected){
            $fields=if($Module -eq 'employees'){@($item.employeeCode,$item.firstName,$item.lastName,$item.email)}elseif($Module -eq 'departments'){@($item.code,$item.name,$item.description)}else{@($item.name,$item.email,$item.company,$item.phone)}
            Assert-Demo (@($fields|Where-Object {([string]$_).IndexOf($Keyword,[StringComparison]::OrdinalIgnoreCase) -ge 0}).Count -gt 0) 'Recherche retournant un contenu hors filtre.'
        }
    }
}

function Test-CRMData($Plan, [string]$Mode = 'API REELLE') {
    $script:TestMode=$Mode; $script:Results=@()
    $rows=@{}; $ids=@{}; $demo=@{}
    $expectedCounts=@{departments=3;employees=8;customers=5;leaves=7;invoices=8;fees=6;payrolls=6}
    $demoCounts=@{departments=3;employees=6;customers=5;leaves=6;invoices=8;fees=6;payrolls=6}
    foreach($module in $script:Paths.Keys){
        $rows[$module]=@(Get-DemoRows $module)
        Confirm-CRM "COUNT-$module" "Nombre global attendu : $($expectedCounts[$module])" ($rows[$module].Count -eq $expectedCounts[$module])
        Confirm-CRM "PLAN-$module" 'Nombre de references DEMO attendu' (@($Plan.$module).Count -eq $demoCounts[$module])
        $demo[$module]=@()
        foreach($entry in $Plan.$module){
            $payload=Get-DemoPayload $entry $ids
            $record=Find-DemoExisting $module $entry $payload $rows[$module]
            Assert-Demo ($null -ne $record) "Reference DEMO absente : $module/$($entry.ref)."
            Assert-DemoRecord $module $entry $payload $record
            $detail=Invoke-DemoApi GET ($script:Paths[$module]+'/'+$record.id)
            Assert-DemoRecord $module $entry $payload $detail
            Assert-Demo ([long]$detail.id -eq [long]$record.id) 'ID detail different de la liste.'
            if($module -eq 'leaves'){
                Assert-Demo ($detail.status -ceq $entry.targetStatus) 'Statut DEMO conge incorrect.'
                Assert-Demo ([datetime]$detail.endDate -ge [datetime]$detail.startDate) 'Dates conge inversees.'
            }
            if($module -eq 'fees') { Assert-Demo (-not ($detail.PSObject.Properties.Name -contains 'currency')) 'Contrat honoraires modifie : devise apparue.' }
            $ids[$entry.ref]=[long]$record.id; $demo[$module]+=$detail
        }
        $distinct=@($demo[$module]|ForEach-Object {$_.id}|Select-Object -Unique).Count
        Confirm-CRM "DETAIL-$module" 'References DEMO uniques, champs, relations et calculs confirmes par GET' ($distinct -eq $demoCounts[$module])
        Test-CRMPage $module $expectedCounts[$module]
        Confirm-CRM "PAGE-$module" 'Pagination size=2 et tri id asc verifies sur toutes les pages' $true
    }
    Test-CRMPage departments 1 'DEMO-TNC-DEV'
    Confirm-CRM SEARCH-departments 'Recherche du code departement DEV' $true
    Test-CRMPage employees 6 'DEMO-TNC-'
    Confirm-CRM SEARCH-employees 'Recherche des six codes employes DEMO' $true
    Test-CRMPage customers 5 'demo.tnc.'
    Confirm-CRM SEARCH-customers 'Recherche des cinq emails clients DEMO' $true
    # All actual leave requests, not just a single page, are checked for overlap.
    $leaves=$rows.leaves
    for($i=0;$i -lt $leaves.Count;$i++){
        for($j=$i+1;$j -lt $leaves.Count;$j++){
            $a=$leaves[$i];$b=$leaves[$j]
            Assert-Demo (-not ([string]$a.employeeId -ceq [string]$b.employeeId -and [datetime]$a.startDate -le [datetime]$b.endDate -and [datetime]$a.endDate -ge [datetime]$b.startDate)) 'Chevauchement de demandes pour le meme employe.'
        }
    }
    Confirm-CRM LEAVE-overlap 'Aucun chevauchement de demandes par employe' $true
    $pending=@($leaves|Where-Object status -CEQ 'PENDING').Count
    Confirm-CRM LEAVE-pending 'Deux demandes PENDING globales' ($pending -eq 2)
    Confirm-CRM LEAVE-statuses 'Statuts globaux conformes au contrat' (@($leaves|Where-Object {$_.status -cnotin @('PENDING','APPROVED','REJECTED')}).Count -eq 0)
    foreach($group in @($leaves|Group-Object status)){Write-Host "$Mode | Repartition globale conges | $($group.Name)=$($group.Count)"}
    foreach($status in @('PENDING','APPROVED','REJECTED')){
        Confirm-CRM "LEAVE-$status" "Deux conges DEMO $status" (@($demo.leaves|Where-Object status -CEQ $status).Count -eq 2)
    }
    foreach($status in @('PAID','SENT','DRAFT')){
        $count=if($status -ceq 'SENT'){4}else{2}
        Confirm-CRM "INVOICE-$status" "Repartition DEMO $status : $count" (@($demo.invoices|Where-Object status -CEQ $status).Count -eq $count)
    }
    $amount=[decimal]0
    foreach($invoice in $demo.invoices){Assert-Demo ($invoice.currency -ceq 'TND') 'Devise facture incorrecte.';$amount += [decimal]$invoice.totalAmount}
    Confirm-CRM INVOICE-total 'Huit factures DEMO : 30583 TND' ($amount -eq 30583)
    Confirm-CRM PAYROLL-contract 'Six paies TND, periode 2026-09, nets verifies sans publication nominative' (@($demo.payrolls|Where-Object { $_.currency -cne 'TND' -or $_.payPeriod -cne '2026-09' }).Count -eq 0)
    # Reissue exactly the count queries used by the Angular Dashboard.
    foreach($module in @('employees','customers','fees','payrolls')){
        $page=Invoke-DemoApi GET ($script:Paths[$module]+'?page=0&size=1&sortBy=id&direction=asc')
        Confirm-CRM "DASH-$module" 'KPI totalElements compatible Dashboard' ($page.totalElements -eq $expectedCounts[$module])
    }
    $rawLeaves=@(Invoke-DemoApi GET $script:Paths.leaves)
    Confirm-CRM DASH-leaves 'Liste non paginee Dashboard : 7 conges, 2 PENDING' ($rawLeaves.Count -eq 7 -and @($rawLeaves|Where-Object status -CEQ PENDING).Count -eq 2)
    $invoicePage=Invoke-DemoApi GET ($script:Paths.invoices+'?page=0&size=100&sortBy=id&direction=asc')
    $total=[decimal]0
    foreach($invoice in $invoicePage.content){Assert-Demo ($invoice.currency -ceq 'TND') 'Devise globale inattendue.';$total += [decimal]$invoice.totalAmount}
    Confirm-CRM DASH-invoices 'Page complete Dashboard : huit factures, 30583 TND' ($invoicePage.totalElements -eq 8 -and @($invoicePage.content).Count -eq 8 -and $total -eq 30583)
    Write-Host "$Mode | Terminee : $($script:Results.Count) controles PASS. Aucun rendu visuel Angular valide."
}

function Invoke-CRMMain {
    try {
        Assert-Demo (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'technova.json')) 'Configuration absente.'
        $plan=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'technova.json') -Raw -Encoding UTF8|ConvertFrom-Json
        if($checkOffline){Write-Host 'LOCAL CHECK : dependances et JSON disponibles ; aucun reseau, aucun login. Utiliser Test-CRM-MVP.Local.ps1 pour les simulations.';return}
        $answer=Read-Host 'Saisir TESTER CRM pour autoriser une connexion SUPER_ADMIN puis uniquement des GET'
        Assert-Demo ($answer -ceq 'TESTER CRM') 'Tests annules avant login.'
        $script:LoginAllowed=$true
        try { Connect-Demo } finally { $script:LoginAllowed=$false }
        Test-CRMData $plan
    } catch {
        if($_.Exception.Data.Contains('DemoSafe')){Write-Host ('ARRET : '+$_.Exception.Message)}
        else{Write-Host 'ARRET : erreur inattendue. Details masques pour proteger les secrets.'}
        exit 1
    } finally { $script:Headers=@{}; $script:LoginAllowed=$false }
}
if($MyInvocation.InvocationName -ne '.') { Invoke-CRMMain }
