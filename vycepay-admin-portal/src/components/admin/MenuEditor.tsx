'use client'

import { FormEvent, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api'
import { PageHeader } from '@/components/layout/PageHeader'
import { ErrorState, SkeletonTable } from '@/components/ui/States'

export function MenuEditor() {
  const params = useParams<{ id: string }>()
  const router = useRouter()
  const queryClient = useQueryClient()
  const [error, setError] = useState('')
  const isNew = params.id === 'new'
  const menu = useQuery({
    queryKey: ['menu', params.id],
    queryFn: () => apiFetch<Record<string, unknown>>(`/menus`).then((trees) => {
      const flat: Record<string, unknown>[] = []
      const walk = (nodes: Record<string, unknown>[]) => nodes.forEach((n) => { flat.push(n); if (Array.isArray(n.children)) walk(n.children as Record<string, unknown>[]) })
      walk(trees as unknown as Record<string, unknown>[])
      return flat.find((m) => String(m.id) === params.id) ?? {}
    }),
    enabled: !isNew,
  })

  const save = useMutation({
    mutationFn: (body: unknown) => isNew
      ? apiFetch('/menus', { method: 'POST', body: JSON.stringify(body) })
      : apiFetch(`/menus/${params.id}`, { method: 'PUT', body: JSON.stringify(body) }),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['menus'] }); router.push('/admin/menus') },
  })

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError('')
    const form = new FormData(e.currentTarget)
    try {
      await save.mutateAsync({
        name: form.get('name'),
        route: form.get('route'),
        icon: form.get('icon'),
        parentId: form.get('parentId') ? Number(form.get('parentId')) : null,
        sortOrder: Number(form.get('sortOrder') ?? 0),
        reason: form.get('reason'),
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    }
  }

  if (!isNew && menu.isLoading) return <SkeletonTable />

  const m = menu.data ?? {}
  return (
    <div className="grid">
      <PageHeader title={isNew ? 'Create Menu' : `Edit Menu: ${m.name ?? params.id}`} description="Portal navigation metadata." />
      <form className="card grid" onSubmit={submit}>
        {error ? <div className="error">{error}</div> : null}
        <input className="input" name="name" placeholder="Name" defaultValue={String(m.name ?? '')} required />
        <input className="input" name="route" placeholder="/route" defaultValue={String(m.route ?? '')} required />
        <input className="input" name="icon" placeholder="icon-name" defaultValue={String(m.icon ?? '')} />
        <input className="input" name="parentId" placeholder="Parent menu ID (optional)" defaultValue={String(m.parentId ?? '')} />
        <input className="input" name="sortOrder" type="number" placeholder="Sort order" defaultValue={String(m.sortOrder ?? 0)} />
        <textarea className="input" name="reason" placeholder="Reason for change" required rows={3} />
        <button className="btn" disabled={save.isPending}>{save.isPending ? 'Saving...' : 'Save menu'}</button>
      </form>
    </div>
  )
}
