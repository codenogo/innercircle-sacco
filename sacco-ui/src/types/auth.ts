export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface AuthSession {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface MemberSummary {
  id: string
  firstName: string
  lastName: string
  memberNumber: string
}

export interface MeResponse {
  id: string
  username: string
  email: string
  enabled: boolean
  roles: string[]
  member: MemberSummary | null
  createdAt: string
  updatedAt: string
}
