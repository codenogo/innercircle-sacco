export type MemberStatus = 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED'

export interface MemberResponse {
  id: string
  memberNumber: string
  firstName: string
  lastName: string
  email: string
  phone: string
  nationalId: string
  dateOfBirth: string
  joinDate: string
  status: MemberStatus
  shareBalance: number | string
  createdAt: string
  updatedAt: string
  createdBy?: string
}

export interface CreateMemberRequest {
  memberNumber: string
  firstName: string
  lastName: string
  email: string
  phone: string
  nationalId: string
  dateOfBirth: string
  joinDate: string
}
