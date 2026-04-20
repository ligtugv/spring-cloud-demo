$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Start-ServiceJar {
    param([string]$JarPath, [string]$ServiceName, [string]$PortArg)
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "java"
    $psi.Arguments = "-jar `"$JarPath`" $PortArg"
    $psi.WorkingDirectory = Split-Path $JarPath
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    $proc.WaitForExit(3000)
    Write-Host "[$ServiceName] Started with PID: $($proc.Id)"
    return $proc.Id
}

$base = "c:\Users\HP\Desktop\demo"

# discovery
Start-ServiceJar -JarPath "$base\discovery\target\discovery-1.0.0.jar" -ServiceName "discovery" -PortArg "--server.port=8761"
Start-Sleep -Seconds 5

# config
Start-ServiceJar -JarPath "$base\config\target\config-1.0.0.jar" -ServiceName "config" -PortArg "--server.port=8888"
Start-Sleep -Seconds 5

# uaa
Start-ServiceJar -JarPath "$base\uaa\target\uaa-1.0.0.jar" -ServiceName "uaa" -PortArg "--server.port=9000"
Start-Sleep -Seconds 8

# gateway
Start-ServiceJar -JarPath "$base\gateway\target\gateway-1.0.0.jar" -ServiceName "gateway" -PortArg "--server.port=7573"
Start-Sleep -Seconds 5

# web
Start-ServiceJar -JarPath "$base\web\target\web-1.0.0.jar" -ServiceName "web" -PortArg "--server.port=8080"
Start-Sleep -Seconds 5

# product
Start-ServiceJar -JarPath "$base\product\target\product-1.0.0.jar" -ServiceName "product" -PortArg "--server.port=8081"
Start-Sleep -Seconds 5

Write-Host "All services started."
Get-Process -Name java -ErrorAction SilentlyContinue | Select-Object Id, @{Name="MainWindowTitle";Expression={$_.MainWindowTitle}}
