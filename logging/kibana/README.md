# Kibana assets for VycePay logs

## One-time setup

1. Start the logging stack (see [docs/LOGGING_ELK.md](../../docs/LOGGING_ELK.md)).
2. Open Kibana at `http://127.0.0.1:5601` (user `elastic` / `ELASTIC_PASSWORD`).
3. Create a **Data view**:
   - Name: `VycePay logs`
   - Index pattern: `logs-vycepay-*`
   - Time field: `@timestamp`
4. Import saved searches from [saved-searches.md](saved-searches.md) (create manually in Discover, or paste KQL).
5. Build dashboards from [dashboards.md](dashboards.md).
6. Configure alerts from [alerts.md](alerts.md).

## Columns for Discover (recommended)

`@timestamp`, `log.level`, `service.name`, `requestId`, `event.action`, `message`, `error.code`, `choiceBankRequestId`
