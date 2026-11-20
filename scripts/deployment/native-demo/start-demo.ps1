[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
# Preparation only. No process, database, or service is started by this script.
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
if ((git -C $repo branch --show-current) -ne 'publication-clean') {
    throw 'Unexpected branch. No startup performed.'
}
$os = Get-CimInstance Win32_OperatingSystem -ErrorAction Stop
$available = [math]::Floor($os.FreePhysicalMemory / 1024)
Write-Host "Available RAM: $available MiB. This is a measurement, not an approval."
Get-Command mysql.exe,mysqld.exe -ErrorAction SilentlyContinue |
    Select-Object Name,Source | Format-Table
Get-CimInstance Win32_Service -ErrorAction Stop |
    Where-Object { $_.Name -match 'mysql|maria' } |
    Select-Object Name,State | Format-Table
throw 'BLOCKED: isolated MySQL, current HR artifact, native memory envelope and TLS binding must be qualified. No startup performed. See README.md.'
