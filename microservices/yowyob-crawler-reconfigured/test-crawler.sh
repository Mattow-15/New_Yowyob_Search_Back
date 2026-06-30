#!/bin/bash

echo "🧪 YowYob Crawler - Test Script"
echo "================================"
echo ""

# Couleurs pour l'affichage
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

CRAWLER_URL="http://localhost:8086/api/crawler"
BACKEND_URL="http://localhost:3000"

# Fonction pour tester un endpoint
test_endpoint() {
    local name=$1
    local url=$2
    
    echo -e "${BLUE}📍 Test: $name${NC}"
    echo "   URL: $url"
    
    response=$(curl -s -X POST "${CRAWLER_URL}/crawl?url=${url}")
    
    # Vérifier le succès
    if echo "$response" | grep -q '"success":true'; then
        items=$(echo "$response" | grep -o '"itemsFound":[0-9]*' | grep -o '[0-9]*')
        echo -e "${GREEN}   ✅ Succès - Items trouvés: $items${NC}"
    else
        error=$(echo "$response" | grep -o '"errorMessage":"[^"]*"' | cut -d'"' -f4)
        echo -e "${RED}   ❌ Échec - Erreur: $error${NC}"
    fi
    echo ""
}

# Vérifier que le crawler est accessible
echo "🔍 Vérification du crawler..."
if curl -s "${CRAWLER_URL}/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Crawler accessible${NC}"
else
    echo -e "${RED}❌ Crawler non accessible sur ${CRAWLER_URL}${NC}"
    echo "   Assurez-vous que le crawler est lancé avec: mvn spring-boot:run"
    exit 1
fi

echo ""

# Vérifier que le backend de test est accessible
echo "🔍 Vérification du backend de test..."
if curl -s "${BACKEND_URL}/" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Backend de test accessible${NC}"
else
    echo -e "${RED}❌ Backend de test non accessible sur ${BACKEND_URL}${NC}"
    echo "   Assurez-vous que le backend est lancé"
    echo "   (ou testez avec une autre URL publique)"
fi

echo ""
echo "🚀 Démarrage des tests..."
echo ""

# Tests
test_endpoint "Page d'accueil" "${BACKEND_URL}/"
test_endpoint "Produit 1" "${BACKEND_URL}/product/1"
test_endpoint "Produit 2" "${BACKEND_URL}/product/2"
test_endpoint "Catégorie Electronics" "${BACKEND_URL}/category/electronics"
test_endpoint "API JSON Products" "${BACKEND_URL}/api/products"

# Test avec un site externe (optionnel)
echo -e "${BLUE}📍 Test: Site externe (example.com)${NC}"
response=$(curl -s -X POST "${CRAWLER_URL}/crawl?url=http://example.com")
if echo "$response" | grep -q '"success":true'; then
    echo -e "${GREEN}   ✅ Succès - Le crawler fonctionne avec des sites externes${NC}"
else
    echo -e "${RED}   ❌ Échec avec site externe${NC}"
fi

echo ""
echo "================================"
echo -e "${GREEN}✅ Tests terminés!${NC}"
echo ""
echo "💡 Pour voir les détails complets d'un crawl:"
echo "   curl -X POST \"${CRAWLER_URL}/crawl?url=${BACKEND_URL}/\" | jq ."
