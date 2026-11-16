DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'product_images'
          AND column_name = 'status'
    ) THEN
        EXECUTE 'DROP TABLE IF EXISTS product_image_files';
        EXECUTE 'DROP TABLE product_images';
        EXECUTE 'CREATE TABLE product_images (
            product_id text PRIMARY KEY,
            thumbnail_image bytea NOT NULL,
            full_image bytea NOT NULL
        )';
    END IF;
END $$;
