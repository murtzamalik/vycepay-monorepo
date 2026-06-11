'use client'

import { FormEvent, useState } from 'react'

export function ConfirmDialog({
  title,
  description,
  actionLabel,
  danger,
  onConfirm,
}: {
  title: string
  description: string
  actionLabel: string
  danger?: boolean
  onConfirm: (reason: string) => Promise<void>
}) {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (reason.trim().length < 10) {
      setError('Enter a clear reason with at least 10 characters.')
      return
    }
    setLoading(true)
    setError('')
    try {
      await onConfirm(reason.trim())
      setOpen(false)
      setReason('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Action failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <button className={`btn ${danger ? 'danger' : 'secondary'}`} onClick={() => setOpen(true)}>{actionLabel}</button>
      {open ? <div className="modal-backdrop" role="dialog" aria-modal="true" aria-label={title}>
        <form className="modal card grid" onSubmit={submit}>
          <div><h2>{title}</h2><p className="muted">{description}</p></div>
          {error ? <div className="error">{error}</div> : null}
          <label>Reason</label>
          <textarea className="input" value={reason} onChange={(e) => setReason(e.target.value)} rows={4} placeholder="Required for audit trail" />
          <div className="actions">
            <button className={`btn ${danger ? 'danger' : ''}`} disabled={loading}>{loading ? 'Submitting...' : 'Confirm'}</button>
            <button className="btn secondary" type="button" onClick={() => setOpen(false)}>Cancel</button>
          </div>
        </form>
      </div> : null}
    </>
  )
}
