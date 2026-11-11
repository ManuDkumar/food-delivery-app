// Smoke test: proves k6 -> app -> PostgreSQL wiring works before any load.
// Public endpoint (permitAll), so no JWT needed.
// Run:  k6 run .\performance-tests\restaurants.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export const options = {
  vus: 1,
  iterations: 10,
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
