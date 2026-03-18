# VycePay Database Schema

## Overview

Single MySQL database. Tables segregated by domain. Choice Bank IDs are references, not primary keys.

## DDL Location

Flyway migrations: `vycepay-database/src/main/resources/db/migration/`

- `V1__initial_schema.sql` - Initial schema

## Table Summary

| Table | Purpose |
|-------|---------|
| customer | Our user identity; external_id for APIs |
| kyc_verification | Choice onboarding tracking; links to callback 0001 |
| wallet | Choice account mapping; balance_cache from callback 0003 |
| transaction | Pending/completed tx; idempotency_key for deduplication |
| choice_bank_callback | Raw callback audit; processed flag for retry |
| activity_log | Compliance audit trail |
| otp_verification | Registration OTP (distinct from Choice OTP) |
