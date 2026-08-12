$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$RuntimeDir = Join-Path $RootDir ".runtime"
$LogDir = Join-Path $RuntimeDir "logs"
$PidDir = Join-Path $RuntimeDir "pids"

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
New-Item -ItemType Directory -Force -Path $PidDir | Out-Null

function Import-DotEnv {
    $envPath = Join-Path $RootDir ".env"
    if (-not (Test-Path $envPath)) {
        throw "Missing .env at $envPath. Create it from .env.example and fill required secrets."
    }

    Get-Content $envPath | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }

        $separator = $line.IndexOf("=")
        if ($separator -le 0) {
            return
        }

        $name = $line.Substring(0, $separator).Trim()
        $value = $line.Substring($separator + 1).Trim()
        Set-Item -Path "Env:$name" -Value $value
    }
}

Import-DotEnv

function Test-ProcessAlive {
    param([int]$ProcessId)

    try {
        $process = Get-Process -Id $ProcessId -ErrorAction Stop
        return -not $process.HasExited
    } catch {
        return $false
    }
}

function Start-GradleModule {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Module
    )

    $pidFile = Join-Path $PidDir "$Module.pid"
    $logFile = Join-Path $LogDir "$Module.log"

    if (Test-Path $pidFile) {
        $existingPid = [int](Get-Content $pidFile -Raw)
        if (Test-ProcessAlive -ProcessId $existingPid) {
            Write-Host "$Module is already running with PID $existingPid"
            return
        }
    }

    $command = @"
Set-Location '$RootDir'
.\gradlew.bat :$Module`:bootRun *> '$logFile'
"@

    $process = Start-Process `
        -FilePath "powershell.exe" `
        -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $command `
        -WindowStyle Hidden `
        -PassThru

    Set-Content -Path $pidFile -Value $process.Id
    Write-Host "Started $Module with PID $($process.Id). Log: $logFile"
}

function Stop-GradleModule {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Module
    )

    $pidFile = Join-Path $PidDir "$Module.pid"
    if (-not (Test-Path $pidFile)) {
        Write-Host "$Module has no PID file."
        return
    }

    $processId = [int](Get-Content $pidFile -Raw)
    if (Test-ProcessAlive -ProcessId $processId) {
        taskkill.exe /PID $processId /T /F | Out-Null
        Write-Host "Stopped $Module with PID $processId"
    } else {
        Write-Host "$Module process $processId is not running."
    }

    Remove-Item -Path $pidFile -Force
}

function Wait-Port {
    param(
        [Parameter(Mandatory = $true)]
        [string]$HostName,

        [Parameter(Mandatory = $true)]
        [int]$Port,

        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $client = New-Object System.Net.Sockets.TcpClient
            $asyncResult = $client.BeginConnect($HostName, $Port, $null, $null)
            $success = $asyncResult.AsyncWaitHandle.WaitOne(1000, $false)
            if ($success -and $client.Connected) {
                $client.Close()
                Write-Host "$HostName`:$Port is ready."
                return
            }
            $client.Close()
        } catch {
        }

        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for $HostName`:$Port"
}
