import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { SkeletonText, SkeletonRow } from '../components/Skeleton'
import { ActionMenu } from '../components/ActionMenu'
import { Modal } from '../components/Modal'
import { Select } from '../components/Select'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useToast } from '../hooks/useToast'
import { ApiError } from '../services/apiClient'
import {
  createContributionSchedule,
  createLoanProduct,
  createPenaltyRule,
  createSystemConfig,
  getContributionSchedules,
  getLoanProducts,
  getPenaltyRules,
  getSystemConfigHealth,
  getSystemConfigs,
  updateContributionSchedule,
  updateLoanProduct,
  updatePenaltyRule,
  updateSystemConfig,
} from '../services/configService'
import type { MeResponse } from '../types/auth'
import type {
  CalculationMethod,
  ConfigHealthResponse,
  ContributionFrequency,
  ContributionScheduleConfigResponse,
  ContributionScheduleRequest,
  InterestMethod,
  LoanProductConfigResponse,
  LoanProductRequest,
  PenaltyFrequency,
  PenaltyRuleRequest,
  PenaltyRuleResponse,
  PenaltyTierRequest,
  PenaltyType,
  SystemConfigResponse,
} from '../types/config'
import './Settings.css'

type SystemInputKind = 'text' | 'currency' | 'month' | 'decimal' | 'day' | 'integer'

interface SystemSettingDefinition {
  key: string
  label: string
  description: string
  kind: SystemInputKind
  min?: number
  max?: number
  step?: string
  readOnly?: boolean
}

const SYSTEM_SETTING_DEFINITIONS: SystemSettingDefinition[] = [
  {
    key: 'chama.name',
    label: 'Chama Name',
    description: 'Official organization name shown across the application.',
    kind: 'text',
  },
  {
    key: 'chama.currency',
    label: 'Currency',
    description: 'Three-letter currency code used for financial display.',
    kind: 'currency',
  },
  {
    key: 'chama.financial_year_start_month',
    label: 'Financial Year Start Month',
    description: 'Month number where the financial year begins (1-12).',
    kind: 'month',
    min: 1,
    max: 12,
  },
  {
    key: 'contribution.welfare.fixed_amount',
    label: 'Welfare Fixed Amount',
    description: 'Fixed welfare deduction for welfare-eligible contribution categories.',
    kind: 'decimal',
    min: 0,
    step: '0.01',
  },
  {
    key: 'loan.batch.processing_day_of_month',
    label: 'Loan Batch Processing Day',
    description: 'Day of month when batch loan processing runs (1-28).',
    kind: 'day',
    min: 1,
    max: 28,
  },
  {
    key: 'loan.batch.new_loan_threshold_day',
    label: 'New Loan Threshold Day',
    description: 'Loans disbursed after this day skip accrual for that month.',
    kind: 'day',
    min: 1,
    max: 31,
  },
  {
    key: 'loan.penalty.grace_period_days',
    label: 'Loan Penalty Grace Period (Days)',
    description: 'Days after due date before penalties apply.',
    kind: 'integer',
    min: 0,
  },
  {
    key: 'loan.penalty.default_threshold_days',
    label: 'Loan Default Threshold (Days)',
    description: 'Days overdue before a loan is marked defaulted.',
    kind: 'integer',
    min: 0,
  },
  {
    key: 'loan.batch.last_processed_date',
    label: 'Last Loan Batch Processed Date',
    description: 'Technical system marker for completed loan batch processing.',
    kind: 'text',
    readOnly: true,
  },
]

const KNOWN_SYSTEM_KEYS = new Set(SYSTEM_SETTING_DEFINITIONS.map(def => def.key))

const INTEREST_METHOD_OPTIONS: { value: InterestMethod; label: string }[] = [
  { value: 'REDUCING_BALANCE', label: 'Reducing Balance' },
  { value: 'FLAT_RATE', label: 'Flat Rate' },
]

const INTEREST_METHOD_LABELS: Record<InterestMethod, string> = {
  REDUCING_BALANCE: 'Reducing Balance',
  FLAT_RATE: 'Flat Rate',
}

const CONTRIBUTION_FREQUENCY_OPTIONS: { value: ContributionFrequency; label: string }[] = [
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'WEEKLY', label: 'Weekly' },
]

const PENALTY_TYPE_OPTIONS: { value: PenaltyType; label: string }[] = [
  { value: 'LATE_CONTRIBUTION', label: 'Late Contribution' },
  { value: 'LOAN_DEFAULT', label: 'Loan Default' },
]

const PENALTY_TYPE_LABELS: Record<PenaltyType, string> = {
  LATE_CONTRIBUTION: 'Late Contribution',
  LOAN_DEFAULT: 'Loan Default',
}

const CALCULATION_METHOD_OPTIONS: { value: CalculationMethod; label: string }[] = [
  { value: 'PERCENTAGE', label: 'Percentage' },
  { value: 'FLAT', label: 'Flat Amount' },
]

const PENALTY_FREQUENCY_OPTIONS: { value: PenaltyFrequency; label: string }[] = [
  { value: 'DAILY', label: 'Daily' },
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'ONCE', label: 'Once' },
]

interface LoanFormState {
  name: string
  interestMethod: InterestMethod
  annualInterestRate: string
  minTermMonths: string
  maxTermMonths: string
  minAmount: string
  maxAmount: string
  contributionCapPercent: string
  poolCapAmount: string
  rolloverEnabled: boolean
  maxRolloverMonths: string
  rolloverSurchargeRate: string
  interestAccrualEnabled: boolean
  requiresGuarantor: boolean
  active: boolean
}

interface ScheduleFormState {
  name: string
  frequency: ContributionFrequency
  amount: string
  dueDayOfMonth: string
  gracePeriodDays: string
  mandatory: boolean
  expectedGrossAmount: string
  penaltyEnabled: boolean
  active: boolean
}

interface PenaltyFormState {
  name: string
  penaltyType: PenaltyType
  rate: string
  calculationMethod: CalculationMethod
  startOverdueDay: string
  endOverdueDay: string
  frequency: PenaltyFrequency
  maxApplications: string
  active: boolean
}

const EMPTY_LOAN_FORM: LoanFormState = {
  name: '',
  interestMethod: 'REDUCING_BALANCE',
  annualInterestRate: '',
  minTermMonths: '',
  maxTermMonths: '',
  minAmount: '',
  maxAmount: '',
  contributionCapPercent: '',
  poolCapAmount: '',
  rolloverEnabled: false,
  maxRolloverMonths: '',
  rolloverSurchargeRate: '',
  interestAccrualEnabled: false,
  requiresGuarantor: false,
  active: true,
}

const EMPTY_SCHEDULE_FORM: ScheduleFormState = {
  name: '',
  frequency: 'MONTHLY',
  amount: '',
  dueDayOfMonth: '',
  gracePeriodDays: '',
  mandatory: true,
  expectedGrossAmount: '',
  penaltyEnabled: true,
  active: true,
}

const EMPTY_PENALTY_FORM: PenaltyFormState = {
  name: '',
  penaltyType: 'LATE_CONTRIBUTION',
  rate: '',
  calculationMethod: 'PERCENTAGE',
  startOverdueDay: '1',
  endOverdueDay: '',
  frequency: 'MONTHLY',
  maxApplications: '',
  active: true,
}

