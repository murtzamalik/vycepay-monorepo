import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  output: 'standalone',
  poweredByHeader: false,
  async headers() {
    const contentSecurityPolicy = [
      "default-src 'self'",
      "script-src 'self' 'unsafe-inline'",
      "style-src 'self' 'unsafe-inline'",
      "img-src 'self' data:",
      "font-src 'self' data:",
      "connect-src 'self'",
      "base-uri 'self'",
      "form-action 'self'",
      "object-src 'none'",
      "frame-ancestors 'none'",
    ].join('; ')

    return [
      {
        source: '/(.*)',
        headers: [
          { key: 'X-Content-Type-Options', value: 'nosniff' },
          { key: 'X-Frame-Options', value: 'DENY' },
          { key: 'Referrer-Policy', value: 'no-referrer' },
          { key: 'Content-Security-Policy', value: contentSecurityPolicy },
        ],
      },
    ]
  },
}

export default nextConfig
