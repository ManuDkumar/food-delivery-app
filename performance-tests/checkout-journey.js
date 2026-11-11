// Rung 3 — the WRITE journey: "customer refills cart and checks out, forever"
//
// Journey per round (per virtual user, ONE user per VU — no cart fights):
//   POST /orders          → checkout (201) — consumes the cart
//   GET  /orders          → order history read
//   POST /cart/items ×3   → refill the cart for the NEXT round
//
// Why per-VU users: placeOrder clears the cart. If two VUs shared one user,
// they'd race on the same cart — we'd be testing the known concurrency bug,
// not normal load. Each VU = own customer (created in setup(), unique emails).
//
// Smoke:        k6 run .\performance-tests\checkout-journey.js
// Load (20 VUs):k6 run --vus 20 --duration 60s .\performance-tests\checkout-journey.js

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const USERS_N  = Number(__ENV.USERS_N || 20);      // test customers created per run
const PWD      = 'Abcd@999!';

export const options = {
  vus: 2,
  iterations: 10,            // defaults = small smoke; override with CLI flags
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  // --- 1. find a restaurant whose menu has >= 3 items -----------------------
  const rests = http.get(`${BASE_URL}/api/v1/restaurants`).json();
  let restId = null, menu = [];
  for (const r of rests) {
    const m = http.get(`${BASE_URL}/api/v1/restaurants/${r.id}/menu`).json();
    if (m.length >= 3) { restId = r.id; menu = m; break; }
  }
  check(restId, { 'found restaurant with 3+ menu items': (ok) => !!ok });
  const items = menu.slice(0, 3).map((x) => x.id);

  // --- 2. create USERS_N customers, each with a pre-loaded cart --------------
  // IDEMPOTENT identity: email AND phone are stable per index (no timestamp!).
  // Run 1 → 201 created; every later run → 409 (already registered) → login reuses it.
  const tokens = [];
  for (let i = 0; i < USERS_N; i++) {
    const email = `load_k6_${i}@test.com`;
    const phone = '98' + String(i).padStart(6, '0') + '77';   // stable AND unique per i

    // LOGIN FIRST: if the user exists from an earlier run, reuse it — no register, no 409s
    let login = http.post(`${BASE_URL}/api/v1/auth/login`,
      JSON.stringify({ email: email, password: PWD }),
      { headers: { 'Content-Type': 'application/json' } });

    if (login.status !== 200) {
      // user doesn't exist yet → register, then login again
      const reg = http.post(`${BASE_URL}/api/v1/auth/customer/register`,
        JSON.stringify({ name: `Load User ${i}`, email: email, password: PWD, phone: phone }),
        { headers: { 'Content-Type': 'application/json' } });
      if (reg.status !== 201) console.log(`register ${i} FAILED: status=${reg.status} body=${reg.body}`);
      check(reg, { [`register user ${i}`]: (r) => r.status === 201 });

      login = http.post(`${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email: email, password: PWD }),
        { headers: { 'Content-Type': 'application/json' } });
    }
    if (login.status !== 200) console.log(`login ${i} FAILED: status=${login.status} body=${login.body}`);
    check(login, { [`login user ${i}`]: (r) => r.status === 200 });
    tokens.push(login.json('token'));

    // pre-load cart with the 3 dishes (so round 1 can check out immediately)
    for (const mi of items) {
      http.post(`${BASE_URL}/api/v1/cart/items`,
        JSON.stringify({ menuItemId: mi, quantity: 1 }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${login.json('token')}` } });
    }
  }
  return { tokens: tokens, items: items };
}

export default function (data) {
  const token = data.tokens[__VU % data.tokens.length];
  const H = { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } };

  // 1) checkout — consumes the cart (409 only if refill failed earlier)
  const order = http.post(`${BASE_URL}/api/v1/orders`, null, { headers: H.headers, tags: { endpoint: 'place-order' } });
  check(order, { 'place order 201': (r) => r.status === 201 });

  // 2) order history read
  const hist = http.get(`${BASE_URL}/api/v1/orders`, { headers: { Authorization: `Bearer ${token}` }, tags: { endpoint: 'my-orders' } });
  check(hist, { 'orders 200': (r) => r.status === 200 });

  // 3) refill cart for the next round (3 items, quantity 1 each)
  for (const mi of data.items) {
    const refill = http.post(`${BASE_URL}/api/v1/cart/items`,
      JSON.stringify({ menuItemId: mi, quantity: 1 }),
      { headers: H.headers, tags: { endpoint: 'add-cart' } });
    check(refill, { 'refill 201': (r) => r.status === 201 });
  }

  sleep(1);
}
