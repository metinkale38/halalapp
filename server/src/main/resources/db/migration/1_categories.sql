DO $$
BEGIN
    IF to_regclass('categories') IS NULL THEN
        CREATE TABLE categories (
            id          text PRIMARY KEY,
            name        text NOT NULL,
            parent_id   text NULL REFERENCES categories(id) ON DELETE SET NULL,
            description text NOT NULL DEFAULT ''
        );
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('category_mapping') IS NULL THEN
        CREATE TABLE category_mapping (
            id          text PRIMARY KEY,
            category_id text NOT NULL REFERENCES categories(id) ON DELETE CASCADE
        );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'products_off' AND column_name = 'categories_hierarchy'
    ) THEN
        ALTER TABLE products_off ADD COLUMN categories_hierarchy text[] NOT NULL DEFAULT '{}';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = current_schema() AND indexname = 'idx_products_off_categories_hierarchy'
    ) THEN
        CREATE INDEX idx_products_off_categories_hierarchy ON products_off USING GIN (categories_hierarchy);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'products_custom' AND column_name = 'category_id'
    ) THEN
        ALTER TABLE products_custom ADD COLUMN category_id text NULL REFERENCES categories(id) ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'products_processed' AND column_name = 'category_id'
    ) THEN
        ALTER TABLE products_processed ADD COLUMN category_id text NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = current_schema() AND indexname = 'idx_products_processed_category'
    ) THEN
        CREATE INDEX idx_products_processed_category ON products_processed (category_id);
    END IF;
END $$;

INSERT INTO categories (id, name, parent_id, description) VALUES
 ('sonstiges', 'Sonstiges', NULL,
  'Produkte, die sich nicht eindeutig einer Kategorie zuordnen lassen. Wir prüfen die Zutaten trotzdem auf tierische und alkoholische Bestandteile.'),
 ('babynahrung', 'Babynahrung', NULL,
  'Gläschen, Breie und Säuglingsmilch – hier lohnt der genaue Blick auf Gelatine, Aromen und Zucker. Wir zeigen dir, welche Produkte unbedenklich sind.'),
 ('brot-backwaren', 'Brot & Backwaren', NULL,
  'Brot, Brötchen, Kuchen und Gebäck können Backemulgatoren, tierische Fette oder Alkohol enthalten. Wir prüfen die Zutatenlisten auf kritische Bestandteile.'),
 ('fertiggerichte-konserven', 'Fertiggerichte & Konserven', NULL,
  'Suppen, Konserven und Fertigmenüs enthalten oft Würzmittel, Fleischextrakte oder Alkohol als Aromaträger. Wir zeigen dir, was unbedenklich ist.'),
 ('fisch-meeresfruechte', 'Fisch & Meeresfrüchte', NULL,
  'Fisch und Meeresfrüchte sind grundsätzlich erlaubt; bei verarbeiteten Produkten zählen Marinaden, Saucen und Zusatzstoffe. Wir prüfen die Zutaten.'),
 ('fruehstueck-aufstriche', 'Frühstück & Aufstriche', NULL,
  'Müsli, Konfitüre und Aufstriche – kritisch sind Gelatine, tierische Fette und Emulgatoren. Wir prüfen die Zutatenliste jedes Produkts.'),
 ('getraenke', 'Getränke', NULL,
  'Limonaden, Säfte und Energy-Drinks können Alkohol als Aromaträger oder tierische Zusatzstoffe enthalten. Wir zeigen dir den Befund.'),
 ('milchprodukte-kaese', 'Milchprodukte & Käse', NULL,
  'Milch, Joghurt und Käse sind meist unbedenklich; bei Käse zählt das Lab, bei Desserts Gelatine. Wir prüfen die Zutaten.'),
 ('saucen-wuerzmittel', 'Saucen & Würzmittel', NULL,
  'Saucen, Dressings und Gewürzmischungen enthalten häufig Alkohol, Emulgatoren oder tierische Extrakte. Wir prüfen die Zutatenlisten.'),
 ('suesswaren-snacks', 'Süßwaren & Snacks', NULL,
  'Schokolade, Gummibärchen und Snacks enthalten oft Gelatine, tierische Fette oder Alkohol. Wir zeigen dir, welche Produkte unbedenklich, zweifelhaft oder haram sind.')
ON CONFLICT (id) DO UPDATE
    SET description = EXCLUDED.description
    WHERE categories.description = '';

