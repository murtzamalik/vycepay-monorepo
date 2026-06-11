'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { navItems } from '@/lib/routes'

type Me = { adminUser?: { username?: string; fullName?: string }; menus?: { route: string; name: string }[] }

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const router = useRouter()
  const { data } = useQuery<Me>({ queryKey: ['me'], queryFn: async () => (await fetch('/api/auth/me', { cache: 'no-store' })).json().then((r) => r.data), retry: false })
  const menus = data?.menus?.length ? data.menus.map((m) => ({ href: m.route, label: m.name })) : navItems
  async function logout() { await fetch('/api/auth/logout', { method: 'POST' }); router.replace('/login') }
  return <div className="shell"><aside className="sidebar"><div className="sidebar-title">VycePay Admin</div>{menus.map((item) => <Link key={item.href} className={`nav-link ${pathname === item.href ? 'active' : ''}`} href={item.href}>{item.label}</Link>)}</aside><main className="main"><header className="topbar"><div><strong>{pathname.split('/').filter(Boolean).join(' / ') || 'Dashboard'}</strong></div><div className="actions"><span className="muted">{data?.adminUser?.fullName || data?.adminUser?.username || 'Admin'}</span><button className="btn secondary" onClick={logout}>Logout</button></div></header><section className="content">{children}</section></main></div>
}
