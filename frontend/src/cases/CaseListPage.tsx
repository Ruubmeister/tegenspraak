import { Link } from 'react-router'
import { useCases } from './casesApi'

export function CaseListPage() {
  const { data: cases, isPending, isError } = useCases()

  if (isPending) {
    return <p className="status">Zaken laden...</p>
  }
  if (isError) {
    return <p className="status error">De zaken konden niet worden geladen.</p>
  }

  return (
    <main className="case-list">
      <h1>Zaken</h1>
      <ul>
        {cases.map((caseSummary) => (
          <li key={caseSummary.id}>
            <Link to={`/zaken/${caseSummary.id}`} className="case-card">
              <h2>{caseSummary.title}</h2>
              <p>{caseSummary.summary}</p>
            </Link>
          </li>
        ))}
      </ul>
    </main>
  )
}
