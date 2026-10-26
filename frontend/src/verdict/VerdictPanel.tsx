import { useState, type FormEvent } from 'react'
import { isApiError } from '../api/http'
import type { Verdict, VerdictAttempt, VerdictQuestion } from '../api/types'
import { useSubmitVerdict, useVerdict } from './verdictApi'

/** Matches the limit of the backend. */
const MAX_ANSWER_LENGTH = 1000

interface VerdictPanelProps {
  teamId: string
  caseId: string
}

/**
 * Lets the team answer the solution questions in its own words. Answering all of them correctly solves the case for
 * the team; after the last wrong attempt the case is failed. Either way the epilogue is shown.
 */
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
      {verdict.solved && <SolvedCase verdict={verdict} />}
      {verdict.failed && <FailedCase verdict={verdict} />}
      {!verdict.solved && !verdict.failed && <VerdictForm teamId={teamId} caseId={caseId} verdict={verdict} />}
      <AttemptHistory verdict={verdict} />
    </article>
  )
}

function SolvedCase({ verdict }: { verdict: Verdict }) {
  const attemptCount = verdict.attempts.length
  return (
    <>
      <h2>Zaak opgelost</h2>
      <p className="verdict-attempts">
        Opgelost in {attemptCount} {attemptCount === 1 ? 'poging' : 'pogingen'}, door{' '}
        {verdict.attempts.at(-1)?.submittedBy}.
      </p>
      <Epilogue text={verdict.epilogue} />
    </>
  )
}

function FailedCase({ verdict }: { verdict: Verdict }) {
  return (
    <>
      <h2>Zaak niet opgelost</h2>
      <p className="verdict-attempts">
        Jullie hebben alle {verdict.maxAttempts} pogingen gebruikt zonder de zaak op te lossen.
      </p>
      <Epilogue text={verdict.epilogue} />
    </>
  )
}

function Epilogue({ text }: { text: string | null }) {
  return (
    <>
      <h3>Wat er echt gebeurde</h3>
      <p>{text}</p>
    </>
  )
}

function VerdictForm({ teamId, caseId, verdict }: { teamId: string; caseId: string; verdict: Verdict }) {
  const submitVerdict = useSubmitVerdict(teamId, caseId)
  const latestAttempt = verdict.attempts.at(-1)
  // Start from the latest attempt, so the team can improve its answers instead of typing them again
  const [answers, setAnswers] = useState<Record<string, string>>(() => ({ ...latestAttempt?.answers }))
  const [isConfirming, setIsConfirming] = useState(false)
  const allAnswered = verdict.questions.every((question) => (answers[question.id] ?? '').trim() !== '')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!allAnswered || submitVerdict.isPending) {
      return
    }
    if (!isConfirming) {
      setIsConfirming(true)
      return
    }
    setIsConfirming(false)
    submitVerdict.mutate(answers)
  }

  return (
    <>
      <h2>Zaak oplossen</h2>
      <p>
        Weet je team wat er is gebeurd? Beantwoord de vragen in je eigen woorden. Alle antwoorden moeten goed zijn om de
        zaak op te lossen. Iedereen in het team kan een oplossing indienen.
      </p>
      <p className="verdict-attempts-left">
        Nog {verdict.attemptsLeft} van de {verdict.maxAttempts} {verdict.maxAttempts === 1 ? 'poging' : 'pogingen'}.
      </p>
      {latestAttempt && (
        <p className="verdict-score" role="status">
          Laatste poging door {latestAttempt.submittedBy}: {latestAttempt.correctAnswers} van de{' '}
          {latestAttempt.totalQuestions} antwoorden kloppen. Zoek verder en probeer het opnieuw.
        </p>
      )}
      <form className="verdict-form" onSubmit={handleSubmit}>
        {verdict.questions.map((question) => (
          <AnswerField
            key={question.id}
            question={question}
            answer={answers[question.id] ?? ''}
            onChange={(answer) => {
              setIsConfirming(false)
              setAnswers((current) => ({ ...current, [question.id]: answer }))
            }}
          />
        ))}
        {submitVerdict.isError && <SubmitError error={submitVerdict.error} />}
        {isConfirming && (
          <p className="verdict-confirm" role="alert">
            Weet je het zeker? Dit kost een poging; daarna {verdict.attemptsLeft - 1 === 1 ? 'is' : 'zijn'} er nog{' '}
            {verdict.attemptsLeft - 1} over.
          </p>
        )}
        <div className="verdict-actions">
          <button type="submit" disabled={!allAnswered || submitVerdict.isPending}>
            {submitButtonLabel(submitVerdict.isPending, isConfirming)}
          </button>
          {isConfirming && (
            <button type="button" className="link-button" onClick={() => setIsConfirming(false)}>
              Annuleren
            </button>
          )}
        </div>
      </form>
    </>
  )
}

function submitButtonLabel(isPending: boolean, isConfirming: boolean): string {
  if (isPending) {
    return 'Wordt beoordeeld...'
  }
  return isConfirming ? 'Ja, indienen' : 'Oplossing indienen'
}

/** A failed submit never costs an attempt: the backend only stores graded attempts. */
function SubmitError({ error }: { error: Error }) {
  const message = isApiError(error, 409)
    ? 'Een teamgenoot heeft net een oplossing ingediend. Bekijk de nieuwe stand voordat je het opnieuw probeert.'
    : 'Je oplossing kon niet worden beoordeeld. Er is geen poging verbruikt; probeer het opnieuw.'
  return (
    <p className="error" role="alert">
      {message}
    </p>
  )
}

interface AnswerFieldProps {
  question: VerdictQuestion
  answer: string
  onChange: (answer: string) => void
}

function AnswerField({ question, answer, onChange }: AnswerFieldProps) {
  const fieldId = `verdict-answer-${question.id}`
  return (
    <div className="verdict-question">
      <label htmlFor={fieldId}>{question.question}</label>
      <textarea
        id={fieldId}
        rows={3}
        maxLength={MAX_ANSWER_LENGTH}
        value={answer}
        onChange={(event) => onChange(event.target.value)}
      />
    </div>
  )
}

function AttemptHistory({ verdict }: { verdict: Verdict }) {
  if (verdict.attempts.length === 0) {
    return null
  }
  return (
    <section className="verdict-history">
      <h3>Eerdere pogingen</h3>
      {verdict.attempts
        .slice()
        .reverse()
        .map((attempt) => (
          <AttemptDetails key={attempt.number} attempt={attempt} questions={verdict.questions} />
        ))}
    </section>
  )
}

function AttemptDetails({ attempt, questions }: { attempt: VerdictAttempt; questions: VerdictQuestion[] }) {
  const answeredQuestions = questions.filter((question) => attempt.answers[question.id] !== undefined)
  return (
    <details>
      <summary>
        Poging {attempt.number} door {attempt.submittedBy}: {attempt.correctAnswers} van de {attempt.totalQuestions}{' '}
        goed
      </summary>
      <dl>
        {answeredQuestions.map((question) => (
          <div key={question.id}>
            <dt>{question.question}</dt>
            <dd>{attempt.answers[question.id]}</dd>
          </div>
        ))}
      </dl>
    </details>
  )
}
