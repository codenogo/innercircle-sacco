import { type ReactNode } from 'react'
import { CheckCircle2, XCircle, AlertTriangle, Info, X } from 'lucide-react'

type AlertVariant = 'success' | 'error' | 'warning' | 'info'

interface AlertProps {
  variant: AlertVariant
  title?: string
  children?: ReactNode
  onDismiss?: () => void
}

const ICONS: Record<AlertVariant, ReactNode> = {
  success: <CheckCircle2 size={16} strokeWidth={1.75} />,
  error:   <XCircle size={16} strokeWidth={1.75} />,
  warning: <AlertTriangle size={16} strokeWidth={1.75} />,
  info:    <Info size={16} strokeWidth={1.75} />,
}

export function Alert({ variant, title, children, onDismiss }: AlertProps) {
  return (
    <div className={`alert alert--${variant}`} role="alert">
      <span className="alert-icon">{ICONS[variant]}</span>
      <div className="alert-content">
        {title && <div className="alert-title">{title}</div>}
        {children && <div className="alert-message">{children}</div>}
      </div>
      {onDismiss && (
        <button className="alert-dismiss" onClick={onDismiss} aria-label="Dismiss">
          <X size={14} strokeWidth={1.75} />
        </button>
      )}
    </div>
  )
}
