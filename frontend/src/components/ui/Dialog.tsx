import { type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'

interface DialogProps {
  title: string
  onClose: () => void
  children: ReactNode
}

/**
 * Portalled to document.body on purpose: if a view sits inside an element
 * with a CSS transform (a fade-in animation, a scaled container, …), that
 * transformed ancestor becomes the containing block for `position: fixed` —
 * a dialog rendered inline would be pinned to the scrolled content box
 * instead of the viewport, i.e. opens off-screen, backdrop and all. Portalling
 * to <body> sidesteps that regardless of where the dialog is invoked from.
 */
export function Dialog({ title, onClose, children }: DialogProps) {
  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-chrome/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative card w-full sm:max-w-md p-5 rounded-b-none sm:rounded-b-xl2 animate-fade-in-up max-h-[92dvh] overflow-y-auto">
        <div className="flex items-start justify-between mb-4">
          <p className="eyebrow">{title}</p>
          <button
            onClick={onClose}
            aria-label="Close"
            className="h-8 w-8 flex items-center justify-center rounded-lg text-ink-mute hover:bg-background"
          >
            <X size={16} />
          </button>
        </div>
        {children}
      </div>
    </div>,
    document.body
  )
}
