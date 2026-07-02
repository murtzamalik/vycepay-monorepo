import { cookies } from 'next/headers'
import { NextRequest, NextResponse } from 'next/server'
import { adminTokenCookieOptions } from '@/lib/auth-cookie'

const ADMIN_API = process.env.ADMIN_API_URL || process.env.NEXT_PUBLIC_ADMIN_API_URL || 'http://localhost:8090'

export async function POST(request: NextRequest) {
  const body = await request.text()
  const upstream = await fetch(`${ADMIN_API}/api/admin/v1/auth/login/mfa`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body,
    cache: 'no-store',
  })
  const text = await upstream.text()
  if (upstream.ok) {
    const json = JSON.parse(text)
    const token = json?.data?.token
    if (token) (await cookies()).set('admin_token', token, adminTokenCookieOptions())
  }
  return new NextResponse(text, { status: upstream.status, headers: { 'Content-Type': upstream.headers.get('Content-Type') || 'application/json' } })
}
