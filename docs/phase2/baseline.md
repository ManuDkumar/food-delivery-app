# Phase 2 — Baseline ("Evidence First")

Date: 2026-11-10/11 · App: Spring Boot 4.1.1, Java 17, port 8081 · DB: PostgreSQL `food_delivery` (local) · Load tool: k6 v2.2.0
Scripts: `performance-tests/*.js` · Data: dev DB (~3+2 restaurants, ~16+ menu items, ~100+ orders after run 3)

## Methodology

- Journeys, not single endpoints: one script = one user story covering several endpoints.
- Intensity levels: 🔥 hot (ramp/20-50 VUs), 🟡 warm (moderate), ❄️ cold (smoke only).
- SQL counting: `show-sql: true` + single manual request → count `Hibernate: select` lines.
  (Boot 4 exposes no `hibernate.*` actuator metrics; `spring.data.repository.invocations` cannot see lazy loads.)
- Always gate load runs on `Started FoodDeliveryAppApplication` (port opens before the app is ready).
- `show-sql: false` during load runs (log I/O would slow every request).
- Login cost note: BCrypt is deliberately slow (~70-450ms) → scripts log in once in `setup()`.

## Endpoint inventory (coverage rule: every endpoint in exactly one journey, no orphans)

| # | Endpoint | Method | R/W | Level | Journey / status |
|---|---|---|---|---|---|
| 1 | /api/v1/auth/customer/register | POST | W | 🟡 | seed.ps1 + k6 setup ✅ |
| 2 | /api/v1/auth/restaurant/register | POST | W | ❄️ | seed.ps1 ✅ |
| 3 | /api/v1/auth/admin/register | POST | W | ❄️ | not exercised (admin-only; smoke candidate) |
| 4 | /api/v1/auth/login | POST | R* | 🔥 | auth-reads setup + checkout setup ✅ |
| 5 | /api/v1/restaurants | GET | R | 🔥 | restaurants.js ✅ measured |
| 6 | /api/v1/restaurants/create | POST | W | ❄️ | seed.ps1 ✅ |
| 7 | /api/v1/restaurants/owner/mine | GET | R | ❄️ | pending |
| 8 | /api/v1/restaurants/{id} | GET | R | 🔥 | pending (same SQL shape as #5) |
| 9 | /api/v1/restaurants/owner/{id}/status | PATCH | W | ❄️ | pending |
| 10 | /api/v1/restaurants/owner/{id}/update | PATCH | W | ❄️ | pending |
| 11 | /api/v1/restaurants/owner/{id}/delete | DELETE | W | ❄️ | pending |
| 12 | /api/v1/restaurants/{rid}/menu | GET | R | 🔥 | covered inside journeys ✅ |
| 13 | /api/v1/restaurants/{rid}/menu | POST | W | ❄️ | seed.ps1 ✅ |
| 14 | /api/v1/menu/{itemId} | PATCH | W | ❄️ | pending |
| 15 | /api/v1/menu/{itemId}/availability | PATCH | W | ❄️ | pending |
| 16 | /api/v1/menu/{itemId} | DELETE | W | ❄️ | pending |
| 17 | /api/v1/cart | GET | R | 🔥 | auth-reads.js ✅ measured |
| 18 | /api/v1/cart/items | POST | W | 🔥 | checkout-journey.js ✅ measured |
| 19 | /api/v1/cart/items/{itemId} | PATCH | W | 🟡 | pending |
| 20 | /api/v1/cart/items/{itemId} | DELETE | W | 🟡 | pending |
| 21 | /api/v1/cart/items | DELETE | W | 🟡 | pending (seed.ps1 uses it) |
| 22 | /api/v1/orders | POST | W | 🔥 | checkout-journey.js ✅ measured |
| 23 | /api/v1/orders | GET | R | 🟡 | checkout-journey.js ✅ measured |
| 24 | /api/v1/orders/{orderId} | GET | R | 🟡 | pending |
| 25 | /api/v1/orders/{orderId}/cancel | POST | W | 🟡 | pending |
| 26 | /api/v1/orders/restaurant/mine | GET | R | ❄️ | pending |
| 27 | /api/v1/orders/{orderId}/status | PATCH | W | 🟡 | pending (owner) |
| 28 | /api/v1/user/profile | GET | R | 🟡 | auth-reads.js ✅ measured |
| 29 | /api/v1/user/update/profile | PATCH | W | ❄️ | pending |
| 30 | /api/v1/user/delete/profile | DELETE | W | ❄️ | pending |
| 31 | /api/v1/admin/users | GET | R | ❄️ | pending |
| 32 | /api/v1/admin/orders | GET | R | ❄️ | pending (known N+1 candidate) |
| 33 | /api/v1/admin/restaurants | GET | R | ❄️ | pending |
| 34 | /api/v1/admin/users/{id}/status | PATCH | W | ❄️ | pending |

Hot paths fully measured; cold admin/owner paths pending (they are low-traffic; smoke suffices).

## Measured results

| # | Journey | Load | p95 | med | errors | checks | SQL per request |
|---|---|---|---|---|---|---|---|
| 1 | GET /restaurants (smoke) | 1 VU ×10 | 135.65ms* | 12.2ms | 0% | 100% | — |
| 2 | GET /restaurants (ramp) | 1→50 VUs | 8.94ms | 3.16ms | 0% | 100% | **1** ✅ |
| 3 | auth reads (cart+profile+orders) | 20 VUs ×60s | 21.69ms | 6.37ms | 0% | 100% | cart = **7** ✅ (3-item cart) |
| 4 | checkout journey | 20 VUs ×60s | 54.68ms | 14.16ms | 0% | 100% | placeOrder = pending count (predicted 8-10) |

### After Step-10 fixes (same measurement protocol, same data)

| Fix | Cart SQL (3 items) | Notes |
|---|---|---|
| baseline | 7 | 3× users + 1 cart + 3× menu_items (N+1) |
| #1 JOIN FETCH on cart ✅ | **4** | `menu_items` lines gone — one `cart_items join menu_items` query |
| #2 single user lookup ✅ | **2** | principal (`AuthUserDetails`) carries the user; `getUser()` reads it, no SQL |
| #3 OrderService: joined cart query + principal user ✅ | — | placeOrder: **16 → 9 statements**, selects 5 → 1 (same single checkout) |
| #4 addItemToCart single-load rewrite ✅ | **4** | add-to-cart: ~12 → **4 statements** (cart size-independent); one joined load serves existing-check + restaurant-check + response; update/remove responses moved to joined query too. Tests 48/48 green |
| #5 getMyOrders JOIN FETCH ✅ | **2** | GET /orders: **142 → 2 lines** (137-order user, same request). Was 1 + N queries (N = order count, unbounded — 544 ms at 137 orders, now 296 ms). Discovered by k6 finale re-run: history read scales with data. Known same-family spot: owner order lists (OrderService ~155), not yet fixed |
| #6 @Version on CartItem (Step 11) ✅ | — | correctness fix, not speed. Lost-update race demonstrated: 2 simultaneous PATCHes → old world: both 200, last write silently wins; new world: 30×200 + **10×409** "changed by another request", zero silent losses. Lesson: catch Spring's translated type (`org.springframework.dao.OptimisticLockingFailureException`), never the Hibernate-internal one. Schema lesson: adding NOT NULL column to non-empty table needs backfill (`ADD COLUMN ... NOT NULL DEFAULT 0`) — prod would use Flyway |
| #7 @Cacheable restaurants (Step 12) ✅ | **0** (after first) | GET /restaurants: 1 SQL → **1 on first call, 0 on every hit**. All 4 write paths `@CacheEvict(allEntries)` — verified live: write → wipe → next read = 1 SQL reprint. Cache DTOs not entities; never version a Boot starter (cache starter was fine, IDE just needed sync); @CachePut unsafe when return type ≠ cached type (list cache) → evict-on-write is the simple-correct default. Smoke: 10 reqs, 1 SQL total, p95 10.19ms |
| #8 pagination on GET /orders ✅ | **4** | my-orders paged: `Page<Order>` + JOIN FETCH restaurant (to-one = pagination-safe) + ONE items query for the page (`WHERE oi.order IN :10`) + count for metadata. **193-order user: was 142 lines + 137-order payload → now 4 lines/page + 10-order payload, constant forever.** `offset ? rows fetch first ? rows only` visible in SQL on page 1. Trap recorded: collection JOIN FETCH + Pageable = in-memory pagination (HHH90003004) — page first, fetch items for the page separately |

*cold start (first request after boot). Warm = row 2.
Volume row 3: 1,160 journeys. Volume row 4: 1,087 journeys (~1,087 orders), 5,520 requests.

## Final k6 comparison — Step 10 complete (2026-11-11, 20 VUs × 60s, identical scripts/settings)

| Journey | Metric | Baseline | After fixes | Change |
|---|---|---|---|---|
| auth-reads | p95 | 21.69ms | **17.01ms** | ↓ 22% |
| auth-reads | journeys / reqs | 1,160 | 1,180 | ↑ throughput |
| checkout-journey | p95 | 54.68ms | **42.02ms** | ↓ 23% |
| checkout-journey | avg | — | 16.08ms | (mid-fix broken run: 53.49ms) |
| checkout-journey | max | — | 186.43ms | (was 1,630ms during data-growth bug) |
| checkout-journey | journeys / reqs | 1,087 / 5,520 | 1,120 / 5,685 | ↑ throughput despite ~3× orders in DB |

Honest note: two intermediate runs (p95 292ms → 708ms) "degraded" between the fixes — that was NOT the fixes.
It was `GET /orders`' unbounded N+1 growing with order count, unmasked by data growth (see fix #5). After fix #5,
p95 is stable and BETTER than baseline. Lesson recorded: a list read whose cost grows with data is a time bomb;
measure re-runs at growing data scale, not just fixed scale.

## SQL shape summary (the durable metric)

| Endpoint | Before | After | Property |
|---|---|---|---|
| GET /cart (N-item cart) | 3 + N | **2** | constant |
| POST /cart/items | ~12 | **4** | constant |
| POST /orders | 16 | **9** | constant |
| GET /orders (N orders) | **N + 5 (unbounded!)** | **4 lines/page** (page + count + items + filter) · payload bounded to page size | constant |

## SQL forensics (proven by show-sql, single requests)

`GET /cart` with N=3 cart items = **7 SQL**, fully accounted:

```
JwtAuthFilter:            2 × users-by-email   (loadUserByUsername + direct findByEmail = redundant duplicate)
CartService.getUser():    1 × users-by-email   (3rd lookup of the same user)
cart_items list:          1 × cart_items
menu_items (lazy N+1):    3 × menu_items       (one identical query per cart row)
```

Formula: `3 + N` for N cart items. Login adds `2 × users` (authenticationManager → UserDetailsService, then AuthService re-fetches at line 105).

## Known hotspots — status after Step 10

1. ~~**Triple user lookup per request**~~ ✅ fixed (#2) — filter loads once, principal carries it
2. ~~**Cart N+1**~~ ✅ fixed (#1, #4) — `findCartWithMenuItems` JOIN FETCH, cart = 2 SQL
3. ~~**placeOrder lazy loop**~~ ✅ fixed (#3) — joined query, checkout = 9 statements
4. ~~**History list N+1**~~ ✅ fixed (#5) — `getMyOrders` JOIN FETCH (discovered by finale re-run)
5. ~~**Cart lost-update race**~~ ✅ fixed (Step 11, fix #6) — `@Version` optimistic locking + 409 handler, race demonstrated with cart-race.js
6. ~~**No pagination on list reads**~~ ✅ fixed (fix #8) — GET /orders paged (PageResponse envelope); owner/admin lists remain (cold paths)
7. **Owner order lists** (OrderService ~155) + admin/owner endpoints — same N+1 family, cold paths, pending smoke/measure

## Caveats / honesty

- Numbers valid for THIS data scale (local DB, small tables). SQL *shape* (queries/request) is the durable metric.
- Data grows every run: ~1,087 orders added by run 3 — re-runs measure a slightly bigger world.
- `http_req_failed` counts 4xx as failure → 409-on-register from non-idempotent setup fails thresholds; scripts were made idempotent (login-first, stable identities).
- Laptop ceiling: ~50 req/s per VU pacing (sleep 1s think time) — realistic-user model, not machine limit.
