import { DetailPage } from '@/components/shared/DetailPage'

export default function Page() {
  return <DetailPage title="Transaction Detail" endpoint={(id) => `/transactions/${id}`} />
}
