#!/bin/sh
echo "=== 1. Login as adm_1 (PRODUCT_ADMIN + USER + EDITOR) ==="
RESP=$(curl -s -X POST "http://localhost:9000/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=adm_1&password=adm_1&client_id=public-client")
echo "Response: $RESP"
echo ""
TOKEN=$(echo "$RESP" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
echo "Token obtained: ${TOKEN:0:50}..."
echo ""

echo "=== 2. List products (GET /api/products) - requires USER role ==="
curl -s -X GET "http://product:8082/api/products" -H "Authorization: Bearer $TOKEN"
echo ""
echo ""

echo "=== 3. Add product (POST /api/products) - requires EDITOR role ==="
curl -s -X POST "http://product:8082/api/products" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"Name":"Test Laptop"}'
echo ""
echo ""

echo "=== 4. List products again ==="
curl -s -X GET "http://product:8082/api/products" -H "Authorization: Bearer $TOKEN"
echo ""
echo ""

echo "=== 5. Test USER role (user_1) - should NOT add (403 Forbidden) ==="
RESP2=$(curl -s -X POST "http://localhost:9000/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user_1&password=user_1&client_id=public-client")
TOKEN2=$(echo "$RESP2" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
echo "user_1 token: ${TOKEN2:0:50}..."
curl -s -X POST "http://product:8082/api/products" \
  -H "Authorization: Bearer $TOKEN2" \
  -H "Content-Type: application/json" \
  -d '{"Name":"Should Fail"}'
echo ""
echo ""

echo "=== 6. Test LDAP user (ldap_editor_1) - should add (EDITOR role) ==="
RESP3=$(curl -s -X POST "http://localhost:9000/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=ldap_editor_1&password=ldap_editor_1&client_id=public-client")
TOKEN3=$(echo "$RESP3" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
echo "ldap_editor_1 token: ${TOKEN3:0:50}..."
curl -s -X POST "http://product:8082/api/products" \
  -H "Authorization: Bearer $TOKEN3" \
  -H "Content-Type: application/json" \
  -d '{"Name":"Added by LDAP Editor"}'
echo ""
echo ""

echo "=== All tests complete ==="
