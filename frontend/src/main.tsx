import { QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { App } from './App'
import { isApiError } from './api/http'
import { currentPlayerQueryKey } from './auth/useCurrentPlayer'
import './index.css'

const queryClient: QueryClient = new QueryClient({
  queryCache: new QueryCache({
    // An expired session shows up as a 401 on any query; marking the player as logged out redirects to the login page
    onError: (error) => {
      if (isApiError(error, 401)) {
        queryClient.setQueryData(currentPlayerQueryKey, null)
      }
    },
  }),
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => !isApiError(error, 401) && !isApiError(error, 404) && failureCount < 2,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
