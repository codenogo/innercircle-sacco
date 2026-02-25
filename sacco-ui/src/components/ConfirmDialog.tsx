import { Modal } from './Modal'
import { Warning, Trash, ShieldCheck } from '@phosphor-icons/react'
import type { ReactNode } from 'react'
import './ConfirmDialog.css'

type ConfirmVariant = 'danger' | 'warning' | 'info'

interface ConfirmDialogProps {
  open: boolean
  onClose: () => void
  onConfirm: () => void
  title: string
  description: string | ReactNode
  confirmLabel?: string
  cancelLabel?: string
  variant?: ConfirmVariant
  loading?: boolean
}

const VARIANT_ICON: Record<ConfirmVariant, ReactNode> = {
  danger: <Trash size={24} weight="duotone" />,
  warning: <Warning size={24} weight="duotone" />,
  info: <ShieldCheck size={24} weight="duotone" />,
}

export function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title,
  description,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  variant = 'warning',
  loading = false,
}: ConfirmDialogProps) {
  return (
    <Modal
      open={open}
      onClose={loading ? () => {} : onClose}
      title={title}
      width="sm"
      footer={
        <>
          <button
            className="btn btn--secondary"
            type="button"
            onClick={onClose}
            disabled={loading}
          >
            {cancelLabel}
          </button>
          <button
            className={`btn ${variant === 'danger' ? 'btn--danger' : 'btn--primary'}`}
            type="button"
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? 'Please wait…' : confirmLabel}
          </button>
        </>
      }
    >
      <div className="confirm-dialog-body">
        <div className={`confirm-dialog-icon confirm-dialog-icon--${variant}`}>
          {VARIANT_ICON[variant]}
        </div>
        <p className="confirm-dialog-description">{description}</p>
      </div>
    </Modal>
  )
}

