# Vorschläge einbinden

Details zum Einreichen von Produktinformationen und Custom-Produkten. Nur lesen,
wenn tatsächlich ein Vorschlag eingereicht oder geprüft werden soll.

## Ablauf

- Produktdaten zuerst in HalalApp prüfen; externe Datenquellen nur nach Rücksprache
  durchsuchen.
- Bestehendes Produkt: Produktinformation über
  `/crm/product_insights/new?product_id=<barcode>` einreichen.
- Stammdaten eines OFF-Produkts korrigieren: `/crm/product_masterdata/new?product_id=<barcode>`
  mit Name, Marke und/oder Frontfoto. Die Änderungen überschreiben die
  OpenFoodFacts-Werte dauerhaft, bis ein Admin sie entfernt.
- Falsch übernommene Zutat eines OFF-Produkts: `/crm/product_ingredient_ignore/new?product_id=<barcode>`.
  Die Zutat wird nur für dieses Produkt von der Bewertung ausgeschlossen; die
  OFF-Rohdaten bleiben unverändert.
- Nicht vorhandenes Produkt: Custom-Produkt über `/crm/products_custom/new`
  einreichen. Der Server lehnt das ab, wenn der Barcode bereits in OpenFoodFacts
  oder HalalApp existiert.
- Inhalt: Herstelleraussage/Quelle als Markdown, Datum, passende Klassifikationen
  (`animal`/`alcohol`) und kurze Notiz an die Admins.
- Hersteller-Mails immer als **direktes Zitat** einreichen, nicht als Zusammenfassung.
  Begrüßung, Anrede, Abschluss und Signatur dürfen entfernt werden; die Erklärung
  und Begründung des Herstellers müssen 1:1 übernommen werden.
- Zutatenlisten nach Möglichkeit über die Herstellerseite oder Händlerseiten
  verifizieren. Keine Daten von OpenFoodFacts, HalalApp oder Seiten verwenden, die
  ihre Daten aus OpenFoodFacts beziehen.
- Klassifikations-IDs müssen exakt den Optionen im Formular entsprechen; unbekannte
  IDs werden serverseitig verworfen.
- Hersteller-Mails vor dem Einreichen anonymisieren: Namen, E-Mail-Adressen und
  Telefonnummern entfernen, Firmennamen behalten.
- Custom-Produkte: `id` (Barcode), `name` ohne Marke, `brand`, optionale
  `categoryId`, `insightContent`, `insightDate`, `note`; optional `front` als
  JPEG/PNG.
- Nach dem Absenden muss „Vorschlag gesendet“ erscheinen. Vorschläge werden von
  Admins geprüft.
- Ein offener Custom-Vorschlag blockiert weitere Vorschläge für denselben Barcode.
  Nachträgliche Änderungen (z. B. Foto) sind für normale Nutzer nicht möglich;
  ggf. einen Admin bitten.
- Pro Produkt und Vorschlagstyp ist maximal ein offener Vorschlag erlaubt. Nach
  der Genehmigung wird das Produkt automatisch neu verarbeitet.
- Admins prüfen Stammdaten- und Zutaten-Vorschläge unter `/crm/product_changes`.
  Der Verlauf liegt unter `/crm/product_changes/history`, dauerhafte Overrides
  werden unter `/crm/product_changes/manage` verwaltet.
- Schreibzugriffe erfordern einen Login über `forum.halalapp.de`; der resultierende
  `halalapp_auth`-Cookie wird nur temporär verwendet.

## Zugangsdaten

Zugangsdaten niemals in diesem Repository, in Commits oder in Dokumentationsdateien
speichern. Liegen sie nicht in der aktuellen Unterhaltung vor, den Nutzer danach
fragen. Cookies und Sessions nur temporär außerhalb des Repositories halten.
