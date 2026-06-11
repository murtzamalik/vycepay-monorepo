# VycePay Admin Service

Spring Boot backoffice API: admin auth, RBAC, sessions, audit, and operational read/mutation endpoints.

## Build

This module is designed to live in the VycePay monorepo next to `vycepay-common` and `vycepay-database`.

From the monorepo root:

```bash
mvn -pl vycepay-admin-service -am test
```

If you clone only this repository, install `vycepay-common` and `vycepay-database` into your local Maven repository from the monorepo first, or adjust `pom.xml` to match your layout.

## Standalone mirror

Source is also mirrored at [github.com/murtzamalik/vycepay-admin-service](https://github.com/murtzamalik/vycepay-admin-service).
