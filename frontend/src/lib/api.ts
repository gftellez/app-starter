/**
 * Thin fetch wrapper for the backend API. Every call sends the auth token
 * header and throws on a non-2xx response, so callers only have to deal with
 * the happy path (or a caught error) — never a silently-ignored 4xx.
 *
 * Replace `getToken` with real auth storage once the app has a login flow.
 */

function getToken(): string {
  return localStorage.getItem('auth-token') ?? ''
}

async function apiFetch<T>(url: string, opts: RequestInit = {}): Promise<T> {
  const resp = await fetch(url, {
    ...opts,
    headers: { ...opts.headers, 'X-Auth-Token': getToken() },
  })
  if (!resp.ok) throw new Error(`API error ${resp.status}`)
  return resp.json()
}

export const api = {
  get: <T>(url: string) => apiFetch<T>(url),
  post: <T>(url: string, body: unknown) =>
    apiFetch<T>(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }),
}
