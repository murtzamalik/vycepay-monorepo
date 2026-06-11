import { cookies } from 'next/headers'
import { NextResponse } from 'next/server'
const ADMIN_API = process.env.ADMIN_API_URL || process.env.NEXT_PUBLIC_ADMIN_API_URL || 'http://localhost:8090'

export async function POST() {
  const cookieStore = await cookies()
  const token = cookieStore.get('admin_token')?.value
  if (token) {
    await fetch(`${ADMIN_API}/api/admin/v1/auth/logout`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      cache: 'no-store',
    }).catch(() => undefined)
  }
  cookieStore.delete('admin_token')
  return NextResponse.json({ success: true })
}
