import { DataPage } from '@/components/shared/DataPage'

export default function Page() {
  return <DataPage title="Transactions" description="Review all transfer and deposit activity." endpoint="/transactions" exportPath="/api/admin/transactions/export" />
}
