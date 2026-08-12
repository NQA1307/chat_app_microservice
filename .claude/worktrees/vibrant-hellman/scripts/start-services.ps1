. "$PSScriptRoot\lib.ps1"

Start-GradleModule -Module "user_service"
Wait-Port -HostName "localhost" -Port 8081 -TimeoutSeconds 120

Start-GradleModule -Module "server_service"
Wait-Port -HostName "localhost" -Port 8082 -TimeoutSeconds 120

Start-GradleModule -Module "message_service"
Wait-Port -HostName "localhost" -Port 8084 -TimeoutSeconds 120

Start-GradleModule -Module "media_service"
Wait-Port -HostName "localhost" -Port 8085 -TimeoutSeconds 120

Start-GradleModule -Module "voice_service"
Wait-Port -HostName "localhost" -Port 8086 -TimeoutSeconds 120

Start-GradleModule -Module "notification_service"
Wait-Port -HostName "localhost" -Port 8087 -TimeoutSeconds 120

Start-GradleModule -Module "api_gateway"
Wait-Port -HostName "localhost" -Port 8080 -TimeoutSeconds 120

Write-Host "Application services are ready."
