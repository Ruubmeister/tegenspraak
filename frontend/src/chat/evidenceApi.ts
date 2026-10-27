import { useQuery } from '@tanstack/react-query'
import { apiRequest } from '../api/http'
import type { EvidenceItem, EvidenceKind } from '../api/types'
import { teamPath } from '../teams/teamsApi'

export function evidenceQueryKey(teamId: string, caseId: string) {
  return ['evidence', teamId, caseId] as const
}

/** Everything the team can attach to a message. Fetched fresh whenever the picker opens, so new unlocks show up. */
export function useEvidence(teamId: string, caseId: string) {
  return useQuery({
    queryKey: evidenceQueryKey(teamId, caseId),
    queryFn: () =>
      apiRequest<EvidenceItem[]>('GET', `${teamPath(teamId)}/cases/${encodeURIComponent(caseId)}/evidence`),
  })
}

const KIND_LABELS: Record<EvidenceKind, string> = {
  mail: 'Mail',
  file: 'Bestand',
  event: 'Afspraak',
  photo: 'Foto',
  webpage: 'Webpagina',
}

export function evidenceKindLabel(kind: EvidenceKind): string {
  return KIND_LABELS[kind]
}

export function isSameEvidence(first: EvidenceItem, second: EvidenceItem): boolean {
  return first.desktopId === second.desktopId && first.appId === second.appId && first.itemId === second.itemId
}
