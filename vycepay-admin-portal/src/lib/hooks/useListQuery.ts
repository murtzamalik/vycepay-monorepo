'use client'

import { useQuery } from '@tanstack/react-query'
import { useRouter, useSearchParams } from 'next/navigation'
import { useCallback, useMemo } from 'react'
import { apiFetch, asPaginated, buildQuery } from '@/lib/api'

export type ListFilters = Record<string, string>

export function useListQuery<T = Record<string, unknown>>(endpoint: string, defaults: ListFilters = {}) {
  const router = useRouter()
  const searchParams = useSearchParams()

  const filters = useMemo(() => {
    const f: ListFilters = { ...defaults }
    searchParams.forEach((value, key) => { f[key] = value })
    return f
  }, [searchParams, defaults])

  const queryKey = useMemo(() => [endpoint, filters], [endpoint, filters])

  const query = useQuery({
    queryKey,
    queryFn: () => apiFetch<unknown>(`${endpoint}${buildQuery(filters)}`),
  })

  const setFilters = useCallback((patch: Partial<ListFilters>) => {
    const next = new URLSearchParams(searchParams.toString())
    Object.entries(patch).forEach(([k, v]) => {
      if (v === undefined || v === null || v === '') next.delete(k)
      else next.set(k, v)
    })
    if (!('page' in patch)) next.set('page', '0')
    router.push(`?${next.toString()}`)
  }, [router, searchParams])

  const data = query.data ? asPaginated<T>(query.data) : null

  return { ...query, data, filters, setFilters }
}
