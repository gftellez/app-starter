import { useState } from 'react'
import { parseUrl, buildUrl, type View } from '@/lib/urlState'
import { NotesView } from '@/components/views/NotesView'

/**
 * Minimal shell: the active view is read from (and written back to) the URL,
 * so the pattern in lib/urlState.ts is wired up from day one. Add more views
 * by extending the View union and this array — the routing itself doesn't
 * change.
 */
const VIEWS: { id: View; label: string }[] = [
  { id: 'home', label: 'Home' },
  { id: 'about', label: 'About' },
]

export default function App() {
  const [view, setView] = useState<View>(() => parseUrl(window.location.search).view)

  function navigate(next: View) {
    setView(next)
    const url = buildUrl({ view: next })
    window.history.pushState(null, '', url || window.location.pathname)
  }

  return (
    <div className="min-h-screen bg-background text-ink">
      <nav className="border-b border-rule bg-surface">
        <div className="mx-auto max-w-3xl flex gap-2 px-4 py-3">
          {VIEWS.map(v => (
            <button
              key={v.id}
              onClick={() => navigate(v.id)}
              className={
                'px-3 py-1.5 rounded-lg text-sm font-medium transition ' +
                (view === v.id ? 'bg-accent text-white' : 'text-ink-mute hover:bg-background')
              }
            >
              {v.label}
            </button>
          ))}
        </div>
      </nav>
      <main className="mx-auto max-w-3xl px-4 py-8">
        {view === 'home' && (
          <div className="card p-6">
            <h1 className="text-xl font-semibold mb-2">Notes</h1>
            <NotesView />
          </div>
        )}
        {view === 'about' && (
          <div className="card p-6">
            <h1 className="text-xl font-semibold mb-2">About</h1>
            <p className="text-ink-soft text-sm">A second placeholder view, to show the URL-based navigation working.</p>
          </div>
        )}
      </main>
    </div>
  )
}
