import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../api/http'
import type { Desktop } from '../api/types'
import { caseOverviewQueryKey } from '../cases/casesApi'
import { teamPath } from '../teams/teamsApi'

/** Live updates arrive through useTeamEvents; this only catches up while the event stream is down. */
const DESKTOP_REFRESH_MS = 60_000

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

/** The image of an open photo; the session cookie authorizes the request like any API call. */
export function photoImagePath(location: DesktopLocation, appId: string, itemId: string): string {
  return `${desktopPath(location)}/apps/${encodeURIComponent(appId)}/items/${encodeURIComponent(itemId)}/image`
}

/** Every desktop of the case, so an unlock can refresh whichever one is open. */
export function caseDesktopsQueryKey(teamId: string, caseId: string) {
  return ['desktop', teamId, caseId] as const
}

function desktopQueryKey({ teamId, caseId, desktopId }: DesktopLocation) {
  return [...caseDesktopsQueryKey(teamId, caseId), desktopId] as const
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
