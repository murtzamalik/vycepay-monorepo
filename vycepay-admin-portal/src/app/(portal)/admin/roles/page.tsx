'use client'

import { ListPage } from '@/components/shared/ListPage'
import { EntityLink } from '@/components/ui/EntityLink'
import type { Column } from '@/lib/columns/types'

const columns: Column[] = [
  { key: 'name', label: 'Role' },
  { key: 'description', label: 'Description' },
  { key: 'menuCount', label: 'Menus' },
  { key: 'permissionCount', label: 'Permissions' },
  { key: 'actions', label: '', render: (r) => <EntityLink href={`/admin/roles/${r.id}`}><span className="btn secondary btn-sm">Edit</span></EntityLink> },
]

export default function Page() {
  return <ListPage title="Role Management" description="Manage role access and action permissions." endpoint="/roles" columns={columns} />
}
