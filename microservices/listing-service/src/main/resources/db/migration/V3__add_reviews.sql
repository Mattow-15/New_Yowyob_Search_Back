-- V3__add_reviews.sql
CREATE TABLE reviews (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    user_id     VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    listing_id  UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE
);

-- Index pour accélérer les requêtes par commerce
CREATE INDEX idx_reviews_listing_id ON reviews(listing_id);

-- Empêche un utilisateur de noter deux fois le même commerce
CREATE UNIQUE INDEX idx_reviews_user_listing
    ON reviews(user_id, listing_id);

-- Colonnes à ajouter dans la table listings
ALTER TABLE listings ADD COLUMN IF NOT EXISTS average_rating DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS review_count   INTEGER DEFAULT 0;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS osm_id         VARCHAR(255) UNIQUE;
