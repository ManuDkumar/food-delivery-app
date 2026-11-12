// Step 11 — the lost-update race, demonstrated.
//
// Two VUs PATCH THE SAME cart item with different quantities, nonstop, for 10s.
// Old world (no @Version): every request 200, last write silently wins — data lost, nobody told.
// New world (@Version):   some requests get 409 "changed by another request" — the DB caught the cheat.
//
// Run: k6 run .\performance-tests\cart-race.js

import http from 'k6/http';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

const ok200      = new Counter('race_status_200');
const conflict   = new Counter('race_status_409');
const other      = new Counter('race_status_other');

export const options = {
  vus: 4,
  iterations: 20,
};

export function setup() {
  const login = http.post(`${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: 'hero@test.com', password: 'Abcd@1234' }),
    { headers: { 'Content-Type': 'application/json' } });
  if (login.status !== 200) {
    throw new Error(`login failed: ${login.status} ${login.body}`);
  }
  const token = login.json('token');
  const cart = http.get(`${BASE_URL}/api/v1/cart`,
    { headers: { Authorization: `Bearer ${token}` } }).json();
  if (!cart.items || cart.items.length === 0) {
    throw new Error('hero cart is empty — add an item first (POST /api/v1/cart/items)');
  }
  const item = cart.items[0];
  console.log(`racing on cart item: ${item.itemId}`);
  return { token: token, itemId: item.itemId };
}

export default function (data) {
  // batch = both PATCHes leave at the SAME instant — deterministic overlap
  const H = { headers: { Authorization: `Bearer ${data.token}`, 'Content-Type': 'application/json' } };
  const res = http.batch([
    { method: 'PATCH', url: `${BASE_URL}/api/v1/cart/items/${data.itemId}`, body: JSON.stringify({ quantity: 2 }), params: H },
    { method: 'PATCH', url: `${BASE_URL}/api/v1/cart/items/${data.itemId}`, body: JSON.stringify({ quantity: 3 }), params: H },
  ]);
  for (const r of res) {
    if (r.status === 200) ok200.add(1);
    else if (r.status === 409) conflict.add(1);
    else other.add(1);
  }
}
