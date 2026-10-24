# Kibana alerting (start simple)

Create in **Stack Management → Rules**.

| Rule name | Type | Condition (example) | Action |
|-----------|------|---------------------|--------|
| VycePay ERROR spike | Elasticsearch query | Count of `log.level: ERROR` over last 5m > 20 | Email / Slack / webhook |
| VycePay callback ERROR | Elasticsearch query | `service.name: "vycepay-callback-service" and log.level: "ERROR"` count > 5 in 5m | Ops channel |
| VycePay Choice failure burst | Elasticsearch query | `event.dataset: "choice.baas" and event.outcome: "failure"` count > 15 in 5m | Ops channel |

Tune thresholds after a few days of baseline traffic.
