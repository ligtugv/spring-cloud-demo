import requests

# Get token
login_resp = requests.post(
    "http://localhost:9000/auth/login",
    data={"username": "adm_1", "password": "adm_1", "client_id": "public-client"},
    headers={"Content-Type": "application/x-www-form-urlencoded"}
)
token = login_resp.json()["access_token"]
print(f"Token: {token[:50]}...")
print(f"Roles: {login_resp.json()['roles']}")
print()

# List products
r = requests.get("http://product:8082/api/products", headers={"Authorization": f"Bearer {token}"})
print(f"List products: {r.status_code} - {r.text}")
print()

# Add product
r = requests.post(
    "http://product:8082/api/products",
    json={"name": "Test Laptop"},
    headers={"Authorization": f"Bearer {token}"}
)
print(f"Add product: {r.status_code} - {r.text}")
print()

# List again
r = requests.get("http://product:8082/api/products", headers={"Authorization": f"Bearer {token}"})
print(f"List again: {r.status_code} - {r.text}")
