DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'products_off' AND column_name = 'category_id'
    ) THEN
        ALTER TABLE public.products_off ADD COLUMN category_id text NULL REFERENCES public.categories(id) ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = 'public' AND indexname = 'idx_products_off_category'
    ) THEN
        CREATE INDEX idx_products_off_category ON public.products_off (category_id);
    END IF;
END $$;
