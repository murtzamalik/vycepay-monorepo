import { NextRequest, NextResponse } from 'next/server'
const ADMIN_API = process.env.ADMIN_API_URL || process.env.NEXT_PUBLIC_ADMIN_API_URL || 'http://localhost:8090'
export async function POST(request: NextRequest) { const upstream=await fetch(`${ADMIN_API}/api/admin/v1/auth/forgot-password`, {method:'POST', headers:{'Content-Type':'application/json'}, body: await request.text(), cache:'no-store'}); return new NextResponse(await upstream.text(), {status:upstream.status, headers:{'Content-Type': upstream.headers.get('Content-Type') || 'application/json'}}) }
