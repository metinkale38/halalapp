DO $$
DECLARE
    owned boolean;
BEGIN
    SELECT c.relowner = (SELECT oid FROM pg_roles WHERE rolname = current_user)
    INTO owned
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relname = 'products' AND c.relkind = 'v';

    IF owned IS TRUE THEN
        DROP VIEW public.products;
    END IF;
END $$;
