// Baseline ramp test: 5 -> 20 -> 50 virtual users on public restaurant browse.
// Use this SAME script before (Step 9) and after (Step 11) optimizations.
// Run:  k6 run .\performance-tests\restaurants-ramp.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export const options = {
  scenarios: {
    restaurant_load: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 5 },
        { duration: '1m', target: 5 },
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 50 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/restaurants`, {
    tags: { endpoint: 'restaurants' },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
