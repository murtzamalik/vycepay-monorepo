import { DataPage } from '@/components/shared/DataPage'

export default function Page() {
  return <DataPage title="Transaction Volume Report" description="Aggregated transaction volume by period." endpoint="/reports/volume" />
}
