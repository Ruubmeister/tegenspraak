import { useState, type FormEvent } from 'react'
import type { Verdict, VerdictQuestion } from '../api/types'
import { useSubmitVerdict, useVerdict } from './verdictApi'

interface VerdictPanelProps {
  teamId: string
  caseId: string
}

/** Lets the team answer the solution questions. Answering all of them correctly solves the case for the team. */
export function VerdictPanel({ teamId, caseId }: VerdictPanelProps) {
  const { data: verdict, isPending, isError } = useVerdict(teamId, caseId)

  if (isPending) {
    return <p className="status">Laden...</p>
  }
  if (isError) {
    return <p className="status error">De vragen konden niet worden geladen.</p>
  }

  return (
    <article className="verdict">
      {verdict.solved ? (
        <SolvedCase verdict={verdict} />
      ) : (
        <VerdictForm teamId={teamId} caseId={caseId} verdict={verdict} />
      )}
    </article>
  )
}

function SolvedCase({ verdict }: { verdict: Verdict }) {
  return (
    <>
      <h2>Zaak opgelost</h2>
      <p className="verdict-attempts">
        Opgelost in {verdict.attempts} {verdict.attempts === 1 ? 'poging' : 'pogingen'}, door{' '}
        {verdict.lastScore?.submittedBy}.
      </p>
      <h3>Wat er echt gebeurde</h3>
      <p>{verdict.epilogue}</p>
    </>
  )
}

function VerdictForm({ teamId, caseId, verdict }: { teamId: string; caseId: string; verdict: Verdict }) {
  const submitVerdict = useSubmitVerdict(teamId, caseId)
  const [answers, setAnswers] = useState<Record<string, string>>({})
  const allAnswered = verdict.questions.every((question) => answers[question.id] !== undefined)

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!allAnswered || submitVerdict.isPending) {
      return
    }
    submitVerdict.mutate(answers)
  }

  return (
    <>
      <h2>Zaak oplossen</h2>
      <p>
        Weet je team wat er is gebeurd? Beantwoord alle vragen goed om de zaak op te lossen. Iedereen in het team kan
        een oplossing indienen.
      </p>
      {verdict.lastScore && (
        <p className="verdict-score" role="status">
          Laatste poging door {verdict.lastScore.submittedBy}: {verdict.lastScore.correctAnswers} van de{' '}
          {verdict.lastScore.totalQuestions} antwoorden kloppen. Zoek verder en probeer het opnieuw.
        </p>
      )}
      <form className="verdict-form" onSubmit={handleSubmit}>
        {verdict.questions.map((question) => (
          <QuestionFieldset
            key={question.id}
            question={question}
            selectedOptionId={answers[question.id]}
            onSelect={(optionId) => setAnswers((current) => ({ ...current, [question.id]: optionId }))}
          />
        ))}
        {submitVerdict.isError && (
          <p className="error" role="alert">
            Je oplossing kon niet worden verstuurd. Probeer het opnieuw.
          </p>
        )}
        <button type="submit" disabled={!allAnswered || submitVerdict.isPending}>
          Oplossing indienen
        </button>
      </form>
    </>
  )
}

interface QuestionFieldsetProps {
  question: VerdictQuestion
  selectedOptionId: string | undefined
  onSelect: (optionId: string) => void
}

function QuestionFieldset({ question, selectedOptionId, onSelect }: QuestionFieldsetProps) {
  return (
    <fieldset>
      <legend>{question.question}</legend>
      {question.options.map((option) => (
        <label key={option.id}>
          <input
            type="radio"
            name={question.id}
            value={option.id}
            checked={selectedOptionId === option.id}
            onChange={() => onSelect(option.id)}
          />
          {option.label}
        </label>
      ))}
    </fieldset>
  )
}
