import { DataPage } from '@/components/shared/DataPage'

export default function Page() {
  return <DataPage title="Wallets" description="View wallet status, balances, and account mappings." endpoint="/wallets" />
}
