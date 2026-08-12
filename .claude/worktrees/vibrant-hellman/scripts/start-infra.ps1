. "$PSScriptRoot\lib.ps1"

Set-Location $RootDir

Write-Host "Starting Docker infrastructure..."
docker compose up -d

Write-Host "Waiting for infrastructure ports..."
Wait-Port -HostName "localhost" -Port 5532 -TimeoutSeconds 90
Wait-Port -HostName "localhost" -Port 6380 -TimeoutSeconds 90
Wait-Port -HostName "localhost" -Port 5773 -TimeoutSeconds 90
Wait-Port -HostName "localhost" -Port 7700 -TimeoutSeconds 90
Wait-Port -HostName "localhost" -Port 7880 -TimeoutSeconds 90

Write-Host "Infrastructure is ready."
