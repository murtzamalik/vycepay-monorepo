import { redirect } from 'next/navigation'

/** Reports index — redirect to volume so /reports does not 404. */
export default function ReportsIndexPage() {
  redirect('/reports/volume')
}
