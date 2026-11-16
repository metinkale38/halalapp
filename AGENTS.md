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

## Workflow

1. Arbeite immer über einen eigenen Branch und einen Pull Request gegen `main`.
   Niemals direkt auf `main` committen oder pushen.
2. Vor dem Commit `git status` und `git diff` prüfen; nur beabsichtigte Dateien stagen.
3. Commit-Nachrichten kurz und im Stil der bestehenden Historie halten.
4. PR per `gh pr create` erstellen und die Änderungen darin kurz beschreiben.
5. Der Build läuft über `./gradlew :server:shadowJar` (siehe CI).

## Deployment

- **Test:** PR mit dem Label `deploy-test` versehen. Der Workflow deployt den
  erfolgreichen Build dieses PRs auf den Testserver. Alternativ manuell über
  `workflow_dispatch` mit PR-Nummer.
- **Prod:** Ein erfolgreicher Build auf `main` deployt automatisch nach Prod.
  Kein manueller Klick nötig; `workflow_dispatch` bleibt als Notfall-Fallback.

## Code-Analyse

- Für die Analyse des Codes bevorzugt `repomix` verwenden, z. B.
  `repomix --output repomix-output.xml`.
- `repomix-output.xml` ist generiert und wird **nicht** eingecheckt (siehe `.gitignore`).
- Gezielte Änderungen anschließend mit den normalen Lese-/Suchwerkzeugen verifizieren.

## Sicherheit

- Dieses Projekt ist Open Source: **keine Secrets, Tokens, Passwörter oder
  Zugangsdaten** im Repository speichern.
- `local.properties`, `.env`-Dateien und ähnliche lokale Konfigurationen bleiben
  ungetrackt. Secrets gehören ausschließlich in GitHub Actions Secrets bzw. in die
  Serverumgebung.

## Konventionen

- Kotlin: 4 Leerzeichen Einrückung, bestehende Muster in `ui/` und `routes/` übernehmen.
- HTML wird mit `kotlinx.html` gebaut; Tailwind-Klassen werden aus den Kotlin-Quellen
  gescannt. Neue Utility-Klassen daher im Kotlin-Code verwenden, nicht nur in JS.
- JavaScript liegt unter `server/src/main/resources/static/`; ES2020+ ist erlaubt.
- Keine zusätzlichen Kommentare im Code, außer sie sind fachlich notwendig.
