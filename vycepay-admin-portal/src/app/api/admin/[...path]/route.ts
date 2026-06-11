import { cookies } from 'next/headers'
import { NextRequest, NextResponse } from 'next/server'
const ADMIN_API = process.env.ADMIN_API_URL || process.env.NEXT_PUBLIC_ADMIN_API_URL || 'http://localhost:8090'

type Ctx = { params: Promise<{ path: string[] }> }
async function proxy(request: NextRequest, ctx: Ctx) {
  const { path } = await ctx.params
  const token = (await cookies()).get('admin_token')?.value
  if (!token) return NextResponse.json({ success: false, code: 'UNAUTHENTICATED', message: 'Login required' }, { status: 401 })
  const url = new URL(request.url)
  const target = `${ADMIN_API}/api/admin/v1/${path.join('/')}${url.search}`
  const body = request.method === 'GET' || request.method === 'HEAD' ? undefined : await request.text()
  const upstream = await fetch(target, { method: request.method, headers: { Authorization: `Bearer ${token}`, 'Content-Type': request.headers.get('Content-Type') || 'application/json' }, body, cache: 'no-store' })
  return new NextResponse(await upstream.arrayBuffer(), { status: upstream.status, headers: { 'Content-Type': upstream.headers.get('Content-Type') || 'application/json', 'Content-Disposition': upstream.headers.get('Content-Disposition') || '' } })
}
export async function GET(r: NextRequest, c: Ctx) { return proxy(r, c) }
export async function POST(r: NextRequest, c: Ctx) { return proxy(r, c) }
export async function PUT(r: NextRequest, c: Ctx) { return proxy(r, c) }
export async function PATCH(r: NextRequest, c: Ctx) { return proxy(r, c) }
export async function DELETE(r: NextRequest, c: Ctx) { return proxy(r, c) }
