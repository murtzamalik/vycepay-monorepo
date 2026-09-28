'use client'

import Link from 'next/link'
import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { apiFetch, errorMessage } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { PermissionGuard } from '@/components/shared/PermissionGuard'

type TemplateDetail = {
  templateKey?: string
  category?: string
  name?: string
  body?: string
  active?: boolean | number
  placeholders?: string[]
  defaultBody?: string
  sampleVars?: Record<string, string>
}

export function SmsTemplateEdit() {
  const params = useParams<{ key: string }>()
  const key = decodeURIComponent(params.key)
  const router = useRouter()

  const { data, isLoading, error: loadError } = useQuery({
    queryKey: ['sms-template', key],
    queryFn: () => apiFetch<TemplateDetail>(`/sms/templates/${encodeURIComponent(key)}`),
  })

  const [name, setName] = useState('')
  const [body, setBody] = useState('')
  const [active, setActive] = useState(true)
  const [reason, setReason] = useState('')
  const [preview, setPreview] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [previewing, setPreviewing] = useState(false)

  useEffect(() => {
    if (!data) return
    setName(String(data.name ?? ''))
    setBody(String(data.body ?? ''))
    setActive(data.active === true || data.active === 1)
  }, [data])

  const placeholders = data?.placeholders ?? []
  const charCount = body.length

  const sampleHint = useMemo(() => {
    const vars = data?.sampleVars
    if (!vars) return ''
    return Object.entries(vars).slice(0, 4).map(([k, v]) => `${k}=${v}`).join(', ')
  }, [data?.sampleVars])

  function insertPlaceholder(token: string) {
    const chip = `{${token}}`
    setBody((prev) => (prev.length + chip.length <= 640 ? prev + chip : prev))
  }

  async function runPreview() {
    setError('')
    setPreviewing(true)
    try {
      const result = await apiFetch<{ rendered?: string }>(
        `/sms/templates/${encodeURIComponent(key)}/preview`,
        { method: 'POST', body: JSON.stringify({ body }) },
      )
      setPreview(String(result?.rendered ?? ''))
    } catch (err) {
      setError(errorMessage(err, 'Preview failed'))
    } finally {
      setPreviewing(false)
    }
  }

  async function onSave(e: FormEvent) {
    e.preventDefault()
    setError('')
    setSaving(true)
    try {
      await apiFetch(`/sms/templates/${encodeURIComponent(key)}`, {
        method: 'PUT',
        body: JSON.stringify({ name, body, active, reason }),
      })
      router.push('/sms/templates')
    } catch (err) {
      setError(errorMessage(err, 'Failed to save template'))
    } finally {
      setSaving(false)
    }
  }

  async function resetToDefault() {
    const def = data?.defaultBody
    if (!def) return
    setError('')
    setSaving(true)
    try {
      await apiFetch(`/sms/templates/${encodeURIComponent(key)}`, {
        method: 'PUT',
        body: JSON.stringify({
          name: data?.name ?? name,
          body: def,
          active: true,
          reason: reason.trim().length >= 10 ? reason : 'Reset SMS template to system default',
        }),
      })
      setBody(def)
      setActive(true)
      router.push('/sms/templates')
    } catch (err) {
      setError(errorMessage(err, 'Failed to reset template'))
    } finally {
      setSaving(false)
    }
  }

  if (isLoading) return <div className="muted">Loading…</div>
  if (loadError) return <div className="error">{errorMessage(loadError, 'Failed to load template')}</div>

  return (
    <PermissionGuard
      permission="sms:template:edit"
      fallback={
        <div className="grid">
          <PageHeader title={key} description="View-only (sms:template:edit required to change)." />
          <div className="card" style={{ whiteSpace: 'pre-wrap' }}>{body || data?.body}</div>
          <Link className="btn secondary" href="/sms/templates">Back</Link>
        </div>
      }
    >
      <div className="grid">
        <PageHeader
          title={key}
          description={`${data?.category ?? ''} · Placeholders: click a chip to insert. ${sampleHint ? `Sample: ${sampleHint}` : ''}`}
        />
        <form className="card grid" onSubmit={onSave} style={{ gap: 12, maxWidth: 720 }}>
          <label className="grid" style={{ gap: 4 }}>
            <span>Display name</span>
            <input value={name} onChange={(e) => setName(e.target.value)} maxLength={128} required />
          </label>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {placeholders.map((p) => (
              <button
                key={p}
                type="button"
                className="btn secondary btn-sm"
                onClick={() => insertPlaceholder(p)}
              >
                {`{${p}}`}
              </button>
            ))}
          </div>
          <label className="grid" style={{ gap: 4 }}>
            <span>Body ({charCount}/640)</span>
            <textarea
              value={body}
              onChange={(e) => setBody(e.target.value.slice(0, 640))}
              rows={6}
              maxLength={640}
              required
            />
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
            <span>Active (inactive falls back to code default)</span>
          </label>
          <label className="grid" style={{ gap: 4 }}>
            <span>Audit reason</span>
            <input
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              minLength={10}
              maxLength={512}
              required
            />
          </label>
          {preview ? (
            <div className="card" style={{ background: 'var(--surface-2, #f6f6f6)', whiteSpace: 'pre-wrap' }}>
              <strong>Preview</strong>
              <div style={{ marginTop: 8 }}>{preview}</div>
            </div>
          ) : null}
          {error ? <div className="error">{error}</div> : null}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            <button className="btn" type="submit" disabled={saving}>
              {saving ? 'Saving…' : 'Save'}
            </button>
            <button className="btn secondary" type="button" onClick={runPreview} disabled={previewing}>
              {previewing ? 'Preview…' : 'Preview'}
            </button>
            <button className="btn secondary" type="button" onClick={resetToDefault} disabled={saving}>
              Reset to default
            </button>
            <Link className="btn secondary" href="/sms/templates">Cancel</Link>
          </div>
        </form>
      </div>
    </PermissionGuard>
  )
}
