#!/bin/bash

# Configuration
API_KEY="AIzaSyDNdNzVsuFCIoCHUcxpwUdTiJYylRFmeFQ"
DB_NAME="yowyob_listings"
DB_USER="postgres"
ES_URL="http://localhost:9200"

echo "=== Démarrage de l'enrichissement des photos Google Places ==="

# Récupérer la liste des établissements sans photos
# Format de sortie : id;title;address;category
docker exec -i yowyob-postgres psql -U $DB_USER -d $DB_NAME -t -A -F ';' -P pager=off -c "SELECT id, title, address, category FROM listings WHERE image_url IS NULL OR image_url = '';" > /tmp/listings_to_enrich.txt

echo "Nombre total d'établissements à enrichir : $(wc -l < /tmp/listings_to_enrich.txt)"

# Lire ligne par ligne avec descripteur de fichier dédié (FD 3) pour éviter que les commandes internes ne consomment l'entrée standard
while IFS=';' read -r id title address category <&3; do
  # Nettoyage
  id=$(echo "$id" | tr -d '\r\n[:space:]')
  title=$(echo "$title" | tr -d '\r\n' | xargs)
  address=$(echo "$address" | tr -d '\r\n' | xargs)
  category=$(echo "$category" | tr -d '\r\n' | xargs)

  if [ -z "$id" ] || [ -z "$title" ]; then
    continue
  fi

  echo "Traitement de : $title ($address) - ID: $id"

  # Construire la requête de recherche Google Places
  encoded_query=$(jq -rn --arg q "$title $address" '$q | @uri')
  search_url="https://maps.googleapis.com/maps/api/place/textsearch/json?query=${encoded_query}&key=${API_KEY}"
  
  # Requête HTTP avec redirection de stdin pour éviter la consommation par curl
  response=$(curl -s "$search_url" < /dev/null)
  status=$(echo "$response" | jq -r '.status')

  if [ "$status" = "OK" ]; then
    # Récupérer la première référence de photo disponible
    photo_ref=$(echo "$response" | jq -r '.results[0].photos[0].photo_reference // empty')
    
    if [ -n "$photo_ref" ] && [ "$photo_ref" != "null" ]; then
      image_url="https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photo_reference=${photo_ref}&key=${API_KEY}"
      echo "  -> De vraie photo Google Places trouvée !"

      # 1. Mettre à jour PostgreSQL
      escaped_image_url=$(echo "$image_url" | sed "s/'/''/g")
      docker exec -i yowyob-postgres psql -U $DB_USER -d $DB_NAME -c "UPDATE listings SET image_url = '${escaped_image_url}' WHERE id = '${id}';" < /dev/null > /dev/null
      
      # 2. Mettre à jour Elasticsearch
      update_payload=$(jq -n --arg img "$image_url" '{doc: {imageUrl: $img}}')
      curl -s -X POST "${ES_URL}/crawler-index/_update/${id}?pretty" \
        -H "Content-Type: application/json" \
        -d "$update_payload" < /dev/null > /dev/null

      echo "  -> PostgreSQL & Elasticsearch mis à jour avec succès."
    else
      echo "  -> Pas de photo disponible sur Google Places pour cet établissement."
      
      # Assigner un logo ou placeholder dynamique de haute qualité selon la catégorie
      placeholder_url=""
      if [[ "$category" == *"RESTAURANT"* || "$title" == *"restaurant"* || "$title" == *"Boulangerie"* || "$title" == *"Restaurant"* || "$title" == *"Lounge"* || "$title" == *"bar"* ]]; then
        placeholder_url="https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80"
      elif [[ "$category" == *"HOTEL"* || "$title" == *"Hotel"* || "$title" == *"HOTEL"* || "$title" == *"hostel"* ]]; then
        placeholder_url="https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80"
      elif [[ "$category" == *"PHARMACIE"* || "$title" == *"pharmacy"* || "$title" == *"Pharmacie"* || "$category" == *"SERVICES_PHARMACIES"* || "$title" == *"Pharmacy"* ]]; then
        placeholder_url="https://images.unsplash.com/photo-1586015555751-63bb77f4322a?auto=format&fit=crop&w=800&q=80"
      elif [[ "$title" == *"Téléphone"* || "$title" == *"Samsung"* || "$category" == *"PRODUCT"* || "$category" == *"Electronique"* || "$title" == *"Infinix"* || "$title" == *"iPhone"* ]]; then
        placeholder_url="https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=800&q=80"
      else
        placeholder_url="https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=800&q=80"
      fi

      if [ -n "$placeholder_url" ]; then
        echo "  -> Attribution d'un placeholder de catégorie premium : $placeholder_url"
        
        # Mettre à jour PostgreSQL
        docker exec -i yowyob-postgres psql -U $DB_USER -d $DB_NAME -c "UPDATE listings SET image_url = '${placeholder_url}' WHERE id = '${id}';" < /dev/null > /dev/null
        
        # Mettre à jour Elasticsearch
        update_payload=$(jq -n --arg img "$placeholder_url" '{doc: {imageUrl: $img}}')
        curl -s -X POST "${ES_URL}/crawler-index/_update/${id}?pretty" \
          -H "Content-Type: application/json" \
          -d "$update_payload" < /dev/null > /dev/null
      fi
    fi
  else
    echo "  -> Google Places a renvoyé le statut : $status pour cette recherche."
  fi

  # Attendre 100ms entre les requêtes
  sleep 0.1

done 3< /tmp/listings_to_enrich.txt

# Nettoyage
rm -f /tmp/listings_to_enrich.txt

echo "=== Enrichissement des photos terminé avec succès ! ==="
