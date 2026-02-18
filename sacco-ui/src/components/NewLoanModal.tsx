import { useState, useMemo, useEffect } from 'react'
import { Modal } from './Modal'
import { Select } from './Select'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import type { LoanApplicationRequest } from '../types/loans'
import type { LoanProductConfigResponse } from '../types/config'

interface NewLoanModalProps {
  open: boolean
  onClose: () => void
  members: { id: string; name: string }[]
  onSubmit: (payload: LoanApplicationRequest) => Promise<void>
  isSubmitting: boolean
}

const FALLBACK_PRODUCTS: LoanProductConfigResponse[] = [
  { id: 'standard', name: 'Standard Loan', interestMethod: 'REDUCING_BALANCE', annualInterestRate: 12, maxTermMonths: 24, maxAmount: 500000, requiresGuarantor: true, active: true, createdAt: '', updatedAt: '' },
  { id: 'emergency', name: 'Emergency Loan', interestMethod: 'FLAT_RATE', annualInterestRate: 10, maxTermMonths: 12, maxAmount: 200000, requiresGuarantor: true, active: true, createdAt: '', updatedAt: '' },
]

const PERIODS = [3, 6, 9, 12]

function fmt(n: number) { return n.toLocaleString('en-KE') }

export function NewLoanModal({ open, onClose, members, onSubmit, isSubmitting }: NewLoanModalProps) {
  const { request } = useAuthenticatedApi()

  const [products, setProducts] = useState<LoanProductConfigResponse[]>(FALLBACK_PRODUCTS)
  const [memberId, setMemberId] = useState('')
  const [productId, setProductId] = useState('')
  const [principal, setPrincipal] = useState('')
  const [period, setPeriod] = useState('12')
  const [purpose, setPurpose] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (!open) return
    let cancelled = false
    async function fetchProducts() {
      try {
        const data = await request<LoanProductConfigResponse[]>(
          '/api/v1/config/loan-products?activeOnly=true',
        )
        if (!cancelled && data.length > 0) {
          setProducts(data)
        }
      } catch {
        // fall back to hardcoded products
      }
    }
    void fetchProducts()
    return () => { cancelled = true }
  }, [open, request])

  const selectedProduct = products.find(p => p.id === productId) ?? products[0]

  const memberOptions = members.map(m => ({ value: m.id, label: m.name }))
  const productOptions = products.map(p => ({
    value: p.id,
    label: `${p.name} — ${p.annualInterestRate}% p.a.`,
  }))
  const periodOptions = PERIODS.filter(p => p <= (selectedProduct?.maxTermMonths ?? 12)).map(p => ({
    value: String(p),
    label: `${p} months`,
  }))

  const monthlyRepayment = useMemo(() => {
    const p = Number(principal)
    const months = Number(period)
    if (!p || !months || !selectedProduct) return null
    const monthlyRate = selectedProduct.annualInterestRate / 100 / 12
    if (monthlyRate === 0) return p / months
    return (p * monthlyRate) / (1 - Math.pow(1 + monthlyRate, -months))
  }, [principal, period, selectedProduct])

  function reset() {
    setMemberId('')
    setProductId('')
    setPrincipal('')
    setPeriod('12')
    setPurpose('')
    setError('')
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')

    const resolvedProductId = productId || products[0]?.id
    if (!resolvedProductId) return

    try {
      await onSubmit({
        memberId,
        loanProductId: resolvedProductId,
        principalAmount: Number(principal),
        termMonths: Number(period),
        purpose: purpose.trim(),
      })
      reset()
    } catch (submitError) {
      if (submitError instanceof Error) {
        setError(submitError.message)
      } else {
        setError('Unable to submit loan application.')
      }
    }
  }

  function handleClose() {
    if (isSubmitting) return
    reset()
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title="New Loan Application"
      subtitle="Submit a loan request"
      width="md"
      footer={
        <>
          <button className="btn btn--secondary" type="button" onClick={handleClose} disabled={isSubmitting}>Cancel</button>
          <button className="btn btn--primary" type="submit" form="new-loan-form" disabled={isSubmitting}>
            {isSubmitting ? 'Submitting...' : 'Submit Application'}
          </button>
        </>
      }
    >
      <form id="new-loan-form" className="modal-form" onSubmit={event => void handleSubmit(event)}>
        {error && (
          <div className="ops-feedback ops-feedback--error" role="alert">
            {error}
          </div>
        )}

        <div className="field">
          <label className="field-label">Member</label>
          <Select options={memberOptions} value={memberId} onChange={setMemberId} placeholder="Select member" required searchable />
        </div>

        <div className="field">
          <label className="field-label">Loan Product</label>
          <Select options={productOptions} value={productId || products[0]?.id || ''} onChange={setProductId} />
          {selectedProduct && (
            <span className="field-hint">
              Interest: {selectedProduct.annualInterestRate}% p.a. ({selectedProduct.interestMethod.replace('_', ' ').toLowerCase()})
            </span>
          )}
        </div>

        <div className="field-row">
          <div className="field">
            <label className="field-label">Principal Amount</label>
            <input
              className="field-input"
              type="number"
              min={0}
              max={selectedProduct?.maxAmount}
              required
              disabled={isSubmitting}
              value={principal}
              onChange={e => setPrincipal(e.target.value)}
              placeholder="0"
            />
            <span className="field-hint">KES{selectedProduct?.maxAmount ? ` (max ${fmt(selectedProduct.maxAmount)})` : ''}</span>
          </div>
          <div className="field">
            <label className="field-label">Repayment Period</label>
            <Select options={periodOptions} value={period} onChange={setPeriod} />
          </div>
        </div>

        <div className="field">
          <label className="field-label">Purpose</label>
          <textarea className="field-input" required disabled={isSubmitting} value={purpose} onChange={e => setPurpose(e.target.value)} placeholder="Reason for the loan..." />
        </div>

        {monthlyRepayment !== null && (
          <div className="modal-computed">
            Est. monthly repayment: KES {fmt(Math.round(monthlyRepayment))}
          </div>
        )}
      </form>
    </Modal>
  )
}
