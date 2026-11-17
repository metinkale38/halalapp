# Deployment

Details zu Test- und Produktions-Deployments. Nur lesen, wenn Deployment- oder
CI-Themen bearbeitet werden.

- **Test:** PR mit dem Label `deploy-test` versehen. Der Workflow deployt den
  erfolgreichen Build dieses PRs auf den Testserver. Alternativ manuell über
  `workflow_dispatch` mit PR-Nummer.
- **Prod:** Ein erfolgreicher Build auf `main` deployt automatisch nach Prod.
  Kein manueller Klick nötig; `workflow_dispatch` bleibt als Notfall-Fallback.
