'use client'

export function ChartCard({ title, subtitle, legend, height = 220, children }: {
  title: string
  subtitle?: string
  legend?: React.ReactNode
  height?: number
  children: React.ReactNode
}) {
  return (
    <div className="card chart-card">
      <div className="section-hdr">
        <div>
          <div className="section-title">{title}</div>
          {subtitle ? <div className="section-sub muted">{subtitle}</div> : null}
        </div>
        {legend}
      </div>
      <div style={{ height }}>{children}</div>
    </div>
  )
}
