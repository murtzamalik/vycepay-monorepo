# VycePay Admin Portal

Production backoffice portal for VycePay operations, finance, compliance, and admin management.

## Development

```bash
npm install
NEXT_PUBLIC_ADMIN_API_URL=http://localhost:8090 npm run dev
```

The Next.js app stores the admin token in an httpOnly cookie through server route handlers and proxies `/api/admin/**` to `vycepay-admin-service`.
