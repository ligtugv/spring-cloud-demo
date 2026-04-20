$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$base = "c:\Users\HP\Desktop\demo"
$logDir = "$base\logs"
New-Item -Path $logDir -ItemType Directory -Force | Out-Null

function Start-Service {
    param([string]$Name, [string]$JarPath, [string]$PortArg, [int]$WaitSecs = 8)
    Write-Host "[$Name] Starting..."
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "java"
    $psi.Arguments = "-jar `"$JarPath`" $PortArg"
    $psi.WorkingDirectory = Split-Path $JarPath
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    Write-Host "[$Name] PID=$($proc.Id) Waiting $WaitSecs sec..."
    Start-Sleep -Seconds $WaitSecs
    if ($proc.HasExited) {
        Write-Host "[$Name] CRASHED! Exit code: $($proc.ExitCode)"
        $stderr = $proc.StandardError.ReadToEnd()
        $stdout = $proc.StandardOutput.ReadToEnd()
        Write-Host "STDOUT: $stdout"
        Write-Host "STDERR: $stderr"
    } else {
        Write-Host "[$Name] Running OK"
    }
    return $proc.Id
}

# Step 1: discovery
Start-Service -Name "discovery" -JarPath "$base\discovery\target\discovery-1.0.0.jar" -PortArg "--server.port=8761" -WaitSecs 6

# Step 2: config
Start-Service -Name "config" -JarPath "$base\config\target\config-1.0.0.jar" -PortArg "--server.port=8888" -WaitSecs 6

# Step 3: uaa
Start-Service -Name "uaa" -JarPath "$base\uaa\target\uaa-1.0.0.jar" -PortArg "--server.port=9000" -WaitSecs 20

# Step 4: gateway
Start-Service -Name "gateway" -JarPath "$base\gateway\target\gateway-1.0.0.jar" -PortArg "--server.port=7573" -WaitSecs 6

# Step 5: web
Start-Service -Name "web" -JarPath "$base\web\target\web-1.0.0.jar" -PortArg "--server.port=8080" -WaitSecs 8

# Step 6: product
Start-Service -Name "product" -JarPath "$base\product\target\product-1.0.0.jar" -PortArg "--server.port=8081" -WaitSecs 6

Write-Host ""
Write-Host "=== Final port check ==="
$ports = @(8761, 8888, 9000, 7573, 8080, 8081)
foreach ($port in $ports) {
    $tn = Test-NetConnection -ComputerName localhost -Port $port -WarningAction SilentlyContinue
    if ($tn.TcpTestSucceeded) {
        Write-Host "[$port] LISTENING"
    } else {
        Write-Host "[$port] NOT listening"
    }
}
