# Vorschläge einbinden

Details zum Einreichen von Produktinformationen und Custom-Produkten. Nur lesen,
wenn tatsächlich ein Vorschlag eingereicht oder geprüft werden soll.

## Ablauf

- Produktdaten zuerst in HalalApp prüfen; externe Datenquellen nur nach Rücksprache
  durchsuchen.
- Bestehendes Produkt: Produktinformation über
  `/crm/product_insights/new?product_id=<barcode>` einreichen.
- Nicht vorhandenes Produkt: Custom-Produkt über `/crm/products_custom/new`
  einreichen. Der Server lehnt das ab, wenn der Barcode bereits in OpenFoodFacts
  oder HalalApp existiert.
- Inhalt: Herstelleraussage/Quelle als Markdown, Datum, passende Klassifikationen
  (`animal`/`alcohol`) und kurze Notiz an die Admins.
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
- Schreibzugriffe erfordern einen Login über `forum.halalapp.de`; der resultierende
  `halalapp_auth`-Cookie wird nur temporär verwendet.

## Zugangsdaten

Zugangsdaten niemals in diesem Repository, in Commits oder in Dokumentationsdateien
speichern. Liegen sie nicht in der aktuellen Unterhaltung vor, den Nutzer danach
fragen. Cookies und Sessions nur temporär außerhalb des Repositories halten.
