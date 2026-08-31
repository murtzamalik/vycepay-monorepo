# VycePay Database Schema

## Overview

Single MySQL database. Tables segregated by domain. Choice Bank IDs are references, not primary keys.

## DDL Location

Flyway migrations: `vycepay-database/src/main/resources/db/migration/`

- `V1__initial_schema.sql` — Initial schema
- `V2__device_token.sql` — FCM device tokens
- `V3__account_statement_job.sql` — Statement jobs
- `V11__account_statement_job_email.sql` — Destination `email` on statement jobs
- `V4__admin_tables.sql` — Admin users/roles
- `V5__kyc_profile_columns.sql` — KYC profile columns
- `V6__fix_kyc_gender_column_type.sql` — KYC gender column fix (if present in env)
- `V7__customer_credentials_and_device.sql` — Username/PIN, `customer_device`, OTP purpose, `auth_audit_event`
- `V8__beneficiary.sql` — Saved transfer beneficiaries (soft-delete)
- `V9__notification_inbox.sql` — `customer_notification` inbox + `push_delivery_log` + admin notification permissions/menu
- `V10__money_event_dedupe.sql` — `customer_notification.dedupe_key` (unique per customer) + unique `transaction.choice_tx_id`

## Table Summary

| Table | Purpose |
|-------|---------|
| customer | Identity + username/PIN hash + lockout columns; external_id for APIs |
| customer_device | Single bound IMEI (ANDROID_ID fingerprint) per customer |
| auth_audit_event | Auth security events (login fail, lockout, device bind, pin reset) — no secrets |
| beneficiary | Saved payees per customer (nickname + rail + account); soft-delete |
| kyc_verification | Choice onboarding tracking; links to callback 0001 |
| wallet | Choice account mapping; balance_cache from callback 0003 |
| transaction | Pending/completed tx; idempotency_key for deduplication |
| choice_bank_callback | Raw callback audit; processed flag for retry |
| activity_log | Compliance audit trail |
| otp_verification | Auth OTPs with `purpose` (SIGNUP, DEVICE_BIND, PIN_RESET, CREDENTIALS_MIGRATE) |
| device_token | FCM push tokens (separate from IMEI binding) |
| customer_notification | In-app notification inbox (callback + admin compose); money events use `dedupe_key=TX:{txId}` |
| push_delivery_log | One row per FCM send attempt (AUTO / COMPOSE / RESEND) |
