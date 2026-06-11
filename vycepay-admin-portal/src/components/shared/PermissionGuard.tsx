'use client'

import { useQuery } from '@tanstack/react-query'

type Me = { permissions?: string[] }

export function usePermissions() {
  const { data, isLoading } = useQuery<Me>({
    queryKey: ['me'],
    queryFn: async () => (await fetch('/api/auth/me', { cache: 'no-store' })).json().then((r) => r.data),
    retry: false,
  })
  return { permissions: data?.permissions ?? [], isLoading }
}

export function PermissionGuard({ permission, children, fallback = null }: { permission: string; children: React.ReactNode; fallback?: React.ReactNode }) {
  const { permissions, isLoading } = usePermissions()
  if (isLoading) return null
  return permissions.includes(permission) ? <>{children}</> : <>{fallback}</>
}
