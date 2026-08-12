import http from "k6/http";
import { check, sleep } from "k6";
import { fail } from "k6";
import { recordResponse } from "./status-metrics.js";
import { loadUsersFromEnv, loginUserPool } from "./user-pool.js";

export const options = {
  stages: [
    { duration: "10s", target: Number(__ENV.K6_RATE_LIMIT_TARGET || 10) },
    { duration: "20s", target: Number(__ENV.K6_RATE_LIMIT_TARGET || 10) },
    { duration: "10s", target: 0 },
  ],
  thresholds: {
    rate_limited_429: ["rate>0"],
    server_errors_5xx: ["rate==0"],
    timeout_or_network_errors: ["rate==0"],
  },
};

const BASE_URL = __ENV.K6_BASE_URL || "http://localhost:8080/api";
const TARGET_PATH = __ENV.K6_RATE_LIMIT_PATH || "/friends";

export function setup() {
  const firstUser = loadUsersFromEnv()[0];
  const users = loginUserPool(BASE_URL, [firstUser]);

  if (!users[0]?.token) {
    fail("Rate-limit test needs one valid user/token");
  }

  return { user: users[0] };
}

export default function (data) {
  const res = http.get(`${BASE_URL}${TARGET_PATH}`, {
    headers: {
      Authorization: `Bearer ${data.user.token}`,
      "Content-Type": "application/json",
    },
    tags: { name: `GET ${TARGET_PATH}` },
  });

  check(res, {
    "rate-limit target returns controlled status": (r) => r.status === 200 || r.status === 429,
  });
  recordResponse(res, `GET ${TARGET_PATH}`, [200, 429]);

  sleep(Number(__ENV.K6_RATE_LIMIT_SLEEP || 0));
}
