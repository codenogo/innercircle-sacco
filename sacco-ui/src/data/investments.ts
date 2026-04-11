import type {
  InvestmentResponse,
  InvestmentIncomeResponse,
  InvestmentValuationResponse,
  InvestmentSummary,
} from '../types/investments'

export const mockInvestments: InvestmentResponse[] = [
  {
    id: 'inv-001', referenceNumber: 'INV-2025-001', name: '364-Day Treasury Bill', investmentType: 'TREASURY_BILL',
    status: 'ACTIVE', institution: 'Central Bank of Kenya', faceValue: 5_000_000, purchasePrice: 4_750_000,
    currentValue: 4_920_000, interestRate: 16.5, purchaseDate: '2025-03-15', maturityDate: '2026-03-14',
    units: null, navPerUnit: null, notes: 'March 2025 auction', approvedBy: 'admin', approvedAt: '2025-03-14',
    createdBy: 'treasurer', createdAt: '2025-03-12T10:00:00Z', updatedAt: '2025-12-01T08:00:00Z',
  },
  {
    id: 'inv-002', referenceNumber: 'INV-2025-002', name: 'KCB 12-Month Fixed Deposit', investmentType: 'FIXED_DEPOSIT',
    status: 'ACTIVE', institution: 'KCB Bank', faceValue: 3_000_000, purchasePrice: 3_000_000,
    currentValue: 3_180_000, interestRate: 12.0, purchaseDate: '2025-01-10', maturityDate: '2026-01-10',
    units: null, navPerUnit: null, notes: null, approvedBy: 'admin', approvedAt: '2025-01-09',
    createdBy: 'treasurer', createdAt: '2025-01-08T09:00:00Z', updatedAt: '2025-11-01T08:00:00Z',
  },
  {
    id: 'inv-003', referenceNumber: 'INV-2024-003', name: 'Westlands Rental Property', investmentType: 'REAL_ESTATE',
    status: 'ACTIVE', institution: 'N/A — Direct ownership', faceValue: 12_000_000, purchasePrice: 12_000_000,
    currentValue: 14_500_000, interestRate: 0, purchaseDate: '2024-06-01', maturityDate: null,
    units: null, navPerUnit: null, notes: '3-bedroom apartment, Westlands', approvedBy: 'admin', approvedAt: '2024-05-28',
    createdBy: 'admin', createdAt: '2024-05-20T09:00:00Z', updatedAt: '2025-10-15T08:00:00Z',
  },
  {
    id: 'inv-004', referenceNumber: 'INV-2025-004', name: 'Oak Fund — Growth Portfolio', investmentType: 'UNIT_TRUST',
    status: 'ACTIVE', institution: 'Oak Capital', faceValue: 2_000_000, purchasePrice: 2_000_000,
    currentValue: 2_340_000, interestRate: 0, purchaseDate: '2025-02-01', maturityDate: null,
    units: 18_520, navPerUnit: 126.35, notes: 'Monthly top-up plan', approvedBy: 'admin', approvedAt: '2025-01-30',
    createdBy: 'treasurer', createdAt: '2025-01-28T10:00:00Z', updatedAt: '2026-01-15T08:00:00Z',
  },
  {
    id: 'inv-005', referenceNumber: 'INV-2025-005', name: 'Mansa X Income Fund', investmentType: 'UNIT_TRUST',
    status: 'ACTIVE', institution: 'Mansa X', faceValue: 1_500_000, purchasePrice: 1_500_000,
    currentValue: 1_620_000, interestRate: 0, purchaseDate: '2025-04-15', maturityDate: null,
    units: 14_250, navPerUnit: 113.68, notes: null, approvedBy: 'admin', approvedAt: '2025-04-14',
    createdBy: 'treasurer', createdAt: '2025-04-12T09:00:00Z', updatedAt: '2026-01-20T08:00:00Z',
  },
  {
    id: 'inv-006', referenceNumber: 'INV-2025-006', name: 'CIC Money Market Fund', investmentType: 'MONEY_MARKET',
    status: 'ACTIVE', institution: 'CIC Asset Management', faceValue: 1_000_000, purchasePrice: 1_000_000,
    currentValue: 1_085_000, interestRate: 10.5, purchaseDate: '2025-01-05', maturityDate: null,
    units: null, navPerUnit: null, notes: null, approvedBy: 'admin', approvedAt: '2025-01-04',
    createdBy: 'treasurer', createdAt: '2025-01-03T10:00:00Z', updatedAt: '2026-01-18T08:00:00Z',
  },
  {
    id: 'inv-007', referenceNumber: 'INV-2025-007', name: 'Safaricom PLC Shares', investmentType: 'EQUITY',
    status: 'ACTIVE', institution: 'Nairobi Securities Exchange', faceValue: 800_000, purchasePrice: 800_000,
    currentValue: 720_000, interestRate: 0, purchaseDate: '2025-06-10', maturityDate: null,
    units: 40_000, navPerUnit: 18.0, notes: '40,000 shares @ KES 20 avg', approvedBy: 'admin', approvedAt: '2025-06-09',
    createdBy: 'treasurer', createdAt: '2025-06-08T09:00:00Z', updatedAt: '2026-02-01T08:00:00Z',
  },
  {
    id: 'inv-008', referenceNumber: 'INV-2025-008', name: 'GoK Infrastructure Bond 2030', investmentType: 'TREASURY_BOND',
    status: 'ACTIVE', institution: 'Central Bank of Kenya', faceValue: 2_000_000, purchasePrice: 2_000_000,
    currentValue: 2_100_000, interestRate: 14.0, purchaseDate: '2025-05-01', maturityDate: '2030-05-01',
    units: null, navPerUnit: null, notes: 'IFB 2030/05', approvedBy: 'admin', approvedAt: '2025-04-30',
    createdBy: 'treasurer', createdAt: '2025-04-28T10:00:00Z', updatedAt: '2025-11-01T08:00:00Z',
  },
  {
    id: 'inv-009', referenceNumber: 'INV-2026-009', name: 'Equity Bank 6-Month Deposit', investmentType: 'FIXED_DEPOSIT',
    status: 'PROPOSED', institution: 'Equity Bank', faceValue: 2_500_000, purchasePrice: 2_500_000,
    currentValue: 2_500_000, interestRate: 11.5, purchaseDate: '2026-03-01', maturityDate: '2026-09-01',
    units: null, navPerUnit: null, notes: 'Awaiting board approval', approvedBy: null, approvedAt: null,
    createdBy: 'treasurer', createdAt: '2026-02-18T09:00:00Z', updatedAt: '2026-02-18T09:00:00Z',
  },
  {
    id: 'inv-010', referenceNumber: 'INV-2024-010', name: '182-Day Treasury Bill (Matured)', investmentType: 'TREASURY_BILL',
    status: 'CLOSED', institution: 'Central Bank of Kenya', faceValue: 2_000_000, purchasePrice: 1_870_000,
    currentValue: 2_000_000, interestRate: 15.0, purchaseDate: '2024-06-15', maturityDate: '2024-12-14',
    units: null, navPerUnit: null, notes: 'Matured and proceeds received', approvedBy: 'admin', approvedAt: '2024-06-14',
    createdBy: 'treasurer', createdAt: '2024-06-12T10:00:00Z', updatedAt: '2024-12-15T08:00:00Z',
  },
]

