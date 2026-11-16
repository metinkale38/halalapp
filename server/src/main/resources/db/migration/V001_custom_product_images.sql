-- Custom product images (stored in Postgres to avoid an extra storage service)
CREATE TABLE IF NOT EXISTS product_images (
    id              text PRIMARY KEY,
    proposal_id     text REFERENCES proposals(id) ON DELETE CASCADE,
    product_id      text,
    kind            text NOT NULL CHECK (kind IN ('FRONT', 'INGREDIENTS')),
    status          text NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'ATTACHED', 'REJECTED', 'REPLACED')),
    original_mime   text NOT NULL,
    original_width  integer NOT NULL,
    original_height integer NOT NULL,
    original_bytes  integer NOT NULL,
    sha256          text NOT NULL,
    created_at      timestamp without time zone NOT NULL DEFAULT now(),
    created_by      text NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_product_images_proposal ON product_images (proposal_id);
CREATE INDEX IF NOT EXISTS idx_product_images_product ON product_images (product_id);
CREATE INDEX IF NOT EXISTS idx_product_images_status_created ON product_images (status, created_at);

CREATE TABLE IF NOT EXISTS product_image_files (
    id         text PRIMARY KEY,
    image_id   text NOT NULL REFERENCES product_images(id) ON DELETE CASCADE,
    width      integer NOT NULL,
    mime       text NOT NULL,
    byte_size  integer NOT NULL,
    sha256     text NOT NULL,
    data       bytea NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_product_image_files_image_width
    ON product_image_files (image_id, width);
CREATE INDEX IF NOT EXISTS idx_product_image_files_image ON product_image_files (image_id);
