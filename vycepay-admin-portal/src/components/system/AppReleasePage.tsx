'use client'

import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch, errorMessage } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { PermissionGuard } from '@/components/shared/PermissionGuard'

type Policy = {
  platform?: string
  minSupportedVersionCode?: number
  latestVersionCode?: number
  latestVersionName?: string
  updateTitle?: string
  updateMessage?: string
  storeUrl?: string | null
  apkUrl?: string | null
  maintenance?: boolean | number
  maintenanceMessage?: string | null
  active?: boolean | number
  updatedAt?: string
  updatedByUsername?: string | null
}

function asBool(v: unknown): boolean {
  return v === true || v === 1 || v === '1'
}

function decide(policy: {
  minSupportedVersionCode: number
  latestVersionCode: number
  maintenance: boolean
}, versionCode: number | null) {
  if (policy.maintenance) {
    return { updateRequired: false, updateRecommended: false, maintenance: true, label: 'Maintenance' }
  }
  if (versionCode === null || Number.isNaN(versionCode) || versionCode < 0) {
    return { updateRequired: false, updateRecommended: false, maintenance: false, label: 'OK (no versionCode)' }
  }
  if (versionCode < policy.minSupportedVersionCode) {
    return { updateRequired: true, updateRecommended: true, maintenance: false, label: 'Force update' }
  }
  if (versionCode < policy.latestVersionCode) {
    return { updateRequired: false, updateRecommended: true, maintenance: false, label: 'Soft update' }
  }
  return { updateRequired: false, updateRecommended: false, maintenance: false, label: 'OK' }
}

