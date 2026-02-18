import { apiRequest } from './apiClient'
import type { CursorPage } from '../types/users'
import type { MemberResponse } from '../types/members'

export type AuthenticatedRequest = <T>(path: string, options?: RequestInit) => Promise<T>

function callApi<T>(
  path: string,
  options: RequestInit | undefined,
  request?: AuthenticatedRequest,
): Promise<T> {
  if (request) return request<T>(path, options)
  return apiRequest<T>(path, options)
}

export async function getMembersPage(
  cursor?: string,
  size = 200,
  request?: AuthenticatedRequest,
): Promise<CursorPage<MemberResponse>> {
  const params = new URLSearchParams({ size: String(size) })
  if (cursor) params.set('cursor', cursor)
  return callApi<CursorPage<MemberResponse>>(`/api/v1/members?${params}`, undefined, request)
}

export async function getAllMembers(
  request?: AuthenticatedRequest,
  pageSize = 200,
  maxPages = 100,
): Promise<MemberResponse[]> {
  let cursor: string | undefined
  let pageNumber = 0
  const all: MemberResponse[] = []

  while (pageNumber < maxPages) {
    const page = await getMembersPage(cursor, pageSize, request)
    all.push(...page.items)

    if (!page.hasMore || !page.nextCursor) break
    cursor = page.nextCursor
    pageNumber += 1
  }

  return all
}
