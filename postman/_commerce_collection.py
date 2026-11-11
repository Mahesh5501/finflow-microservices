"""Current Commerce HTTP contracts and ordered, repeatable Postman scenarios.

Imported by _gen_extra_collections; it performs no writes or HTTP calls itself.
Keep fixture IDs collection-scoped so an Identity/Wallet environment cannot
silently replace a freshly captured Commerce order or product ID.
"""
from __future__ import annotations

import json

SCHEMA = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
BASE = "{{commerceBaseUrl}}/api/v1"
SHIPPING = {
    "line1": "1 FinFlow Test Road", "line2": None, "city": "Hyderabad",
    "state": "Telangana", "postalCode": "500001", "country": "IN",
}

PRE_REQUEST = """// Local mode never needs a token; integrated mode uses the shared Identity environment.
const mode = String(pm.variables.get('commerceMode') || 'local').toLowerCase();
const provider = String(pm.variables.get('commercePaymentProvider') || 'simulated').toLowerCase();
if (!['local', 'integrated'].includes(mode)) throw new Error('commerceMode must be local or integrated');
if (!['simulated', 'wallet'].includes(provider)) throw new Error('commercePaymentProvider must match the server: simulated or wallet');
if (mode === 'local' && provider !== 'simulated') throw new Error('Local standalone mode requires the simulated provider');
pm.variables.set('commerceMode', mode);
pm.variables.set('commercePaymentProvider', provider);
pm.request.headers.remove('Authorization');
let orgId, userId;
if (mode === 'local') {
    orgId = pm.variables.get('commerceLocalOrgId') || '10';
    userId = pm.variables.get('commerceLocalUserId') || '1';
} else {
    orgId = pm.variables.get('orgId');
    userId = pm.variables.get('userId');
    const token = pm.variables.get('accessToken');
    if (!token) throw new Error('Integrated mode requires accessToken from Identity login');
    pm.request.headers.upsert({key: 'Authorization', value: 'Bearer ' + token});
}
if (!/^[1-9][0-9]*$/.test(String(orgId))) throw new Error('Set a positive orgId (integrated) or commerceLocalOrgId (local)');
if (!/^[1-9][0-9]*$/.test(String(userId))) throw new Error('Set userId from Identity login or commerceLocalUserId for local mode');
// Request-local overrides avoid changing the shared Identity/Wallet environment.
pm.variables.set('orgId', String(orgId));
pm.variables.set('commerceExpectedUserId', String(userId));
"""

RESPONSE_PRINCIPAL = """if (pm.response.code >= 200 && pm.response.code < 300) {
    const result = pm.response.json();
    if (result && Object.prototype.hasOwnProperty.call(result, 'customerUserId')) {
        pm.test('Response belongs to the configured customer', function () {
            pm.expect(result.customerUserId).to.eql(Number(pm.variables.get('commerceExpectedUserId')));
        });
    }
}
"""


def event(kind: str, script: str) -> dict:
    return {"listen": kind, "script": {"type": "text/javascript", "exec": script.strip().splitlines()}}


