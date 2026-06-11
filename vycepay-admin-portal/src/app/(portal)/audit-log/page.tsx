import { DataPage } from '@/components/shared/DataPage'

export default function Page() {
  return <DataPage title="Audit Log" description="Customer and admin activity audit trail." endpoint="/audit-log" exportPath="/api/admin/audit-log/export" />
}
