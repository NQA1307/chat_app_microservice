param(
    [int]$Count = 30,
    [long]$ServerId = 10,
    [long]$ChannelId = 7,
    [string]$DbContainer = "postgres-dev",
    [string]$UserDbName = "user_db",
    [string]$ServerDbName = "server_db",
    [string]$DbUser = "postgres",
    [string]$EmailDomain = "example.test",
    [string]$RedisContainer = "redis-dev"
)

$ErrorActionPreference = "Stop"

if ($Count -lt 1) {
    throw "Count must be greater than 0"
}

$safeEmailDomain = $EmailDomain.Replace("'", "''").Trim().ToLowerInvariant()

$userIdsRaw = & docker exec $DbContainer psql -U $DbUser -d $UserDbName -t -A -c "select id from users where email like 'loadtest%@${safeEmailDomain}' order by email limit $Count;"
$userIds = $userIdsRaw | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object { $_.Trim() }

if ($userIds.Count -lt $Count) {
    throw "Expected $Count load-test users, found $($userIds.Count). Run scripts/load/seed-load-users.ps1 first."
}

$values = $userIds | ForEach-Object {
    "($ServerId, '$($_.Replace("'", "''"))', 'GUEST', NOW(), NOW())"
}

$sql = @"
INSERT INTO server_members (
    server_id,
    user_id,
    role,
    joined_at,
    updated_at
)
VALUES
    $($values -join ",`n    ")
ON CONFLICT (server_id, user_id) DO UPDATE SET
    role = 'GUEST',
    updated_at = NOW();
"@

& docker exec $DbContainer psql -U $DbUser -d $ServerDbName -v ON_ERROR_STOP=1 -c $sql

if ($ChannelId -gt 0) {
    foreach ($userId in $userIds) {
        & docker exec $RedisContainer redis-cli DEL "permission:channel:${ChannelId}:user:${userId}" | Out-Null
    }
}

Write-Host ""
Write-Host "Seeded $($userIds.Count) load-test users as GUEST members of server $ServerId."
Write-Host "Use this channel for message load test:"
Write-Host "`$env:K6_CHANNEL_ID=`"$ChannelId`""