def request(name, method, path, *, body=None, checks="", status=200,
            save=None, requires=(), key=None, fresh_key=False, before=""):
    """Build one HTTP request with visible success/error expectations and assertions."""
    url = path if path.startswith("{{commerceBaseUrl}}") else BASE + path
    headers = []
    req = {"method": method, "header": headers, "url": url}
    if body is not None:
        raw = json.dumps(body, indent=2)
        for variable in ("orgId", "commerceProductId", "commerceCategoryId"):
            raw = raw.replace('"{{' + variable + '}}"', '{{' + variable + '}}')
        req["body"] = {"mode": "raw", "raw": raw, "options": {"raw": {"language": "json"}}}
        headers.append({"key": "Content-Type", "value": "application/json"})
    prerequisites = list(requires)
    if key:
        headers.append({"key": "Idempotency-Key", "value": "{{" + key + "}}"})
        if fresh_key:
            before += "\npm.collectionVariables.set('" + key + "', 'commerce-' + pm.variables.replaceIn('{{$guid}}'));"
        else:
            prerequisites.append(key)
    if prerequisites:
        before += "\n" + "\n".join(
            f"if (!pm.variables.get('{variable}')) throw new Error('Missing {variable}; set it in collection Variables or run its create request first');"
            for variable in prerequisites
        )
    codes = list(status) if isinstance(status, tuple) else [status]
    tests = (
        f"pm.test('HTTP status {codes}', function () {{ pm.expect(pm.response.code).to.be.oneOf({json.dumps(codes)}); }});\n"
        "pm.test('JSON response', function () { pm.expect(pm.response.headers.get('Content-Type')).to.match(/json/i); });\n"
        "const body = pm.response.json();\n" + checks
    )
    if save:
        field, variable = save
        tests += f"""
pm.test('Capture valid {field}', function () {{
    pm.expect(body.{field}).to.be.a('number').above(0);
}});
if ({json.dumps(codes)}.includes(pm.response.code) && Number.isInteger(body.{field}) && body.{field} > 0) {{
    pm.collectionVariables.set('{variable}', String(body.{field}));
}}
"""
    events = ([event("prerequest", before)] if before.strip() else []) + [event("test", tests)]
    return {"name": name, "request": req, "event": events}


def group(name, items, description, *, before=""):
    result = {"name": name, "description": description, "item": items}
    if before:
        result["event"] = [event("prerequest", before)]
    return result


def order_body(outcome=None):
    return {"items": [{"productId": "{{commerceProductId}}", "quantity": 1}],
            "shippingAddress": SHIPPING, "couponCode": None, "paymentOutcome": outcome}


def order_checks(state="CONFIRMED", total=100):
    return f"""pm.test('Order state, price and snapshots', function () {{
    pm.expect(body.status).to.eql('{state}');
    pm.expect(body.currencyCode).to.eql('INR');
    pm.expect(body.organisationId).to.eql(Number(pm.variables.get('orgId')));
    pm.expect(Number(body.totalAmount)).to.eql({total});
    pm.expect(body.items).to.be.an('array').that.is.not.empty;
    pm.expect(body.items[0].productId).to.eql(Number(pm.variables.get('commerceProductId')));
}});
""" + ("""pm.test('Payment references match the configured provider', function () {
    if (pm.variables.get('commercePaymentProvider') === 'simulated') {
        pm.expect(body.paymentWalletId).to.eql(null);
        pm.expect(body.paymentTransactionId).to.eql(null);
    } else {
        pm.expect(body.paymentWalletId).to.be.a('number').above(0);
        pm.expect(body.paymentTransactionId).to.be.a('number').above(0);
    }
});
""" if state == "CONFIRMED" else "")


def create_order(name, variable, *, outcome=None):
    return request(name, "POST", "/customer/orders", body=order_body(outcome), status=201,
                   key=variable + "Key", fresh_key=True, requires=("commerceProductId",),
                   save=("orderId", variable),
                   checks=order_checks({"UNCERTAIN": "ACTION_REQUIRED", "DECLINED": "PAYMENT_FAILED"}.get(outcome, "CONFIRMED")))


def state_check(state):
    return f"pm.test('State is {state}', function () {{ pm.expect(body.status).to.eql('{state}'); }});\n"


def error_check(code):
    return f"pm.test('Business error is {code}', function () {{ pm.expect(body.code).to.eql('{code}'); }});\n"


def return_request(name, order_variable, return_variable):
    return request(name, "POST", "/customer/orders/{{" + order_variable + "}}/returns",
                   body={"reason": "Damaged on arrival"}, status=201, requires=(order_variable,),
                   save=("returnId", return_variable), checks=state_check("REQUESTED"))


