import { DataPage } from '@/components/shared/DataPage'

export default function Page() {
  return <DataPage title="Failed Transactions" description="Analyze failed transactions and Choice Bank errors." endpoint="/transactions/failed" />
}
