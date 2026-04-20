$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$base = "c:\Users\HP\Desktop\demo"

function Start-JarService {
    param([string]$Name, [string]$JarPath, [string]$PortArg, [int]$WaitSeconds = 5)
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "java"
    $psi.Arguments = "-jar `"$JarPath`" $PortArg"
    $psi.WorkingDirectory = Split-Path $JarPath
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    Write-Host "[$Name] Started PID=$($proc.Id)"
    Start-Sleep -Seconds $WaitSeconds
    return $proc.Id
}

Write-Host "=== Starting all services ==="
Start-JarService -Name "discovery" -JarPath "$base\discovery\target\discovery-1.0.0.jar" -PortArg "--server.port=8761" -WaitSeconds 6
Start-JarService -Name "config" -JarPath "$base\config\target\config-1.0.0.jar" -PortArg "--server.port=8888" -WaitSeconds 6
Start-JarService -Name "uaa" -JarPath "$base\uaa\target\uaa-1.0.0.jar" -PortArg "--server.port=9000" -WaitSeconds 15
Start-JarService -Name "gateway" -JarPath "$base\gateway\target\gateway-1.0.0.jar" -PortArg "--server.port=7573" -WaitSeconds 6
Start-JarService -Name "web" -JarPath "$base\web\target\web-1.0.0.jar" -PortArg "--server.port=8080" -WaitSeconds 6
Start-JarService -Name "product" -JarPath "$base\product\target\product-1.0.0.jar" -PortArg "--server.port=8081" -WaitSeconds 6

Write-Host "=== Checking ports ==="
Start-Sleep -Seconds 3
$ports = @(8761, 8888, 9000, 7573, 8080, 8081)
foreach ($port in $ports) {
    $listening = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($listening) {
        Write-Host "[OK] Port $port is listening"
    } else {
        Write-Host "[FAIL] Port $port is NOT listening"
    }
}

Write-Host "=== Done ==="
