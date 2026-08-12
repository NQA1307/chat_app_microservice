import http from "k6/http";
import { check, group, sleep } from "k6";
import { recordResponse } from "./status-metrics.js";
import { assertCapacityUserPool, loadUsersFromEnv, loginUserPool, pickUser } from "./user-pool.js";

const TARGET_1 = Number(__ENV.K6_TARGET_1 || 10);
const TARGET_2 = Number(__ENV.K6_TARGET_2 || 30);
const PEAK_VUS = Math.max(TARGET_1, TARGET_2);

export const options = {
  stages: [
    { duration: "30s", target: TARGET_1 },
    { duration: "1m", target: TARGET_2 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<500"],
    rate_limited_429: ["rate<0.01"],
    server_errors_5xx: ["rate<0.01"],
    timeout_or_network_errors: ["rate<0.01"],
  },
};

const BASE_URL = __ENV.K6_BASE_URL || "http://localhost:8080/api";
const CHANNEL_ID = __ENV.K6_CHANNEL_ID;

export function setup() {
  const users = loginUserPool(BASE_URL, loadUsersFromEnv());
  assertCapacityUserPool(users, PEAK_VUS);

  return { users };
}

export default function (data) {
  const user = pickUser(data.users);
  const headers = {
    Authorization: `Bearer ${user.token}`,
    "Content-Type": "application/json",
  };

  group("read APIs", () => {
    const friendsRes = http.get(`${BASE_URL}/friends`, {
      headers,
      tags: { name: "GET /friends" },
    });
    check(friendsRes, {
      "friends status 200": (r) => r.status === 200,
    });
    recordResponse(friendsRes, "GET /friends", [200]);

    const serversRes = http.get(`${BASE_URL}/servers`, {
      headers,
      tags: { name: "GET /servers" },
    });
    check(serversRes, {
      "servers status 200": (r) => r.status === 200,
    });
    recordResponse(serversRes, "GET /servers", [200]);

    const unreadRes = http.get(`${BASE_URL}/notifications/unread-count`, {
      headers,
      tags: { name: "GET /notifications/unread-count" },
    });
    check(unreadRes, {
      "unread-count status 200": (r) => r.status === 200,
    });
    recordResponse(unreadRes, "GET /notifications/unread-count", [200]);
  });

  if (CHANNEL_ID) {
    group("optional message send", () => {
      const sendRes = http.post(
        `${BASE_URL}/messages`,
        JSON.stringify({
          channelId: Number(CHANNEL_ID),
          content: `k6 load test ${Date.now()}-${__VU}-${__ITER}`,
          messageType: "TEXT",
        }),
        {
          headers,
          tags: { name: "POST /messages" },
        },
      );

      check(sendRes, {
        "send message controlled status": (r) =>
          r.status === 200 || r.status === 201 || r.status === 403 || r.status === 503,
      });
      recordResponse(sendRes, "POST /messages", [200, 201, 403, 503]);
    });
  }

  sleep(1);
}
