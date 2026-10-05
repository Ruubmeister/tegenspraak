import { useOutletContext } from 'react-router'
import type { CaseOverview } from '../api/types'

/** The case overview loaded by CasePage, available to its nested routes. */
export function useCaseContext(): CaseOverview {
  return useOutletContext<CaseOverview>()
}
