import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '@/auth/AuthContext'
import { Cargando } from './Estados'
import type { Rol } from '@/api/tipos'

export function RutaProtegida({ children, roles }: { children: ReactNode; roles?: Rol[] }) {
  const { usuario, cargando } = useAuth()
  const ubicacion = useLocation()
  if (cargando) return <div className="mx-auto max-w-5xl px-4"><Cargando /></div>
  if (!usuario) return <Navigate to={`/ingresar?volver=${encodeURIComponent(ubicacion.pathname + ubicacion.search)}`} replace />
  if (roles && !roles.includes(usuario.rol)) return <Navigate to="/" replace />
  return <>{children}</>
}
