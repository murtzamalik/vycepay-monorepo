import { DetailPage } from '@/components/shared/DetailPage'
import { AdminPasswordResetAction } from '@/components/shared/ResourceActions'

export default function Page() {
  return <DetailPage title="Admin User Detail" endpoint={(id) => `/users/${id}`} actions={(id) => <AdminPasswordResetAction id={id} />} />
}
