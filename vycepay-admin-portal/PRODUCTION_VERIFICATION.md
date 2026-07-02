# Admin Portal UX — Production Verification Checklist

Run against production or prod-like staging before sign-off.

## Build and deploy
- [x] `mvn -pl vycepay-admin-service -am test` passes
- [x] `npm ci && npm run typecheck && npm run lint && npm test && npm run build` passes
- [x] Docker images build; compose configs validate

## Auth and security
- [ ] Login + MFA works
- [ ] `/reset-password?token=...` flow works
- [ ] Logout revokes session
- [ ] PII masking for roles without `customer:view_pii`
- [ ] KYC documents gated by `kyc:view_documents`
- [ ] Callback payload gated by `callback:view`

## Functional smoke (all 26 screens)
- [ ] Dashboard charts load with custom date range
- [ ] Customer list: search, filter, paginate, View → Customer 360 (all 4 tabs)
- [ ] KYC, Wallet, Transaction, Callback lists → details with cross-links
- [ ] Failed transactions: date + error filters
- [ ] Reports: volume, KYC funnel, growth with from/to dates
- [ ] Audit log: customer vs admin tabs
- [ ] System health status cards
- [ ] Role/menu/user admin screens

## Data spot-checks
- [ ] Customer balance consistent across list/detail/wallet
- [ ] Report volume total matches transaction sum for date range
- [ ] Funnel stages monotonically decrease
