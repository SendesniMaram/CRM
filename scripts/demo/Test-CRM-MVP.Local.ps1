# Offline functional checker tests: all responses are generated in memory, no HTTP.
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Test-CRM-MVP.ps1')
$plan=Get-Content (Join-Path $PSScriptRoot 'technova.json') -Raw -Encoding UTF8|ConvertFrom-Json
$script:Fixture=@{}; $ids=@{}; $script:Methods=@(); $script:BrokenPagination=$false
foreach($module in $script:Paths.Keys){$script:Fixture[$module]=@()}
$script:Fixture.employees=@(1..2|ForEach-Object {[pscustomobject]@{id=$_;employeeCode="EXISTING-$_";email="existing$_@example.invalid";firstName='Existing';lastName='Fixture'}})
$script:Fixture.leaves=@([pscustomobject]@{id=1;employeeId='1';startDate='2027-02-15';endDate='2027-02-17';reason='Existing fixture';status='REJECTED'})
foreach($module in $script:Paths.Keys){
    foreach($entry in $plan.$module){
        $payload=Get-DemoPayload $entry $ids
        $copy=[ordered]@{id=($script:Fixture[$module].Count+1)}
        foreach($key in $payload.Keys){$copy[$key]=$payload[$key]}
        if($module -eq 'leaves'){$copy.status=$entry.targetStatus}
        if($module -eq 'invoices'){$copy.totalAmount=[decimal]$payload.subtotal+[decimal]$payload.tax}
        if($module -eq 'fees'){$copy.amount=[decimal]$payload.hourlyRate*[decimal]$payload.workedHours}
        if($module -eq 'payrolls'){$copy.netSalary=[decimal]$payload.baseSalary+[decimal]$payload.bonuses-[decimal]$payload.deductions-[decimal]$payload.taxAmount}
        $record=[pscustomobject]$copy; $script:Fixture[$module]+=$record; $ids[$entry.ref]=$record.id
    }
}
function Invoke-CRMFixture([string]$Method,[string]$Path,$Body=$null){
    $script:Methods+= $Method
    Assert-Demo ($Method -ceq 'GET') 'Mock refuses any real write or authentication.'
    $module=@($script:Paths.Keys|Where-Object {$Path.StartsWith($script:Paths[$_])})[0]
    $rows=@($script:Fixture[$module])
    if($Path -ceq $script:Paths[$module]){return $rows}
    if(-not $Path.Contains('?')){return @($rows|Where-Object id -EQ ([long]$Path.Split('/')[-1]))[0]}
    $page=[int]([regex]::Match($Path,'page=(\d+)').Groups[1].Value)
    $size=[int]([regex]::Match($Path,'size=(\d+)').Groups[1].Value)
    $keyword=[uri]::UnescapeDataString([regex]::Match($Path,'keyword=([^&]+)').Groups[1].Value)
    if($keyword){
        $rows=@($rows|Where-Object {
            $r=$_
            $values=if($module -eq 'employees'){@($r.employeeCode,$r.firstName,$r.lastName,$r.email)}elseif($module -eq 'departments'){@($r.code,$r.name,$r.description)}else{@($r.name,$r.email,$r.company,$r.phone)}
            @($values|Where-Object {([string]$_).IndexOf($keyword,[StringComparison]::OrdinalIgnoreCase) -ge 0}).Count -gt 0
        })
    }
    $content=@($rows|Select-Object -Skip ($page*$size) -First $size)
    if($script:BrokenPagination -and $module -ceq 'employees' -and $size -eq 2 -and $page -eq 1){$content=@($rows|Select-Object -First $size)}
    return [pscustomobject]@{content=$content;totalElements=$rows.Count;totalPages=[int][math]::Ceiling($rows.Count/[double]$size);size=$size;number=$page}
}
$script:SharedApi=${function:Invoke-CRMFixture}
Test-CRMData $plan 'SIMULATION LOCALE'
Assert-Demo (@($script:Methods|Where-Object {$_ -cne 'GET'}).Count -eq 0) 'Business write attempted.'
$checks=$script:Results.Count
function Assert-CRMFailure([scriptblock]$Action,[string]$Expected){
    $failed=$false
    try{& $Action}catch{$failed=$_.Exception.Message.Contains($Expected)}
    Assert-Demo $failed 'Expected failure was not detected.'
}
$script:Fixture.payrolls[0].netSalary++
Assert-CRMFailure {Test-CRMData $plan 'SIMULATION NEGATIVE'} 'Calcul net incorrect'
$script:Fixture.payrolls[0].netSalary--
$script:Fixture.fees[0]|Add-Member NoteProperty currency 'TND'
Assert-CRMFailure {Test-CRMData $plan 'SIMULATION NEGATIVE'} 'devise apparue'
$script:Fixture.fees[0].PSObject.Properties.Remove('currency')
$script:Fixture.invoices[0].customerId='99999'
Assert-CRMFailure {Test-CRMData $plan 'SIMULATION NEGATIVE'} 'Conflit de contenu'
$script:Fixture.invoices[0].customerId=[string]$ids.C01
$script:Fixture.leaves[0].status='PENDING'
Assert-CRMFailure {Test-CRMData $plan 'SIMULATION NEGATIVE'} 'LEAVE-pending'
$script:Fixture.leaves[0].status='REJECTED'
$script:BrokenPagination=$true
Assert-CRMFailure {Test-CRMPage employees 8} 'Doublon entre pages'
$script:BrokenPagination=$false
foreach($method in @('POST','PUT','PATCH','DELETE')){
    $before=$script:Methods.Count
    Assert-CRMFailure {Invoke-DemoApi $method '/employee/api/employees'} 'methode interdite'
    Assert-Demo ($script:Methods.Count -eq $before) 'Blocked write reached transport.'
}
# One and only one authentication POST is allowed when explicitly enabled.
$script:SharedApi={param($Method,$Path,$Body) return [pscustomobject]@{accepted=$true}}
$script:LoginAllowed=$true
$null=Invoke-DemoApi POST '/identity/api/auth/login' @{}
Assert-CRMFailure {Invoke-DemoApi POST '/identity/api/auth/login' @{}} 'methode interdite'
$script:Headers=@{}
Write-Host "LOCAL CHECKER TESTS PASS : $checks positive checks; wrong net, currency, relation, pending count, pagination and write guard tested. No real login, HTTP or database write."
