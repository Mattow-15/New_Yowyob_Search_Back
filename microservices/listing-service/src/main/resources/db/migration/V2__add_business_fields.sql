-- V2__add_business_fields.sql
ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS phone          VARCHAR(30),
    ADD COLUMN IF NOT EXISTS opening_hours  VARCHAR(512),
    ADD COLUMN IF NOT EXISTS rating         NUMERIC(3,1),
    ADD COLUMN IF NOT EXISTS reviews_count  INTEGER,
    ADD COLUMN IF NOT EXISTS image_url      VARCHAR(255);

COMMENT ON COLUMN listings.rating        IS 'Null tant que pas de source davis reelle integree';
COMMENT ON COLUMN listings.reviews_count IS 'Null tant que pas de source davis reelle integree';
