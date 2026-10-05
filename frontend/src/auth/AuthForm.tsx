import { useState, type FormEvent } from 'react'
import type { Credentials } from './authApi'

interface AuthFormProps {
  submitLabel: string
  isSubmitting: boolean
  errorMessage: string | null
  passwordMinLength?: number
  onSubmit: (credentials: Credentials) => void
}

export function AuthForm({ submitLabel, isSubmitting, errorMessage, passwordMinLength, onSubmit }: AuthFormProps) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSubmit({ username: username.trim(), password })
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <label>
        Gebruikersnaam
        <input
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          autoComplete="username"
          required
          minLength={3}
          maxLength={50}
        />
      </label>
      <label>
        Wachtwoord
        <input
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete={passwordMinLength ? 'new-password' : 'current-password'}
          required
          minLength={passwordMinLength}
          maxLength={72}
        />
      </label>
      {errorMessage && (
        <p className="error" role="alert">
          {errorMessage}
        </p>
      )}
      <button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </button>
    </form>
  )
}