export const mockInvestmentIncome: InvestmentIncomeResponse[] = [
  { id: 'inc-001', investmentId: 'inv-002', incomeType: 'INTEREST', amount: 90_000, incomeDate: '2025-04-10', referenceNumber: 'INT-Q1-2025', notes: 'Q1 interest', recordedBy: 'treasurer', createdAt: '2025-04-11T08:00:00Z' },
  { id: 'inc-002', investmentId: 'inv-002', incomeType: 'INTEREST', amount: 90_000, incomeDate: '2025-07-10', referenceNumber: 'INT-Q2-2025', notes: 'Q2 interest', recordedBy: 'treasurer', createdAt: '2025-07-11T08:00:00Z' },
  { id: 'inc-003', investmentId: 'inv-003', incomeType: 'RENT', amount: 85_000, incomeDate: '2025-11-05', referenceNumber: 'RENT-NOV-2025', notes: 'November rent', recordedBy: 'treasurer', createdAt: '2025-11-06T08:00:00Z' },
  { id: 'inc-004', investmentId: 'inv-003', incomeType: 'RENT', amount: 85_000, incomeDate: '2025-12-05', referenceNumber: 'RENT-DEC-2025', notes: 'December rent', recordedBy: 'treasurer', createdAt: '2025-12-06T08:00:00Z' },
  { id: 'inc-005', investmentId: 'inv-004', incomeType: 'DIVIDEND', amount: 52_000, incomeDate: '2025-09-15', referenceNumber: 'DIV-OAK-Q3', notes: 'Q3 distribution', recordedBy: 'treasurer', createdAt: '2025-09-16T08:00:00Z' },
  { id: 'inc-006', investmentId: 'inv-005', incomeType: 'DIVIDEND', amount: 38_000, incomeDate: '2025-12-20', referenceNumber: 'DIV-MANSA-H2', notes: 'H2 distribution', recordedBy: 'treasurer', createdAt: '2025-12-21T08:00:00Z' },
  { id: 'inc-007', investmentId: 'inv-006', incomeType: 'INTEREST', amount: 52_500, incomeDate: '2025-07-05', referenceNumber: 'INT-CIC-H1', notes: 'H1 interest', recordedBy: 'treasurer', createdAt: '2025-07-06T08:00:00Z' },
  { id: 'inc-008', investmentId: 'inv-007', incomeType: 'DIVIDEND', amount: 24_000, incomeDate: '2025-10-28', referenceNumber: 'DIV-SCOM-FY25', notes: 'Safaricom FY2025 dividend', recordedBy: 'treasurer', createdAt: '2025-10-29T08:00:00Z' },
  { id: 'inc-009', investmentId: 'inv-008', incomeType: 'COUPON', amount: 140_000, incomeDate: '2025-11-01', referenceNumber: 'CPN-IFB2030', notes: 'Semi-annual coupon', recordedBy: 'treasurer', createdAt: '2025-11-02T08:00:00Z' },
  { id: 'inc-010', investmentId: 'inv-010', incomeType: 'INTEREST', amount: 130_000, incomeDate: '2024-12-14', referenceNumber: 'MAT-TBILL-182', notes: 'Maturity proceeds', recordedBy: 'treasurer', createdAt: '2024-12-15T08:00:00Z' },
]

