param(
    [string]$Token,
    [string]$Email =  "quanganhd132@gmail.com",
    [string]$Password = "test12345",
    [long]$ServerId,
    [long]$ChannelId,
    [string]$UserId,
    [string]$InternalSecret = $env:INTERNAL_SERVICE_SECRET,
    [string]$GatewayBaseUrl = "http://localhost:8080/api",
    [string]$ServerServiceBaseUrl = "http://localhost:8082",
    [string]$VoiceServiceBaseUrl = "http://localhost:8086",
    [switch]$SkipVoiceToken
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Result {
    param(
        [string]$Label,
        [object]$Value
    )
    Write-Host ("{0}: " -f $Label) -NoNewline -ForegroundColor Yellow
    Write-Host $Value
}

function Invoke-JsonRequest {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )

    try {
        $params = @{
            Method  = $Method
            Uri     = $Url
            Headers = $Headers
        }

        if ($null -ne $Body) {
            $params.ContentType = "application/json"
            $params.Body = ($Body | ConvertTo-Json -Depth 10)
        }

        return Invoke-RestMethod @params
    } catch {
        Write-Host ""
        Write-Host "REQUEST FAILED" -ForegroundColor Red
        Write-Result "Method" $Method
        Write-Result "URL" $Url

        if ($_.Exception.Response) {
            $response = $_.Exception.Response
            Write-Result "Status" ([int]$response.StatusCode)
        }

        if ($_.ErrorDetails.Message) {
            Write-Result "Body" $_.ErrorDetails.Message
        } elseif ($_.Exception.Response) {
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                if ($stream) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    $bodyText = $reader.ReadToEnd()
                    if ($bodyText) {
                        Write-Result "Body" $bodyText
                    } else {
                        Write-Result "Body" "(empty)"
                    }
                }
            } catch {
                Write-Result "Body" "(failed to read response body)"
            }
        } else {
            Write-Result "Error" $_.Exception.Message
        }

        throw
    }
}

function Unwrap-ApiData {
    param([object]$Response)

    if ($null -eq $Response) {
        return $null
    }

    if ($Response.PSObject.Properties.Name -contains "data") {
        return $Response.data
    }

    return $Response
}

function Find-VoiceChannel {
    param([object]$Server)

    $channels = @()
    if ($Server.PSObject.Properties.Name -contains "channels") {
        $channels = @($Server.channels)
    }

    if ($channels.Count -eq 0) {
        return $null
    }

    $voice = $channels | Where-Object {
        $type = [string]$_.type
        $name = [string]$_.name
        $type.ToUpperInvariant() -eq "VOICE" -or
        $type.ToUpperInvariant() -eq "VOICE_CHANNEL" -or
        $name.ToLowerInvariant().Contains("voice")
    } | Select-Object -First 1

    return $voice
}

function Get-PropertyValue {
    param(
        [object]$Object,
        [string[]]$Names
    )

    if ($null -eq $Object) {
        return $null
    }

    foreach ($name in $Names) {
        if ($Object.PSObject.Properties.Name -contains $name) {
            return $Object.$name
        }
    }

    return $null
}

function Resolve-LoginToken {
    param([object]$LoginResponse)

    $payload = Unwrap-ApiData $LoginResponse
    $token = Get-PropertyValue $payload @("accessToken", "access_token", "token")

    if (-not $token) {
        $token = Get-PropertyValue $LoginResponse @("accessToken", "access_token", "token")
    }

    return $token
}

if (-not $InternalSecret -or $InternalSecret.Trim().Length -eq 0) {
    throw "Missing internal secret. Set env INTERNAL_SERVICE_SECRET or pass -InternalSecret."
}

if ((-not $Token) -and $Email -and $Password) {
    Write-Step "Logging in through gateway"
    $loginResponse = Invoke-JsonRequest -Method "POST" -Url "$GatewayBaseUrl/auth/login" -Body @{
        email = $Email
        password = $Password
    }

    $Token = Resolve-LoginToken $loginResponse
    if (-not $Token) {
        throw "Login succeeded but access token could not be found in the response."
    }

    Write-Result "accessToken" ($Token.Substring(0, [Math]::Min(24, $Token.Length)) + "...")
}

