import http from "k6/http";
import { check, sleep } from "k6";
import { fail } from "k6";
import { recordResponse } from "./status-metrics.js";

export function loadUsersFromEnv() {
  const tokens = parseTokens(__ENV.K6_TOKENS);
  if (tokens.length > 0) {
    return tokens.map((token, index) => ({
      email: `token-${index + 1}`,
      token,
    }));
  }

  const usersJson = (__ENV.K6_USERS_JSON || "").trim();
  if (usersJson) {
    try {
      return JSON.parse(usersJson).map(normalizeUser);
    } catch (error) {
      fail(`Invalid K6_USERS_JSON. Expected JSON array. error=${error.message}`);
    }
  }

  const users = (__ENV.K6_USERS || "")
    .split(",")
    .map((entry) => entry.trim())
    .filter(Boolean)
    .map(parseUserEntry);

  if (users.length > 0) {
    return users;
  }

  return [
    normalizeUser({
      email: __ENV.K6_EMAIL || "quanganhd132@gmail.com",
      password: __ENV.K6_PASSWORD || "test12345",
    }),
  ];
}

export function loginUserPool(baseUrl, users) {
  const loginDelaySeconds = Number(__ENV.K6_LOGIN_DELAY_SECONDS || 1.1);

  return users.map((user, index) => {
    if (user.token) {
      return user;
    }

    const loginRes = http.post(
      `${baseUrl}/auth/login`,
      JSON.stringify({
        email: user.email,
        password: user.password,
      }),
      {
        headers: { "Content-Type": "application/json" },
        tags: { name: "POST /auth/login" },
      },
    );

    const loginOk = check(loginRes, {
      "login status 200": (r) => r.status === 200,
      "login has access token": (r) => Boolean(r.json("data.accessToken")),
    });
    recordResponse(loginRes, "POST /auth/login", [200]);

    const token = loginRes.json("data.accessToken");
    if (!loginOk || !token) {
      fail(`Login failed for ${user.email}. status=${loginRes.status}, body=${loginRes.body}`);
    }

    if (index < users.length - 1 && loginDelaySeconds > 0) {
      sleep(loginDelaySeconds);
    }

    return {
      email: user.email,
      token,
    };
  });
}

export function pickUser(users) {
  if (!users || users.length === 0) {
    fail("No users available in setup data");
  }

  return users[(__VU - 1) % users.length];
}

export function assertCapacityUserPool(users, peakVus) {
  const allowTokenReuse = (__ENV.K6_ALLOW_TOKEN_REUSE || "0") === "1";
  if (allowTokenReuse || users.length >= peakVus) {
    return;
  }

  fail(
    `Capacity test needs at least ${peakVus} users/tokens to avoid measuring rate limit again. ` +
      `Current users=${users.length}. Add K6_USERS/K6_USERS_JSON/K6_TOKENS or set K6_ALLOW_TOKEN_REUSE=1 intentionally.`,
  );
}

function parseTokens(raw) {
  return (raw || "")
    .split(",")
    .map((token) => token.trim())
    .filter(Boolean);
}

function parseUserEntry(entry) {
  const separatorIndex = entry.indexOf(":");
  if (separatorIndex < 1) {
    fail(`Invalid K6_USERS entry "${entry}". Expected email:password`);
  }

  return normalizeUser({
    email: entry.slice(0, separatorIndex),
    password: entry.slice(separatorIndex + 1),
  });
}

function normalizeUser(user) {
  const email = String(user.email || "").trim().toLowerCase();
  const password = String(user.password || "");
  const token = String(user.token || "").trim();

  if (token) {
    return { email: email || "token-user", token };
  }

  if (!email || !password) {
    fail(`Invalid user config. Each user needs email/password or token. value=${JSON.stringify(user)}`);
  }

  return { email, password };
}
