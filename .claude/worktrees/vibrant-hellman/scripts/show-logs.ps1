param(
    [Parameter(Mandatory = $true)]
    [string]$Module
)

. "$PSScriptRoot\lib.ps1"

$logFile = Join-Path $LogDir "$Module.log"
if (-not (Test-Path $logFile)) {
    throw "Log file not found: $logFile"
}

Get-Content -Path $logFile -Tail 120 -Wait