export function AppReleasePage() {
  const queryClient = useQueryClient()
  const { data, isLoading, error: loadError } = useQuery({
    queryKey: ['app-release-policies'],
    queryFn: () => apiFetch<Policy[]>('/app-release-policies'),
  })

  const [platform, setPlatform] = useState<'ANDROID' | 'IOS'>('ANDROID')
  const [minSupportedVersionCode, setMin] = useState(1)
  const [latestVersionCode, setLatest] = useState(1)
  const [latestVersionName, setLatestName] = useState('1.0.0')
  const [updateTitle, setTitle] = useState('Update available')
  const [updateMessage, setMessage] = useState('Please update VycePay to continue.')
  const [storeUrl, setStoreUrl] = useState('')
  const [apkUrl, setApkUrl] = useState('')
  const [maintenance, setMaintenance] = useState(false)
  const [maintenanceMessage, setMaintenanceMessage] = useState('')
  const [active, setActive] = useState(true)
  const [reason, setReason] = useState('')
  const [sampleVersionCode, setSampleVersionCode] = useState('1')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [baselineMin, setBaselineMin] = useState(1)

  const selected = useMemo(
    () => (data ?? []).find((p) => String(p.platform).toUpperCase() === platform),
    [data, platform],
  )

  useEffect(() => {
    if (!selected) return
    setMin(Number(selected.minSupportedVersionCode ?? 1))
    setLatest(Number(selected.latestVersionCode ?? 1))
    setLatestName(String(selected.latestVersionName ?? '1.0.0'))
    setTitle(String(selected.updateTitle ?? 'Update available'))
    setMessage(String(selected.updateMessage ?? ''))
    setStoreUrl(String(selected.storeUrl ?? ''))
    setApkUrl(String(selected.apkUrl ?? ''))
    setMaintenance(asBool(selected.maintenance))
    setMaintenanceMessage(String(selected.maintenanceMessage ?? ''))
    setActive(asBool(selected.active))
    setBaselineMin(Number(selected.minSupportedVersionCode ?? 1))
    setError('')
  }, [selected])

  const preview = useMemo(() => {
    const vc = sampleVersionCode.trim() === '' ? null : Number(sampleVersionCode)
    return decide(
      { minSupportedVersionCode, latestVersionCode, maintenance },
      vc === null || Number.isNaN(vc) ? null : vc,
    )
  }, [minSupportedVersionCode, latestVersionCode, maintenance, sampleVersionCode])

  async function onSave(e: FormEvent) {
    e.preventDefault()
    setError('')
    if (minSupportedVersionCode > latestVersionCode) {
      setError('Min supported version code must be <= latest version code')
      return
    }
    if (reason.trim().length < 10) {
      setError('Audit reason must be at least 10 characters')
      return
    }
    if (minSupportedVersionCode > baselineMin) {
      const ok = window.confirm(
        `Raise min supported version code from ${baselineMin} to ${minSupportedVersionCode}? `
          + 'This will force-update all apps below the new min.',
      )
      if (!ok) return
    }
    setSaving(true)
    try {
      await apiFetch(`/app-release-policies/${platform}`, {
        method: 'PUT',
        body: JSON.stringify({
          minSupportedVersionCode,
          latestVersionCode,
          latestVersionName: latestVersionName.trim(),
          updateTitle: updateTitle.trim(),
          updateMessage: updateMessage.trim(),
          storeUrl: storeUrl.trim() || null,
          apkUrl: apkUrl.trim() || null,
          maintenance,
          maintenanceMessage: maintenanceMessage.trim() || null,
          active,
          reason: reason.trim(),
        }),
      })
      setReason('')
      await queryClient.invalidateQueries({ queryKey: ['app-release-policies'] })
    } catch (err) {
      setError(errorMessage(err, 'Failed to save app release policy'))
    } finally {
      setSaving(false)
    }
  }

  if (isLoading) return <div className="muted">Loading…</div>
  if (loadError) return <div className="error">{errorMessage(loadError, 'Failed to load policies')}</div>

  const form = (
    <form className="card grid" onSubmit={onSave} style={{ gap: 12, maxWidth: 720 }}>
      <p className="muted" style={{ margin: 0 }}>
        Force update = raise <strong>Min supported version code</strong>. Soft prompt = raise{' '}
        <strong>Latest version code</strong> without raising min. Block everyone ={' '}
        <strong>Maintenance</strong>.
      </p>

      <div style={{ display: 'flex', gap: 8 }}>
        {(['ANDROID', 'IOS'] as const).map((p) => (
          <button
            key={p}
            type="button"
            className={platform === p ? 'btn' : 'btn secondary'}
            onClick={() => setPlatform(p)}
          >
            {p}
          </button>
        ))}
      </div>

      <label className="grid" style={{ gap: 4 }}>
        <span>Min supported version code (force floor)</span>
        <input
          type="number"
          min={1}
          value={minSupportedVersionCode}
          onChange={(e) => setMin(Number(e.target.value))}
          required
        />
      </label>
      <label className="grid" style={{ gap: 4 }}>
        <span>Latest version code (soft target)</span>
        <input
          type="number"
          min={1}
          value={latestVersionCode}
          onChange={(e) => setLatest(Number(e.target.value))}
          required
        />
      </label>
      <label className="grid" style={{ gap: 4 }}>
        <span>Latest version name</span>
        <input value={latestVersionName} onChange={(e) => setLatestName(e.target.value)} maxLength={32} required />
      </label>
      <label className="grid" style={{ gap: 4 }}>
        <span>Update title</span>
        <input value={updateTitle} onChange={(e) => setTitle(e.target.value)} maxLength={128} required />
      </label>
      <label className="grid" style={{ gap: 4 }}>
        <span>Update message</span>
        <textarea
          value={updateMessage}
          onChange={(e) => setMessage(e.target.value.slice(0, 512))}
          rows={3}
          maxLength={512}
          required
        />
      </label>
      <label className="grid" style={{ gap: 4 }}>
        <span>Play Store / store URL</span>
        <input value={storeUrl} onChange={(e) => setStoreUrl(e.target.value)} maxLength={512} placeholder="https://..." />
      </label>
      <label className="grid" style={{ gap: 4 }}>
        <span>Sideload APK URL</span>
        <input value={apkUrl} onChange={(e) => setApkUrl(e.target.value)} maxLength={512} placeholder="https://... (optional)" />
      </label>
      <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <input type="checkbox" checked={maintenance} onChange={(e) => setMaintenance(e.target.checked)} />
        <span>Maintenance mode (blocks all clients)</span>
      </label>
      <label className="grid" style={{ gap: 4 }}>
        <span>Maintenance message</span>
        <textarea
          value={maintenanceMessage}
          onChange={(e) => setMaintenanceMessage(e.target.value.slice(0, 512))}
          rows={2}
          maxLength={512}
          disabled={!maintenance}
        />
      </label>
      <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
        <span>Policy active (off falls back to safe code defaults — use carefully)</span>
      </label>

      <div className="card" style={{ background: 'var(--surface-2, #f6f6f6)', gap: 8, display: 'grid' }}>
        <strong>Impact preview</strong>
        <label className="grid" style={{ gap: 4 }}>
          <span>Sample client versionCode</span>
          <input value={sampleVersionCode} onChange={(e) => setSampleVersionCode(e.target.value)} />
        </label>
        <div>
          Result: <strong>{preview.label}</strong>
          {' · '}updateRequired={String(preview.updateRequired)}
          {' · '}updateRecommended={String(preview.updateRecommended)}
          {' · '}maintenance={String(preview.maintenance)}
        </div>
      </div>

      <label className="grid" style={{ gap: 4 }}>
        <span>Audit reason</span>
        <input value={reason} onChange={(e) => setReason(e.target.value)} minLength={10} maxLength={512} required />
      </label>

      {selected ? (
        <p className="muted" style={{ margin: 0 }}>
          Last updated: {String(selected.updatedAt ?? '—')}
          {selected.updatedByUsername ? ` by ${selected.updatedByUsername}` : ''}
        </p>
      ) : null}

      {error ? <div className="error">{error}</div> : null}
      <button className="btn" type="submit" disabled={saving}>
        {saving ? 'Saving…' : 'Save'}
      </button>
    </form>
  )

  return (
    <div className="grid">
      <PageHeader
        title="App release"
        description="Manage mobile min/latest version codes, update copy, store URLs, and maintenance."
      />
      <PermissionGuard
        permission="mobile:release:edit"
        fallback={
          <div className="grid">
            <p className="muted">View-only (mobile:release:edit required to change).</p>
            <fieldset disabled style={{ border: 0, padding: 0, margin: 0 }}>
              {form}
            </fieldset>
          </div>
        }
      >
        {form}
      </PermissionGuard>
    </div>
  )
}
