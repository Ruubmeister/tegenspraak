import { Route, Routes } from 'react-router'
import { AccountPage } from './account/AccountPage'
import { RequireAdmin } from './admin/RequireAdmin'
import { UserAdminPage } from './admin/UserAdminPage'
import { LoginPage } from './auth/LoginPage'
import { RequireAuth } from './auth/RequireAuth'
import { CaseBriefing } from './cases/CaseBriefing'
import { CasePage } from './cases/CasePage'
import { ChatRoute } from './chat/ChatRoute'
import { DesktopRoute } from './desktop/DesktopRoute'
import { AppLayout } from './layout/AppLayout'
import { TeamListPage } from './teams/TeamListPage'
import { TeamPage } from './teams/TeamPage'
import { VerdictRoute } from './verdict/VerdictRoute'

export function App() {
  return (
    <Routes>
      <Route path="/inloggen" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route index element={<TeamListPage />} />
        <Route path="teams/:teamId" element={<TeamPage />} />
        <Route path="account" element={<AccountPage />} />
        <Route
          path="beheer/gebruikers"
          element={
            <RequireAdmin>
              <UserAdminPage />
            </RequireAdmin>
          }
        />
        <Route path="teams/:teamId/zaken/:caseId" element={<CasePage />}>
          <Route index element={<CaseBriefing />} />
          <Route path="chat/:characterId" element={<ChatRoute />} />
          <Route path="computer/:desktopId" element={<DesktopRoute />} />
          <Route path="oplossen" element={<VerdictRoute />} />
        </Route>
      </Route>
      <Route path="*" element={<p className="status">Pagina niet gevonden.</p>} />
    </Routes>
  )
}
