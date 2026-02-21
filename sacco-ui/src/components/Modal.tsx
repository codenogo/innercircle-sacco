import { useEffect, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { X } from '@phosphor-icons/react'
import './Modal.css'

interface ModalProps {
  open: boolean
  onClose: () => void
  title: string
  subtitle?: string
  children: ReactNode
  footer?: ReactNode
  width?: 'sm' | 'md' | 'lg'
}

const FOCUSABLE = 'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])'

export function Modal({ open, onClose, title, subtitle, children, footer, width = 'md' }: ModalProps) {
  const cardRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = '' }
  }, [open])

  useEffect(() => {
    if (!open) return
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') { onClose(); return }
      if (e.key === 'Tab' && cardRef.current) {
        const focusable = cardRef.current.querySelectorAll<HTMLElement>(FOCUSABLE)
        if (focusable.length === 0) return
        const first = focusable[0]
        const last = focusable[focusable.length - 1]
        if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus() }
        else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus() }
      }
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [open, onClose])

  useEffect(() => {
    if (!open || !cardRef.current) return
    const first = cardRef.current.querySelector<HTMLElement>(FOCUSABLE)
    first?.focus()
  }, [open])

  if (!open) return null

  return createPortal(
    <div className="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="modal-title" aria-describedby={subtitle ? 'modal-subtitle' : undefined} onClick={e => { if (e.target === e.currentTarget) onClose() }}>
      <div ref={cardRef} className={`modal-card modal-card--${width}`}>
        <div className="modal-header">
          <div>
            <h2 id="modal-title" className="modal-title">{title}</h2>
            {subtitle && <p id="modal-subtitle" className="modal-subtitle">{subtitle}</p>}
          </div>
          <button className="modal-close" onClick={onClose} aria-label="Close">
            <X size={16} />
          </button>
        </div>
        <hr className="rule" />
        <div className="modal-body">{children}</div>
        {footer && (
          <>
            <hr className="rule" />
            <div className="modal-footer">{footer}</div>
          </>
        )}
      </div>
    </div>,
    document.body
  )
}
