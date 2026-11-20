param(
 [Parameter(Mandatory=$true)][string]$CampaignDirectory,
 [Parameter(Mandatory=$true)][string]$TestEnvFile,
 [Parameter(Mandatory=$true)][string]$RepoRoot,
 [ValidateSet('core','all')][string]$Stage='core',
 [switch]$AuthorizeStart,
 [switch]$RestartValidatedCampaign
)
$ErrorActionPreference='Stop'
$campaignExpress=(Resolve-Path -LiteralPath $CampaignDirectory).Path
$repoExpress=(Resolve-Path -LiteralPath $RepoRoot).Path
if ((Split-Path $campaignExpress -Parent) -ne (Join-Path $repoExpress 'target') -or (Split-Path $campaignExpress -Leaf) -notmatch '^crm-express-\d{8}-\d{6}-(a|demo|full)$') {throw 'Unexpected campaign location'}
if ((git -C $repoExpress branch --show-current) -ne 'publication-clean') {throw 'Unexpected branch'}
$manifestExpress=Get-Content -LiteralPath (Join-Path $campaignExpress 'manifest.json') -Raw | ConvertFrom-Json
$composeExpress=Join-Path $campaignExpress 'compose.json'
$envExpress=(Resolve-Path -LiteralPath $TestEnvFile).Path
if ($envExpress.StartsWith($repoExpress.TrimEnd('\')+'\',[System.StringComparison]::OrdinalIgnoreCase)) {throw 'Secrets must be external'}
$freeExpress=[math]::Floor((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1024)
if ($freeExpress -lt $manifestExpress.hostFreeRequiredMiB) {throw "Host RAM insufficient: $freeExpress MiB; required $($manifestExpress.hostFreeRequiredMiB)"}
if ([System.IO.DriveInfo]::new([System.IO.Path]::GetPathRoot($campaignExpress)).AvailableFreeSpace -lt 20GB) {throw 'Insufficient disk margin'}
$contextExpress=(& docker context inspect --format '{{json .Endpoints.docker.Host}}' 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $contextExpress -notmatch '^"(npipe:|unix:)') {throw 'Local Docker context required'}
$dockerMemoryExpress=(& docker info --format '{{.MemTotal}}' 2>$null | Out-String).Trim()
[long]$bytesExpress=0
if ($LASTEXITCODE -ne 0 -or -not [long]::TryParse($dockerMemoryExpress,[ref]$bytesExpress) -or $bytesExpress/1MB -lt $manifestExpress.dockerRequiredMiB) {throw 'Docker engine unavailable or memory allocation insufficient'}
$renderExpress=(& docker compose -p $manifestExpress.project --env-file $envExpress -f $composeExpress config --quiet 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0) {$renderExpress=$null;throw 'Resolved Compose config invalid; no secrets printed'}
$renderExpress=$null
node "$repoExpress/scripts/deployment/express/verify.mjs" $campaignExpress
if ($LASTEXITCODE -ne 0) {throw 'Campaign integrity check failed'}
if ($Stage -eq 'all') {
 $acceptancePathExpress=Join-Path $campaignExpress 'evidence/core-api-results.json'
 if (-not (Test-Path -LiteralPath $acceptancePathExpress)) {throw 'Validate core API/RBAC on this campaign before any business service'}
 $acceptanceExpress=Get-Content -LiteralPath $acceptancePathExpress -Raw | ConvertFrom-Json
 $manifestHashExpress=(Get-FileHash -LiteralPath (Join-Path $campaignExpress 'manifest.json') -Algorithm SHA256).Hash.ToLowerInvariant()
 if ($acceptanceExpress.status -ne 'PASSED' -or $acceptanceExpress.project -ne $manifestExpress.project -or $acceptanceExpress.commit -ne $manifestExpress.commit -or $acceptanceExpress.manifestSha256 -ne $manifestHashExpress) {throw 'Core acceptance missing, failed or stale; no business service started'}
}
if (-not $AuthorizeStart) {Write-Output 'STATIC/RESOURCE PREFLIGHT PASSED; no service started. Explicit operational authorization required.';return}
$existingVolumesExpress=@(& docker volume ls --filter "label=com.docker.compose.project=$($manifestExpress.project)" --format '{{.Name}}')
$existingContainersExpress=@(& docker ps -a --filter "label=com.docker.compose.project=$($manifestExpress.project)" --format '{{.Names}}')
foreach($volumeExpress in @('mysql-data','hr-documents')) {
 $volumeNameExpress="$($manifestExpress.project)_$volumeExpress"
 $labelExpress=(& docker volume inspect --format '{{index .Labels "com.docker.compose.project"}}' $volumeNameExpress 2>$null | Out-String).Trim()
 if ($LASTEXITCODE -eq 0 -and (-not $RestartValidatedCampaign -or $labelExpress -ne $manifestExpress.project)) {throw 'Existing or foreign volume name collision; nothing removed'}
}
$networkNameExpress="$($manifestExpress.project)_isolated"
$networkLabelExpress=(& docker network inspect --format '{{index .Labels "com.docker.compose.project"}}' $networkNameExpress 2>$null | Out-String).Trim()
if ($LASTEXITCODE -eq 0 -and (-not $RestartValidatedCampaign -or $networkLabelExpress -ne $manifestExpress.project)) {throw 'Existing or foreign network collision'}
if (($existingVolumesExpress.Count -gt 0 -or $existingContainersExpress.Count -gt 0) -and -not $RestartValidatedCampaign) {throw 'Campaign already exists; choose a new ID or explicitly review restart'}
if (Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object LocalPort -in 18080,18761,14200) {throw 'Candidate port occupied; no process will be terminated'}
foreach($jarExpress in $manifestExpress.jars) {
 if (-not $jarExpress.sha256 -or (Get-FileHash -LiteralPath $jarExpress.path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $jarExpress.sha256) {throw 'JAR fingerprint mismatch; rebuild and regenerate campaign'}
 $mainExpress=Join-Path $repoExpress "microservices/$($jarExpress.service)/src/main"
 $jarDateExpress=(Get-Item -LiteralPath $jarExpress.path).LastWriteTimeUtc
 if (Get-ChildItem -LiteralPath $mainExpress -Recurse -File | Where-Object LastWriteTimeUtc -gt $jarDateExpress) {throw 'JAR older than its main sources; package and regenerate candidate first'}
 $sqlFolderExpress=Join-Path $mainExpress 'resources/db/migration'
 if (Test-Path -LiteralPath $sqlFolderExpress) {
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $zipExpress=[System.IO.Compression.ZipFile]::OpenRead($jarExpress.path)
  try {
   $sqlExpress=@(Get-ChildItem -LiteralPath $sqlFolderExpress -Filter '*.sql')
   if (@($zipExpress.Entries | Where-Object FullName -like 'BOOT-INF/classes/db/migration/*.sql').Count -ne $sqlExpress.Count) {throw 'Embedded migration inventory differs from source'}
   foreach($sqlFileExpress in $sqlExpress) {
    $entryExpress=$zipExpress.GetEntry('BOOT-INF/classes/db/migration/'+$sqlFileExpress.Name)
    if (-not $entryExpress) {throw 'Migration missing from JAR'}
    $streamExpress=$entryExpress.Open();$shaExpress=[System.Security.Cryptography.SHA256]::Create()
    try {$hashExpress=([BitConverter]::ToString($shaExpress.ComputeHash($streamExpress))).Replace('-','')} finally {$streamExpress.Dispose();$shaExpress.Dispose()}
    if ($hashExpress -ne (Get-FileHash -LiteralPath $sqlFileExpress.FullName -Algorithm SHA256).Hash) {throw 'Embedded SQL differs from source; do not start'}
   }
  } finally {$zipExpress.Dispose()}
 }
}
$modelExpress=Get-Content -LiteralPath $composeExpress -Raw | ConvertFrom-Json
foreach($buildServiceExpress in $manifestExpress.jars.service) {
 & docker compose -p $manifestExpress.project --env-file $envExpress -f $composeExpress build $buildServiceExpress
 if ($LASTEXITCODE -ne 0) {throw 'Sequential image build failed; no application started'}
}
$orderedExpress=@('mysql','discovery-service','identity-service','gateway-service','department-service','employee-service','hr-service','customer-service','invoice-service','payroll-service','fees-service','frontend')
$evidenceExpress=@()
foreach($serviceExpress in $orderedExpress) {
 if ($Stage -eq 'core' -and $serviceExpress -notin @('mysql','discovery-service','identity-service','gateway-service','frontend')) {continue}
 if (-not $modelExpress.services.PSObject.Properties[$serviceExpress]) {continue}
 & docker compose -p $manifestExpress.project --env-file $envExpress -f $composeExpress up -d --no-deps $serviceExpress
 if ($LASTEXITCODE -ne 0) {throw "Start failed: $serviceExpress; do not continue"}
 $deadlineExpress=(Get-Date).AddSeconds(240);$healthyExpress=$false
 do {
  $containerExpress=(& docker compose -p $manifestExpress.project --env-file $envExpress -f $composeExpress ps -q $serviceExpress | Out-String).Trim()
  $stateExpress=(& docker inspect --format '{{.State.Health.Status}}' $containerExpress 2>$null | Out-String).Trim()
  if ($stateExpress -eq 'healthy') {$healthyExpress=$true;break}
  Start-Sleep -Seconds 2
 } while((Get-Date) -lt $deadlineExpress)
 if (-not $healthyExpress) {throw "Health deadline failed: $serviceExpress; retain volumes/evidence, stop this campaign explicitly"}
 $imageExpress=(& docker inspect --format '{{.Image}}' $containerExpress | Out-String).Trim()
 $oomExpress=(& docker inspect --format '{{.State.OOMKilled}}' $containerExpress | Out-String).Trim()
 $restartsExpress=(& docker inspect --format '{{.RestartCount}}' $containerExpress | Out-String).Trim()
 $evidenceExpress += [pscustomobject]@{Service=$serviceExpress;ImageID=$imageExpress;OOMKilled=$oomExpress;RestartCount=$restartsExpress;Health='healthy'}
 $evidenceExpress | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $campaignExpress 'evidence/containers.json') -Encoding UTF8
 if ($oomExpress -eq 'true' -or $restartsExpress -ne '0') {throw 'OOM/restart observed; halt campaign progression'}
 if ([math]::Floor((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1024) -lt 512) {throw 'Host memory pressure; halt campaign progression'}
}
Write-Output 'Containers healthy. API/RBAC/first-password/browser acceptance NOT YET VALIDATED.'
