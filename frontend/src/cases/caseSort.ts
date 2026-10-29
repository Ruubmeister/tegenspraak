import type { CaseDifficulty, CaseSummary } from '../api/types'

export type CaseSortOrder = 'easiest-first' | 'hardest-first' | 'title'

export const CASE_SORT_LABELS: Record<CaseSortOrder, string> = {
  'easiest-first': 'Moeilijkheid (makkelijk eerst)',
  'hardest-first': 'Moeilijkheid (moeilijk eerst)',
  title: 'Titel (A-Z)',
}

const DIFFICULTY_RANK: Record<CaseDifficulty, number> = {
  oefenzaak: 0,
  makkelijk: 1,
  gemiddeld: 2,
  moeilijk: 3,
  expert: 4,
}

function compareTitles(a: CaseSummary, b: CaseSummary) {
  return a.title.localeCompare(b.title, 'nl')
}

/** A sorted copy of the cases; cases of the same difficulty are sorted by title. */
export function sortCases(cases: CaseSummary[], order: CaseSortOrder): CaseSummary[] {
  if (order === 'title') {
    return [...cases].sort(compareTitles)
  }
  const direction = order === 'easiest-first' ? 1 : -1
  return [...cases].sort(
    (a, b) => direction * (DIFFICULTY_RANK[a.difficulty] - DIFFICULTY_RANK[b.difficulty]) || compareTitles(a, b),
  )
}
