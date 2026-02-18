export interface CursorPage<T> {
  items: T[]
  nextCursor: string | null
  hasMore: boolean
  size: number
}

export interface UserResponse {
  id: string
  username: string
  email: string
  enabled: boolean
  accountNonLocked: boolean
  roles: string[]
  createdAt: string
  updatedAt: string
}
