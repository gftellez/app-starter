/**
 * Navigable state lives in the query string, not in React state, so the back
 * button, a refresh and a shared link all land in the same place. Defaults
 * are omitted from the URL — a plain `/` still means "the home view".
 *
 * This is a minimal seed: two views and no filters. A real app will likely
 * grow more state here (a period, list filters, …) — follow the same shape:
 * add a field to AppState, read it in parseUrl, write it in buildUrl only
 * when it differs from the default.
 */

export type View = 'home' | 'about'

export interface AppState {
  view: View
}

const VIEWS: View[] = ['home', 'about']
const DEFAULT_VIEW: View = 'home'

export function parseUrl(search: string): AppState {
  const q = new URLSearchParams(search)
  const view = q.get('v')
  return {
    view: VIEWS.includes(view as View) ? (view as View) : DEFAULT_VIEW,
  }
}

export function buildUrl(state: AppState): string {
  const q = new URLSearchParams()
  if (state.view !== DEFAULT_VIEW) q.set('v', state.view)
  const s = q.toString()
  return s ? `?${s}` : ''
}
