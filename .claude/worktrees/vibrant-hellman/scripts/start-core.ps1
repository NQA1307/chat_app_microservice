. "$PSScriptRoot\lib.ps1"

Start-GradleModule -Module "eureka_server"
Wait-Port -HostName "localhost" -Port 8761 -TimeoutSeconds 120

Start-GradleModule -Module "config_server"
Wait-Port -HostName "localhost" -Port 8888 -TimeoutSeconds 120

Write-Host "Core services are ready."
