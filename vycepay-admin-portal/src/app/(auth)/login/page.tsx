'use client'

import { FormEvent, useState } from 'react'
import { useRouter } from 'next/navigation'

export default function LoginPage() {
  const router = useRouter(); const [error, setError] = useState(''); const [loading, setLoading] = useState(false); const [mfaJti, setMfaJti] = useState('')
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setLoading(true); setError(''); const form = new FormData(event.currentTarget); const payload = mfaJti ? { jti: mfaJti, totpCode: form.get('totpCode') } : { username: form.get('username'), password: form.get('password') }; const res = await fetch(mfaJti ? '/api/auth/login/mfa' : '/api/auth/login', { method: 'POST', body: JSON.stringify(payload) }); const json = await res.json().catch(() => null); setLoading(false); if (!res.ok) { setError(mfaJti ? 'MFA verification failed' : 'Login failed'); return } if (json?.data?.mfaRequired && json?.data?.jti) { setMfaJti(json.data.jti); return } router.replace('/dashboard') }
  return <main className="auth-page"><form className="auth-card grid" onSubmit={submit}><div><h1>VycePay Admin</h1><p className="muted">{mfaJti ? 'Enter your authenticator app code.' : 'Sign in to the backoffice portal.'}</p></div>{error ? <div className="error">{error}</div> : null}{mfaJti ? <input className="input" name="totpCode" inputMode="numeric" pattern="[0-9]{6}" placeholder="6-digit MFA code" required /> : <><input className="input" name="username" placeholder="Username or email" required /><input className="input" name="password" type="password" placeholder="Password" required /></>}<button className="btn" disabled={loading}>{loading ? 'Signing in...' : mfaJti ? 'Verify MFA' : 'Sign in'}</button><a className="muted" href="/forgot-password">Forgot password?</a></form></main>
}
