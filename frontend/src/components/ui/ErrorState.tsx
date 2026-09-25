interface Props {
  onRetry: () => void
  message?: string
}

/**
 * What a failed request looks like. Without it a view falls through to its empty state, and
 * "something broke" becomes indistinguishable from "there is nothing here" — a user who sees an
 * empty list after a 500 believes their data is gone. Every query-backed view renders this on
 * `isError`, before its empty state.
 */
export function ErrorState({ onRetry, message = "This couldn't be loaded." }: Props) {
  return (
    <div role="alert" className="py-12 text-center space-y-3">
      <p className="text-sm text-ink-soft">{message}</p>
      <button
        onClick={onRetry}
        className="px-3 py-1.5 rounded-lg text-sm font-medium bg-accent text-white hover:bg-accent-hover transition"
      >
        Try again
      </button>
    </div>
  )
}
