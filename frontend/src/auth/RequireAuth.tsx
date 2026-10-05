import type { ReactNode } from 'react'
import { Navigate } from 'react-router'
import { useCurrentPlayer } from './useCurrentPlayer'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { data: player, isPending, isError } = useCurrentPlayer()

  if (isPending) {
    return <p className="status">Laden...</p>
  }
  if (isError) {
    return <p className="status error">De server is niet bereikbaar.</p>
  }
  if (player === null) {
    return <Navigate to="/inloggen" replace />
  }
  return children
}
