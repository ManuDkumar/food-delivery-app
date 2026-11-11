// Rung 2 — authenticated READ journey (as hero, a normal customer)
//
// What this does, in one sentence:
//   1 fake user logs in ONCE, then visits cart -> profile -> order history,
//   like a customer checking their app, 10 times, 1s pause between rounds.
//
// Why it matters:
//   GET /cart is where we suspect the N+1 (1 SQL per cart row).
//   Compare its timing + SQL count against restaurants (1 SQL, ~4ms warm).

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

// Runs ONCE before the loop: login, keep the token (like a browser session)
export function setup() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: 'hero@test.com', password: 'Abcd@1234' }),
    { headers: { 'Content-Type': 'application/json' }, tags: { endpoint: 'login' } }
  );
  check(res, { 'login ok 200': (r) => r.status === 200 });
  const token = res.json('token');
  check(token, { 'token present': (t) => typeof t === 'string' && t.length > 10 });
  return { token: token };
}

// The loop: 3 authenticated reads per round
export default function (data) {
  const params = { headers: { Authorization: `Bearer ${data.token}` } };

  const cart = http.get(`${BASE_URL}/api/v1/cart`, { ...params, tags: { endpoint: 'get-cart' } });
  check(cart, { 'cart 200': (r) => r.status === 200 });

  const profile = http.get(`${BASE_URL}/api/v1/user/profile`, { ...params, tags: { endpoint: 'profile' } });
  check(profile, { 'profile 200': (r) => r.status === 200 });

  const orders = http.get(`${BASE_URL}/api/v1/orders`, { ...params, tags: { endpoint: 'my-orders' } });
  check(orders, { 'orders 200': (r) => r.status === 200 });

  sleep(1);
}
