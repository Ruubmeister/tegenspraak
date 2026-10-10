import type { EvidenceItem } from '../api/types'

/** Page paths within a case a team is working on; these are app routes, not API paths. */
export function casePagePath(teamId: string, caseId: string): string {
  return `/teams/${encodeURIComponent(teamId)}/zaken/${encodeURIComponent(caseId)}`
}

export function chatPagePath(teamId: string, caseId: string, characterId: string): string {
  return `${casePagePath(teamId, caseId)}/chat/${encodeURIComponent(characterId)}`
}

/** Opens the computer with the mail or file already selected. */
export function evidencePagePath(teamId: string, caseId: string, evidence: EvidenceItem): string {
  const selection = new URLSearchParams({ app: evidence.appId, item: evidence.itemId })
  return `${casePagePath(teamId, caseId)}/computer/${encodeURIComponent(evidence.desktopId)}?${selection}`
}
