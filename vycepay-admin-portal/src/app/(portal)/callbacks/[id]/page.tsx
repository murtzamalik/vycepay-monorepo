import { DetailPage } from '@/components/shared/DetailPage'
import { CallbackActions } from '@/components/shared/ResourceActions'

export default function Page() {
  return <DetailPage title="Callback Detail" endpoint={(id) => `/callbacks/${id}`} actions={(id) => <CallbackActions id={id} />} />
}
