import { ApiError } from '../services/apiClient'

export interface ApprovalOverrideRequest {
  overrideReason: string
}

export function isMakerCheckerViolation(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    error.status === 403 &&
    error.message.toLowerCase().includes('maker-checker violation')
  )
}
