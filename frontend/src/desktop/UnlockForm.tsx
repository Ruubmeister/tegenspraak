import { useState, type FormEvent } from 'react'
import { isApiError } from '../api/http'
import type { Lock } from '../api/types'

interface UnlockFormProps {
  lock: Lock
  /** What is locked, for the label, such as "dit bestand". */
  subject: string
  isPending: boolean
  error: Error | null
  onSubmit: (code: string) => void
}

/** Shows the hint of a lock and lets the player enter the code. */
export function UnlockForm({ lock, subject, isPending, error, onSubmit }: UnlockFormProps) {
  const [code, setCode] = useState('')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSubmit(code.trim())
  }

  return (
    <form className="unlock-form" onSubmit={handleSubmit}>
      <p className="unlock-title">
        <LockIcon /> {capitalize(subject)} is vergrendeld.
      </p>
      <p className="unlock-hint">{lock.hint}</p>
      <label>
        Code
        <input
          value={code}
          onChange={(event) => setCode(event.target.value)}
          autoComplete="off"
          required
          maxLength={100}
        />
      </label>
      {error && (
        <p className="error" role="alert">
          {unlockErrorMessage(error)}
        </p>
      )}
      <button type="submit" disabled={isPending}>
        Ontgrendelen
      </button>
    </form>
  )
}

export function LockIcon() {
  return (
    <svg className="lock-icon" viewBox="0 0 16 16" width="14" height="14" role="img" aria-label="Vergrendeld">
      <path d="M4 7V5a4 4 0 0 1 8 0v2h1v8H3V7h1Zm2 0h4V5a2 2 0 0 0-4 0v2Z" fill="currentColor" fillRule="evenodd" />
    </svg>
  )
}

function unlockErrorMessage(error: Error): string {
  if (isApiError(error, 422)) {
    return 'Deze code klopt niet.'
  }
  if (isApiError(error, 429)) {
    return 'Te veel foute codes. Wacht een minuut en probeer het opnieuw.'
  }
  return 'Ontgrendelen is mislukt. Probeer het opnieuw.'
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}
