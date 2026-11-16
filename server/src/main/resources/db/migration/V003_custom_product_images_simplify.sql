DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'product_images'
          AND column_name = 'kind'
    ) THEN
        EXECUTE 'DELETE FROM product_images WHERE kind = ''INGREDIENTS''';
        EXECUTE 'ALTER TABLE product_images DROP COLUMN kind';
    END IF;
END $$;

UPDATE products_custom
SET images = jsonb_build_object('front', COALESCE(images -> 'front', '{}'::jsonb))
WHERE images ? 'ingredients';
