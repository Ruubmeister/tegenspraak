import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, Navigate, useNavigate } from 'react-router'
import { isApiError } from '../api/http'
import { login } from './authApi'
import { AuthForm } from './AuthForm'
import { currentPlayerQueryKey, useCurrentPlayer } from './useCurrentPlayer'

export function LoginPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  // Fetching the current player also provides the XSRF-TOKEN cookie that the login request needs
  const { data: player } = useCurrentPlayer()
  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (loggedInPlayer) => {
      queryClient.setQueryData(currentPlayerQueryKey, loggedInPlayer)
      navigate('/')
    },
  })

  if (player) {
    return <Navigate to="/" replace />
  }

  return (
    <main className="auth-page">
      <h1>Tegenspraak</h1>
      <h2>Inloggen</h2>
      <AuthForm
        submitLabel="Inloggen"
        isSubmitting={loginMutation.isPending}
        errorMessage={loginErrorMessage(loginMutation.error)}
        onSubmit={(credentials) => loginMutation.mutate(credentials)}
      />
      <p>
        Nog geen account? <Link to="/registreren">Registreer je hier</Link>.
      </p>
    </main>
  )
}

function loginErrorMessage(error: Error | null): string | null {
  if (!error) {
    return null
  }
  if (isApiError(error, 401)) {
    return 'Onjuiste gebruikersnaam of wachtwoord.'
  }
  return 'Inloggen is mislukt. Probeer het later opnieuw.'
}
