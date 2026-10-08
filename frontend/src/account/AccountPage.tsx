import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { isApiError } from '../api/http'
import { changePassword } from '../auth/authApi'
import { useCurrentPlayer } from '../auth/useCurrentPlayer'

export function AccountPage() {
  const { data: player } = useCurrentPlayer()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [repeatedPassword, setRepeatedPassword] = useState('')
  const [mismatch, setMismatch] = useState(false)
  const changePasswordMutation = useMutation({
    mutationFn: changePassword,
    onSuccess: () => {
      setCurrentPassword('')
      setNewPassword('')
      setRepeatedPassword('')
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const passwordsDiffer = newPassword !== repeatedPassword
    setMismatch(passwordsDiffer)
    if (!passwordsDiffer) {
      changePasswordMutation.mutate({ currentPassword, newPassword })
    }
  }

  return (
    <main className="account-page">
      <h1>Mijn account</h1>
      <p>Ingelogd als {player?.username}.</p>
      <h2>Wachtwoord wijzigen</h2>
      <form className="auth-form" onSubmit={handleSubmit}>
        <label>
          Huidig wachtwoord
          <input
            type="password"
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            autoComplete="current-password"
            required
            maxLength={72}
          />
        </label>
        <label>
          Nieuw wachtwoord
          <input
            type="password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            autoComplete="new-password"
            required
            minLength={8}
            maxLength={72}
          />
        </label>
        <label>
          Herhaal nieuw wachtwoord
          <input
            type="password"
            value={repeatedPassword}
            onChange={(event) => setRepeatedPassword(event.target.value)}
            autoComplete="new-password"
            required
            minLength={8}
            maxLength={72}
          />
        </label>
        {mismatch && (
          <p className="error" role="alert">
            De nieuwe wachtwoorden zijn niet gelijk.
          </p>
        )}
        {changePasswordMutation.isError && (
          <p className="error" role="alert">
            {changePasswordErrorMessage(changePasswordMutation.error)}
          </p>
        )}
        {changePasswordMutation.isSuccess && <p role="status">Je wachtwoord is gewijzigd.</p>}
        <button type="submit" disabled={changePasswordMutation.isPending}>
          Wachtwoord wijzigen
        </button>
      </form>
    </main>
  )
}

function changePasswordErrorMessage(error: Error): string {
  if (isApiError(error, 422)) {
    return 'Je huidige wachtwoord klopt niet.'
  }
  if (isApiError(error, 400)) {
    return 'Het nieuwe wachtwoord moet 8 tot 72 tekens lang zijn.'
  }
  return 'Je wachtwoord kon niet worden gewijzigd. Probeer het later opnieuw.'
}
