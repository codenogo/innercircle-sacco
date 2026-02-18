export type AuditAction = 'CREATED' | 'UPDATED' | 'DELETED' | 'APPROVED' | 'REJECTED' | 'PROCESSING' | 'LOCKED' | 'UNLOCKED'
export type AuditEntityType = 'MEMBER' | 'LOAN' | 'PAYOUT' | 'CONFIG' | 'SECURITY' | 'CONTRIBUTION'

export interface AuditEventResponse {
  id: string
  timestamp: string
  actor: string
  actorName: string
  action: AuditAction
  entityType: AuditEntityType
  entityId: string
  beforeSnapshot: string | null
  afterSnapshot: string | null
  ipAddress: string | null
}
