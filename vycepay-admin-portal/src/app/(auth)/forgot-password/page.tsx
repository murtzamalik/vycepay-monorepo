'use client'

import { FormEvent, useState } from 'react'

export default function ForgotPasswordPage() {
  const [message, setMessage] = useState('')
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const form = new FormData(event.currentTarget); await fetch('/api/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email: form.get('email') }) }); setMessage('If the account exists, a reset flow has been started.') }
  return <main className="auth-page"><form className="auth-card grid" onSubmit={submit}><h1>Reset Password</h1>{message ? <div className="card">{message}</div> : null}<input className="input" name="email" type="email" placeholder="Admin email" required /><button className="btn">Request reset</button><a className="muted" href="/login">Back to login</a></form></main>
}
