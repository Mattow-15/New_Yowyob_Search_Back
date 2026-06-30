#!/bin/bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"test@example.com","password":"Test1234!"}' | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
echo "Token: $TOKEN"
echo "--- PROFILE TEST ---"
curl -s -w "\nHTTP_CODE: %{http_code}\n" http://localhost:8080/api/users/profile -H "Authorization: Bearer $TOKEN"
echo "--- SEARCH TEST ---"
curl -s "http://localhost:8080/api/search?query=restaurant" | head -n 20