function fmtCurrency(value: number | string): string {
  const parsed = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(parsed)) return '0'
  return parsed.toLocaleString('en-KE')
}

function fmtRate(rate: number, method: CalculationMethod): string {
  if (method === 'PERCENTAGE') return `${rate}%`
  return `KES ${fmtCurrency(rate)}`
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

function toLoanForm(product: LoanProductConfigResponse): LoanFormState {
  return {
    name: product.name,
    interestMethod: product.interestMethod,
    annualInterestRate: String(product.annualInterestRate),
    minTermMonths: String(product.minTermMonths),
    maxTermMonths: String(product.maxTermMonths),
    minAmount: String(product.minAmount),
    maxAmount: String(product.maxAmount),
    contributionCapPercent: product.contributionCapPercent == null ? '' : String(product.contributionCapPercent),
    poolCapAmount: product.poolCapAmount == null ? '' : String(product.poolCapAmount),
    rolloverEnabled: Boolean(product.rolloverEnabled),
    maxRolloverMonths: product.maxRolloverMonths == null ? '' : String(product.maxRolloverMonths),
    rolloverSurchargeRate: product.rolloverSurchargeRate == null ? '' : String(product.rolloverSurchargeRate),
    interestAccrualEnabled: Boolean(product.interestAccrualEnabled),
    requiresGuarantor: product.requiresGuarantor,
    active: product.active,
  }
}

function toScheduleForm(schedule: ContributionScheduleConfigResponse): ScheduleFormState {
  return {
    name: schedule.name,
    frequency: schedule.frequency,
    amount: String(schedule.amount),
    dueDayOfMonth: String(schedule.dueDayOfMonth),
    gracePeriodDays: String(schedule.gracePeriodDays),
    mandatory: Boolean(schedule.mandatory),
    expectedGrossAmount: String(schedule.expectedGrossAmount),
    penaltyEnabled: schedule.penaltyEnabled,
    active: schedule.active,
  }
}

function toPenaltyForm(rule: PenaltyRuleResponse): PenaltyFormState {
  const tier = rule.tiers?.find(item => item.active) ?? rule.tiers?.[0]
  return {
    name: rule.name,
    penaltyType: rule.penaltyType,
    rate: String(rule.rate),
    calculationMethod: rule.calculationMethod,
    startOverdueDay: String(tier?.startOverdueDay ?? 1),
    endOverdueDay: tier?.endOverdueDay == null ? '' : String(tier.endOverdueDay),
    frequency: tier?.frequency ?? 'MONTHLY',
    maxApplications: tier?.maxApplications == null ? '' : String(tier.maxApplications),
    active: rule.active,
  }
}

function toPenaltyTier(form: PenaltyFormState): PenaltyTierRequest {
  return {
    sequence: 1,
    startOverdueDay: Number.parseInt(form.startOverdueDay, 10),
    endOverdueDay: form.endOverdueDay.trim() ? Number.parseInt(form.endOverdueDay, 10) : undefined,
    frequency: form.frequency,
    calculationMethod: form.calculationMethod,
    rate: Number(form.rate),
    maxApplications: form.maxApplications.trim() ? Number.parseInt(form.maxApplications, 10) : undefined,
    active: true,
  }
}

function normalizeSystemValue(def: SystemSettingDefinition, rawValue: string): string {
  const trimmed = rawValue.trim()
  switch (def.kind) {
    case 'currency':
      return trimmed.toUpperCase()
    case 'month':
    case 'day':
    case 'integer':
      return String(Number.parseInt(trimmed, 10))
    case 'decimal':
      return Number(trimmed).toFixed(2)
    default:
      return trimmed
  }
}

function validateSystemValue(def: SystemSettingDefinition, rawValue: string): string | null {
  const trimmed = rawValue.trim()
  if (!def.readOnly && trimmed.length === 0) return 'Value is required.'

  switch (def.kind) {
    case 'currency':
      if (!/^[a-zA-Z]{3}$/.test(trimmed)) return 'Currency must be a 3-letter code (e.g. KES).'
      return null
    case 'month': {
      const n = Number.parseInt(trimmed, 10)
      if (Number.isNaN(n) || n < 1 || n > 12) return 'Month must be an integer between 1 and 12.'
      return null
    }
    case 'day': {
      const n = Number.parseInt(trimmed, 10)
      const min = def.min ?? 1
      const max = def.max ?? 31
      if (Number.isNaN(n) || n < min || n > max) return `Value must be an integer between ${min} and ${max}.`
      return null
    }
    case 'integer': {
      const n = Number.parseInt(trimmed, 10)
      const min = def.min ?? 0
      if (Number.isNaN(n) || n < min) return `Value must be an integer greater than or equal to ${min}.`
      return null
    }
    case 'decimal': {
      const n = Number(trimmed)
      const min = def.min ?? 0
      if (Number.isNaN(n) || n < min) return `Value must be a number greater than or equal to ${min}.`
      return null
    }
    default:
      return null
  }
}

export function Settings() {
  const { request } = useAuthenticatedApi()
  const toast = useToast()

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [profile, setProfile] = useState<MeResponse | null>(null)
  const [configReadRestricted, setConfigReadRestricted] = useState(false)

  const [systemConfigs, setSystemConfigs] = useState<SystemConfigResponse[]>([])
  const [configHealth, setConfigHealth] = useState<ConfigHealthResponse | null>(null)
  const [loanProducts, setLoanProducts] = useState<LoanProductConfigResponse[]>([])
  const [schedules, setSchedules] = useState<ContributionScheduleConfigResponse[]>([])
  const [penaltyRules, setPenaltyRules] = useState<PenaltyRuleResponse[]>([])

  const [systemValues, setSystemValues] = useState<Record<string, string>>({})
  const [systemSavingKey, setSystemSavingKey] = useState<string | null>(null)

  const [showLoanModal, setShowLoanModal] = useState(false)
  const [editingLoan, setEditingLoan] = useState<LoanProductConfigResponse | null>(null)
  const [loanForm, setLoanForm] = useState<LoanFormState>(EMPTY_LOAN_FORM)
  const [loanFormError, setLoanFormError] = useState<string | null>(null)
  const [submittingLoan, setSubmittingLoan] = useState(false)
  const [loanActionId, setLoanActionId] = useState<string | null>(null)

  const [showScheduleModal, setShowScheduleModal] = useState(false)
  const [editingSchedule, setEditingSchedule] = useState<ContributionScheduleConfigResponse | null>(null)
  const [scheduleForm, setScheduleForm] = useState<ScheduleFormState>(EMPTY_SCHEDULE_FORM)
  const [scheduleFormError, setScheduleFormError] = useState<string | null>(null)
  const [submittingSchedule, setSubmittingSchedule] = useState(false)
  const [scheduleActionId, setScheduleActionId] = useState<string | null>(null)

  const [showPenaltyModal, setShowPenaltyModal] = useState(false)
  const [editingPenalty, setEditingPenalty] = useState<PenaltyRuleResponse | null>(null)
  const [penaltyForm, setPenaltyForm] = useState<PenaltyFormState>(EMPTY_PENALTY_FORM)
  const [penaltyFormError, setPenaltyFormError] = useState<string | null>(null)
  const [submittingPenalty, setSubmittingPenalty] = useState(false)
  const [penaltyActionId, setPenaltyActionId] = useState<string | null>(null)

  const isAdmin = Boolean(profile?.roles?.includes('ADMIN'))

  const systemConfigMap = useMemo(() => {
    const map = new Map<string, SystemConfigResponse>()
    for (const config of systemConfigs) map.set(config.configKey, config)
    return map
  }, [systemConfigs])

  const otherSystemConfigs = useMemo(() => (
    systemConfigs.filter(config => !KNOWN_SYSTEM_KEYS.has(config.configKey))
  ), [systemConfigs])

  const loadSettings = useCallback(async () => {
    setLoading(true)
    setError(null)
    setConfigReadRestricted(false)
    try {
      const me = await request<MeResponse>('/api/v1/me')
      setProfile(me)

      try {
        const [configs, products, contributionSchedules, penalties] = await Promise.all([
          getSystemConfigs(request),
          getLoanProducts(undefined, request),
          getContributionSchedules(undefined, request),
          getPenaltyRules(undefined, request),
        ])

        const health = await getSystemConfigHealth(request).catch(() => null)

        setSystemConfigs(configs)
        setConfigHealth(health)
        setLoanProducts(products)
        setSchedules(contributionSchedules)
        setPenaltyRules(penalties)

        const nextValues: Record<string, string> = {}
        for (const def of SYSTEM_SETTING_DEFINITIONS) {
          nextValues[def.key] = configs.find(config => config.configKey === def.key)?.configValue ?? ''
        }
        setSystemValues(nextValues)
      } catch (configError) {
        if (configError instanceof ApiError && configError.status === 403 && !me.roles.includes('ADMIN')) {
          setConfigReadRestricted(true)
          setSystemConfigs([])
          setConfigHealth(null)
          setLoanProducts([])
          setSchedules([])
          setPenaltyRules([])
          setSystemValues({})
        } else {
          throw configError
        }
      }
    } catch (loadError) {
      setError(toErrorMessage(loadError, 'Failed to load settings'))
    } finally {
      setLoading(false)
    }
  }, [request])

  useEffect(() => {
    void loadSettings()
  }, [loadSettings])

  const displayName = profile?.member
    ? `${profile.member.firstName} ${profile.member.lastName}`.trim()
    : profile?.username ?? '-'
  const displayRole = profile?.roles?.length ? profile.roles.join(', ') : '-'

  function updateSystemValue(key: string, value: string) {
    setSystemValues(prev => ({ ...prev, [key]: value }))
  }

  async function handleSaveSystemSetting(def: SystemSettingDefinition) {
    if (!isAdmin || def.readOnly) return
    const rawValue = systemValues[def.key] ?? ''
    const validationError = validateSystemValue(def, rawValue)
    if (validationError) {
      toast.error(`Invalid ${def.label}`, validationError)
      return
    }

    const normalizedValue = normalizeSystemValue(def, rawValue)
    setSystemSavingKey(def.key)

    try {
      let saved: SystemConfigResponse
      try {
        saved = await updateSystemConfig(def.key, normalizedValue, request)
      } catch (updateError) {
        if (!(updateError instanceof ApiError) || updateError.status !== 404) {
          throw updateError
        }
        saved = await createSystemConfig(def.key, normalizedValue, def.description, request)
      }

      setSystemConfigs(prev => {
        const index = prev.findIndex(config => config.configKey === def.key)
        if (index === -1) return [saved, ...prev]
        return prev.map(config => (config.configKey === def.key ? saved : config))
      })
      setSystemValues(prev => ({ ...prev, [def.key]: saved.configValue }))
      toast.success('Setting saved', `${def.label} updated.`)
    } catch (saveError) {
      toast.error('Unable to save setting', toErrorMessage(saveError, 'Failed to save setting.'))
    } finally {
      setSystemSavingKey(null)
    }
  }

  function renderSystemInput(def: SystemSettingDefinition, value: string) {
    if (!isAdmin || def.readOnly) return null
    const inputType = def.kind === 'text' || def.kind === 'currency' ? 'text' : 'number'
    return (
      <input
        className="field-input settings-inline-input"
        type={inputType}
        min={def.min}
        max={def.max}
        step={def.step ?? (inputType === 'number' ? '1' : undefined)}
        value={value}
        onChange={event => updateSystemValue(def.key, event.target.value)}
        disabled={systemSavingKey === def.key}
        aria-label={def.label}
      />
    )
  }

  function openNewLoanModal() {
    setEditingLoan(null)
    setLoanForm(EMPTY_LOAN_FORM)
    setLoanFormError(null)
    setShowLoanModal(true)
  }

  function openEditLoanModal(product: LoanProductConfigResponse) {
    setEditingLoan(product)
    setLoanForm(toLoanForm(product))
    setLoanFormError(null)
    setShowLoanModal(true)
  }

  function closeLoanModal() {
    if (submittingLoan) return
    setShowLoanModal(false)
    setEditingLoan(null)
    setLoanFormError(null)
  }

  async function handleLoanSubmit(event: FormEvent) {
    event.preventDefault()
    if (!isAdmin) return

    const name = loanForm.name.trim()
    if (!name) {
      setLoanFormError('Loan product name is required.')
      return
    }

    const annualInterestRate = Number(loanForm.annualInterestRate)
    if (Number.isNaN(annualInterestRate) || annualInterestRate <= 0) {
      setLoanFormError('Annual interest rate must be greater than 0.')
      return
    }

    const minTermMonths = Number.parseInt(loanForm.minTermMonths, 10)
    if (Number.isNaN(minTermMonths) || minTermMonths < 1) {
      setLoanFormError('Min term must be at least 1 month.')
      return
    }

    const maxTermMonths = Number.parseInt(loanForm.maxTermMonths, 10)
    if (Number.isNaN(maxTermMonths) || maxTermMonths < 1) {
      setLoanFormError('Max term must be at least 1 month.')
      return
    }
    if (minTermMonths > maxTermMonths) {
      setLoanFormError('Min term cannot be greater than max term.')
      return
    }

    const minAmount = Number(loanForm.minAmount)
    if (Number.isNaN(minAmount) || minAmount <= 0) {
      setLoanFormError('Min amount must be greater than 0.')
      return
    }

    const maxAmount = Number(loanForm.maxAmount)
    if (Number.isNaN(maxAmount) || maxAmount <= 0) {
      setLoanFormError('Max amount must be greater than 0.')
      return
    }
    if (minAmount > maxAmount) {
      setLoanFormError('Min amount cannot be greater than max amount.')
      return
    }

    const contributionCapPercentRaw = loanForm.contributionCapPercent.trim()
    const contributionCapPercent = contributionCapPercentRaw.length > 0 ? Number(contributionCapPercentRaw) : Number.NaN
    if (
      contributionCapPercentRaw.length > 0
      && (Number.isNaN(contributionCapPercent) || contributionCapPercent < 0 || contributionCapPercent > 100)
    ) {
      setLoanFormError('Contribution cap percent must be between 0 and 100.')
      return
    }

    const poolCapAmountRaw = loanForm.poolCapAmount.trim()
    const poolCapAmount = poolCapAmountRaw.length > 0 ? Number(poolCapAmountRaw) : Number.NaN
    if (poolCapAmountRaw.length > 0 && (Number.isNaN(poolCapAmount) || poolCapAmount < 0)) {
      setLoanFormError('Pool cap amount must be greater than or equal to 0.')
      return
    }

    const maxRolloverMonthsRaw = loanForm.maxRolloverMonths.trim()
    const maxRolloverMonths = maxRolloverMonthsRaw.length > 0 ? Number.parseInt(maxRolloverMonthsRaw, 10) : undefined
    if (loanForm.rolloverEnabled && (maxRolloverMonths == null || Number.isNaN(maxRolloverMonths) || maxRolloverMonths < 0)) {
      setLoanFormError('Max rollover months is required and must be non-negative when rollover is enabled.')
      return
    }

    const rolloverSurchargeRateRaw = loanForm.rolloverSurchargeRate.trim()
    const rolloverSurchargeRate = rolloverSurchargeRateRaw.length > 0 ? Number(rolloverSurchargeRateRaw) : undefined
    if (loanForm.rolloverEnabled && (rolloverSurchargeRate == null || Number.isNaN(rolloverSurchargeRate) || rolloverSurchargeRate < 0)) {
      setLoanFormError('Rollover surcharge rate is required and must be non-negative when rollover is enabled.')
      return
    }

    const payload: LoanProductRequest = {
      name,
      interestMethod: loanForm.interestMethod,
      annualInterestRate,
      minTermMonths,
      maxTermMonths,
      minAmount,
      maxAmount,
      contributionCapPercent: contributionCapPercentRaw.length > 0 ? contributionCapPercent : undefined,
      poolCapAmount: poolCapAmountRaw.length > 0 ? poolCapAmount : undefined,
      rolloverEnabled: loanForm.rolloverEnabled,
      maxRolloverMonths: loanForm.rolloverEnabled ? maxRolloverMonths : undefined,
      rolloverSurchargeRate: loanForm.rolloverEnabled ? rolloverSurchargeRate : undefined,
      interestAccrualEnabled: loanForm.interestAccrualEnabled,
      requiresGuarantor: loanForm.requiresGuarantor,
      active: loanForm.active,
    }

    setSubmittingLoan(true)
    setLoanFormError(null)
    try {
      const saved = editingLoan
        ? await updateLoanProduct(editingLoan.id, payload, request)
        : await createLoanProduct(payload, request)
      setLoanProducts(prev => (
        editingLoan ? prev.map(item => (item.id === saved.id ? saved : item)) : [saved, ...prev]
      ))
      setShowLoanModal(false)
      setEditingLoan(null)
      toast.success(
        editingLoan ? 'Loan product updated' : 'Loan product created',
        `"${saved.name}" ${editingLoan ? 'updated' : 'created'}.`,
      )
    } catch (submitError) {
      setLoanFormError(toErrorMessage(submitError, 'Unable to save loan product.'))
    } finally {
      setSubmittingLoan(false)
    }
  }

  async function handleLoanToggle(product: LoanProductConfigResponse) {
    if (!isAdmin) return
    const targetState = !product.active
    if (!window.confirm(`${targetState ? 'Activate' : 'Deactivate'} "${product.name}"?`)) return

    setLoanActionId(product.id)
    const payload: LoanProductRequest = {
      name: product.name,
      interestMethod: product.interestMethod,
      annualInterestRate: product.annualInterestRate,
      minTermMonths: product.minTermMonths,
      maxTermMonths: product.maxTermMonths,
      minAmount: product.minAmount,
      maxAmount: product.maxAmount,
      contributionCapPercent: product.contributionCapPercent,
      poolCapAmount: product.poolCapAmount,
      rolloverEnabled: Boolean(product.rolloverEnabled),
      maxRolloverMonths: product.maxRolloverMonths,
      rolloverSurchargeRate: product.rolloverSurchargeRate,
      interestAccrualEnabled: Boolean(product.interestAccrualEnabled),
      requiresGuarantor: product.requiresGuarantor,
      active: targetState,
    }

    try {
      const updated = await updateLoanProduct(product.id, payload, request)
      setLoanProducts(prev => prev.map(item => (item.id === updated.id ? updated : item)))
      toast.success(
        `Loan product ${updated.active ? 'activated' : 'deactivated'}`,
        `"${updated.name}" ${updated.active ? 'activated' : 'deactivated'}.`,
      )
    } catch (toggleError) {
      toast.error('Unable to update loan product', toErrorMessage(toggleError, 'Failed to update loan product.'))
    } finally {
      setLoanActionId(null)
    }
  }

  function openNewScheduleModal() {
    setEditingSchedule(null)
    setScheduleForm(EMPTY_SCHEDULE_FORM)
    setScheduleFormError(null)
    setShowScheduleModal(true)
  }

  function openEditScheduleModal(schedule: ContributionScheduleConfigResponse) {
    setEditingSchedule(schedule)
    setScheduleForm(toScheduleForm(schedule))
    setScheduleFormError(null)
    setShowScheduleModal(true)
  }

  function closeScheduleModal() {
    if (submittingSchedule) return
    setShowScheduleModal(false)
    setEditingSchedule(null)
    setScheduleFormError(null)
  }

  async function handleScheduleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!isAdmin) return

    const name = scheduleForm.name.trim()
    if (!name) {
      setScheduleFormError('Schedule name is required.')
      return
    }

    const amount = Number(scheduleForm.amount)
    if (Number.isNaN(amount) || amount <= 0) {
      setScheduleFormError('Amount must be greater than 0.')
      return
    }

    const dueDayOfMonth = Number.parseInt(scheduleForm.dueDayOfMonth, 10)
    if (Number.isNaN(dueDayOfMonth) || dueDayOfMonth < 1 || dueDayOfMonth > 31) {
      setScheduleFormError('Due day must be between 1 and 31.')
      return
    }

    const gracePeriodDays = Number.parseInt(scheduleForm.gracePeriodDays, 10)
    if (Number.isNaN(gracePeriodDays) || gracePeriodDays < 0) {
      setScheduleFormError('Grace period days must be greater than or equal to 0.')
      return
    }

    const expectedGrossAmount = Number(scheduleForm.expectedGrossAmount)
    if (Number.isNaN(expectedGrossAmount) || expectedGrossAmount < 0) {
      setScheduleFormError('Expected gross amount must be greater than or equal to 0.')
      return
    }

    const payload: ContributionScheduleRequest = {
      name,
      frequency: scheduleForm.frequency,
      amount,
      dueDayOfMonth,
      gracePeriodDays,
      mandatory: scheduleForm.mandatory,
      expectedGrossAmount,
      penaltyEnabled: scheduleForm.penaltyEnabled,
      active: scheduleForm.active,
    }

    setSubmittingSchedule(true)
    setScheduleFormError(null)
    try {
      const saved = editingSchedule
        ? await updateContributionSchedule(editingSchedule.id, payload, request)
        : await createContributionSchedule(payload, request)
      setSchedules(prev => (
        editingSchedule ? prev.map(item => (item.id === saved.id ? saved : item)) : [saved, ...prev]
      ))
      setShowScheduleModal(false)
      setEditingSchedule(null)
      toast.success(
        editingSchedule ? 'Schedule updated' : 'Schedule created',
        `"${saved.name}" ${editingSchedule ? 'updated' : 'created'}.`,
      )
    } catch (submitError) {
      setScheduleFormError(toErrorMessage(submitError, 'Unable to save contribution schedule.'))
    } finally {
      setSubmittingSchedule(false)
    }
  }

  async function handleScheduleToggle(schedule: ContributionScheduleConfigResponse) {
    if (!isAdmin) return
    const targetState = !schedule.active
    if (!window.confirm(`${targetState ? 'Activate' : 'Deactivate'} "${schedule.name}"?`)) return

    setScheduleActionId(schedule.id)
    const payload: ContributionScheduleRequest = {
      name: schedule.name,
      frequency: schedule.frequency,
      amount: schedule.amount,
      dueDayOfMonth: schedule.dueDayOfMonth,
      gracePeriodDays: schedule.gracePeriodDays,
      mandatory: Boolean(schedule.mandatory),
      expectedGrossAmount: schedule.expectedGrossAmount,
      penaltyEnabled: schedule.penaltyEnabled,
      active: targetState,
    }

    try {
      const updated = await updateContributionSchedule(schedule.id, payload, request)
      setSchedules(prev => prev.map(item => (item.id === updated.id ? updated : item)))
      toast.success(
        `Schedule ${updated.active ? 'activated' : 'deactivated'}`,
        `"${updated.name}" ${updated.active ? 'activated' : 'deactivated'}.`,
      )
    } catch (toggleError) {
      toast.error('Unable to update schedule', toErrorMessage(toggleError, 'Failed to update schedule.'))
    } finally {
      setScheduleActionId(null)
    }
  }

  function openNewPenaltyModal() {
    setEditingPenalty(null)
    setPenaltyForm(EMPTY_PENALTY_FORM)
    setPenaltyFormError(null)
    setShowPenaltyModal(true)
  }

  function openEditPenaltyModal(rule: PenaltyRuleResponse) {
    setEditingPenalty(rule)
    setPenaltyForm(toPenaltyForm(rule))
    setPenaltyFormError(null)
    setShowPenaltyModal(true)
  }

  function closePenaltyModal() {
    if (submittingPenalty) return
    setShowPenaltyModal(false)
    setEditingPenalty(null)
    setPenaltyFormError(null)
  }

  async function handlePenaltySubmit(event: FormEvent) {
    event.preventDefault()
    if (!isAdmin) return

    const name = penaltyForm.name.trim()
    if (!name) {
      setPenaltyFormError('Penalty rule name is required.')
      return
    }

    const rate = Number(penaltyForm.rate)
    if (Number.isNaN(rate) || rate < 0) {
      setPenaltyFormError('Rate must be greater than or equal to 0.')
      return
    }

    const startOverdueDay = Number.parseInt(penaltyForm.startOverdueDay, 10)
    if (Number.isNaN(startOverdueDay) || startOverdueDay < 1) {
      setPenaltyFormError('Tier start overdue day must be at least 1.')
      return
    }

    const endOverdueDay = penaltyForm.endOverdueDay.trim()
      ? Number.parseInt(penaltyForm.endOverdueDay, 10)
      : undefined
    if (endOverdueDay != null && (Number.isNaN(endOverdueDay) || endOverdueDay < startOverdueDay)) {
      setPenaltyFormError('Tier end overdue day must be greater than or equal to tier start overdue day.')
      return
    }

    const maxApplications = penaltyForm.maxApplications.trim()
      ? Number.parseInt(penaltyForm.maxApplications, 10)
      : undefined
    if (maxApplications != null && (Number.isNaN(maxApplications) || maxApplications < 1)) {
      setPenaltyFormError('Tier max applications must be at least 1 when provided.')
      return
    }

    const payload: PenaltyRuleRequest = {
      name,
      penaltyType: penaltyForm.penaltyType,
      rate,
      calculationMethod: penaltyForm.calculationMethod,
      tiers: [{
        sequence: 1,
        startOverdueDay,
        endOverdueDay,
        frequency: penaltyForm.frequency,
        calculationMethod: penaltyForm.calculationMethod,
        rate,
        maxApplications,
        active: true,
      }],
      active: penaltyForm.active,
    }

    setSubmittingPenalty(true)
    setPenaltyFormError(null)
    try {
      const saved = editingPenalty
        ? await updatePenaltyRule(editingPenalty.id, payload, request)
        : await createPenaltyRule(payload, request)
      setPenaltyRules(prev => (
        editingPenalty ? prev.map(item => (item.id === saved.id ? saved : item)) : [saved, ...prev]
      ))
      setShowPenaltyModal(false)
      setEditingPenalty(null)
      toast.success(
        editingPenalty ? 'Penalty rule updated' : 'Penalty rule created',
        `"${saved.name}" ${editingPenalty ? 'updated' : 'created'}.`,
      )
    } catch (submitError) {
      setPenaltyFormError(toErrorMessage(submitError, 'Unable to save penalty rule.'))
    } finally {
      setSubmittingPenalty(false)
    }
  }

  async function handlePenaltyToggle(rule: PenaltyRuleResponse) {
    if (!isAdmin) return
    const targetState = !rule.active
    if (!window.confirm(`${targetState ? 'Activate' : 'Deactivate'} "${rule.name}"?`)) return

    setPenaltyActionId(rule.id)
    const payload: PenaltyRuleRequest = {
      name: rule.name,
      penaltyType: rule.penaltyType,
      rate: rule.rate,
      calculationMethod: rule.calculationMethod,
      tiers: rule.tiers?.length ? rule.tiers : [toPenaltyTier(toPenaltyForm(rule))],
      active: targetState,
    }

    try {
      const updated = await updatePenaltyRule(rule.id, payload, request)
      setPenaltyRules(prev => prev.map(item => (item.id === updated.id ? updated : item)))
      toast.success(
        `Penalty rule ${updated.active ? 'activated' : 'deactivated'}`,
        `"${updated.name}" ${updated.active ? 'activated' : 'deactivated'}.`,
      )
    } catch (toggleError) {
      toast.error('Unable to update penalty rule', toErrorMessage(toggleError, 'Failed to update penalty rule.'))
    } finally {
      setPenaltyActionId(null)
    }
  }

  if (loading) {
    return (
      <div className="settings-page">
        <div className="page-header">
          <h1 className="page-title">Settings</h1>
        </div>
        <hr className="rule rule--strong" />
        <div className="settings-group">
          <SkeletonText size="lg" />
          <hr className="rule" />
          <SkeletonRow cells={2} />
          <SkeletonRow cells={2} />
          <SkeletonRow cells={2} />
        </div>
        <div className="settings-group">
          <SkeletonText size="lg" />
          <hr className="rule" />
          <SkeletonRow cells={2} />
          <SkeletonRow cells={2} />
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="settings-page">
        <div className="page-header">
          <h1 className="page-title">Settings</h1>
        </div>
        <hr className="rule rule--strong" />
        <div className="ops-feedback ops-feedback--error" role="status">{error}</div>
      </div>
    )
  }

  return (
    <div className="settings-page">
      <div className="page-header">
        <h1 className="page-title">Settings</h1>
      </div>

      <hr className="rule rule--strong" />

      <div className="settings-group">
        <h2 className="settings-group-title">Profile</h2>
        <hr className="rule" />
        <div className="settings-row">
          <div>
            <span className="settings-row-label">Name</span>
            <span className="settings-row-desc">Your display name in the system</span>
          </div>
          <span className="settings-row-value">{displayName}</span>
        </div>
        <div className="settings-row">
          <div>
            <span className="settings-row-label">Email</span>
          </div>
          <span className="settings-row-value">{profile?.email ?? '-'}</span>
        </div>
        <div className="settings-row">
          <div>
            <span className="settings-row-label">Role</span>
          </div>
          <span className="badge badge--active">{displayRole}</span>
        </div>
      </div>

      {configReadRestricted && (
        <div className="ops-feedback ops-feedback--error" role="status">
          Read-only settings data is unavailable until config API read permissions are enabled for your role.
        </div>
      )}

      {!configReadRestricted && (
        <>
          <div className="settings-group">
            <h2 className="settings-group-title">System</h2>
            <hr className="rule" />
            {configHealth && (
              <div className={`ops-feedback ${configHealth.healthy ? 'ops-feedback--success' : 'ops-feedback--error'}`} role="status">
                {configHealth.healthy
                  ? 'Policy config health: healthy.'
                  : `Policy config health: ${configHealth.missingKeys.length} missing, ${configHealth.invalidKeys.length} invalid.`}
              </div>
            )}
            {configHealth && !configHealth.healthy && (
              <div className="settings-row-block settings-row-block--compact">
                {configHealth.missingKeys.length > 0 && (
                  <div className="settings-row">
                    <div>
                      <span className="settings-row-label">Missing Keys</span>
                      <span className="settings-row-desc">{configHealth.missingKeys.join(', ')}</span>
                    </div>
                  </div>
                )}
                {configHealth.invalidKeys.length > 0 && (
                  <div className="settings-row">
                    <div>
                      <span className="settings-row-label">Invalid Keys</span>
                      <span className="settings-row-desc">{configHealth.invalidKeys.join(', ')}</span>
                    </div>
                  </div>
                )}
              </div>
            )}
            {SYSTEM_SETTING_DEFINITIONS.map(def => {
              const value = systemValues[def.key] ?? systemConfigMap.get(def.key)?.configValue ?? ''
              return (
                <div key={def.key} className="settings-row-block">
                  <div className="settings-row">
                    <div>
                      <span className="settings-row-label">{def.label}</span>
                      <span className="settings-row-desc">{def.description}</span>
                    </div>
                    {!isAdmin || def.readOnly ? (
                      <div className="settings-row-actions">
                        <span className="settings-row-value">{value || '-'}</span>
                        {def.readOnly && <span className="badge badge--pending">Read-only</span>}
                      </div>
                    ) : (
                      <div className="settings-row-control">
                        {renderSystemInput(def, value)}
                        <button
                          type="button"
                          className="btn btn--secondary"
                          onClick={() => void handleSaveSystemSetting(def)}
                          disabled={systemSavingKey === def.key}
                        >
                          {systemSavingKey === def.key ? 'Saving...' : 'Save'}
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              )
            })}

            {otherSystemConfigs.length > 0 && (
              <>
                <hr className="rule" />
                <h3 className="settings-subtitle">Other System Keys (Read-only)</h3>
                {otherSystemConfigs.map(config => (
                  <div key={config.id} className="settings-row">
                    <div>
                      <span className="settings-row-label data">{config.configKey}</span>
                      <span className="settings-row-desc">{config.description || 'No description provided.'}</span>
                    </div>
                    <span className="settings-row-value">{config.configValue || '-'}</span>
                  </div>
                ))}
              </>
            )}
          </div>

          <div className="settings-group">
            <div className="settings-group-header">
              <h2 className="settings-group-title">Loan Products</h2>
              {isAdmin && (
                <button type="button" className="btn btn--primary" onClick={openNewLoanModal}>
                  New Loan Product
                </button>
              )}
            </div>
            <hr className="rule" />
            {loanProducts.length === 0 ? (
              <div className="settings-row">
                <span className="settings-row-label">No loan products configured</span>
              </div>
            ) : loanProducts.map(product => (
              <div key={product.id} className="settings-row">
                <div>
                  <span className="settings-row-label">{product.name}</span>
                  <span className="settings-row-desc">
                    {INTEREST_METHOD_LABELS[product.interestMethod]} &middot; Max term {product.maxTermMonths} months
                    &middot; Max amount KES {fmtCurrency(product.maxAmount)}
                    {product.requiresGuarantor ? ' · Guarantor required' : ' · No guarantor'}
                  </span>
                </div>
                <div className="settings-row-actions">
                  <span className="settings-row-value">{product.annualInterestRate}% p.a.</span>
                  <span className={`badge ${product.active ? 'badge--active' : 'badge--inactive'}`}>
                    {product.active ? 'Active' : 'Inactive'}
                  </span>
                  {isAdmin && (
                    <ActionMenu
                      actions={[
                        { label: 'Edit', onClick: () => openEditLoanModal(product), disabled: loanActionId === product.id },
                        {
                          label: product.active ? 'Deactivate' : 'Activate',
                          onClick: () => void handleLoanToggle(product),
                          disabled: loanActionId === product.id,
                          variant: product.active ? 'danger' : 'default',
                        },
                      ]}
                    />
                  )}
                </div>
              </div>
            ))}
          </div>

          <div className="settings-group">
            <div className="settings-group-header">
              <h2 className="settings-group-title">Contribution Schedules</h2>
              {isAdmin && (
                <button type="button" className="btn btn--primary" onClick={openNewScheduleModal}>
                  New Schedule
                </button>
              )}
            </div>
            <hr className="rule" />
            {schedules.length === 0 ? (
              <div className="settings-row">
                <span className="settings-row-label">No contribution schedules configured</span>
              </div>
            ) : schedules.map(schedule => (
              <div key={schedule.id} className="settings-row">
                <div>
                  <span className="settings-row-label">{schedule.name}</span>
                  <span className="settings-row-desc">
                    {schedule.frequency} &middot; Penalty {schedule.penaltyEnabled ? 'enabled' : 'disabled'}
                  </span>
                </div>
                <div className="settings-row-actions">
                  <span className="settings-row-value">KES {fmtCurrency(schedule.amount)}</span>
                  <span className={`badge ${schedule.active ? 'badge--active' : 'badge--inactive'}`}>
                    {schedule.active ? 'Active' : 'Inactive'}
                  </span>
                  {isAdmin && (
                    <ActionMenu
                      actions={[
                        { label: 'Edit', onClick: () => openEditScheduleModal(schedule), disabled: scheduleActionId === schedule.id },
                        {
                          label: schedule.active ? 'Deactivate' : 'Activate',
                          onClick: () => void handleScheduleToggle(schedule),
                          disabled: scheduleActionId === schedule.id,
                          variant: schedule.active ? 'danger' : 'default',
                        },
                      ]}
                    />
                  )}
                </div>
              </div>
            ))}
          </div>

          <div className="settings-group">
            <div className="settings-group-header">
              <h2 className="settings-group-title">Penalty Rules</h2>
              {isAdmin && (
                <button type="button" className="btn btn--primary" onClick={openNewPenaltyModal}>
                  New Penalty Rule
                </button>
              )}
            </div>
            <hr className="rule" />
            {penaltyRules.length === 0 ? (
              <div className="settings-row">
                <span className="settings-row-label">No penalty rules configured</span>
              </div>
            ) : penaltyRules.map(rule => (
              <div key={rule.id} className="settings-row">
                <div>
                  <span className="settings-row-label">{rule.name}</span>
                  <span className="settings-row-desc">
                    {PENALTY_TYPE_LABELS[rule.penaltyType]} &middot; Compounding {rule.compounding ? 'enabled' : 'disabled'} (read-only)
                  </span>
                </div>
                <div className="settings-row-actions">
                  <span className="settings-row-value penalty-value">{fmtRate(rule.rate, rule.calculationMethod)}</span>
                  <span className={`badge ${rule.active ? 'badge--active' : 'badge--inactive'}`}>
                    {rule.active ? 'Active' : 'Inactive'}
                  </span>
                  {isAdmin && (
                    <ActionMenu
                      actions={[
                        { label: 'Edit', onClick: () => openEditPenaltyModal(rule), disabled: penaltyActionId === rule.id },
                        {
                          label: rule.active ? 'Deactivate' : 'Activate',
                          onClick: () => void handlePenaltyToggle(rule),
                          disabled: penaltyActionId === rule.id,
                          variant: rule.active ? 'danger' : 'default',
                        },
                      ]}
                    />
                  )}
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      <hr className="rule rule--strong settings-bottom-rule" />

      <Modal
        open={showLoanModal}
        onClose={closeLoanModal}
        title={editingLoan ? 'Edit Loan Product' : 'New Loan Product'}
        subtitle="Configure loan product defaults"
        footer={
          <>
            <button type="button" className="btn btn--secondary" onClick={closeLoanModal} disabled={submittingLoan}>
              Cancel
            </button>
            <button type="submit" className="btn btn--primary" form="loan-product-form" disabled={submittingLoan}>
              {submittingLoan ? 'Saving...' : editingLoan ? 'Update' : 'Create'}
            </button>
          </>
        }
      >
        <form id="loan-product-form" className="modal-form" onSubmit={event => void handleLoanSubmit(event)}>
          {loanFormError && <div className="ops-feedback ops-feedback--error" role="status">{loanFormError}</div>}
          <div className="field">
            <label className="field-label field-label--required">Name</label>
            <input
              className="field-input"
              value={loanForm.name}
              onChange={event => setLoanForm(prev => ({ ...prev, name: event.target.value }))}
              required
              disabled={submittingLoan}
            />
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Interest Method</label>
              <Select
                value={loanForm.interestMethod}
                onChange={value => setLoanForm(prev => ({ ...prev, interestMethod: value as InterestMethod }))}
                options={INTEREST_METHOD_OPTIONS}
                required
              />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Annual Interest Rate (%)</label>
              <input
                className="field-input"
                type="number"
                min={0.01}
                step="0.01"
                value={loanForm.annualInterestRate}
                onChange={event => setLoanForm(prev => ({ ...prev, annualInterestRate: event.target.value }))}
                required
                disabled={submittingLoan}
              />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Min Term (Months)</label>
              <input
                className="field-input"
                type="number"
                min={1}
                step="1"
                value={loanForm.minTermMonths}
                onChange={event => setLoanForm(prev => ({ ...prev, minTermMonths: event.target.value }))}
                required
                disabled={submittingLoan}
              />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Max Term (Months)</label>
              <input
                className="field-input"
                type="number"
                min={1}
                step="1"
                value={loanForm.maxTermMonths}
                onChange={event => setLoanForm(prev => ({ ...prev, maxTermMonths: event.target.value }))}
                required
                disabled={submittingLoan}
              />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Min Amount (KES)</label>
              <input
                className="field-input"
                type="number"
                min={0.01}
                step="0.01"
                value={loanForm.minAmount}
                onChange={event => setLoanForm(prev => ({ ...prev, minAmount: event.target.value }))}
                required
                disabled={submittingLoan}
              />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Max Amount (KES)</label>
              <input
                className="field-input"
                type="number"
                min={0.01}
                step="0.01"
                value={loanForm.maxAmount}
                onChange={event => setLoanForm(prev => ({ ...prev, maxAmount: event.target.value }))}
                required
                disabled={submittingLoan}
              />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label">Contribution Cap (%)</label>
              <input
                className="field-input"
                type="number"
                min={0}
                max={100}
                step="0.01"
                value={loanForm.contributionCapPercent}
                onChange={event => setLoanForm(prev => ({ ...prev, contributionCapPercent: event.target.value }))}
                placeholder="Optional"
                disabled={submittingLoan}
              />
            </div>
            <div className="field">
              <label className="field-label">Pool Cap Amount (KES)</label>
              <input
                className="field-input"
                type="number"
                min={0}
                step="0.01"
                value={loanForm.poolCapAmount}
                onChange={event => setLoanForm(prev => ({ ...prev, poolCapAmount: event.target.value }))}
                placeholder="Optional"
                disabled={submittingLoan}
              />
            </div>
          </div>
          <div className="field">
            <label className="field-checkbox">
              <input
                type="checkbox"
                checked={loanForm.rolloverEnabled}
                onChange={event => setLoanForm(prev => ({ ...prev, rolloverEnabled: event.target.checked }))}
                disabled={submittingLoan}
              />
              <span>Rollover enabled</span>
            </label>
          </div>
          {loanForm.rolloverEnabled && (
            <div className="field-row">
              <div className="field">
                <label className="field-label field-label--required">Max Rollover Months</label>
                <input
                  className="field-input"
                  type="number"
                  min={0}
                  step="1"
                  value={loanForm.maxRolloverMonths}
                  onChange={event => setLoanForm(prev => ({ ...prev, maxRolloverMonths: event.target.value }))}
                  required
                  disabled={submittingLoan}
                />
              </div>
              <div className="field">
                <label className="field-label field-label--required">Rollover Surcharge Rate (%)</label>
                <input
                  className="field-input"
                  type="number"
                  min={0}
                  step="0.01"
                  value={loanForm.rolloverSurchargeRate}
                  onChange={event => setLoanForm(prev => ({ ...prev, rolloverSurchargeRate: event.target.value }))}
                  required
                  disabled={submittingLoan}
                />
              </div>
            </div>
          )}
          <div className="field">
            <label className="field-checkbox">
              <input
                type="checkbox"
                checked={loanForm.interestAccrualEnabled}
                onChange={event => setLoanForm(prev => ({ ...prev, interestAccrualEnabled: event.target.checked }))}
                disabled={submittingLoan}
              />
              <span>Daily interest accrual enabled</span>
            </label>
          </div>
          <div className="field">
            <label className="field-checkbox">
              <input
                type="checkbox"
                checked={loanForm.requiresGuarantor}
                onChange={event => setLoanForm(prev => ({ ...prev, requiresGuarantor: event.target.checked }))}
                disabled={submittingLoan}
              />
              <span>Requires guarantor</span>
            </label>
          </div>
          <div className="field">
            <label className="field-checkbox">
              <input
                type="checkbox"
                checked={loanForm.active}
                onChange={event => setLoanForm(prev => ({ ...prev, active: event.target.checked }))}
                disabled={submittingLoan}
              />
              <span>Active</span>
            </label>
          </div>
        </form>
      </Modal>

      <Modal
        open={showScheduleModal}
        onClose={closeScheduleModal}
        title={editingSchedule ? 'Edit Contribution Schedule' : 'New Contribution Schedule'}
        subtitle="Configure contribution schedule defaults"
        footer={
          <>
            <button type="button" className="btn btn--secondary" onClick={closeScheduleModal} disabled={submittingSchedule}>
              Cancel
            </button>
            <button type="submit" className="btn btn--primary" form="schedule-form" disabled={submittingSchedule}>
              {submittingSchedule ? 'Saving...' : editingSchedule ? 'Update' : 'Create'}
            </button>
          </>
        }
      >
        <form id="schedule-form" className="modal-form" onSubmit={event => void handleScheduleSubmit(event)}>
          {scheduleFormError && <div className="ops-feedback ops-feedback--error" role="status">{scheduleFormError}</div>}
          <div className="field">
            <label className="field-label field-label--required">Name</label>
            <input
              className="field-input"
              value={scheduleForm.name}
              onChange={event => setScheduleForm(prev => ({ ...prev, name: event.target.value }))}
              required
              disabled={submittingSchedule}
            />
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Frequency</label>
              <Select
                value={scheduleForm.frequency}
                onChange={value => setScheduleForm(prev => ({ ...prev, frequency: value as ContributionFrequency }))}
                options={CONTRIBUTION_FREQUENCY_OPTIONS}
                required
              />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Amount (KES)</label>
              <input
                className="field-input"
                type="number"
                min={0.01}
                step="0.01"
                value={scheduleForm.amount}
                onChange={event => setScheduleForm(prev => ({ ...prev, amount: event.target.value }))}
                required
                disabled={submittingSchedule}
              />
            </div>
          </div>
          <div className="field">
            <label className="field-checkbox">
              <input
                type="checkbox"
                checked={scheduleForm.penaltyEnabled}
                onChange={event => setScheduleForm(prev => ({ ...prev, penaltyEnabled: event.target.checked }))}
                disabled={submittingSchedule}
              />
              <span>Penalty enabled</span>
            </label>
          </div>
          <div className="field">
            <label className="field-checkbox">
              <input
                type="checkbox"
                checked={scheduleForm.active}
                onChange={event => setScheduleForm(prev => ({ ...prev, active: event.target.checked }))}
                disabled={submittingSchedule}
              />
              <span>Active</span>
            </label>
          </div>
        </form>
      </Modal>

      <Modal
        open={showPenaltyModal}
        onClose={closePenaltyModal}
        title={editingPenalty ? 'Edit Penalty Rule' : 'New Penalty Rule'}
        subtitle="Configure penalty policy defaults"
        footer={
          <>
            <button type="button" className="btn btn--secondary" onClick={closePenaltyModal} disabled={submittingPenalty}>
              Cancel
            </button>
            <button type="submit" className="btn btn--primary" form="penalty-form" disabled={submittingPenalty}>
              {submittingPenalty ? 'Saving...' : editingPenalty ? 'Update' : 'Create'}
            </button>
          </>
        }
      >
        <form id="penalty-form" className="modal-form" onSubmit={event => void handlePenaltySubmit(event)}>
          {penaltyFormError && <div className="ops-feedback ops-feedback--error" role="status">{penaltyFormError}</div>}
          <div className="field">
            <label className="field-label field-label--required">Name</label>
            <input
              className="field-input"
              value={penaltyForm.name}
              onChange={event => setPenaltyForm(prev => ({ ...prev, name: event.target.value }))}
              required
              disabled={submittingPenalty}
            />
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Penalty Type</label>
              <Select
                value={penaltyForm.penaltyType}
                onChange={value => setPenaltyForm(prev => ({ ...prev, penaltyType: value as PenaltyType }))}
                options={PENALTY_TYPE_OPTIONS}
                required
              />
            </div>
            <div className="field">
              <label className="field-label field-label--required">Calculation Method</label>
              <Select
                value={penaltyForm.calculationMethod}
                onChange={value => setPenaltyForm(prev => ({ ...prev, calculationMethod: value as CalculationMethod }))}
                options={CALCULATION_METHOD_OPTIONS}
                required
              />
            </div>
          </div>
          <div className="field">
            <label className="field-label field-label--required">
              Rate ({penaltyForm.calculationMethod === 'PERCENTAGE' ? '%' : 'KES'})
            </label>
            <input
              className="field-input"
              type="number"
              min={0}
              step="0.01"
              value={penaltyForm.rate}
              onChange={event => setPenaltyForm(prev => ({ ...prev, rate: event.target.value }))}
              required
              disabled={submittingPenalty}
            />
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Tier Start Overdue Day</label>
              <input
                className="field-input"
                type="number"
                min={1}
                step="1"
                value={penaltyForm.startOverdueDay}
                onChange={event => setPenaltyForm(prev => ({ ...prev, startOverdueDay: event.target.value }))}
                required
                disabled={submittingPenalty}
              />
            </div>
            <div className="field">
              <label className="field-label">Tier End Overdue Day</label>
              <input
                className="field-input"
                type="number"
                min={1}
                step="1"
                value={penaltyForm.endOverdueDay}
                onChange={event => setPenaltyForm(prev => ({ ...prev, endOverdueDay: event.target.value }))}
                placeholder="Optional"
                disabled={submittingPenalty}
              />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label className="field-label field-label--required">Tier Frequency</label>
              <Select
                value={penaltyForm.frequency}
                onChange={value => setPenaltyForm(prev => ({ ...prev, frequency: value as PenaltyFrequency }))}
                options={PENALTY_FREQUENCY_OPTIONS}
                required
              />
            </div>
            <div className="field">
              <label className="field-label">Tier Max Applications</label>
              <input
                className="field-input"
                type="number"
                min={1}
                step="1"
                value={penaltyForm.maxApplications}
                onChange={event => setPenaltyForm(prev => ({ ...prev, maxApplications: event.target.value }))}
                placeholder="Optional"
                disabled={submittingPenalty}
              />
            </div>
          </div>
          <div className="field">
            <label className="field-checkbox">
              <input
                type="checkbox"
                checked={penaltyForm.active}
                onChange={event => setPenaltyForm(prev => ({ ...prev, active: event.target.checked }))}
                disabled={submittingPenalty}
              />
              <span>Active</span>
            </label>
          </div>
        </form>
      </Modal>
    </div>
  )
}
