import Link from 'next/link'
import { DashboardIcon, type IconName } from '@/components/ui/DashboardIcon'

export type AlertItem = {
  id: string
  label: string
  count: number
  href: string
  severity: 'danger' | 'warning' | 'ok'
  icon: IconName
}

/**
 * Operational alert list with severity styling and deep links.
 */
export function AlertPanel({
  items,
  loading,
}: {
  items: AlertItem[]
  loading?: boolean
}) {
  if (loading) {
    return (
      <div className="card alert-panel">
        <div className="section-title">Operational alerts</div>
        <div className="kpi-skeleton" style={{ marginTop: 12 }} />
        <div className="kpi-skeleton" style={{ marginTop: 8 }} />
        <div className="kpi-skeleton" style={{ marginTop: 8 }} />
      </div>
    )
  }

  const active = items.filter((i) => i.count > 0)
  const clear = items.filter((i) => i.count === 0)

  return (
    <div className="card alert-panel">
      <div className="section-hdr">
        <div>
          <div className="section-title">Operational alerts</div>
          <div className="section-sub muted">
            {active.length ? `${active.length} need attention` : 'All clear'}
          </div>
        </div>
      </div>
      <div className="alert-list">
        {items.map((item) => {
          const sev = item.count > 0 ? item.severity : 'ok'
          return (
            <Link key={item.id} href={item.href} className={`alert-row alert-${sev}`}>
              <span className="alert-row-icon">
                <DashboardIcon name={item.count > 0 ? item.icon : 'check'} size={16} />
              </span>
              <span className="alert-row-label">{item.label}</span>
              <span className="alert-row-count mono">{item.count.toLocaleString()}</span>
            </Link>
          )
        })}
      </div>
      {clear.length === items.length ? (
        <div className="muted" style={{ fontSize: 12, marginTop: 8 }}>No stuck work in the monitored windows.</div>
      ) : null}
    </div>
  )
}
