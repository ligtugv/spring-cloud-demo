$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$jarPath = "c:\Users\HP\Desktop\demo\uaa\target\uaa-1.0.0.jar"
$portArg = "--server.port=9000"

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "java"
$psi.Arguments = "-jar `"$jarPath`" $portArg"
$psi.WorkingDirectory = Split-Path $jarPath
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.CreateNoWindow = $true

$proc = [System.Diagnostics.Process]::Start($psi)

# Read output for 15 seconds
$timeout = 15000
$sw = [System.Diagnostics.Stopwatch]::StartNew()
$stdout = ""
$stderr = ""

while (!$proc.HasExited -and $sw.ElapsedMilliseconds -lt $timeout) {
    if (!$proc.StandardOutput.EndOfStream) {
        $stdout += $proc.StandardOutput.ReadToEnd()
    }
    if (!$proc.StandardError.EndOfStream) {
        $stderr += $proc.StandardError.ReadToEnd()
    }
    Start-Sleep -Milliseconds 500
}

$sw.Stop()

Write-Host "=== STDOUT ==="
Write-Host $stdout
Write-Host "=== STDERR ==="
Write-Host $stderr
Write-Host "=== Exit Code: $($proc.ExitCode) ==="
Write-Host "Has Exited: $($proc.HasExited)"
