#!/bin/bash
# Test script - fetches a fresh token automatically
# Usage: ./curl-test.sh <username> <password>
# Example: ./curl-test.sh adm_1 adm_1

USERNAME="${1:-adm_1}"
PASSWORD="${2:-adm_1}"
CLIENT_ID="public-client"

echo "=== Fetching token for user: $USERNAME ==="
TOKEN_RESPONSE=$(curl -s -X POST "http://localhost:7573/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${USERNAME}&password=${PASSWORD}&client_id=${CLIENT_ID}")

TOKEN=$(echo $TOKEN_RESPONSE | grep -o '"access_token":"[^"]*' | sed 's/"access_token":"//')

if [ -z "$TOKEN" ]; then
  echo "FAILED: Could not get token. Response:"
  echo $TOKEN_RESPONSE
  exit 1
fi

echo "Token obtained successfully."
echo ""

echo "=== List products ==="
curl -s -X GET "http://localhost:7573/product/api/products" \
  -H "Authorization: Bearer $TOKEN"
echo ""
echo ""

echo "=== Add product ==="
curl -s -X POST "http://localhost:7573/product/api/products" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  --data-raw '{"name":"Test Laptop"}'
echo ""
echo ""

echo "=== Done ==="
