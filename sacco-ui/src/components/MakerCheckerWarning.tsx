import { useState } from 'react'
import { AlertTriangle } from 'lucide-react'
import { Modal } from './Modal'
import './MakerCheckerWarning.css'

interface MakerCheckerWarningProps {
  open: boolean
  onClose: () => void
  isAdmin: boolean
  onOverride: (reason: string) => void
  submitting?: boolean
  action?: string
}

export function MakerCheckerWarning({
  open,
  onClose,
  isAdmin,
  onOverride,
  submitting = false,
  action = 'approve',
}: MakerCheckerWarningProps) {
  const [reason, setReason] = useState('')

  function handleOverride() {
    if (!reason.trim()) return
    onOverride(reason.trim())
  }

  function handleClose() {
    setReason('')
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title="Dual Authorization Required"
      subtitle="Maker-checker control"
      width="sm"
      footer={
        isAdmin ? (
          <>
            <button
              type="button"
              className="btn btn--secondary"
              onClick={handleClose}
              disabled={submitting}
            >
              Cancel
            </button>
            <button
              type="button"
              className="btn btn--primary"
              onClick={handleOverride}
              disabled={!reason.trim() || submitting}
            >
              {submitting ? 'Processing...' : `Override & ${action.charAt(0).toUpperCase() + action.slice(1)}`}
            </button>
          </>
        ) : (
          <button type="button" className="btn btn--secondary" onClick={handleClose}>
            Close
          </button>
        )
      }
    >
      <div className="maker-checker-warning">
        <div className="maker-checker-warning__icon">
          <AlertTriangle size={20} strokeWidth={1.75} />
        </div>
        <p className="maker-checker-warning__message">
          You cannot {action} this record because you created it. A different authorized user must {action} it.
        </p>

        {isAdmin && (
          <div className="maker-checker-warning__override">
            <p className="maker-checker-warning__override-note">
              As an administrator, you may override this control with a documented reason.
            </p>
            <label className="form-label" htmlFor="override-reason">
              Override Reason <span className="form-required">*</span>
            </label>
            <textarea
              id="override-reason"
              className="form-textarea"
              rows={3}
              maxLength={500}
              value={reason}
              onChange={e => setReason(e.target.value)}
              placeholder="Explain why this override is necessary..."
              disabled={submitting}
            />
            <span className="maker-checker-warning__char-count">
              {reason.length}/500
            </span>
          </div>
        )}
      </div>
    </Modal>
  )
}
