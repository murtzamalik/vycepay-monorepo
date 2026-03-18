# VycePay Documentation

This folder contains all project documentation for the VycePay Digital Wallet Platform.

## Start Here (including for Claude / AI)

| Document | Description |
|----------|-------------|
| [**VYCEPAY_OVERVIEW.md**](VYCEPAY_OVERVIEW.md) | **Entry point for the app: purpose, doc map, architecture and flow summary. Use this first when working with Claude or any AI on this codebase.** |
| [APPLICATION_PURPOSE_AND_SCOPE.md](APPLICATION_PURPOSE_AND_SCOPE.md) | Product purpose, users, in-scope features, out-of-scope |
| [ARCHITECTURE_DETAILED.md](ARCHITECTURE_DETAILED.md) | Services, ports, layers, BFF, callbacks, deployment |
| [BUSINESS_LOGIC_AND_FLOWS.md](BUSINESS_LOGIC_AND_FLOWS.md) | Step-by-step flows: auth, KYC, wallet, transactions, callbacks |
| [COMMUNICATION_AND_INTEGRATION.md](COMMUNICATION_AND_INTEGRATION.md) | Mobile↔BFF, BFF↔backends, Choice Bank API, webhooks, DB |
| [TECHNICAL_REFERENCE.md](TECHNICAL_REFERENCE.md) | Ports, config, key classes, DB summary, conventions |
| [CHOICE_BANK_INTEGRATION.md](CHOICE_BANK_INTEGRATION.md) | How Choice Bank is integrated: signing, requestId, outbound APIs, callbacks, config, code |
| [MOBILE_API_CONTRACT.md](MOBILE_API_CONTRACT.md) | Mobile-facing API: base URL, auth, headers, flows, error codes |
| [MOBILE_API_CONTRACT_DETAILED.md](MOBILE_API_CONTRACT_DETAILED.md) | Full mobile contract: request/response shapes for every endpoint |

## Other Documentation

| Document | Description |
|----------|-------------|
| [PROJECT_STATE.md](PROJECT_STATE.md) | Current state, what's done, pending – use when continuing development |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Short architecture summary (see ARCHITECTURE_DETAILED for full) |
| [ARCHITECTURE_PLAN.md](ARCHITECTURE_PLAN.md) | Full reference plan, Choice Bank behavior, implementation status |
| [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) | MySQL DDL, table explanations, migration strategy |
| [CHOICE_BANK_API.md](CHOICE_BANK_API.md) | Choice Bank BaaS API behavior, request/response, callbacks, async flows |
| [API_CONTRACTS.md](API_CONTRACTS.md) | Internal API contracts, endpoints, error envelopes |
| [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) | Implementation order, package layout, coding standards |
| [ACTUATOR.md](ACTUATOR.md) | Health checks, metrics, actuator configuration |

## Quick Links

- **Choice Bank Docs**: https://choice-bank.gitbook.io/choice-bank
- **Project Rule Book**: `.cursor/rules/vycepay-architecture.mdc`
