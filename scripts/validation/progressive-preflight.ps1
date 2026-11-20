param(
 [Parameter(Mandatory=$true)][string]$CampaignDirectory,
 [Parameter(Mandatory=$true)][ValidateSet('A','B','C','D','E-bootstrap','E-payroll','E-fees')][string]$Stage,
 [Parameter(Mandatory=$true)][string]$TestEnvFile
)
# Future read-only operational checks. Never launches Docker Desktop or a container.
$ErrorActionPreference='Stop'
$campaign=(Resolve-Path -LiteralPath $CampaignDirectory).Path
node "$PSScriptRoot/prepare-progressive.mjs" --check $campaign
if ($LASTEXITCODE -ne 0) { throw 'Static campaign check failed' }
$plan=Get-Content -LiteralPath "$campaign/plan.json" -Raw | ConvertFrom-Json
$budget=$plan.stages.$Stage
$os=Get-CimInstance Win32_OperatingSystem
$freeMiB=[math]::Floor($os.FreePhysicalMemory/1024)
if ($freeMiB -lt $budget.hostFreeRequiredMiB) { throw "Insufficient host memory: $freeMiB MiB free, $($budget.hostFreeRequiredMiB) MiB required" }
$disk=[System.IO.DriveInfo]::new([System.IO.Path]::GetPathRoot($campaign))
if ($disk.AvailableFreeSpace -lt 20GB) { throw 'Less than 20 GiB disk margin available' }
$listeners=@(Get-NetTCPConnection -State Listen | Where-Object { $_.LocalPort -in 18080,18761,14200 })
if ($listeners.Count -ne 0) { throw 'A validation port is occupied; stop here, do not terminate user processes' }
$contextText=(& docker context inspect --format '{{json .Endpoints.docker.Host}}' 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $contextText -notmatch '^"(npipe:|unix:)') { throw 'Docker context must be local and accessible' }
$memoryText=(& docker info --format '{{.MemTotal}}' 2>&1 | Out-String).Trim()
[long]$dockerBytes=0
if ($LASTEXITCODE -ne 0 -or -not [long]::TryParse($memoryText,[ref]$dockerBytes)) { throw 'Docker engine inaccessible; Docker Desktop was not started' }
if ($dockerBytes/1MB -lt $budget.dockerTotalRequiredMiB) { throw 'Docker total allocation below stage budget plus 512 MiB margin' }
if (-not [System.IO.Path]::IsPathRooted($TestEnvFile) -or -not (Test-Path -LiteralPath $TestEnvFile -PathType Leaf)) { throw 'Absolute external test environment file required' }
$repo=(Resolve-Path "$PSScriptRoot/../..").Path
$envPath=(Resolve-Path -LiteralPath $TestEnvFile).Path
if ($envPath.StartsWith($repo.TrimEnd('\')+'\',[System.StringComparison]::OrdinalIgnoreCase)) { throw 'Test credentials must remain outside the workspace' }
$compose=Join-Path $campaign "$Stage.compose.json"
# Capture CLI output: never echo interpolation diagnostics or rendered secrets.
$composeOutput=(& docker compose -p $budget.project --env-file $envPath -f $compose config --quiet 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0) { $composeOutput=$null;throw 'Compose configuration failed; review approved credentials/certificate paths without printing secrets' }
$composeOutput=$null
[pscustomobject]@{Stage=$Stage;HostFreeMiB=$freeMiB;ContainerLimitsMiB=$budget.limitMiB;HostRequiredMiB=$budget.hostFreeRequiredMiB;DockerTotalMiB=[math]::Floor($dockerBytes/1MB);Status='RESOURCE/CONFIG CHECK PASSED; TLS validity, isolation labels and workload measurements still required'}
