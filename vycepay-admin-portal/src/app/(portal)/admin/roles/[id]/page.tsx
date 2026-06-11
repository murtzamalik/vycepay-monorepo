import { DetailPage } from '@/components/shared/DetailPage'

export default function Page() {
  return <DetailPage title="Role Detail" endpoint={(id) => `/roles/${id}`} />
}
