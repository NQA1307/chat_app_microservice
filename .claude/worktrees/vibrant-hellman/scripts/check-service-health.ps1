param(
    [string]$GatewayBaseUrl = "http://localhost:8080",
    [int]$TimeoutSeconds = 3
)

$ErrorActionPreference = "Continue"

$services = @(
    @{ Name = "api-gateway"; Url = "http://localhost:8080/actuator/health" },
    @{ Name = "user-service"; Url = "http://localhost:8081/actuator/health" },
    @{ Name = "server-service"; Url = "http://localhost:8082/actuator/health" },
    @{ Name = "message-service"; Url = "http://localhost:8084/actuator/health" },
    @{ Name = "media-service"; Url = "http://localhost:8085/actuator/health" },
    @{ Name = "voice-service"; Url = "http://localhost:8086/actuator/health" },
    @{ Name = "notification-service"; Url = "http://localhost:8087/actuator/health" },
    @{ Name = "config-server"; Url = "http://localhost:8888/actuator/health" },
    @{ Name = "eureka-server"; Url = "http://localhost:8761/actuator/health" },
    @{ Name = "prometheus"; Url = "http://localhost:9090/-/ready" },
    @{ Name = "grafana"; Url = "http://localhost:3001/api/health" },
    @{ Name = "rabbitmq-management"; Url = "http://localhost:15773/api/overview"; Username = "guest"; Password = "guest" },
    @{ Name = "meilisearch"; Url = "http://localhost:7700/health" }
)

$results = foreach ($service in $services) {
    $startedAt = Get-Date
    try {
        $headers = @{}
        if ($service.Username -and $service.Password) {
            $rawToken = "$($service.Username):$($service.Password)"
            $encodedToken = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($rawToken))
            $headers.Authorization = "Basic $encodedToken"
        }

        $response = Invoke-WebRequest `
            -Uri $service.Url `
            -Method Get `
            -Headers $headers `
            -TimeoutSec $TimeoutSeconds `
            -UseBasicParsing

        $durationMs = [math]::Round(((Get-Date) - $startedAt).TotalMilliseconds)
        $body = $response.Content
        $healthStatus = "UP"

        if ($body -match '"status"\s*:\s*"([^"]+)"') {
            $healthStatus = $Matches[1]
        }

        [pscustomobject]@{
            Service = $service.Name
            Status = $healthStatus
            Http = [int]$response.StatusCode
            LatencyMs = $durationMs
            Url = $service.Url
        }
    } catch {
        $durationMs = [math]::Round(((Get-Date) - $startedAt).TotalMilliseconds)
        [pscustomobject]@{
            Service = $service.Name
            Status = "DOWN"
            Http = ""
            LatencyMs = $durationMs
            Url = $service.Url
        }
    }
}

$results | Sort-Object Service | Format-Table -AutoSize

if ($results.Status -contains "DOWN") {
    exit 1
}
