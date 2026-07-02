/** Cookie options for admin session token (HTTP test servers need ADMIN_ALLOW_HTTP_COOKIES=true). */
export function adminTokenCookieOptions() {
  const allowHttp = process.env.ADMIN_ALLOW_HTTP_COOKIES === 'true'
  return {
    httpOnly: true,
    secure: !allowHttp && process.env.NODE_ENV === 'production',
    sameSite: 'lax' as const,
    path: '/',
    maxAge: 15 * 60,
  }
}
