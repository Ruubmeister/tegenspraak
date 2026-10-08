import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../api/http'
import type { Desktop } from '../api/types'
import { caseOverviewQueryKey } from '../cases/casesApi'
import { teamPath } from '../teams/teamsApi'

/** Teammates can unlock mails and files, so an open desktop is refreshed regularly. */
const DESKTOP_REFRESH_MS = 10_000

/** Identifies a desktop as seen by one team. */
export interface DesktopLocation {
  teamId: string
  caseId: string
  desktopId: string
}

export interface ItemUnlock {
  appId: string
  itemId: string
  code: string
}

function desktopPath({ teamId, caseId, desktopId }: DesktopLocation): string {
  return `${teamPath(teamId)}/cases/${encodeURIComponent(caseId)}/desktops/${encodeURIComponent(desktopId)}`
}

function desktopQueryKey({ teamId, caseId, desktopId }: DesktopLocation) {
  return ['desktop', teamId, caseId, desktopId] as const
}

export function useDesktop(location: DesktopLocation) {
  return useQuery({
    queryKey: desktopQueryKey(location),
    queryFn: () => apiRequest<Desktop>('GET', desktopPath(location)),
    refetchInterval: DESKTOP_REFRESH_MS,
  })
}

/** Both unlocks answer with the updated desktop, which replaces the cached one. */
function useStoreUnlockedDesktop(location: DesktopLocation) {
  const queryClient = useQueryClient()
  return (desktop: Desktop) => {
    queryClient.setQueryData(desktopQueryKey(location), desktop)
    return queryClient.invalidateQueries({ queryKey: caseOverviewQueryKey(location.teamId, location.caseId) })
  }
}

export function useUnlockDesktop(location: DesktopLocation) {
  const storeDesktop = useStoreUnlockedDesktop(location)
  return useMutation({
    mutationFn: (code: string) => apiRequest<Desktop>('POST', `${desktopPath(location)}/unlock`, { code }),
    onSuccess: storeDesktop,
  })
}

export function useUnlockItem(location: DesktopLocation) {
  const storeDesktop = useStoreUnlockedDesktop(location)
  return useMutation({
    mutationFn: ({ appId, itemId, code }: ItemUnlock) =>
      apiRequest<Desktop>(
        'POST',
        `${desktopPath(location)}/apps/${encodeURIComponent(appId)}/items/${encodeURIComponent(itemId)}/unlock`,
        { code },
      ),
    onSuccess: storeDesktop,
  })
}
