#!/bin/bash

# Test du KeywordParser via une requête HTTP simple

echo "=========================================="
echo "🧪 Test du parseur de mots-clés"
echo "=========================================="
echo ""

# Vérifier si le service est en cours d'exécution
echo "⏳ Attente du service (5 secondes)..."
sleep 5

# Test 1
echo ""
echo "Test 1️⃣ : Requête simple avec stopwords"
echo "Requête : 'je veux les agences de voyage à yaoundé'"
echo ""
curl -s -G "http://localhost:8080/api/search" \
  --data-urlencode "q=je veux les agences de voyage à yaoundé" | jq . 2>/dev/null || \
curl -s -G "http://localhost:8080/api/search" \
  --data-urlencode "q=je veux les agences de voyage à yaoundé"

echo ""
echo "---"

# Test 2
echo ""
echo "Test 2️⃣ : Requête avec ponctuation"
echo "Requête : 'hôtels, agences de voyage!'"
echo ""
curl -s -G "http://localhost:8080/api/search" \
  --data-urlencode "q=hôtels, agences de voyage!" | jq . 2>/dev/null || \
curl -s -G "http://localhost:8080/api/search" \
  --data-urlencode "q=hôtels, agences de voyage!"

echo ""
echo "---"

# Test 3
echo ""
echo "Test 3️⃣ : Requête au pluriel"
echo "Requête : 'recherche agences voyages'"
echo ""
curl -s -G "http://localhost:8080/api/search" \
  --data-urlencode "q=recherche agences voyages" | jq . 2>/dev/null || \
curl -s -G "http://localhost:8080/api/search" \
  --data-urlencode "q=recherche agences voyages"

echo ""
echo "=========================================="
echo "✅ Tests terminés"
echo "=========================================="
