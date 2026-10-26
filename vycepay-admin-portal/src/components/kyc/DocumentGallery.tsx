'use client'

type Docs = {
  idFrontUrl?: string | null
  idBackUrl?: string | null
  idPortraitUrl?: string | null
  selfieUrl?: string | null
} | null | undefined

function Tile({ label, href }: { label: string; href?: string | null }) {
  if (!href) {
    return (
      <div className="card" style={{ padding: 12, minHeight: 120 }}>
        <div className="muted" style={{ fontSize: 12, marginBottom: 8 }}>{label}</div>
        <div className="muted">—</div>
      </div>
    )
  }
  return (
    <div className="card" style={{ padding: 12, minHeight: 120 }}>
      <div className="muted" style={{ fontSize: 12, marginBottom: 8 }}>{label}</div>
      <a href={href} target="_blank" rel="noreferrer">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={href} alt={label} style={{ maxWidth: '100%', maxHeight: 160, objectFit: 'contain', borderRadius: 8 }} />
      </a>
      <div style={{ marginTop: 8 }}>
        <a href={href} target="_blank" rel="noreferrer" className="btn secondary btn-sm">Open ↗</a>
      </div>
    </div>
  )
}

export function DocumentGallery({ documents, restricted }: { documents: Docs; restricted?: boolean }) {
  if (restricted) {
    return <div className="card muted">Document access restricted (kyc:view_documents).</div>
  }
  const d = documents ?? {}
  return (
    <div className="card">
      <div className="section-title" style={{ marginBottom: 14 }}>Documents</div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: 12 }}>
        <Tile label="ID Front" href={d.idFrontUrl} />
        <Tile label="ID Back" href={d.idBackUrl} />
        <Tile label="ID Portrait" href={d.idPortraitUrl} />
        <Tile label="Selfie" href={d.selfieUrl} />
      </div>
    </div>
  )
}
