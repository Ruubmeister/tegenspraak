import { useState, type FormEvent } from 'react'
import { isApiError } from '../api/http'
import type { UserAccount } from '../api/types'
import { useCreateUser, useDeleteUser, useResetPassword, useUsers } from './userAdminApi'

const ROLE_LABELS = { ADMIN: 'Beheerder', PLAYER: 'Speler' } as const

/** Lets the admin create players, reset their passwords and delete them. The admin account itself is fixed. */
export function UserAdminPage() {
  const { data: users, isPending, isError } = useUsers()

  return (
    <main className="admin-page">
      <h1>Gebruikersbeheer</h1>
      <section>
        <h2>Nieuwe speler</h2>
        <CreateUserForm />
      </section>
      <section>
        <h2>Gebruikers</h2>
        {isPending && <p className="status">Gebruikers laden...</p>}
        {isError && <p className="status error">De gebruikers konden niet worden geladen.</p>}
        {users && <UserTable users={users} />}
      </section>
    </main>
  )
}

function CreateUserForm() {
  const createUser = useCreateUser()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    createUser.mutate(
      { username: username.trim(), password },
      {
        onSuccess: () => {
          setUsername('')
          setPassword('')
        },
      },
    )
  }

  return (
    <form className="inline-form" onSubmit={handleSubmit}>
      <label>
        Gebruikersnaam
        <input
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          autoComplete="off"
          required
          minLength={3}
          maxLength={50}
          pattern="[A-Za-z0-9_.\-]+"
        />
      </label>
      <label>
        Wachtwoord
        <input
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="new-password"
          required
          minLength={8}
          maxLength={72}
        />
      </label>
      <button type="submit" disabled={createUser.isPending}>
        Speler aanmaken
      </button>
      {createUser.isSuccess && <p role="status">Speler {createUser.data.username} is aangemaakt.</p>}
      {createUser.isError && (
        <p className="error" role="alert">
          {createUserErrorMessage(createUser.error)}
        </p>
      )}
    </form>
  )
}

function createUserErrorMessage(error: Error): string {
  if (isApiError(error, 409)) {
    return 'Deze gebruikersnaam is al bezet.'
  }
  if (isApiError(error, 400)) {
    return 'Gebruik 3 tot 50 letters, cijfers of . _ - als naam en minstens 8 tekens als wachtwoord.'
  }
  return 'De speler kon niet worden aangemaakt. Probeer het later opnieuw.'
}

function UserTable({ users }: { users: UserAccount[] }) {
  return (
    <table className="user-table">
      <thead>
        <tr>
          <th>Gebruikersnaam</th>
          <th>Rol</th>
          <th>Aangemaakt</th>
          <th>
            <span className="visually-hidden">Acties</span>
          </th>
        </tr>
      </thead>
      <tbody>
        {users.map((user) => (
          <UserRow key={user.id} user={user} />
        ))}
      </tbody>
    </table>
  )
}

function UserRow({ user }: { user: UserAccount }) {
  const deleteUser = useDeleteUser()
  const [isResetting, setIsResetting] = useState(false)

  function handleDelete() {
    if (window.confirm(`Weet je zeker dat je ${user.username} wilt verwijderen? Alle voortgang gaat verloren.`)) {
      deleteUser.mutate(user.id)
    }
  }

  return (
    <tr>
      <td>{user.username}</td>
      <td data-label="Rol">{ROLE_LABELS[user.role]}</td>
      <td data-label="Aangemaakt">{new Date(user.createdAt).toLocaleDateString('nl-NL')}</td>
      <td className="user-actions">
        {user.role === 'PLAYER' && (
          <>
            <button type="button" className="link-button" onClick={() => setIsResetting(!isResetting)}>
              Wachtwoord resetten
            </button>
            <button type="button" className="link-button" onClick={handleDelete} disabled={deleteUser.isPending}>
              Verwijderen
            </button>
            {deleteUser.isError && (
              <p className="error" role="alert">
                {user.username} kon niet worden verwijderd.
              </p>
            )}
            {isResetting && <ResetPasswordForm user={user} onDone={() => setIsResetting(false)} />}
          </>
        )}
      </td>
    </tr>
  )
}

function ResetPasswordForm({ user, onDone }: { user: UserAccount; onDone: () => void }) {
  const resetPassword = useResetPassword(user.id)
  const [newPassword, setNewPassword] = useState('')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    resetPassword.mutate(newPassword, { onSuccess: onDone })
  }

  return (
    <form className="inline-form" onSubmit={handleSubmit}>
      <label>
        Nieuw wachtwoord voor {user.username}
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
      <button type="submit" disabled={resetPassword.isPending}>
        Opslaan
      </button>
      {resetPassword.isError && (
        <p className="error" role="alert">
          Het wachtwoord kon niet worden gewijzigd.
        </p>
      )}
    </form>
  )
}
