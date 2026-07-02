'use client'

import { FormEvent, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { Suspense } from 'react'

function ResetPasswordForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError('')
    const form = new FormData(e.currentTarget)
    const res = await fetch('/api/auth/reset-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, newPassword: form.get('newPassword') }),
    })
    if (!res.ok) {
      setError('Password reset failed. The link may have expired.')
      return
    }
    setMessage('Password updated. You can sign in now.')
    setTimeout(() => router.replace('/login'), 2000)
  }

  return (
    <main className="auth-page">
      <form className="auth-card grid" onSubmit={submit}>
        <h1>Reset Password</h1>
        {message ? <div className="card">{message}</div> : null}
        {error ? <div className="error">{error}</div> : null}
        <input className="input" name="newPassword" type="password" placeholder="New password" required minLength={12} />
        <button className="btn" disabled={!token}>Update password</button>
        <a className="muted" href="/login">Back to login</a>
      </form>
    </main>
  )
}

export default function Page() {
  return <Suspense><ResetPasswordForm /></Suspense>
}
