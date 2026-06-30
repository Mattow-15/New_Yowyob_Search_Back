#!/bin/bash

# Configuration
LISTING_SERVICE_URL="http://localhost:8083/api/listings"

# Function to create a listing
create_listing() {
    local title="$1"
    local description="$2"
    local price="$3"
    local category="$4"
    local sellerId="$5"
    local address="$6"

    curl -X POST "$LISTING_SERVICE_URL" \
         -H "Content-Type: application/json" \
         -d "{
               \"title\": \"$title\",
               \"description\": \"$description\",
               \"price\": $price,
               \"category\": \"$category\",
               \"sellerId\": \"$sellerId\",
               \"address\": \"$address\",
               \"status\": \"ACTIVE\"
             }"
    echo ""
}

echo "creating listings..."

# Seller ID (random UUID for testing)
SELLER_ID="550e8400-e29b-41d4-a716-446655440000"

create_listing "iPhone 15 Pro" "Smartphone Apple dernier cri, 256GB, Titane Naturel. Comme neuf." 1100.0 "ELECTRONICS" "$SELLER_ID" "Paris, France"
create_listing "Canapé Lit 3 places" "Canapé convertible confortable, couleur gris anthracite. A venir chercher." 250.0 "HOME" "$SELLER_ID" "Lyon, France"
create_listing "Leçons de Piano" "Professeur diplômé donne cours de piano pour débutants et intermédiaires." 30.0 "SERVICES" "$SELLER_ID" "Marseille, France"
create_listing "MacBook Air M2" "Ordinateur portable léger et puissant, parfait pour étudiants. 8GB/256GB." 950.0 "ELECTRONICS" "$SELLER_ID" "Bordeaux, France"
create_listing "Vélo de route" "Vélo de course vintage, cadre acier, très bon état." 120.0 "SPORTS" "$SELLER_ID" "Nantes, France"

echo "Done!"
