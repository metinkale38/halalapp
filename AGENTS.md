# AGENTS.md

Hinweise für agentische Assistenten, die an diesem Repository arbeiten.

## Projekt

HalalApp ist eine Open-Source-Web-App (Kotlin/Ktor + kotlinx.html) zur Analyse von
Supermarktprodukten. Der relevante Anwendungscode liegt unter `server/`. Das
`android/`-Verzeichnis ist ein separater Wrapper und wird nur auf ausdrücklichen
Wunsch bearbeitet.

Wichtige Pfade:

- `server/src/main/kotlin/dev/metinkale/halalapp/routes/` – HTTP-Routen
- `server/src/main/kotlin/dev/metinkale/halalapp/ui/` – HTML-Seiten und Komponenten
- `server/src/main/resources/static/` – statische Assets (JS, CSS, Bibliotheken)
- `.github/workflows/` – CI (Build, Deployment)

Ausführliche Hinweise liegen unter `docs/agents/`. Diese Dateien nur lesen, wenn der
jeweilige Bereich tatsächlich bearbeitet wird.

## Workflow

1. Arbeite immer über einen eigenen Branch und einen Pull Request gegen `main`.
   Niemals direkt auf `main` committen oder pushen.
2. Vor dem Commit `git status` und `git diff` prüfen; nur beabsichtigte Dateien stagen.
3. Commit-Nachrichten kurz und im Stil der bestehenden Historie halten.
4. PR per `gh pr create` erstellen und die Änderungen darin kurz beschreiben.
5. Der Build läuft über `./gradlew :server:shadowJar` (siehe CI).

## Deployment

Details: [`docs/agents/deployment.md`](docs/agents/deployment.md). Nur lesen, wenn
Deployment- oder CI-Themen bearbeitet werden.

## Code-Analyse

Details: [`docs/agents/code-analysis.md`](docs/agents/code-analysis.md). Nur lesen,
wenn eine größere Codeanalyse nötig ist.

## Vorschläge einbinden

Details: [`docs/agents/proposals.md`](docs/agents/proposals.md). Nur lesen, wenn
Produktvorschläge eingereicht oder geprüft werden.

## Sicherheit

- Dieses Projekt ist Open Source: **keine Secrets, Tokens, Passwörter oder
  Zugangsdaten** im Repository speichern.
- Zugangsdaten niemals in `AGENTS.md`, Code, Commits oder Projektdateien
  dokumentieren. Liegen sie nicht in der aktuellen Unterhaltung vor, den Nutzer
  danach fragen.
- Cookies und Sessions nur temporär außerhalb des Repositories halten und nicht
  einchecken.
- `local.properties`, `.env`-Dateien und ähnliche lokale Konfigurationen bleiben
  ungetrackt. Secrets gehören ausschließlich in GitHub Actions Secrets bzw. in die
  Serverumgebung.

## Konventionen

- Kotlin: 4 Leerzeichen Einrückung, bestehende Muster in `ui/` und `routes/` übernehmen.
- HTML wird mit `kotlinx.html` gebaut; Tailwind-Klassen werden aus den Kotlin-Quellen
  gescannt. Neue Utility-Klassen daher im Kotlin-Code verwenden, nicht nur in JS.
- JavaScript liegt unter `server/src/main/resources/static/`; ES2020+ ist erlaubt.
- Keine zusätzlichen Kommentare im Code, außer sie sind fachlich notwendig.