INSERT INTO categories (id, name, parent_id, description) VALUES
 ('glaeschen-breie', 'Gläschen & Breie', 'babynahrung',
  'Babygläschen und Breie – wir prüfen auf Gelatine, tierische Fette und versteckte Aromen.'),
 ('saeuglingsmilch', 'Säuglingsmilch', 'babynahrung',
  'Säuglingsmilch und Folgenahrung: kritische Zusätze und tierische Bestandteile im Blick.'),
 ('brot-broetchen', 'Brot & Brötchen', 'brot-backwaren',
  'Brot und Brötchen können Backemulgatoren oder tierische Fette enthalten. Wir prüfen die Zutaten.'),
 ('kuchen-gebaeck', 'Kuchen & Gebäck', 'brot-backwaren',
  'Kuchen und Gebäck enthalten oft Gelatine, Alkohol oder tierische Fette – wir prüfen die Zutatenliste.'),
 ('suppen-eintoepfe', 'Suppen & Eintöpfe', 'fertiggerichte-konserven',
  'Suppen und Eintöpfe: Fleischextrakte, Würzmittel und Alkohol im Fokus.'),
 ('konserven', 'Konserven', 'fertiggerichte-konserven',
  'Konserven und haltbar gemachte Produkte – wir prüfen Zusatzstoffe und tierische Bestandteile.'),
 ('fertigmenues-tiefkuehl', 'Fertigmenüs & Tiefkühl', 'fertiggerichte-konserven',
  'Fertigmenüs und Tiefkühlgerichte enthalten oft komplexe Zutatenlisten. Wir prüfen sie auf kritische Bestandteile.'),
 ('nudel-reisgerichte', 'Nudel- & Reisgerichte', 'fertiggerichte-konserven',
  'Nudel- und Reisgerichte: kritisch sind Würzmischungen, Fleischextrakte und Alkohol.'),
 ('obst-gemuese-konserviert', 'Obst & Gemüse (konserviert)', 'fertiggerichte-konserven',
  'Konserviertes Obst und Gemüse – wir prüfen auf Zusätze und tierische Bestandteile.'),
 ('fisch', 'Fisch', 'fisch-meeresfruechte',
  'Fischprodukte: Marinaden, Saucen und Zusatzstoffe entscheiden über die Einstufung.'),
 ('meeresfruechte', 'Meeresfrüchte', 'fisch-meeresfruechte',
  'Meeresfrüchte und verarbeitete Produkte – wir prüfen die Zutaten auf kritische Bestandteile.'),
 ('muesli-cerealien', 'Müsli & Cerealien', 'fruehstueck-aufstriche',
  'Müsli und Cerealien können tierische Fette, Honig oder Aromen enthalten. Wir prüfen die Zutaten.'),
 ('konfituere-honig', 'Konfitüre & Honig', 'fruehstueck-aufstriche',
  'Konfitüre und Honig – wir prüfen auf Gelatine, tierische Fette und Zusatzstoffe.'),
 ('nuss-schokoaufstriche', 'Nuss- & Schokoaufstriche', 'fruehstueck-aufstriche',
  'Nuss- und Schokoaufstriche enthalten oft Emulgatoren und tierische Fette. Wir prüfen die Zutaten.'),
 ('limonaden-energy', 'Limonaden & Energy', 'getraenke',
  'Limonaden und Energy-Drinks können Alkohol als Aromaträger enthalten. Wir prüfen die Zutaten.'),
 ('saefte-nektar', 'Säfte & Nektar', 'getraenke',
  'Säfte und Nektare – wir prüfen auf Zusätze und kritische Aromen.'),
 ('kaffee-tee-kakao', 'Kaffee, Tee & Kakao', 'getraenke',
  'Kaffee, Tee und Kakao: verarbeitete Varianten können Aromen und Zusätze enthalten.'),
 ('joghurt-quark', 'Joghurt & Quark', 'milchprodukte-kaese',
  'Joghurt und Quark sind meist unbedenklich; Dessertvarianten prüfen wir auf Gelatine und Zusätze.'),
 ('kaese', 'Käse', 'milchprodukte-kaese',
  'Käse – entscheidend ist das Lab. Wir prüfen die Zutatenliste jedes Produkts.'),
 ('saucen-dressings', 'Saucen & Dressings', 'saucen-wuerzmittel',
  'Saucen und Dressings enthalten oft Alkohol, Emulgatoren oder tierische Extrakte. Wir prüfen die Zutaten.'),
 ('gewuerzmischungen', 'Gewürzmischungen', 'saucen-wuerzmittel',
  'Gewürzmischungen können Aromen, Alkohol und Zusatzstoffe enthalten. Wir prüfen die Zutaten.'),
 ('schokolade', 'Schokolade', 'suesswaren-snacks',
  'Schokolade kann Emulgatoren und tierische Fette enthalten. Wir prüfen die Zutatenliste.'),
 ('gummibaerchen-bonbons', 'Gummibärchen & Bonbons', 'suesswaren-snacks',
  'Gummibärchen und Bonbons enthalten häufig Gelatine. Wir zeigen dir, welche Produkte unbedenklich sind.'),
 ('kekse', 'Kekse', 'suesswaren-snacks',
  'Kekse enthalten oft tierische Fette, Emulgatoren oder Alkohol. Wir prüfen die Zutaten.'),
 ('chips-salzgebaeck', 'Chips & Salzgebäck', 'suesswaren-snacks',
  'Chips und Salzgebäck: kritisch sind Aromen, tierische Fette und Zusatzstoffe.'),
 ('eis', 'Eis', 'suesswaren-snacks',
  'Eis kann Emulgatoren, Gelatine und Alkohol enthalten. Wir prüfen die Zutaten.')
ON CONFLICT (id) DO UPDATE
    SET description = EXCLUDED.description
    WHERE categories.description = '';
