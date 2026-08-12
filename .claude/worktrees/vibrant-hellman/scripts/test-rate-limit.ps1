param(
    [string]$GatewayBaseUrl = "http://localhost:8080",
    [string]$BearerToken = "",
    [int]$Requests = 25,
    [int]$DelayMs = 0,
    [ValidateSet("all", "auth", "friends", "servers", "messages", "dm", "media")]
    [string]$Case = "all",
    [string]$AuthEmail = "rate-limit@test.local",
    [string]$AuthPassword = "wrong-password"
)

$ErrorActionPreference = "Stop"

function New-RateLimitCase {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [bool]$RequiresToken,
        [object]$Body = $null,
        [hashtable]$ExtraHeaders = @{}
    )

    return [PSCustomObject]@{
        Name = $Name
        Method = $Method
        Path = $Path
        RequiresToken = $RequiresToken
        Body = $Body
        ExtraHeaders = $ExtraHeaders
    }
}

function Invoke-RateLimitRequest {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers,
        [object]$Body = $null
    )

    $invokeArgs = @{
        Method = $Method
        Uri = $Url
        Headers = $Headers
        UseBasicParsing = $true
        TimeoutSec = 20
    }

    if ($null -ne $Body) {
        $invokeArgs.ContentType = "application/json"
        $invokeArgs.Body = ($Body | ConvertTo-Json -Depth 10)
    }

    try {
        $response = Invoke-WebRequest @invokeArgs
        return [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [int]$_.Exception.Response.StatusCode
        }

        Write-Host "Request failed before receiving HTTP status: $($_.Exception.Message)" -ForegroundColor Red
        return -1
    }
}

function Test-RateLimitCase {
    param(
        [object]$TestCase
    )

    if ($TestCase.RequiresToken -and [string]::IsNullOrWhiteSpace($BearerToken)) {
        Write-Host ""
        Write-Host "SKIP $($TestCase.Name): protected route requires -BearerToken." -ForegroundColor Yellow
        return
    }

    $url = "$($GatewayBaseUrl.TrimEnd('/'))$($TestCase.Path)"
    $headers = @{}

    foreach ($key in $TestCase.ExtraHeaders.Keys) {
        $headers[$key] = $TestCase.ExtraHeaders[$key]
    }

    if ($TestCase.RequiresToken) {
        $headers["Authorization"] = "Bearer $BearerToken"
    }

    Write-Host ""
    Write-Host "Testing $($TestCase.Name): $($TestCase.Method) $url"
    Write-Host "Requests: $Requests, delay: ${DelayMs}ms"

    $statuses = New-Object System.Collections.Generic.List[int]
    for ($i = 1; $i -le $Requests; $i++) {
        $status = Invoke-RateLimitRequest `
            -Method $TestCase.Method `
            -Url $url `
            -Headers $headers `
            -Body $TestCase.Body

        $statuses.Add($status)
        Write-Host ("{0,3}. HTTP {1}" -f $i, $status)

        if ($DelayMs -gt 0) {
            Start-Sleep -Milliseconds $DelayMs
        }
    }

    $groups = $statuses | Group-Object | Sort-Object Name
    $summary = ($groups | ForEach-Object { "$($_.Name)=$($_.Count)" }) -join ", "
    $tooManyRequests = ($statuses | Where-Object { $_ -eq 429 }).Count

    Write-Host "Summary: $summary"
    if ($tooManyRequests -gt 0) {
        Write-Host "PASS: $($TestCase.Name) returned $tooManyRequests HTTP 429 responses." -ForegroundColor Green
    } else {
        Write-Host "WARN: $($TestCase.Name) did not return HTTP 429. Check Redis, Gateway config, token validity, or increase -Requests." -ForegroundColor Yellow
    }
}

$probeId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$cases = @()
$cases += New-RateLimitCase `
    -Name "auth" `
    -Method "POST" `
    -Path "/api/auth/login" `
    -RequiresToken $false `
    -Body @{ email = $AuthEmail; password = $AuthPassword } `
    -ExtraHeaders @{ "X-Forwarded-For" = "rate-limit-auth-$probeId" }

$cases += New-RateLimitCase `
    -Name "friends" `
    -Method "GET" `
    -Path "/api/friends/requests/incoming" `
    -RequiresToken $true

$cases += New-RateLimitCase `
    -Name "servers" `
    -Method "GET" `
    -Path "/api/servers/1" `
    -RequiresToken $true

$cases += New-RateLimitCase `
    -Name "messages" `
    -Method "GET" `
    -Path "/api/messages/1" `
    -RequiresToken $true

$cases += New-RateLimitCase `
    -Name "dm" `
    -Method "GET" `
    -Path "/api/dm/conversations" `
    -RequiresToken $true

$cases += New-RateLimitCase `
    -Name "media" `
    -Method "GET" `
    -Path "/api/media/__rate_limit_probe__" `
    -RequiresToken $true

$selectedCases = if ($Case -eq "all") {
    $cases
} else {
    $cases | Where-Object { $_.Name -eq $Case }
}

Write-Host "Gateway: $GatewayBaseUrl"
Write-Host "Case: $Case"
Write-Host "Note: protected cases need a valid JWT because JwtAuth runs before RequestRateLimiter."

foreach ($testCase in $selectedCases) {
    Test-RateLimitCase -TestCase $testCase
}
