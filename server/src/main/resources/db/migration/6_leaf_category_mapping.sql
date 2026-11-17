INSERT INTO public.categories (id, name, parent_id, description) VALUES
    ('weitere-getraenke', 'Weitere Getränke', 'getraenke',
     'Getränke, die sich keiner genaueren Unterkategorie zuordnen lassen. Wir prüfen die Zutaten auf Alkohol und tierische Bestandteile.'),
    ('weitere-milchprodukte', 'Weitere Milchprodukte', 'milchprodukte-kaese',
     'Milchprodukte, die sich keiner genaueren Unterkategorie zuordnen lassen. Wir prüfen die Zutatenlisten.'),
    ('weitere-backwaren', 'Weitere Backwaren', 'brot-backwaren',
     'Backwaren, die sich keiner genaueren Unterkategorie zuordnen lassen. Wir prüfen die Zutatenlisten.'),
    ('weitere-snacks', 'Weitere Snacks', 'suesswaren-snacks',
     'Snacks, die sich keiner genaueren Unterkategorie zuordnen lassen. Wir prüfen die Zutatenlisten.'),
    ('weitere-aufstriche', 'Weitere Aufstriche', 'fruehstueck-aufstriche',
     'Aufstriche, die sich keiner genaueren Unterkategorie zuordnen lassen. Wir prüfen die Zutatenlisten.'),
    ('weitere-fertiggerichte', 'Weitere Fertiggerichte', 'fertiggerichte-konserven',
     'Fertiggerichte, die sich keiner genaueren Unterkategorie zuordnen lassen. Wir prüfen die Zutatenlisten.'),
    ('weitere-fischprodukte', 'Weitere Fischprodukte', 'fisch-meeresfruechte',
     'Fischprodukte, die sich keiner genaueren Unterkategorie zuordnen lassen. Wir prüfen die Zutatenlisten.'),
    ('weitere-saucen', 'Weitere Saucen & Würzmittel', 'saucen-wuerzmittel',
     'Saucen und Würzmittel, die sich keiner genaueren Unterkategorie zuordnen lassen. Wir prüfen die Zutatenlisten.')
ON CONFLICT (id) DO NOTHING;

UPDATE public.category_mapping m
SET category_id = v.category_id
FROM (VALUES
    ('en:beverages', 'weitere-getraenke'),
    ('en:waters', 'weitere-getraenke'),
    ('en:fruit-juices', 'saefte-nektar'),
    ('en:fruit-nectars', 'saefte-nektar'),
    ('en:sodas', 'limonaden-energy'),
    ('en:energy-drinks', 'limonaden-energy'),
    ('en:coffees', 'kaffee-tee-kakao'),
    ('en:teas', 'kaffee-tee-kakao'),

    ('en:dairies', 'weitere-milchprodukte'),
    ('en:cheeses', 'kaese'),
    ('en:yogurts', 'joghurt-quark'),
    ('en:milks', 'weitere-milchprodukte'),
    ('en:butters', 'weitere-milchprodukte'),
    ('en:eggs', 'weitere-milchprodukte'),
    ('en:fermented-milk-products', 'joghurt-quark'),
    ('en:quarks', 'joghurt-quark'),

    ('en:breads', 'brot-broetchen'),
    ('en:cakes', 'kuchen-gebaeck'),
    ('en:pastries', 'kuchen-gebaeck'),
    ('en:viennoiseries', 'kuchen-gebaeck'),
    ('en:bakery-products', 'weitere-backwaren'),

    ('en:snacks', 'weitere-snacks'),
    ('en:sweet-snacks', 'weitere-snacks'),
    ('en:chocolates', 'schokolade'),
    ('en:candies', 'gummibaerchen-bonbons'),
    ('en:biscuits', 'kekse'),
    ('en:chips-and-fries', 'chips-salzgebaeck'),
    ('en:crisps', 'chips-salzgebaeck'),
    ('en:ice-creams', 'eis'),

    ('en:breakfast-cereals', 'muesli-cerealien'),
    ('en:spreads', 'weitere-aufstriche'),
    ('en:jams', 'konfituere-honig'),
    ('en:honeys', 'konfituere-honig'),
    ('en:chocolate-spreads', 'nuss-schokoaufstriche'),
    ('en:nut-butters', 'nuss-schokoaufstriche'),

    ('en:meals', 'weitere-fertiggerichte'),
    ('en:soups', 'suppen-eintoepfe'),
    ('en:canned-foods', 'konserven'),
    ('en:frozen-foods', 'fertigmenues-tiefkuehl'),
    ('en:pasta-dishes', 'nudel-reisgerichte'),

    ('en:fishes', 'fisch'),
    ('en:seafood', 'meeresfruechte'),
    ('en:crustaceans', 'meeresfruechte'),
    ('en:mollusc', 'meeresfruechte'),

    ('en:condiments', 'weitere-saucen'),
    ('en:sauces', 'saucen-dressings'),
    ('en:salad-dressings', 'saucen-dressings'),
    ('en:vegetable-oils', 'weitere-saucen'),
    ('en:vinegars', 'weitere-saucen'),
    ('en:spices', 'gewuerzmischungen'),

    ('en:baby-foods', 'glaeschen-breie'),
    ('en:infant-formulas', 'saeuglingsmilch')
) AS v(id, category_id)
WHERE m.id = v.id;
