import { DataPage } from '@/components/shared/DataPage'

export default function Page() {
  return <DataPage title="Customers" description="Search and monitor registered customers." endpoint="/customers" exportPath="/api/admin/customers/export" />
}
