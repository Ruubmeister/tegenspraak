import { Link } from 'react-router'
import { useSolvedCaseIds } from '../verdict/verdictApi'
import { useCases } from './casesApi'

/** The cases a team can investigate, with the ones the team solved marked. */
export function CaseList({ teamId }: { teamId: string }) {
  const { data: cases, isPending, isError } = useCases()
  const { data: solvedCaseIds = [] } = useSolvedCaseIds(teamId)

  if (isPending) {
    return <p className="status">Zaken laden...</p>
  }
  if (isError) {
    return <p className="status error">De zaken konden niet worden geladen.</p>
  }

  return (
    <ul className="case-cards">
      {cases.map((caseSummary) => (
        <li key={caseSummary.id}>
          <Link to={`/teams/${teamId}/zaken/${caseSummary.id}`} className="case-card">
            <h3>{caseSummary.title}</h3>
            {solvedCaseIds.includes(caseSummary.id) && <span className="badge">Opgelost</span>}
            <p>{caseSummary.summary}</p>
          </Link>
        </li>
      ))}
    </ul>
  )
}
