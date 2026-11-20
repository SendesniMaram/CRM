param([Parameter(Mandatory=$true)][string]$Destination,[Parameter(Mandatory=$true)][string]$RepoRoot)
$ErrorActionPreference='Stop'
$repoSecret=(Resolve-Path -LiteralPath $RepoRoot).Path
$targetSecret=[System.IO.Path]::GetFullPath($Destination)
if ($targetSecret.StartsWith($repoSecret.TrimEnd('\')+'\',[System.StringComparison]::OrdinalIgnoreCase)) {throw 'External directory required'}
if (Test-Path -LiteralPath $targetSecret) {throw 'Existing material will never be overwritten'}
$opensslSecret='C:\Program Files\Git\usr\bin\openssl.exe'
if (-not (Test-Path -LiteralPath $opensslSecret)) {throw 'Review installed OpenSSL path first'}
$null=New-Item -ItemType Directory -Path $targetSecret
$aclSecret=New-Object System.Security.AccessControl.DirectorySecurity
$aclSecret.SetAccessRuleProtection($true,$false)
$userSecret=[System.Security.Principal.WindowsIdentity]::GetCurrent().User
$inheritSecret=[System.Security.AccessControl.InheritanceFlags]'ContainerInherit,ObjectInherit'
$propSecret=[System.Security.AccessControl.PropagationFlags]::None
$aclSecret.AddAccessRule((New-Object System.Security.AccessControl.FileSystemAccessRule($userSecret,'FullControl',$inheritSecret,$propSecret,'Allow')))
$aclSecret.AddAccessRule((New-Object System.Security.AccessControl.FileSystemAccessRule('SYSTEM','FullControl',$inheritSecret,$propSecret,'Allow')))
Set-Acl -LiteralPath $targetSecret -AclObject $aclSecret
function New-SecretText([int]$Count) {
 $randomSecret=New-Object byte[] $Count
 $rngSecret=[System.Security.Cryptography.RandomNumberGenerator]::Create()
 try {$rngSecret.GetBytes($randomSecret)} finally {$rngSecret.Dispose()}
 return ([BitConverter]::ToString($randomSecret)).Replace('-','').ToLowerInvariant()
}
$utfSecret=New-Object System.Text.UTF8Encoding($false)
$adminSecret=New-SecretText 24
$mysqlSecret=New-SecretText 32
$jwtSecret=New-SecretText 64
$tlsSecret=New-SecretText 24
$envPathSecret=Join-Path $targetSecret 'test.env'
$materialSecret=$targetSecret.Replace('\','/')
[IO.File]::WriteAllText($envPathSecret,"CRM_2B_MYSQL_PASSWORD=$mysqlSecret`nCRM_2B_JWT_SECRET=$jwtSecret`nCRM_2B_ADMIN_PASSWORD=$adminSecret`nCRM_PROFILE_MATERIAL_DIR=$materialSecret`n",$utfSecret)
[IO.File]::WriteAllText((Join-Path $targetSecret 'employee-tls-password'),$tlsSecret,$utfSecret)
function Invoke-OpenSslSecret([string[]]$Arguments) {
 $ErrorActionPreference='Continue'
 $resultSecret=(& $opensslSecret @Arguments 2>&1 | Out-String)
 $codeSecret=$LASTEXITCODE;$resultSecret=$null
 if ($codeSecret -ne 0) {throw 'Dedicated certificate operation failed; material retained privately for review'}
}
$employeeKeySecret=Join-Path $targetSecret 'employee-tls-key.pem'
$certSecret=Join-Path $targetSecret 'employee-tls.pem'
Invoke-OpenSslSecret @('req','-x509','-newkey','rsa:3072','-sha256','-days','14','-nodes','-keyout',$employeeKeySecret,'-out',$certSecret,'-subj','/CN=employee-service','-addext','subjectAltName=DNS:employee-service,DNS:localhost','-addext','extendedKeyUsage=serverAuth')
Invoke-OpenSslSecret @('pkcs12','-export','-name','employee','-inkey',$employeeKeySecret,'-in',$certSecret,'-out',(Join-Path $targetSecret 'employee-tls.p12'),'-passout',('file:'+(Join-Path $targetSecret 'employee-tls-password')))
Invoke-OpenSslSecret @('genpkey','-algorithm','RSA','-pkeyopt','rsa_keygen_bits:3072','-out',(Join-Path $targetSecret 'hr-signing-private.pem'))
Invoke-OpenSslSecret @('pkey','-in',(Join-Path $targetSecret 'hr-signing-private.pem'),'-pubout','-out',(Join-Path $targetSecret 'hr-signing-public.pem'))
Invoke-OpenSslSecret @('verify','-CAfile',$certSecret,'-verify_hostname','employee-service',$certSecret)
Invoke-OpenSslSecret @('pkcs12','-in',(Join-Path $targetSecret 'employee-tls.p12'),'-passin',('file:'+(Join-Path $targetSecret 'employee-tls-password')),'-info','-noout')
$adminSecret=$null;$mysqlSecret=$null;$jwtSecret=$null;$tlsSecret=$null
Write-Output 'Dedicated credentials and internal certificates created outside repository; values not printed. Validity 14 days; revalidate before use. No services started.'
