import { useCaseContext } from './caseContext'

export function CaseBriefing() {
  const caseOverview = useCaseContext()
  return (
    <article className="briefing">
      <h2>Briefing</h2>
      <p>{caseOverview.introduction}</p>
    </article>
  )
}
