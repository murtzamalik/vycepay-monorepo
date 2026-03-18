# Actuator Configuration

All services include Spring Boot Actuator for health checks and metrics.

## Endpoints

| Endpoint | Description | Default |
|----------|-------------|---------|
| /actuator/health | Liveness and readiness | Enabled |
| /actuator/info | Application info | Enabled |
| /actuator/metrics | Metrics (JVM, HTTP, etc.) | Exposed, may require auth in prod |

## Health

- **Liveness**: `/actuator/health/liveness` – Kubernetes liveness probe
- **Readiness**: `/actuator/health/readiness` – Kubernetes readiness (includes DB)
- **Aggregate**: `/actuator/health` – Combined status

## Configuration

To enable additional endpoints in production, add to `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized  # or "always" for dev
```
