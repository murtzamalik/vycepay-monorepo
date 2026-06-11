import { DetailPage } from '@/components/shared/DetailPage'
import { CustomerActions } from '@/components/shared/ResourceActions'

export default function Page() {
  return <DetailPage title="Customer Detail" endpoint={(id) => `/customers/${id}`} actions={(id, data) => <CustomerActions id={id} status={data?.status} />} />
}
