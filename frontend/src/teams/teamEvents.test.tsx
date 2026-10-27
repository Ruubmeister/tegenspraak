import { QueryClient, QueryClientProvider, type Query } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi, type MockInstance } from 'vitest'
import { FakeEventSource } from '../test/fakeEventSource'
import { reloadForEvent, useTeamEvents } from './teamEvents'

function renderTeamEvents(teamId = 'team-1') {
  const queryClient = new QueryClient()
  const invalidate = vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue()
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  const { unmount } = renderHook(() => useTeamEvents(teamId), { wrapper })
  return { invalidate, unmount, source: FakeEventSource.instances.at(-1)! }
}

function invalidatedKeys(invalidate: MockInstance<QueryClient['invalidateQueries']>) {
  return invalidate.mock.calls.map(([filters]) => filters?.queryKey)
}

/** Only the query key matters to the reconnect filter. */
function queryWithKey(queryKey: unknown[]): Query {
  return { queryKey } as unknown as Query
}

describe('useTeamEvents', () => {
  it('useTeamEvents_givenMounted_expectStreamOfTeamOpened', () => {
    const { source } = renderTeamEvents()

    expect(source.url).toBe('/api/teams/team-1/events')
  })

  it('useTeamEvents_givenUnmounted_expectStreamClosed', () => {
    const { source, unmount } = renderTeamEvents()

    unmount()

    expect(source.closed).toBe(true)
  })

  it('useTeamEvents_givenChatEvent_expectConversationAndUnreadReloaded', () => {
    const { source, invalidate } = renderTeamEvents()

    act(() => source.emit({ type: 'CHAT', caseId: 'het-lek', characterId: 'sanne-bakker' }))

    expect(invalidatedKeys(invalidate)).toEqual([
      ['conversation', 'team-1', 'het-lek', 'sanne-bakker'],
      ['unread-conversations', 'team-1', 'het-lek'],
    ])
  })

  it('useTeamEvents_givenFirstConnect_expectNothingReloaded', () => {
    const { source, invalidate } = renderTeamEvents()

    act(() => source.open())

    expect(invalidate).not.toHaveBeenCalled()
  })

  it('useTeamEvents_givenReconnect_expectAllQueriesOfTeamReloaded', () => {
    const { source, invalidate } = renderTeamEvents()
    act(() => source.open())

    act(() => source.open())

    const predicate = invalidate.mock.calls[0][0]?.predicate
    expect(predicate?.(queryWithKey(['desktop', 'team-1', 'het-lek', 'werkplek']))).toBe(true)
    expect(predicate?.(queryWithKey(['desktop', 'team-2', 'het-lek', 'werkplek']))).toBe(false)
  })
})

describe('reloadForEvent', () => {
  it('reloadForEvent_givenCaseEvent_expectCaseDesktopsAndEvidenceReloaded', async () => {
    const queryClient = new QueryClient()
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue()

    await reloadForEvent(queryClient, 'team-1', { type: 'CASE', caseId: 'het-lek', characterId: null })

    expect(invalidatedKeys(invalidate)).toEqual([
      ['cases', 'team-1', 'het-lek'],
      ['desktop', 'team-1', 'het-lek'],
      ['evidence', 'team-1', 'het-lek'],
    ])
  })

  it('reloadForEvent_givenVerdictEvent_expectVerdictAndSolvedCasesReloaded', async () => {
    const queryClient = new QueryClient()
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue()

    await reloadForEvent(queryClient, 'team-1', { type: 'VERDICT', caseId: 'het-lek', characterId: null })

    expect(invalidatedKeys(invalidate)).toEqual([
      ['verdict', 'team-1', 'het-lek'],
      ['solved-cases', 'team-1'],
    ])
  })

  it('reloadForEvent_givenMembersEvent_expectTeamReloaded', async () => {
    const queryClient = new QueryClient()
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue()

    await reloadForEvent(queryClient, 'team-1', { type: 'MEMBERS', caseId: null, characterId: null })

    expect(invalidatedKeys(invalidate)).toEqual([['teams', 'team-1']])
  })
})
