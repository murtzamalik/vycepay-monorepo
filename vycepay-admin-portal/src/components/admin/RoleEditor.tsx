'use client'

import { FormEvent, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { ErrorState, SkeletonTable } from '@/components/ui/States'

type MenuNode = { id: number; name: string; depth: number }

function flattenMenus(nodes: Record<string, unknown>[], depth = 0): MenuNode[] {
  const result: MenuNode[] = []
  for (const node of nodes) {
    result.push({ id: Number(node.id), name: String(node.name ?? ''), depth })
    if (Array.isArray(node.children)) {
      result.push(...flattenMenus(node.children as Record<string, unknown>[], depth + 1))
    }
  }
  return result
}

export function RoleEditor() {
  const params = useParams<{ id: string }>()
  const router = useRouter()
  const queryClient = useQueryClient()
  const [error, setError] = useState('')
  const role = useQuery({ queryKey: ['role', params.id], queryFn: () => apiFetch<Record<string, unknown>>(`/roles/${params.id}`) })
  const permissions = useQuery({ queryKey: ['permissions'], queryFn: () => apiFetch<{ code: string }[]>('/roles/permissions') })
  const menus = useQuery({ queryKey: ['menus'], queryFn: () => apiFetch<Record<string, unknown>[]>('/menus') })

  const save = useMutation({
    mutationFn: (body: unknown) => apiFetch(`/roles/${params.id}`, { method: 'PUT', body: JSON.stringify(body) }),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['role', params.id] }); router.refresh() },
  })

  const assignedMenuIds = new Set((role.data?.menuIds as number[] | undefined) ?? [])
  const menuNodes = menus.data ? flattenMenus(menus.data) : []

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError('')
    const form = new FormData(e.currentTarget)
    const permissionCodes = permissions.data?.filter((p) => form.get(`perm-${p.code}`) === 'on').map((p) => p.code) ?? []
    const menuIds = menuNodes
      .filter((m) => form.get(`menu-${m.id}`) === 'on')
      .map((m) => m.id)
    try {
      await save.mutateAsync({
        name: form.get('name'),
        description: form.get('description'),
        menuIds,
        permissionCodes,
        reason: form.get('reason'),
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    }
  }

  if (role.isLoading) return <SkeletonTable />
  if (role.error || !role.data) return <ErrorState message="Unable to load role." />

  return (
    <div className="grid">
      <PageHeader title={`Role: ${role.data.name}`} description={String(role.data.description ?? '')} />
      <form className="card grid" onSubmit={submit}>
        {error ? <div className="error">{error}</div> : null}
        <input className="input" name="name" defaultValue={String(role.data.name)} required />
        <input className="input" name="description" defaultValue={String(role.data.description ?? '')} />
        <div className="section-title">Menus</div>
        <div className="grid">
          {menuNodes.map((m) => (
            <label key={m.id} style={{ display: 'flex', gap: 8, paddingLeft: m.depth * 16 }}>
              <input type="checkbox" name={`menu-${m.id}`} defaultChecked={assignedMenuIds.has(m.id)} />
              <span>{m.name}</span>
              <span className="muted mono">#{m.id}</span>
            </label>
          ))}
        </div>
        <div className="section-title">Permissions</div>
        <div className="grid">
          {permissions.data?.map((p) => (
            <label key={p.code} style={{ display: 'flex', gap: 8 }}>
              <input type="checkbox" name={`perm-${p.code}`} defaultChecked={(role.data.permissionCodes as string[] | undefined)?.includes(p.code)} />
              <span className="mono">{p.code}</span>
            </label>
          ))}
        </div>
        <textarea className="input" name="reason" placeholder="Reason for change" required rows={3} />
        <button className="btn" disabled={save.isPending}>{save.isPending ? 'Saving...' : 'Save role'}</button>
      </form>
    </div>
  )
}
