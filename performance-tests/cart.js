import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export const options = {
    vus: 1,
    iterations: 3,
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
        checks: ['rate>0.99']
    },
};

export function setup() {

    const email = `pref_${Date.now()}@test.com`;
    const phone = '9' + String(Date.now()).slice(-9);
    const password = 'Pref@1234';

    const registerRes = http.post(
        `${BASE_URL}/api/v1/auth/customer/register`,
        JSON.stringify({name: 'Pref User', email, password, phone}),
        {
            headers: {'Content-Type': 'application/json'},
            tags: {endpoint: 'auth-register', phase: 'setup'}
        }
    );

    if(registerRes.status !== 201) {
        throw new Error(`Register failed: ${registerRes.status} ${registerRes.body}`);
    }
    const token = registerRes.json().token;


    const restaurantsRes = http.get(`${BASE_URL}/api/v1/restaurants`, {
        tags: {endpoint: 'restaurants', phase: 'setup'},
    });

    if(restaurantsRes.status !== 200) {
        throw new Error(`Restaurants request failed: ${restaurantsRes.status}`);
    }
    const restaurants = restaurantsRes.json();
    if(!Array.isArray(restaurants) || restaurants.length === 0) {
        throw new Error('No restaurants found');
    }

    const restaurantId = restaurants[0].id;

    const menuRes = http.get(`${BASE_URL}/api/v1/restaurants/${restaurantId}/menu`, {
        tags: {endpoint: 'menu', phase: 'setup'}
    });

    if(menuRes.status !== 200){
        throw new Error('Menu not available');
    }
    const menu = menuRes.json();

    if(!Array.isArray(menu)) {
        throw new Error('Menu not available');
    }

    const availableItem = menu.find((item) => item.available === true);

    if(!availableItem) {
        throw new Error("No available menu item found");
    }

    return {token, menuItemId: availableItem.id};
}

export default function(data) {
    const authHeader = {Authorization: `Bearer ${data.token}`};


    const getRes = http.get(`${BASE_URL}/api/v1/cart`, {
        headers: authHeader,
        tags: {endpoint: 'cart-get'},
    });

    check(getRes, {
        'GET cart status is 200': (r) => r.status === 200,
        'GET cart has items array': (r) => Array.isArray(r.json().items),
    });

    const addRes = http.post(
        `${BASE_URL}/api/v1/cart/items`,
        JSON.stringify({menuItemId: data.menuItemId, quantity: 1}),
        {
            headers: {
                Authorization: `Bearer ${data.token}`,
                'Content-Type': 'application/json',
            },
            tags: {endpoint: 'cart-add'},
        }
    );
    check(addRes, {
        'POST cart item status is 201': (r) => r.status === 201,
        'POST has at least one item': (r) => r.status === 201 && r.json().items.length >= 1,
    });

    const clearRes = http.del(`${BASE_URL}/api/v1/cart/items`, null, {
        headers: authHeader,
        tags: {endpoint: 'cart-clear'},
    });

    check(clearRes, {
        'DELETE cart status is 204': (r) => r.status === 204,
    });
    sleep(1);
}