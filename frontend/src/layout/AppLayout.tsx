import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, Outlet, useNavigate } from 'react-router'
import { logout } from '../auth/authApi'
import { useCurrentPlayer } from '../auth/useCurrentPlayer'

export function AppLayout() {
  const { data: player } = useCurrentPlayer()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      // Drop all cached player data; the login page fetches a fresh session state and CSRF cookie
      queryClient.clear()
      navigate('/inloggen')
    },
  })

  return (
    <div className="app">
      <header className="app-header">
        <Link to="/" className="brand">
          Tegenspraak
        </Link>
        <div className="app-header-player">
          {player?.role === 'ADMIN' && <Link to="/beheer/gebruikers">Gebruikersbeheer</Link>}
          <Link to="/account">{player?.username}</Link>
          <button type="button" className="link-button" onClick={() => logoutMutation.mutate()}>
            Uitloggen
          </button>
        </div>
      </header>
      <Outlet />
    </div>
  )
}
