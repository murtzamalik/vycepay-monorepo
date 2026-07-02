'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { navSections } from '@/lib/routes'
import { Breadcrumb } from '@/components/layout/Breadcrumb'

type Me = { adminUser?: { username?: string; fullName?: string }; menus?: { route: string; name: string }[] }

function isActive(pathname: string, href: string) {
  return pathname === href || pathname.startsWith(`${href}/`)
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const router = useRouter()
  const { data } = useQuery<Me>({ queryKey: ['me'], queryFn: async () => (await fetch('/api/auth/me', { cache: 'no-store' })).json().then((r) => r.data), retry: false })

  async function logout() {
    await fetch('/api/auth/logout', { method: 'POST' })
    router.replace('/login')
  }

  const crumbs = [{ label: 'Home', href: '/dashboard' }, ...pathname.split('/').filter(Boolean).map((part, i, arr) => ({
    label: part,
    href: i < arr.length - 1 ? `/${arr.slice(0, i + 1).join('/')}` : undefined,
  }))]

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="sidebar-title">VycePay Admin</div>
        {navSections.map((section) => (
          <div key={section.label}>
            <div className="nav-section">{section.label}</div>
            {section.items.map((item) => (
              <Link key={item.href} className={`nav-link ${item.sub ? 'sub' : ''} ${isActive(pathname, item.href) ? 'active' : ''}`} href={item.href}>
                {item.label}
              </Link>
            ))}
          </div>
        ))}
      </aside>
      <main className="main">
        <header className="topbar">
          <Breadcrumb items={crumbs} />
          <div className="actions">
            <span className="muted">{data?.adminUser?.fullName || data?.adminUser?.username || 'Admin'}</span>
            <button className="btn secondary" onClick={logout}>Logout</button>
          </div>
        </header>
        <section className="content">{children}</section>
      </main>
    </div>
  )
}
