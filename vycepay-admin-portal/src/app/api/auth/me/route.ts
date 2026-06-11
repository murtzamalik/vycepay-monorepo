import { cookies } from 'next/headers'
import { NextResponse } from 'next/server'
const ADMIN_API = process.env.ADMIN_API_URL || process.env.NEXT_PUBLIC_ADMIN_API_URL || 'http://localhost:8090'
export async function GET() { const token=(await cookies()).get('admin_token')?.value; if(!token) return NextResponse.json({success:false}, {status:401}); const upstream=await fetch(`${ADMIN_API}/api/admin/v1/auth/me`, {headers:{Authorization:`Bearer ${token}`}, cache:'no-store'}); return new NextResponse(await upstream.text(), {status:upstream.status, headers:{'Content-Type': upstream.headers.get('Content-Type') || 'application/json'}}) }
