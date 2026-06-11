import { DetailPage } from '@/components/shared/DetailPage'
import { WalletActions } from '@/components/shared/ResourceActions'

export default function Page() {
  return <DetailPage title="Wallet Detail" endpoint={(id) => `/wallets/${id}`} actions={(id, data) => <WalletActions id={id} status={data?.status} />} />
}