def commerce_collection() -> dict:
    fixture_reset = """// A new fixture run gets unique slugs, codes and keys; replay requests keep their original key.
const run = pm.variables.replaceIn('{{$guid}}').replace(/-/g, '');
pm.collectionVariables.set('commerceRunId', run);
pm.collectionVariables.set('commerceCouponCode', 'SAVE' + run.toUpperCase());
for (const key of ['commerceCategoryId', 'commerceProductId', 'commerceOrderId', 'commerceCheckoutOrderId',
    'commerceSelectedSellerOrderId', 'commerceCancelOrderId', 'commerceApprovedReturnId',
    'commerceRejectedOrderId', 'commerceRejectedReturnId', 'commerceUncertainOrderId', 'commerceDeclinedOrderId']) {
    pm.collectionVariables.set(key, '');
}
"""
    cart_item = {"productId": "{{commerceProductId}}", "quantity": 1}
    checkout = {"shippingAddress": SHIPPING, "couponCode": "{{commerceCouponCode}}", "paymentOutcome": None}
    cart_checks = "pm.test('Cart contains an items array', function () { pm.expect(body.items).to.be.an('array'); });\n"
    product_checks = """pm.test('Product response matches the fixture', function () {
    pm.expect(body.organisationId).to.eql(Number(pm.variables.get('orgId')));
    pm.expect(body.status).to.eql('ACTIVE');
    pm.expect(body.categoryId).to.eql(Number(pm.variables.get('commerceCategoryId')));
});
"""
    health = group("00 Health", [
        request("Actuator health (UP or reported DOWN)", "GET", "{{commerceBaseUrl}}/actuator/health",
                status=(200, 503), checks="pm.test('Health reports a state', function () { pm.expect(body.status).to.be.a('string'); });"),
        request("Actuator info", "GET", "{{commerceBaseUrl}}/actuator/info",
                checks="pm.test('Info is an object', function () { pm.expect(body).to.be.an('object'); });"),
        request("Dependency health (inspect DOWN details)", "GET", "/health/dependencies", status=(200, 503), checks="""
pm.test('Dependency report and HTTP status agree', function () {
    pm.expect(body.service).to.eql('commerce-service');
    pm.expect(body.checks).to.be.an('array');
    pm.expect(body.checks.map(c => c.name)).to.include.members(['database', 'redis', 'identity-jwks', 'wallet']);
    const down = body.checks.some(c => c.status === 'DOWN');
    pm.expect(body.status).to.eql(down ? 'DOWN' : 'UP');
    pm.expect(pm.response.code).to.eql(down ? 503 : 200);
});
if (pm.variables.get('commerceMode') === 'local') {
    pm.test('Local mode skips Identity and Wallet', function () {
        pm.expect(body.checks.find(c => c.name === 'identity-jwks').status).to.eql('SKIPPED');
        pm.expect(body.checks.find(c => c.name === 'wallet').status).to.eql('SKIPPED');
    });
}
"""),
    ], "No dependencies are fabricated as UP. Dependency 503/DOWN is a valid report; inspect its checks before testing affected operations.")
    fixtures = group("01 Catalogue and coupon fixtures", [
        request("Create unique category", "POST", "/merchant/categories", status=201, before=fixture_reset,
                body={"organisationId": "{{orgId}}", "name": "Postman Electronics", "slug": "electronics-{{commerceRunId}}"},
                checks="pm.test('Category belongs to the seller', function () { pm.expect(body.organisationId).to.eql(Number(pm.variables.get('orgId'))); });",
                save=("categoryId", "commerceCategoryId")),
        request("List marketplace categories", "GET", "/customer/categories", checks="""
pm.test('Categories response is an array', function () {
    pm.expect(body).to.be.an('array');
});
const categoryId = pm.variables.get('commerceCategoryId');
if (categoryId) {
    pm.test('Created category is listed', function () {
        pm.expect(body.some(c => c.categoryId === Number(categoryId))).to.eql(true);
    });
}"""),
        request("Create product", "POST", "/merchant/products", status=201, requires=("commerceCategoryId",),
                body={"organisationId": "{{orgId}}", "name": "Postman Laptop {{commerceRunId}}", "unitPrice": "120.00",
                      "availableQuantity": 100, "categoryId": "{{commerceCategoryId}}"},
                save=("productId", "commerceProductId"), checks=product_checks),
        request("Get marketplace product", "GET", "/customer/products/{{commerceProductId}}",
                requires=("commerceProductId",), checks=product_checks),
        request("Search by name and category with pagination", "GET",
                "/customer/products/search?q=Laptop&categoryId={{commerceCategoryId}}&page=0&size=20",
                before="if (!pm.variables.get('commerceCategoryId')) pm.request.url.query.remove('categoryId');", checks="""
pm.test('Search returns a valid page, including empty results', function () {
    pm.expect(body.content).to.be.an('array');
    pm.expect(body.totalElements).to.be.at.least(0);
});
const productId = pm.variables.get('commerceProductId');
if (productId) {
    pm.test('Paged search contains the created product', function () {
        pm.expect(body.content.some(p => p.productId === Number(productId))).to.eql(true);
        pm.expect(body.totalElements).to.be.at.least(1);
    });
}"""),
        request("Update price and stock", "PATCH", "/merchant/products/{{commerceProductId}}",
                requires=("commerceProductId",), body={"unitPrice": "100.00", "availableQuantity": 100},
                checks=product_checks + "pm.test('Updated price and stock', function () { pm.expect(Number(body.unitPrice)).to.eql(100); pm.expect(body.availableQuantity).to.eql(100); });"),
        request("Create unique 10 percent coupon", "POST", "/merchant/coupons", status=201,
                body={"organisationId": "{{orgId}}", "code": "{{commerceCouponCode}}", "percentOff": "10.00", "expiresAt": None},
                before="if (!pm.variables.get('commerceCouponCode')) pm.collectionVariables.set('commerceCouponCode', 'SAVE' + pm.variables.replaceIn('{{$guid}}').replace(/-/g, '').toUpperCase());", checks="""
pm.test('Coupon is active and correctly priced', function () {
    pm.expect(body.code).to.eql(pm.variables.get('commerceCouponCode'));
    pm.expect(Number(body.percentOff)).to.eql(10);
    pm.expect(body.active).to.eql(true);
});"""),
    ], "Run first. Unique fixtures are created on each run; nothing is deleted from catalogue. Later scenarios share this product, not order/return state.")
    cart = group("02 Cart CRUD and coupon checkout", [
        request("Clear test customer's cart", "DELETE", "/customer/carts", checks=cart_checks),
        request("Get merged cart", "GET", "/customer/carts", checks=cart_checks),
        request("Add product", "POST", "/customer/carts/items", body=cart_item, requires=("commerceProductId",), checks=cart_checks),
        request("Replace quantity", "PUT", "/customer/carts/items/{{commerceProductId}}", body={"quantity": 2},
                requires=("commerceProductId",), checks=cart_checks + "pm.test('Quantity replaced', function () { pm.expect(body.items.find(i => i.productId === Number(pm.variables.get('commerceProductId'))).quantity).to.eql(2); });"),
        request("Remove product", "DELETE", "/customer/carts/items/{{commerceProductId}}", requires=("commerceProductId",),
                checks=cart_checks + "pm.test('Removed line is absent', function () { pm.expect(body.items.some(i => i.productId === Number(pm.variables.get('commerceProductId')))).to.eql(false); });"),
        request("Add two units for checkout", "POST", "/customer/carts/items", body={**cart_item, "quantity": 2},
                requires=("commerceProductId",), checks=cart_checks),
        request("Checkout with coupon and automatic seller", "POST", "/customer/carts/checkout", status=201, body=checkout,
                requires=("commerceProductId", "commerceCouponCode"), key="commerceCheckoutKey", fresh_key=True,
                save=("orderId", "commerceCheckoutOrderId"), checks=order_checks(total=180) + "pm.test('Coupon discount applied', function () { pm.expect(Number(body.discountAmount)).to.eql(20); });"),
        request("Purchased seller cart is empty", "GET", "/customer/carts", checks=cart_checks + "pm.test('Cart cleared after checkout', function () { pm.expect(body.items).to.have.lengthOf(0); });"),
    ], "Clears this test customer's cart. Exercises live-price cart DTOs, zero-independent remove, automatic seller inference and coupon pricing.")
    selected_seller = group("03 Explicit seller checkout", [
        request("Add one product for selected seller", "POST", "/customer/carts/items", body=cart_item,
                requires=("commerceProductId",), checks=cart_checks),
        request("Checkout selected seller", "POST", "/customer/carts/checkout?sellerOrganisationId={{orgId}}",
                body={**checkout, "couponCode": None}, status=201, key="commerceSelectedSellerKey", fresh_key=True,
                requires=("commerceProductId",), save=("orderId", "commerceSelectedSellerOrderId"), checks=order_checks()),
    ], "Exercises the existing sellerOrganisationId query parameter with this run's seller. It is also used for multi-seller carts; this fixture does not invent a second merchant membership.")
    orders = group("04 Direct order and replay", [
        create_order("Create confirmed order", "commerceOrderId"),
        request("Replay exact order with same key", "POST", "/customer/orders", body=order_body(), status=201,
                requires=("commerceOrderId",), key="commerceOrderIdKey", checks=order_checks() + "pm.test('Replay returns original order', function () { pm.expect(body.orderId).to.eql(Number(pm.variables.get('commerceOrderId'))); });"),
        request("List own customer orders", "GET", "/customer/orders", checks="""
pm.test('Orders response is an array', function () { pm.expect(body).to.be.an('array'); });
const orderId = pm.variables.get('commerceOrderId');
if (orderId) {
    pm.test('Own order is listed', function () {
        pm.expect(body.some(o => o.orderId === Number(orderId))).to.eql(true);
    });
}"""),
        request("Get own order", "GET", "/customer/orders/{{commerceOrderId}}", requires=("commerceOrderId",), checks=order_checks()),
        request("List merchant organisation orders", "GET", "/merchant/orders?organisationId={{orgId}}&mineOnly=false", checks="pm.test('Merchant list is an array', function () { pm.expect(body).to.be.an('array'); });"),
        request("List merchant customer's orders", "GET", "/merchant/orders?organisationId={{orgId}}&mineOnly=true", checks="pm.test('Filtered merchant list is an array', function () { pm.expect(body).to.be.an('array'); });"),
        request("Get merchant-visible order", "GET", "/merchant/orders/{{commerceOrderId}}", requires=("commerceOrderId",), checks=order_checks()),
        request("Paying confirmed order is rejected [409]", "POST", "/customer/orders/{{commerceOrderId}}/pay",
                requires=("commerceOrderId",), status=409, checks=error_check("PAY_NOT_ALLOWED")),
    ], "Replay deliberately keeps its original payload/key. The confirmed order is reserved for fulfilment/approved return, never cancellation.")
    cancellation = group("05 Cancellation on a separate order", [
        create_order("Create cancellation order", "commerceCancelOrderId"),
        request("Cancel confirmed order and refund", "POST", "/customer/orders/{{commerceCancelOrderId}}/cancel",
                requires=("commerceCancelOrderId",), checks=state_check("CANCELLED")),
        request("Cancelled order remains cancelled", "GET", "/customer/orders/{{commerceCancelOrderId}}",
                requires=("commerceCancelOrderId",), checks=state_check("CANCELLED")),
    ], "Uses a separate order ID; cancellation cannot invalidate the fulfilment/return scenario.")
    returns = group("06 Fulfilment and approved return", [
        request("Start fulfilment", "POST", "/merchant/orders/{{commerceOrderId}}/fulfilment/start",
                requires=("commerceOrderId",), checks=state_check("FULFILMENT_PENDING")),
        request("Complete fulfilment", "POST", "/merchant/orders/{{commerceOrderId}}/fulfilment/complete",
                requires=("commerceOrderId",), checks=state_check("FULFILLED")),
        return_request("Customer requests return", "commerceOrderId", "commerceApprovedReturnId"),
        request("List order returns", "GET", "/customer/orders/{{commerceOrderId}}/returns", requires=("commerceOrderId",), checks="""
pm.test('Returns response is an array', function () { pm.expect(body).to.be.an('array'); });
const returnId = pm.variables.get('commerceApprovedReturnId');
if (returnId) {
    pm.test('Created return is listed', function () {
        pm.expect(body.some(r => r.returnId === Number(returnId))).to.eql(true);
    });
}"""),
        request("Merchant approves return", "POST", "/merchant/returns/{{commerceApprovedReturnId}}/approve",
                requires=("commerceApprovedReturnId",), checks=state_check("APPROVED")),
        request("Complete approved return and refund", "POST", "/merchant/returns/{{commerceApprovedReturnId}}/complete",
                requires=("commerceApprovedReturnId",), checks=state_check("COMPLETED")),
        request("Repeated completion is rejected [409]", "POST", "/merchant/returns/{{commerceApprovedReturnId}}/complete",
                requires=("commerceApprovedReturnId",), status=409, checks=error_check("RETURN_STATE")),
    ], "Run after 04. Uses the confirmed direct order; approval/refund never shares a return ID with rejection. Wallet mode requires a caller authorized to refund the original customer's wallet.")
    rejection = group("07 Rejected return on a separate order", [
        create_order("Create rejection scenario order", "commerceRejectedOrderId"),
        request("Fulfil rejection scenario order", "POST", "/merchant/orders/{{commerceRejectedOrderId}}/fulfilment/complete",
                requires=("commerceRejectedOrderId",), checks=state_check("FULFILLED")),
        return_request("Request separate return", "commerceRejectedOrderId", "commerceRejectedReturnId"),
        request("Reject separate return", "POST", "/merchant/returns/{{commerceRejectedReturnId}}/reject",
                requires=("commerceRejectedReturnId",), checks=state_check("REJECTED")),
        request("Rejected return cannot complete [409]", "POST", "/merchant/returns/{{commerceRejectedReturnId}}/complete",
                requires=("commerceRejectedReturnId",), status=409, checks=error_check("RETURN_STATE")),
    ], "Creates its own order/return. The documented direct CONFIRMED -> FULFILLED transition is accepted by the current implementation.")
    outcomes = group("08 Simulated uncertain and declined payments", [
        create_order("Create uncertain payment", "commerceUncertainOrderId", outcome="UNCERTAIN"),
        request("Retry remains uncertain without state-transition error", "POST", "/customer/orders/{{commerceUncertainOrderId}}/pay",
                requires=("commerceUncertainOrderId",), checks=order_checks("ACTION_REQUIRED")),
        request("Cancel uncertain reservation", "POST", "/customer/orders/{{commerceUncertainOrderId}}/cancel",
                requires=("commerceUncertainOrderId",), checks=state_check("CANCELLED")),
        create_order("Create declined payment", "commerceDeclinedOrderId", outcome="DECLINED"),
        request("Declined order cannot be paid [409]", "POST", "/customer/orders/{{commerceDeclinedOrderId}}/pay",
                requires=("commerceDeclinedOrderId",), status=409, checks=error_check("PAY_NOT_ALLOWED")),
    ], "Simulation only: wallet ignores sandbox outcomes. Requests are skipped before sending when commercePaymentProvider=wallet.", before="""
if (String(pm.variables.get('commercePaymentProvider')).toLowerCase() !== 'simulated') {
    console.info('Skipping simulated payment scenario for Wallet provider');
    pm.execution.skipRequest();
}
""")
    variables = {
        "commerceBaseUrl": "http://127.0.0.1:18083", "commerceMode": "local",
        "commercePaymentProvider": "simulated", "commerceLocalUserId": "1", "commerceLocalOrgId": "10",
        "accessToken": "", "userId": "", "orgId": "",
    }
    # Declare generated values too: Postman's Variables tab must show every ID/key
    # before any request has run. Empty means "not created yet", never a fake ID.
    generated = (
        "commerceRunId", "commerceCouponCode", "commerceExpectedUserId",
        "commerceCategoryId", "commerceProductId", "commerceOrderId",
        "commerceCheckoutOrderId", "commerceSelectedSellerOrderId", "commerceCancelOrderId",
        "commerceApprovedReturnId", "commerceRejectedOrderId", "commerceRejectedReturnId",
        "commerceUncertainOrderId", "commerceDeclinedOrderId", "commerceCheckoutKey",
        "commerceSelectedSellerKey", "commerceOrderIdKey", "commerceCancelOrderIdKey",
        "commerceRejectedOrderIdKey", "commerceUncertainOrderIdKey", "commerceDeclinedOrderIdKey",
    )
    variables.update({key: "" for key in generated})
    descriptions = {
        "commerceBaseUrl": "Address of the running Commerce service.",
        "commerceMode": "local: no token; integrated: use Identity credentials.",
        "commercePaymentProvider": "simulated or wallet; must match the server configuration.",
        "commerceLocalUserId": "Fixed local caller; defaults to server test user 1.",
        "commerceLocalOrgId": "Fixed local organisation; defaults to server test organisation 10.",
        "accessToken": "Integrated mode only. Leave empty for local mode.",
        "userId": "Integrated mode only. Local caller comes from commerceLocalUserId.",
        "orgId": "Integrated mode only. In local mode this is derived per request from commerceLocalOrgId.",
    }
    for key in generated:
        descriptions[key] = (
            "Generated automatically when sending a new order/checkout. Replay keeps its original key."
            if key.endswith('Key') else
            "Captured/generated automatically by setup requests. For an individual request, enter the relevant existing ID/value here."
        )
    descriptions['commerceRunId'] = 'Generated by Create unique category to keep fixtures unique between runs.'
    descriptions['commerceCouponCode'] = 'Generated by setup or Create unique coupon; enter a code here to use an existing coupon.'
    descriptions['commerceExpectedUserId'] = 'Derived per request from the local fixture or integrated userId; no manual entry needed.'
    return {
        "info": {"_postman_id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                 "name": "FinFlow - Commerce Service",
                 "schema": SCHEMA,
                 "description": "Current customer/merchant APIs. Run folders 00-08 in order with a dedicated test customer. "
                 "Standalone: start Commerce with local profile, select No Environment in Postman, and use this collection's Variables; "
                 "user 1/org 10, no Identity/Wallet/token, simulated payments. MySQL/Redis remain normal manual-run dependencies. "
                 "Integrated: select FinFlow Local environment, set commerceMode=integrated and commercePaymentProvider=wallet "
                 "(or simulated to match server), then supply accessToken/userId/orgId from Identity and fund Wallet if used. "
                 "Setup creates unique catalogue/coupon fixtures; Cart clears the test customer's cart. "
                 "IDs stay Commerce collection-scoped. Fresh keys are generated for new orders; replay retains its key. "
                 "Expected 409 cases are explicit; health can report 503/DOWN without violating its contract. "
                 "Only simulated outcome cases are skipped in wallet mode. No automatic production-service startup."},
        "auth": {"type": "noauth"},
        "variable": [{"key": key, "value": value, "description": descriptions[key]} for key, value in variables.items()],
        "event": [event("prerequest", PRE_REQUEST), event("test", RESPONSE_PRINCIPAL)],
        "item": [health, fixtures, cart, selected_seller, orders, cancellation, returns, rejection, outcomes],
    }
