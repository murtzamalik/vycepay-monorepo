'use client'

import Link from 'next/link'

export function EntityLink({ href, children }: { href: string; children: React.ReactNode }) {
  return <Link className="entity-link" href={href}>{children}</Link>
}
