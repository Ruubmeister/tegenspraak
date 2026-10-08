import type { ReactNode } from 'react'
import { Navigate } from 'react-router'
import { useCurrentPlayer } from '../auth/useCurrentPlayer'

/** Hides admin pages from players. The backend enforces the same rule; this only keeps the UI consistent. */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const { data: player, isPending } = useCurrentPlayer()

  if (isPending) {
    return <p className="status">Laden...</p>
  }
  if (player?.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }
  return children
}
