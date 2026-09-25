import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { ErrorState } from '@/components/ui/ErrorState'

interface Note {
  id: string
  text: string
  createdAt: string
}

/**
 * The example screen, backed by the example endpoint (GET /api/notes). Delete both together —
 * but keep the four states every query-backed view has, in this order: loading, error, empty,
 * data. Skipping the error state is how a failed request comes to look like an empty ledger.
 */
export function NotesView() {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['notes'],
    queryFn: () => api.get<Note[]>('/api/notes'),
  })

  if (isLoading) return <p className="text-sm text-ink-mute py-12 text-center">Loading…</p>
  if (isError) return <ErrorState onRetry={() => refetch()} />
  if (!data || data.length === 0) {
    return <p className="text-sm text-ink-mute py-12 text-center">No notes yet.</p>
  }
  return (
    <ul className="divide-y divide-rule">
      {data.map(n => (
        <li key={n.id} className="py-3 text-sm">
          {n.text}
        </li>
      ))}
    </ul>
  )
}
