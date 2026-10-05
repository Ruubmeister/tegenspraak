import { Route, Routes } from 'react-router'
import { LoginPage } from './auth/LoginPage'
import { RegisterPage } from './auth/RegisterPage'
import { RequireAuth } from './auth/RequireAuth'
import { CaseListPage } from './cases/CaseListPage'
import { CaseBriefing } from './cases/CaseBriefing'
import { CasePage } from './cases/CasePage'
import { ChatRoute } from './chat/ChatRoute'
import { DesktopRoute } from './desktop/DesktopRoute'
import { AppLayout } from './layout/AppLayout'

export function App() {
  return (
    <Routes>
      <Route path="/inloggen" element={<LoginPage />} />
      <Route path="/registreren" element={<RegisterPage />} />
      <Route
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route index element={<CaseListPage />} />
        <Route path="zaken/:caseId" element={<CasePage />}>
          <Route index element={<CaseBriefing />} />
          <Route path="chat/:characterId" element={<ChatRoute />} />
          <Route path="computer/:desktopId" element={<DesktopRoute />} />
        </Route>
      </Route>
      <Route path="*" element={<p className="status">Pagina niet gevonden.</p>} />
    </Routes>
  )
}
