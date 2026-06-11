import { DetailPage } from '@/components/shared/DetailPage'

export default function Page() {
  return <DetailPage title="KYC Detail" endpoint={(id) => `/kyc/${id}`} />
}
