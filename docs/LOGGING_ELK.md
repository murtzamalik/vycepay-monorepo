# VycePay Structured Logging (Filebeat → Elasticsearch → Kibana)

## Architecture

```
Spring Boot services (JSON stdout)
        → Docker json-file (rotated)
        → Filebeat (app host)
        → Elasticsearch (data indices + ILM 14d)
        → Kibana Discover / Dashboards / Alerts
```

- **No Logstash / Kafka** in v1.
- Multi-JVM in one container is supported: every log line includes `service.name`.
- Correlate with `requestId` (API / `X-Request-Id`) and `choiceBankRequestId` (Choice BaaS).

Prefer running Elasticsearch + Kibana on a **separate host** from the 7-JVM app container. Filebeat must run on the **app host** (reads `/var/lib/docker/containers`).

## App changes (already in codebase)

| Piece | Location |
|-------|----------|
| JSON Logback | `vycepay-common/src/main/resources/logback-spring.xml` |
| Encoder dep | `logstash-logback-encoder` in `vycepay-common/pom.xml` |
| MDC keys / helpers | `com.vycepay.common.logging.*` |
| Request + customer MDC | `RequestIdFilter` |
| Async MDC | `MdcTaskDecorator` + `CallbackAsyncConfig` |
| Prod defaults | `application-prod.yml` in common: Choice `log-bodies: false`, HTTP audit traces off |
| Docker log rotation | `json-file` `max-size: 50m` / `max-file: 5` on app compose files |

### Never log (deny-list)

JWT secrets, Choice private keys, OTP codes, PIN, Firebase service-account JSON, raw national-ID photos.

### Prod Choice bodies

With `SPRING_PROFILES_ACTIVE=prod`, outbound Choice JSON bodies are **off** by default. Enable only for short debug windows:

```yaml
vycepay.choice-bank.logging.log-bodies: true
```

`/internal/choice-bank/http-traces` is **disabled** in prod (`vycepay.choice-bank.audit.http.enabled: false`). Keep it blocked at the reverse proxy even in non-prod if exposed publicly.

## JSON field schema

| Field | Purpose |
|-------|---------|
| `@timestamp` | UTC event time |
| `log.level` | INFO / WARN / ERROR |
| `message` | Human one-liner |
| `service.name` | `spring.application.name` |
| `service.environment` | `VYCEPAY_ENV` or active profiles |
| `requestId` | Cross-service HTTP correlation |
| `customerId` | From `X-Customer-Id` when present |
| `choiceBankRequestId` | Choice outbound / callback id |
| `onboardingRequestId` | KYC |
| `transactionId` | Local tx external id |
| `choiceTxId` | Choice tx id |
| `idempotencyKey` | Client idempotency |
| `event.dataset` | `vyce.api`, `choice.baas`, `choice.callback`, `vyce.sms`, `vyce.push`, `vyce.security` |
| `event.action` | e.g. `choice.baas.request`, `callback.0002` |
| `event.outcome` | `success` / `failure` / `unknown` |
| `error.code` | Catalog / business code |
| `error.stack_trace` | Exceptions only |
| `choice.path` / `choice.code` / `choice.msg` | BaaS metadata |

## Start logging stack

```bash
cp logging/.env.example logging/.env
# edit ELASTIC_PASSWORD (strong)

docker compose -f docker-compose.logging.yml --env-file logging/.env up -d

# First boot: set kibana_system password if Kibana cannot auth
docker compose -f docker-compose.logging.yml --env-file logging/.env --profile setup run --rm es-setup
```

- Kibana: `http://127.0.0.1:5601` (bound to localhost by default)
- ES: `http://127.0.0.1:9200` (localhost only; do not publish to `0.0.0.0` on the internet)

Filebeat mounts the host Docker log directory and ships only containers whose name contains `vycepay`.

### ILM

Policy `vycepay-logs-14d`: rollover daily / 10GB, **delete after 14 days**. See `logging/filebeat/ilm-policy.json`.

## Kibana

See [logging/kibana/](../logging/kibana/) for data view, saved searches, dashboards, and alerts.

## Support playbook

1. Ask the user / mobile for **`requestId`** from the API error envelope.
2. Kibana Discover → data view `logs-vycepay-*` → KQL: `requestId: "<id>"` → sort `@timestamp` ascending.
3. Read BFF → backend → `choice.baas.request` / `choice.baas.response` in order.
4. If async (KYC / transfer result): also search `choiceBankRequestId`, `onboardingRequestId`, or `transactionId`.
5. Open ERROR documents for `error.code`, `error.stack_trace`, `choice.code` / `choice.msg`.

## Smoke test

With the app container running:

```bash
./scripts/logging-smoke.sh
# or: CONTAINER=vycepay-server ./scripts/logging-smoke.sh
```

Expect JSON lines containing `"service.name"`.

## Security checklist

- [ ] `ELASTIC_PASSWORD` set; never commit `logging/.env`
- [ ] ES/Kibana published only on `127.0.0.1` or behind VPN
- [ ] ES heap capped (`ES_HEAP=512m` default) if sharing app host
- [ ] Prod: Choice log bodies off; http-traces off
- [ ] Kibana access limited to ops/support roles
