import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import '@fontsource/marcellus/400.css'
import '@fontsource-variable/figtree'
import './index.css'
import App from './App'
import { ProveedorAuth } from './auth/AuthContext'
import { ProveedorAvisos } from './componentes/Avisos'

const clienteConsultas = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 15_000 } },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={clienteConsultas}>
      <BrowserRouter>
        <ProveedorAuth>
          <ProveedorAvisos>
            <App />
          </ProveedorAvisos>
        </ProveedorAuth>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
)
