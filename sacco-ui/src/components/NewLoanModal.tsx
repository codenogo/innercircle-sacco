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

function fmt(n: number) { return n.toLocaleString('en-KE') }

interface LoanFieldErrors {
  principal?: string
  purpose?: string
  memberId?: string
}

export function NewLoanModal({ open, onClose, members, onSubmit, isSubmitting }: NewLoanModalProps) {
  const { request } = useAuthenticatedApi()

  const [products, setProducts] = useState<LoanProductConfigResponse[]>([])
  const [loadingProducts, setLoadingProducts] = useState(false)
  const [productsError, setProductsError] = useState<string | null>(null)
  const [memberId, setMemberId] = useState('')
  const [productId, setProductId] = useState('')
  const [principal, setPrincipal] = useState('')
  const [period, setPeriod] = useState('')
  const [purpose, setPurpose] = useState('')
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<LoanFieldErrors>({})

  useEffect(() => {
    if (!open) return
    let cancelled = false
    async function fetchProducts() {
      setLoadingProducts(true)
      setProductsError(null)
      try {
        const data = await request<LoanProductConfigResponse[]>(
          '/api/v1/config/loan-products?activeOnly=true',
        )
        if (!cancelled) {
          setProducts(data)
          if (data.length === 0) {
            setProductsError('No active loan products are configured. Add one in Settings before submitting a loan.')
          }
        }
      } catch (fetchError) {
        if (!cancelled) {
          setProducts([])
          if (fetchError instanceof Error) {
            setProductsError(fetchError.message)
          } else {
            setProductsError('Unable to load loan products right now.')
          }
        }
      } finally {
        if (!cancelled) {
          setLoadingProducts(false)
        }
      }
    }
    void fetchProducts()
    return () => { cancelled = true }
  }, [open, request])

  useEffect(() => {
    if (products.length === 0) {
      setProductId('')
      return
    }
    if (!productId || !products.some(product => product.id === productId)) {
      setProductId(products[0].id)
    }
  }, [products, productId])

  const selectedProduct = products.find(p => p.id === productId) ?? products[0]

  const periodOptions = useMemo(() => {
    if (!selectedProduct) return []
    const minTerm = Math.max(1, selectedProduct.minTermMonths)
    const maxTerm = Math.max(minTerm, selectedProduct.maxTermMonths)
    return Array.from({ length: maxTerm - minTerm + 1 }, (_, index) => {
      const term = minTerm + index
      return { value: String(term), label: `${term} month${term === 1 ? '' : 's'}` }
    })
  }, [selectedProduct])

  useEffect(() => {
    if (!selectedProduct || periodOptions.length === 0) {
      setPeriod('')
      return
    }
    if (!period || !periodOptions.some(option => option.value === period)) {
      setPeriod(periodOptions[0].value)
    }
  }, [selectedProduct, periodOptions, period])

  const memberOptions = members.map(m => ({ value: m.id, label: m.name }))
  const productOptions = products.map(p => ({
    value: p.id,
    label: `${p.name} — ${p.annualInterestRate}% p.a.`,
  }))

  const monthlyRepayment = useMemo(() => {
    const p = Number(principal)
    const months = Number(period)
    if (!p || !months || !selectedProduct) return null
    const monthlyRate = selectedProduct.annualInterestRate / 100 / 12
    if (monthlyRate === 0) return p / months
    return (p * monthlyRate) / (1 - Math.pow(1 + monthlyRate, -months))
  }, [principal, period, selectedProduct])

  function validatePrincipal(value: string): string | undefined {
    const n = Number(value)
    if (!value || Number.isNaN(n) || n <= 0) return 'Enter a positive amount.'
    if (selectedProduct?.minAmount && n < selectedProduct.minAmount) {
      return `Minimum amount is KES ${fmt(selectedProduct.minAmount)}.`
    }
    if (selectedProduct?.maxAmount && n > selectedProduct.maxAmount) {
      return `Maximum amount is KES ${fmt(selectedProduct.maxAmount)}.`
    }
    return undefined
  }

  function blurPrincipal() {
    const err = validatePrincipal(principal)
    setFieldErrors(prev => ({ ...prev, principal: err }))
  }

  function reset() {
    setMemberId('')
    setProductId('')
    setPrincipal('')
    setPeriod('')
    setPurpose('')
    setError('')
    setFieldErrors({})
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')

    const errors: LoanFieldErrors = {}
    if (!memberId) {
      errors.memberId = 'Member is required.'
    }
    const principalErr = validatePrincipal(principal)
    if (principalErr) errors.principal = principalErr
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    const resolvedProductId = productId || products[0]?.id
    if (!resolvedProductId) {
      setError('No active loan product is available for submission.')
      return
    }
    if (!period) {
      setError('Repayment period is required.')
      return
    }

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
          <button className="btn btn--primary" type="submit" form="new-loan-form" disabled={isSubmitting || loadingProducts || !selectedProduct}>
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
          <label className="field-label field-label--required">Member</label>
          <Select options={memberOptions} value={memberId} onChange={setMemberId} placeholder="Select member" required searchable />
          {fieldErrors.memberId && <span className="field-error">{fieldErrors.memberId}</span>}
        </div>

        <div className="field">
          <label className="field-label field-label--required">Loan Product</label>
          <Select
            options={productOptions}
            value={productId || products[0]?.id || ''}
            onChange={setProductId}
            placeholder={loadingProducts ? 'Loading products...' : 'Select loan product'}
            disabled={loadingProducts || productOptions.length === 0}
          />
          {productsError && <span className="field-error">{productsError}</span>}
          {selectedProduct && (
            <span className="field-hint">
              Interest: {selectedProduct.annualInterestRate}% p.a. ({selectedProduct.interestMethod.replace('_', ' ').toLowerCase()})
            </span>
          )}
        </div>

        <div className="field-row">
          <div className="field">
            <label className="field-label field-label--required">Principal Amount</label>
            <input
              className={`field-input${fieldErrors.principal ? ' field-input--error' : ''}`}
              type="number"
              min={0}
              max={selectedProduct?.maxAmount}
              required
              disabled={isSubmitting}
              value={principal}
              onChange={e => { setPrincipal(e.target.value); if (fieldErrors.principal) setFieldErrors(prev => ({ ...prev, principal: undefined })) }}
              onBlur={blurPrincipal}
              placeholder="0"
              aria-invalid={!!fieldErrors.principal}
              aria-describedby={fieldErrors.principal ? 'principal-error' : undefined}
            />
            {fieldErrors.principal
              ? <span id="principal-error" className="field-error">{fieldErrors.principal}</span>
              : (
                <span className="field-hint">
                  KES
                  {selectedProduct?.minAmount ? ` (min ${fmt(selectedProduct.minAmount)})` : ''}
                  {selectedProduct?.maxAmount ? ` (max ${fmt(selectedProduct.maxAmount)})` : ''}
                </span>
              )}
          </div>
          <div className="field">
            <label className="field-label field-label--required">Repayment Period</label>
            <Select options={periodOptions} value={period} onChange={setPeriod} disabled={periodOptions.length === 0} />
          </div>
        </div>

        <div className="field">
          <label className="field-label field-label--required">Purpose</label>
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