$authHeaders = @{}
if ($Token -and $Token.Trim().Length -gt 0) {
    $authHeaders.Authorization = "Bearer $Token"
}

if (-not $UserId) {
    if (-not $Token) {
        throw "Missing UserId. Pass -UserId or pass -Token so the script can call /users/me."
    }

    Write-Step "Resolving current user via gateway"
    $meResponse = Invoke-JsonRequest -Method "GET" -Url "$GatewayBaseUrl/users/me" -Headers $authHeaders
    $me = Unwrap-ApiData $meResponse

    if ($me.PSObject.Properties.Name -contains "id") {
        $UserId = [string]$me.id
    } elseif ($me.PSObject.Properties.Name -contains "userId") {
        $UserId = [string]$me.userId
    }

    if (-not $UserId) {
        throw "Could not resolve user id from /users/me response."
    }
}

if (-not $ChannelId) {
    if (-not $ServerId) {
        throw "Missing ServerId. Pass -ServerId, or pass -ChannelId directly."
    }
    if (-not $Token) {
        throw "Missing Token. A token is required to fetch server channels through gateway."
    }

    Write-Step "Resolving voice channel from server detail"
    $serverResponse = Invoke-JsonRequest -Method "GET" -Url "$GatewayBaseUrl/servers/$ServerId" -Headers $authHeaders
    $server = Unwrap-ApiData $serverResponse
    $voiceChannel = Find-VoiceChannel $server

    if (-not $voiceChannel) {
        throw "Could not find a voice channel in server $ServerId. Pass -ChannelId manually."
    }

    $ChannelId = [long]$voiceChannel.id
}

if (-not $ServerId -and -not $SkipVoiceToken) {
    Write-Host "ServerId was not provided, so the script will skip voice token creation." -ForegroundColor DarkYellow
    $SkipVoiceToken = $true
}

Write-Step "Resolved test context"
Write-Result "UserId" $UserId
Write-Result "ServerId" ($(if ($ServerId) { $ServerId } else { "(not provided)" }))
Write-Result "ChannelId" $ChannelId

$internalHeaders = @{
    "X-Internal-Secret" = $InternalSecret
}

Write-Step "Testing server_service can-join-voice"
$canJoinUrl = "$ServerServiceBaseUrl/api/servers/channels/$ChannelId/can-join-voice?userId=$UserId"
$canJoinResponse = Invoke-JsonRequest -Method "GET" -Url $canJoinUrl -Headers $internalHeaders
$canJoinData = Unwrap-ApiData $canJoinResponse
Write-Result "canJoinVoice" $canJoinData

if (-not [bool]$canJoinData) {
    Write-Host ""
    Write-Host "server_service is reachable and internal secret is correct, but this user cannot join the voice channel." -ForegroundColor DarkYellow
    Write-Host "Check membership, ban/mute state, channelId, and CONNECT_VOICE permission." -ForegroundColor DarkYellow
}

if (-not $SkipVoiceToken) {
    Write-Step "Testing voice_service token creation directly"
    $voiceHeaders = @{
        "X-Internal-Secret" = $InternalSecret
        "X-User-Id" = $UserId
        "X-User-Username" = "dev-test"
    }

    $tokenUrl = "$VoiceServiceBaseUrl/api/voice/channels/$ChannelId/token"
    $tokenResponse = Invoke-JsonRequest -Method "POST" -Url $tokenUrl -Headers $voiceHeaders -Body @{ serverId = $ServerId }
    $tokenData = Unwrap-ApiData $tokenResponse

    Write-Result "voiceToken.roomName" $tokenData.roomName
    Write-Result "voiceToken.livekitUrl" $tokenData.livekitUrl
    Write-Result "voiceToken.hasToken" ([bool]$tokenData.token)
}

Write-Host ""
Write-Host "Done." -ForegroundColor Green
