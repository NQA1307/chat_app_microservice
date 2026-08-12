# k6 load testing

## 1. Verify rate limiting

This test intentionally uses one user/token to confirm that gateway rate limiting returns `429`.

```powershell
$env:K6_EMAIL="user@example.com"
$env:K6_PASSWORD="password"
k6 run scripts/load/api-rate-limit-test.js
```

Useful overrides:

```powershell
$env:K6_RATE_LIMIT_PATH="/friends"
$env:K6_RATE_LIMIT_TARGET="20"
$env:K6_RATE_LIMIT_SLEEP="0"
```

Expected result: `rate_limited_429` is greater than `0`, while `server_errors_5xx` and `timeout_or_network_errors` stay at `0`.

## 2. Run capacity test with many users

Use at least as many users/tokens as the peak VUs. By default `api-load-test.js` peaks at `30` VUs, so provide at least `30` users or lower `K6_TARGET_2`.

Option A, email/password list:

```powershell
$env:K6_USERS="user1@example.com:password,user2@example.com:password,user3@example.com:password"
$env:K6_TARGET_1="3"
$env:K6_TARGET_2="3"
k6 run scripts/load/api-load-test.js
```

Option B, JSON list:

```powershell
$env:K6_USERS_JSON='[{"email":"user1@example.com","password":"password"},{"email":"user2@example.com","password":"password"}]'
$env:K6_TARGET_1="2"
$env:K6_TARGET_2="2"
k6 run scripts/load/api-load-test.js
```

Option C, pre-generated access tokens:

```powershell
$env:K6_TOKENS="token1,token2,token3"
$env:K6_TARGET_1="3"
$env:K6_TARGET_2="3"
k6 run scripts/load/api-load-test.js
```

The setup phase logs users in sequentially with `K6_LOGIN_DELAY_SECONDS=1.1` by default so `/auth/login` does not hit the IP rate limit. If you provide `K6_TOKENS`, login is skipped.

## 3. Read the result

For a valid capacity test, `rate_limited_429` should be close to `0`. Then inspect:

- `http_req_duration`: latency, especially `p(95)` and `p(99)`.
- `server_errors_5xx`: backend failures.
- `timeout_or_network_errors`: network/timeouts.
- `auth_errors_401_403`: token or permission setup problems.
- `unexpected_status`: responses outside the expected list for each request.

To reduce console noise during a real capacity run:

```powershell
$env:K6_LOG_FAILURES="0"
k6 run scripts/load/api-load-test.js
```

To intentionally reuse fewer tokens than VUs, opt in explicitly:

```powershell
$env:K6_ALLOW_TOKEN_REUSE="1"
```

That mode can still measure rate limiting, so keep it off for normal capacity testing.
