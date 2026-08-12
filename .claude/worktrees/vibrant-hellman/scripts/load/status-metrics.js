import { Counter, Rate } from "k6/metrics";

export const status2xx = new Rate("status_2xx");
export const status3xx = new Rate("status_3xx");
export const status4xx = new Rate("status_4xx");
export const status5xx = new Rate("status_5xx");
export const rateLimited429 = new Rate("rate_limited_429");
export const authErrors401403 = new Rate("auth_errors_401_403");
export const serverErrors5xx = new Rate("server_errors_5xx");
export const timeoutOrNetworkErrors = new Rate("timeout_or_network_errors");
export const unexpectedStatus = new Rate("unexpected_status");

export const unexpectedStatusCount = new Counter("unexpected_status_count");
export const rateLimited429Count = new Counter("rate_limited_429_count");
export const serverErrors5xxCount = new Counter("server_errors_5xx_count");
export const authErrors401403Count = new Counter("auth_errors_401_403_count");
export const timeoutOrNetworkErrorsCount = new Counter("timeout_or_network_errors_count");

const LOG_FAILURES = (__ENV.K6_LOG_FAILURES || "1") !== "0";
const MAX_FAILURE_LOGS_PER_VU = Number(__ENV.K6_MAX_FAILURE_LOGS_PER_VU || 5);
let failureLogsForVu = 0;

export function recordResponse(res, requestName, expectedStatuses = [200]) {
  const status = typeof res?.status === "number" ? res.status : 0;
  const expected = expectedStatuses.includes(status);
  const networkError = status === 0 || Boolean(res?.error);
  const authError = status === 401 || status === 403;
  const rateLimited = status === 429;
  const serverError = status >= 500;

  status2xx.add(status >= 200 && status < 300);
  status3xx.add(status >= 300 && status < 400);
  status4xx.add(status >= 400 && status < 500);
  status5xx.add(serverError);
  rateLimited429.add(rateLimited);
  authErrors401403.add(authError);
  serverErrors5xx.add(serverError);
  timeoutOrNetworkErrors.add(networkError);
  unexpectedStatus.add(!expected);

  if (!expected) {
    unexpectedStatusCount.add(1);
  }
  if (rateLimited) {
    rateLimited429Count.add(1);
  }
  if (serverError) {
    serverErrors5xxCount.add(1);
  }
  if (authError) {
    authErrors401403Count.add(1);
  }
  if (networkError) {
    timeoutOrNetworkErrorsCount.add(1);
  }

  if (!expected && LOG_FAILURES && failureLogsForVu < MAX_FAILURE_LOGS_PER_VU) {
    failureLogsForVu += 1;
    console.log(
      `[failed-request] name="${requestName}" status=${status} expected=${expectedStatuses.join("|")} ` +
        `error="${res?.error || ""}" body="${previewBody(res?.body)}"`,
    );
  }
}

function previewBody(body) {
  if (!body) {
    return "";
  }

  const normalized = String(body).replace(/\s+/g, " ").trim();
  return normalized.length > 300 ? `${normalized.slice(0, 300)}...` : normalized;
}
