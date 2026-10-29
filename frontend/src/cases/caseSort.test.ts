import { describe, expect, it } from 'vitest'
import type { CaseDifficulty, CaseSummary } from '../api/types'
import { sortCases } from './caseSort'

function caseSummary(title: string, difficulty: CaseDifficulty): CaseSummary {
  return { id: title.toLowerCase(), title, summary: 'Samenvatting', difficulty }
}

const cases = [
  caseSummary('Testament', 'expert'),
  caseSummary('Lek', 'oefenzaak'),
  caseSummary('Puzzelmaker', 'expert'),
  caseSummary('Kunstbrand', 'gemiddeld'),
]

function titles(sorted: CaseSummary[]) {
  return sorted.map((sortedCase) => sortedCase.title)
}

describe('caseSort', () => {
  it('sortCases_givenEasiestFirst_expectAscendingDifficultyThenTitle', () => {
    expect(titles(sortCases(cases, 'easiest-first'))).toEqual(['Lek', 'Kunstbrand', 'Puzzelmaker', 'Testament'])
  })

  it('sortCases_givenHardestFirst_expectDescendingDifficultyThenTitle', () => {
    expect(titles(sortCases(cases, 'hardest-first'))).toEqual(['Puzzelmaker', 'Testament', 'Kunstbrand', 'Lek'])
  })

  it('sortCases_givenTitle_expectAlphabetical', () => {
    expect(titles(sortCases(cases, 'title'))).toEqual(['Kunstbrand', 'Lek', 'Puzzelmaker', 'Testament'])
  })

  it('sortCases_givenAnyOrder_expectInputUnchanged', () => {
    sortCases(cases, 'title')

    expect(titles(cases)).toEqual(['Testament', 'Lek', 'Puzzelmaker', 'Kunstbrand'])
  })
})
