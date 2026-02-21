import { useCallback, useState, useRef, useEffect, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { CheckCircle, XCircle, Warning, Info, X } from '@phosphor-icons/react'
import { ToastContext, type ToastAPI } from '../context/ToastContext'

/* ─── Types ─── */

type ToastVariant = 'success' | 'error' | 'warning' | 'info'

interface ToastItem {
  id: string
  variant: ToastVariant
  title: string
  message?: string
  duration: number
  exiting?: boolean
}

/* ─── Icons ─── */

const ICONS: Record<ToastVariant, ReactNode> = {
  success: <CheckCircle size={16} />,
  error:   <XCircle size={16} />,
  warning: <Warning size={16} />,
  info:    <Info size={16} />,
}

/* ─── Single Toast ─── */

function Toast({ item, onDismiss }: { item: ToastItem; onDismiss: (id: string) => void }) {
  const [elapsed, setElapsed] = useState(0)
  const startRef = useRef(Date.now())

  useEffect(() => {
    if (item.exiting) return
    const frame = () => {
      setElapsed(Date.now() - startRef.current)
      raf = requestAnimationFrame(frame)
    }
    let raf = requestAnimationFrame(frame)
    return () => cancelAnimationFrame(raf)
  }, [item.exiting])

  const pct = Math.min(100, (elapsed / item.duration) * 100)

  return (
    <div className={`toast toast--${item.variant}${item.exiting ? ' toast--exiting' : ''}`} role="alert">
      <span className="toast-icon">{ICONS[item.variant]}</span>
      <div className="toast-body">
        <div className="toast-title">{item.title}</div>
        {item.message && <div className="toast-message">{item.message}</div>}
      </div>
      <button className="toast-dismiss" onClick={() => onDismiss(item.id)} aria-label="Dismiss">
        <X size={14} />
      </button>
      <div className="toast-countdown">
        <div className="toast-countdown-fill" style={{ width: `${100 - pct}%` }} />
      </div>
    </div>
  )
}

/* ─── Provider ─── */

let nextId = 0

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const timersRef = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map())

  const dismiss = useCallback((id: string) => {
    setToasts(prev => prev.map(t => t.id === id ? { ...t, exiting: true } : t))
    // Remove after exit animation
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id))
    }, 250)
    const timer = timersRef.current.get(id)
    if (timer) { clearTimeout(timer); timersRef.current.delete(id) }
  }, [])

  const add = useCallback((variant: ToastVariant, title: string, message?: string, duration = 4000) => {
    const id = `toast-${++nextId}`
    setToasts(prev => [...prev, { id, variant, title, message, duration }])
    const timer = setTimeout(() => dismiss(id), duration)
    timersRef.current.set(id, timer)
  }, [dismiss])

  const api: ToastAPI = {
    success: (t, m, d) => add('success', t, m, d),
    error:   (t, m, d) => add('error', t, m, d),
    warning: (t, m, d) => add('warning', t, m, d),
    info:    (t, m, d) => add('info', t, m, d),
    dismiss,
  }

  return (
    <ToastContext.Provider value={api}>
      {children}
      {createPortal(
        <div className="toast-container">
          {toasts.map(t => <Toast key={t.id} item={t} onDismiss={dismiss} />)}
        </div>,
        document.body
      )}
    </ToastContext.Provider>
  )
}
