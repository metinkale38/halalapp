CREATE TABLE IF NOT EXISTS product_images (
    product_id      text PRIMARY KEY,
    thumbnail_image bytea NOT NULL,
    full_image      bytea NOT NULL
);
