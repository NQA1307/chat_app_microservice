import http from "k6/http";
import { check, sleep } from "k6";
import { fail } from "k6";
import { recordResponse } from "./status-metrics.js";

export const options = {
  stages: [
    { duration: "30s", target: 10 },
    { duration: "30s", target: 20 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<500"],
  },
};

const BASE_URL = "http://localhost:8080/api";
const EMAIL = __ENV.K6_EMAIL || "quanganhd132@gmail.com";
const PASSWORD = __ENV.K6_PASSWORD || "test12345";

export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({
      email: EMAIL,
      password: PASSWORD,
    }),
    {
      headers: { "Content-Type": "application/json" },
    },
  );

  const loginOk = check(loginRes, {
    "login 200": (r) => r.status === 200,
  });
  recordResponse(loginRes, "POST /auth/login", [200]);

  const token = loginRes.json("data.accessToken");

  if (!loginOk || !token) {
    fail(`Login failed. status=${loginRes.status}, body=${loginRes.body}`);
  }

  return { token };
}

export default function (data) {
  const headers = {
    Authorization: `Bearer ${data.token}`,
    "Content-Type": "application/json",
  };

  const friendsRes = http.get(`${BASE_URL}/friends`, { headers });

  check(friendsRes, {
    "friends 200": (r) => r.status === 200,
  });
  recordResponse(friendsRes, "GET /friends", [200]);

  sleep(1);
}
