. "$PSScriptRoot\lib.ps1"

$modules = @(
    "api_gateway",
    "notification_service",
    "voice_service",
    "media_service",
    "message_service",
    "server_service",
    "user_service",
    "config_server",
    "eureka_server"
)

foreach ($module in $modules) {
    Stop-GradleModule -Module $module
}

Write-Host "Stopped tracked Gradle services."
