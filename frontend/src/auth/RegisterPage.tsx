import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, Navigate, useNavigate } from 'react-router'
import { isApiError } from '../api/http'
import { register } from './authApi'
import { AuthForm } from './AuthForm'
import { currentPlayerQueryKey, useCurrentPlayer } from './useCurrentPlayer'

export function RegisterPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { data: player } = useCurrentPlayer()
  const registerMutation = useMutation({
    mutationFn: register,
    onSuccess: (registeredPlayer) => {
      queryClient.setQueryData(currentPlayerQueryKey, registeredPlayer)
      navigate('/')
    },
  })

  if (player) {
    return <Navigate to="/" replace />
  }

  return (
    <main className="auth-page">
      <h1>Tegenspraak</h1>
      <h2>Registreren</h2>
      <AuthForm
        submitLabel="Account aanmaken"
        isSubmitting={registerMutation.isPending}
        errorMessage={registerErrorMessage(registerMutation.error)}
        passwordMinLength={8}
        onSubmit={(credentials) => registerMutation.mutate(credentials)}
      />
      <p>
        Al een account? <Link to="/inloggen">Log hier in</Link>.
      </p>
    </main>
  )
}

function registerErrorMessage(error: Error | null): string | null {
  if (!error) {
    return null
  }
  if (isApiError(error, 409)) {
    return 'Deze gebruikersnaam is al bezet.'
  }
  if (isApiError(error, 400)) {
    return 'Gebruik 3 tot 50 letters, cijfers of . _ - als naam en minstens 8 tekens als wachtwoord.'
  }
  return 'Registreren is mislukt. Probeer het later opnieuw.'
}
