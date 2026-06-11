import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

const PUBLIC_PATHS = ['/login', '/forgot-password', '/reset-password']

export function middleware(request: NextRequest) {
  const path = request.nextUrl.pathname
  if (path.startsWith('/api') || path.startsWith('/_next') || path === '/favicon.ico') return NextResponse.next()
  const token = request.cookies.get('admin_token')?.value
  const isPublic = PUBLIC_PATHS.some((p) => path.startsWith(p))
  if (!token && !isPublic) return NextResponse.redirect(new URL('/login', request.url))
  if (token && (path === '/login' || path === '/')) return NextResponse.redirect(new URL('/dashboard', request.url))
  return NextResponse.next()
}

export const config = { matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'] }
