import Link from 'next/link'
import type { ReactNode } from 'react'
import { DashboardIcon, type IconName } from '@/components/ui/DashboardIcon'

export type KpiTone = 'brand' | 'success' | 'warning' | 'danger' | 'muted'

/**
 * Metric tile for operational dashboards — icon, formatted value, optional link.
 */
export function KpiCard({
  label,
  value,
  sub,
  icon,
  tone = 'brand',
  href,
  loading,
}: {
  label: string
  value?: ReactNode
  sub?: string
  icon?: IconName
  tone?: KpiTone
  href?: string
  loading?: boolean
}) {
  const body = (
    <div className={`card kpi-card kpi-tone-${tone}${href ? ' kpi-card-link' : ''}`}>
      <div className="kpi-card-top">
        {icon ? (
          <span className={`kpi-icon kpi-icon-${tone}`} aria-hidden>
            <DashboardIcon name={icon} size={18} />
          </span>
        ) : null}
        <div className="kpi-label">{label}</div>
      </div>
      {loading ? (
        <div className="kpi-skeleton" />
      ) : (
        <div className="kpi-value">{value ?? '—'}</div>
      )}
      {sub && !loading ? <div className="muted kpi-sub">{sub}</div> : null}
    </div>
  )

  if (href && !loading) {
    return (
      <Link href={href} className="kpi-link">
        {body}
      </Link>
    )
  }
  return body
}
