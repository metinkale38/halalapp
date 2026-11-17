ALTER TABLE products_off ADD COLUMN IF NOT EXISTS category_id text NULL REFERENCES categories(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_products_off_category ON products_off (category_id);
