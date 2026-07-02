'use client'

export function DetailLayout({
  header,
  actions,
  tabs,
  activeTab,
  onTabChange,
  children,
}: {
  header: React.ReactNode
  actions?: React.ReactNode
  tabs?: { id: string; label: string }[]
  activeTab?: string
  onTabChange?: (id: string) => void
  children: React.ReactNode
}) {
  return (
    <div className="grid">
      <div className="detail-header card">{header}{actions ? <div className="actions detail-actions">{actions}</div> : null}</div>
      {tabs?.length ? (
        <div className="tab-bar">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              className={`tab-btn ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => onTabChange?.(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </div>
      ) : null}
      <div>{children}</div>
    </div>
  )
}
