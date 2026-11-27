# Deployment

Details zu Test- und Produktions-Deployments. Nur lesen, wenn Deployment- oder
CI-Themen bearbeitet werden.

- **Test:** PR mit dem Label `deploy-test` versehen. Der Workflow deployt den
  erfolgreichen Build dieses PRs auf den Testserver. Alternativ manuell über
  `workflow_dispatch` mit PR-Nummer.
- **Prod:** Ein erfolgreicher Build auf `main` deployt automatisch nach Prod.
  Kein manueller Klick nötig; `workflow_dispatch` bleibt als Notfall-Fallback.
- **Docker:** Der Workflow `docker.yml` baut bei Pushes auf `main`, bei Tags
  `v*` und manuell ein Image und veröffentlicht es unter
  `ghcr.io/metinkale38/halalapp`. Pull Requests bauen das Image nur zur
  Validierung, ohne zu pushen. Tags: `latest` und `main` auf dem Hauptbranch,
  `sha-<commit>` immer sowie SemVer-Tags für `v*`-Tags. Neue GHCR-Pakete sind
  zunächst privat; für öffentliche Images die Sichtbarkeit in den
  GitHub-Package-Einstellungen auf `public` stellen.