export const mockValuations: InvestmentValuationResponse[] = [
  { id: 'val-001', investmentId: 'inv-004', marketValue: 2_340_000, navPerUnit: 126.35, valuationDate: '2026-01-15', source: 'Oak Capital portal', createdAt: '2026-01-15T08:00:00Z' },
  { id: 'val-002', investmentId: 'inv-004', marketValue: 2_280_000, navPerUnit: 123.11, valuationDate: '2025-12-15', source: 'Oak Capital portal', createdAt: '2025-12-15T08:00:00Z' },
  { id: 'val-003', investmentId: 'inv-005', marketValue: 1_620_000, navPerUnit: 113.68, valuationDate: '2026-01-20', source: 'Mansa X statement', createdAt: '2026-01-20T08:00:00Z' },
  { id: 'val-004', investmentId: 'inv-007', marketValue: 720_000, navPerUnit: 18.0, valuationDate: '2026-02-01', source: 'NSE closing price', createdAt: '2026-02-01T08:00:00Z' },
  { id: 'val-005', investmentId: 'inv-003', marketValue: 14_500_000, navPerUnit: null, valuationDate: '2025-10-15', source: 'Independent valuation — Knight Frank', createdAt: '2025-10-15T08:00:00Z' },
]

export const mockInvestmentSummary: InvestmentSummary = {
  totalInvested: 30_550_000,
  currentValue: 33_465_000,
  unrealisedGain: 2_915_000,
  incomeYtd: 1_890_000,
  activeCount: 8,
  maturedCount: 0,
  proposedCount: 1,
  closedCount: 1,
  byType: [
    { type: 'TREASURY_BILL', amount: 4_920_000, percentage: 14.7 },
    { type: 'TREASURY_BOND', amount: 2_100_000, percentage: 6.3 },
    { type: 'FIXED_DEPOSIT', amount: 3_180_000, percentage: 9.5 },
    { type: 'REAL_ESTATE', amount: 14_500_000, percentage: 43.3 },
    { type: 'UNIT_TRUST', amount: 3_960_000, percentage: 11.8 },
    { type: 'MONEY_MARKET', amount: 1_085_000, percentage: 3.2 },
    { type: 'EQUITY', amount: 720_000, percentage: 2.2 },
  ],
}

