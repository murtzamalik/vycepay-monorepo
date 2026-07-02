export const navSections = [
  {
    label: 'Operations',
    items: [
      { label: 'Dashboard', href: '/dashboard' },
      { label: 'Customers', href: '/customers' },
      { label: 'KYC', href: '/kyc' },
      { label: 'Wallets', href: '/wallets' },
    ],
  },
  {
    label: 'Transactions',
    items: [
      { label: 'All Transactions', href: '/transactions' },
      { label: 'Failed', href: '/transactions/failed', sub: true },
      { label: 'Callbacks', href: '/callbacks' },
    ],
  },
  {
    label: 'Reports',
    items: [
      { label: 'Volume', href: '/reports/volume' },
      { label: 'KYC Funnel', href: '/reports/kyc-funnel' },
      { label: 'Growth', href: '/reports/growth' },
    ],
  },
  {
    label: 'System',
    items: [
      { label: 'Audit Log', href: '/audit-log' },
      { label: 'System Health', href: '/system-health' },
    ],
  },
  {
    label: 'Admin',
    items: [
      { label: 'Menus', href: '/admin/menus' },
      { label: 'Roles', href: '/admin/roles' },
      { label: 'Admin Users', href: '/admin/users' },
    ],
  },
]

export const navItems = navSections.flatMap((s) => s.items)
