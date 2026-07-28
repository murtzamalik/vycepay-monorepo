# VycePay Database Schema

## Overview

Single MySQL database. Tables segregated by domain. Choice Bank IDs are references, not primary keys.

## DDL Location

Flyway migrations: `vycepay-database/src/main/resources/db/migration/`

- `V1__initial_schema.sql` — Initial schema
- `V2__device_token.sql` — FCM device tokens
- `V3__account_statement_job.sql` — Statement jobs
- `V4__admin_tables.sql` — Admin users/roles
- `V5__kyc_profile_columns.sql` — KYC profile columns
- `V6__customer_credentials_and_device.sql` — Username/PIN, `customer_device`, OTP purpose, `auth_audit_event`

## Table Summary

| Table | Purpose |
|-------|---------|
| customer | Identity + username/PIN hash + lockout columns; external_id for APIs |
| customer_device | Single bound IMEI (ANDROID_ID fingerprint) per customer |
| auth_audit_event | Auth security events (login fail, lockout, device bind, pin reset) — no secrets |
| kyc_verification | Choice onboarding tracking; links to callback 0001 |
| wallet | Choice account mapping; balance_cache from callback 0003 |
| transaction | Pending/completed tx; idempotency_key for deduplication |
| choice_bank_callback | Raw callback audit; processed flag for retry |
| activity_log | Compliance audit trail |
| otp_verification | Auth OTPs with `purpose` (SIGNUP, DEVICE_BIND, PIN_RESET, CREDENTIALS_MIGRATE) |
| device_token | FCM push tokens (separate from IMEI binding) |
