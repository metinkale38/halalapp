CREATE TABLE IF NOT EXISTS public.product_overrides (
    product_id          text PRIMARY KEY,
    name                text NULL,
    brand               text NULL,
    front_image_version text NULL,
    updated_by          text NOT NULL,
    updated_at          timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.product_ignored_ingredients (
    product_id    text NOT NULL,
    ingredient_id text NOT NULL,
    reason        text NOT NULL DEFAULT '',
    proposal_id   text NULL,
    created_by    text NOT NULL,
    created_at    timestamp NOT NULL DEFAULT now(),
    PRIMARY KEY (product_id, ingredient_id)
);

