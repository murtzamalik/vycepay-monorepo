'use client'

export function JsonViewer({ data, redacted }: { data: unknown; redacted?: boolean }) {
  const text = JSON.stringify(data, null, 2)
  return (
    <div className="json-viewer-wrap">
      {redacted ? <p className="muted">Payload redacted — insufficient permission.</p> : null}
      <pre className="json-viewer card">{redacted ? '{}' : text}</pre>
      {!redacted ? (
        <button className="btn secondary btn-sm" type="button" onClick={() => navigator.clipboard.writeText(text)}>Copy JSON</button>
      ) : null}
    </div>
  )
}
