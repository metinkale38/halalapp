# Login und authentifizierte Aufrufe

Nur lesen, wenn Produktinformationen eingereicht oder Admin-Seiten aufgerufen
werden sollen. Zugangsdaten stehen nie im Repository; sie müssen aus der
Unterhaltung oder aus der Serverumgebung kommen.

## Ablauf

1. Im Forum `https://forum.halalapp.de/` mit dem eigenen Konto anmelden. Der
   Login läuft über das Flarum-Login-Modal (`.LogInModal`,
   `input[name=identification]`, `input[name=password]`).
2. Nach dem Login setzt das Forum den Cookie `halalapp_auth` für die Domain
   `.halalapp.de`. Der Cookie enthält ein JWT und ist `httpOnly`, `secure` und
   `SameSite=Lax`.
3. Den Cookie bei Aufrufen von `https://www.halalapp.de` mitschicken, z. B. als
   Header `Cookie: halalapp_auth=<jwt>` oder über einen gemeinsamen Cookie-Jar.
4. Ohne gültigen Cookie leitet HalalApp auf
   `https://forum.halalapp.de/?loginAndRedirectTo=<Ziel-URL>` um.

## Rechte

- Das JWT enthält die Gruppen im Claim `groups`. Normale Konten haben
  `groups: []`.
- Seiten unter `/crm/...`, die `ensureAdmin()` verwenden, brauchen
  `groups: ["Admin"]`.
- Vorschläge für Produktinformationen (`/crm/product_insights/new`) und
  Stammdaten (`/crm/product_masterdata/new`) funktionieren ohne Admin-Gruppe.
- Der Cookie ist etwa 12 Stunden gültig. HalalApp erneuert ihn, solange er
  gültig ist.

## Produktinformation einreichen

Formular: `GET /crm/product_insights/new?product_id=<barcode>`

Absenden per `POST /crm/product_insights/new` mit:

- `product_id`: Barcode des bestehenden Produkts
- `content`: Herstelleraussage als Markdown, anonymisiert (keine Namen,
  E-Mail-Adressen oder Telefonnummern)
- `date`: Datum im Format `YYYY-MM-DD`
- `animal`: Klassifikations-ID aus dem Formular, z. B. `halal`
- `alcohol`: Klassifikations-ID aus dem Formular, z. B. `alcohol_lowamount`
- `note`: kurze Notiz an die Admins

Bei Erfolg antwortet der Server mit „Vorschlag gesendet. Vielen Dank.“ Der
Vorschlag wird erst nach Admin-Freigabe wirksam.

## Automatisierter Login (Playwright)

Zugangsdaten nur über Umgebungsvariablen übergeben und nicht in Dateien
schreiben. Der Cookie-Jar bleibt außerhalb des Repositories, z. B. unter
`/tmp`.

```js
const page = await context.newPage();
await page.goto(
  'https://forum.halalapp.de/?loginAndRedirectTo=' +
    encodeURIComponent('https://www.halalapp.de/crm/product_insights')
);
await page.locator('input[name="identification"]').fill(process.env.HALAL_EMAIL);
await page.locator('input[name="password"]').fill(process.env.HALAL_PASSWORD);
await page.locator('.LogInModal button[type="submit"]').click();
await page.waitForURL('https://www.halalapp.de/**');
const cookies = await context.cookies('https://www.halalapp.de');
const auth = cookies.find(c => c.name === 'halalapp_auth');
```

Den so ermittelten Cookie-Wert für weitere Requests verwenden.
