param(
    [int]$Count = 30,
    [string]$DbContainer = "postgres-dev",
    [string]$DbName = "user_db",
    [string]$DbUser = "postgres",
    [string]$TemplateEmail = "",
    [string]$Password = "",
    [string]$EmailDomain = "example.test"
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($TemplateEmail)) {
    $TemplateEmail = if ($env:K6_EMAIL) { $env:K6_EMAIL } else { "quanganhd132@gmail.com" }
}

if ([string]::IsNullOrWhiteSpace($Password)) {
    $Password = if ($env:K6_PASSWORD) { $env:K6_PASSWORD } else { "test12345" }
}

if ($Count -lt 1) {
    throw "Count must be greater than 0"
}

$safeTemplateEmail = $TemplateEmail.Replace("'", "''").Trim().ToLowerInvariant()
$safeEmailDomain = $EmailDomain.Replace("'", "''").Trim().ToLowerInvariant()

$sql = @"
WITH template_password AS (
    SELECT password
    FROM users
    WHERE email = '$safeTemplateEmail'
    LIMIT 1
),
generated AS (
    SELECT generate_series(1, $Count) AS i
)
INSERT INTO users (
    id,
    username,
    email,
    password,
    display_name,
    avatar_url,
    role,
    email_verified,
    email_verified_at,
    locked,
    banned,
    created_at,
    updated_at
)
SELECT
    ('00000000-0000-0000-0000-' || lpad(i::text, 12, '0'))::uuid,
    'loadtest' || lpad(i::text, 3, '0'),
    'loadtest' || lpad(i::text, 3, '0') || '@$safeEmailDomain',
    template_password.password,
    'Load Test ' || lpad(i::text, 3, '0'),
    NULL,
    'USER',
    TRUE,
    NOW(),
    FALSE,
    FALSE,
    NOW(),
    NOW()
FROM generated
CROSS JOIN template_password
ON CONFLICT (email) DO UPDATE SET
    password = EXCLUDED.password,
    display_name = EXCLUDED.display_name,
    email_verified = TRUE,
    email_verified_at = COALESCE(users.email_verified_at, NOW()),
    locked = FALSE,
    banned = FALSE,
    updated_at = NOW();
"@

& docker exec $DbContainer psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -c $sql

$emails = 1..$Count | ForEach-Object {
    "loadtest$($_.ToString('000'))@$safeEmailDomain`:$Password"
}

Write-Host ""
Write-Host "Seeded $Count users using template password from $TemplateEmail."
Write-Host "Use this for k6 capacity test:"
Write-Host "`$env:K6_USERS=`"$($emails -join ',')`""
