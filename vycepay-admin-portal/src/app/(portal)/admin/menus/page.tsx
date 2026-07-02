'use client'

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { EntityLink } from '@/components/ui/EntityLink'
import { SkeletonTable } from '@/components/ui/States'

function MenuTree({ nodes, depth = 0 }: { nodes: Record<string, unknown>[]; depth?: number }) {
  return (
    <ul style={{ listStyle: 'none', paddingLeft: depth * 16 }}>
      {nodes.map((n) => (
        <li key={String(n.id)} style={{ padding: '6px 0' }}>
          <EntityLink href={`/admin/menus/${n.id}`}>{String(n.name)}</EntityLink>
          <span className="mono muted" style={{ marginLeft: 8, fontSize: 11 }}>{String(n.route)}</span>
          {Array.isArray(n.children) && n.children.length ? <MenuTree nodes={n.children as Record<string, unknown>[]} depth={depth + 1} /> : null}
        </li>
      ))}
    </ul>
  )
}

export default function Page() {
  const { data, isLoading } = useQuery({ queryKey: ['menus'], queryFn: () => apiFetch<Record<string, unknown>[]>('/menus') })
  return (
    <div className="grid">
      <PageHeader title="Menu Management" description="Manage portal navigation entries." actions={<EntityLink href="/admin/menus/new"><span className="btn">Create menu</span></EntityLink>} />
      {isLoading ? <SkeletonTable /> : <div className="card"><MenuTree nodes={data ?? []} /></div>}
    </div>
  )
}
