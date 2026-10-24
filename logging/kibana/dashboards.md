# Dashboard specs (Lens)

Build in Kibana **Dashboard** → create panels with Lens against data view `logs-vycepay-*`.

## 1. VycePay — Ops Overview

| Panel | Type | Config |
|-------|------|--------|
| Log volume by service | Bar / area | X: `@timestamp`, breakdown: `service.name` |
| ERROR count (24h) | Metric | Filter `log.level: "ERROR"`, count |
| WARN count (24h) | Metric | Filter `log.level: "WARN"` |
| Top error.code | Table | Terms on `error.code`, metric count |
| Top choice.code | Table | Terms on `choice.code`, metric count |

## 2. VycePay — Choice Bank

| Panel | Type | Config |
|-------|------|--------|
| BaaS call rate | Line | Filter `event.dataset: "choice.baas"`, count over time |
| Outcome pie | Pie | Filter choice.baas, terms `event.outcome` |
| Top paths | Table | Terms `choice.path` |
| Failures | Table | Filter `event.outcome: "failure"`, columns path/code/msg |

## 3. VycePay — Callbacks

| Panel | Type | Config |
|-------|------|--------|
| By notification action | Bar | Filter `event.dataset: "choice.callback"`, terms `event.action` |
| Callback errors | Metric | Filter callback service + ERROR |

## 4. VycePay — Auth & Security

| Panel | Type | Config |
|-------|------|--------|
| Auth WARN/ERROR | Line | `service.name: "vycepay-auth-service" and log.level: ("WARN" or "ERROR")` |
| Forbidden / security | Table | `event.dataset: "vyce.security"` |
