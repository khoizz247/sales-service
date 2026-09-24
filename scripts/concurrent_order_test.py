"""Destructive smoke test for the disposable MySQL database created by CI only."""

import json
import sys
import uuid
from concurrent.futures import ThreadPoolExecutor
from threading import Barrier
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE_URL = "http://localhost:8080"


def call(method, path, body=None, token=None):
    headers = {"Accept": "application/json"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if token is not None:
        headers["Authorization"] = "Bearer " + token
    request = Request(
        BASE_URL + path,
        data=json.dumps(body).encode("utf-8") if body is not None else None,
        headers=headers,
        method=method,
    )
    try:
        with urlopen(request, timeout=30) as response:
            return response.status, json.load(response)
    except HTTPError as error:
        return error.code, json.load(error)


def main():
    status, catalog = call("GET", "/api/products?page=0&size=20")
    if status != 200:
        raise AssertionError(f"Catalog unavailable: {status}, {catalog}")
    product = next((item for item in catalog["items"] if item["stockQuantity"] > 0), None)
    if product is None:
        raise AssertionError("Disposable database has no stocked product")
    product_id = product["id"]
    stock_before = product["stockQuantity"]

    tokens = []
    for _ in range(2):
        email = "ci-" + uuid.uuid4().hex + "@example.com"
        status, registered = call("POST", "/api/auth/register", {
            "fullName": "CI concurrency customer",
            "email": email,
            "password": "Test@12345",
        })
        if status != 201:
            raise AssertionError(f"Registration failed: {status}, {registered}")
        tokens.append(registered["accessToken"])

    order = {
        "recipientName": "CI customer",
        "recipientPhone": "0901234567",
        "shippingAddress": "CI disposable database",
        "items": [{"productId": product_id, "quantity": stock_before}],
    }
    barrier = Barrier(2)

    def purchase(token):
        barrier.wait(timeout=10)
        return call("POST", "/api/orders", order, token)

    with ThreadPoolExecutor(max_workers=2) as pool:
        futures = [pool.submit(purchase, token) for token in tokens]
        results = [future.result(timeout=40) for future in futures]

    statuses = sorted(status for status, _ in results)
    if statuses != [201, 409]:
        raise AssertionError(f"Expected one 201 and one 409, got {results}")
    status, remaining = call("GET", f"/api/products/{product_id}")
    if status != 200 or remaining["stockQuantity"] != 0:
        raise AssertionError(f"Unexpected remaining stock: {status}, {remaining}")
    print(f"MySQL concurrency PASS: product {product_id}, initial stock {stock_before}, "
          "one order created, one rejected, remaining stock 0")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"MySQL concurrency FAIL: {error}", file=sys.stderr)
        raise
