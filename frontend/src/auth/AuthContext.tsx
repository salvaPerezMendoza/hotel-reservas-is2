import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, guardarToken, registrarAlExpirarSesion, tokenActual } from '@/api/cliente'
import type { Sesion, Usuario } from '@/api/tipos'

interface ValorAuth {
  usuario: Usuario | null
  cargando: boolean
  iniciarSesion: (sesion: Sesion) => void
  cerrarSesion: () => void
  actualizarUsuario: (u: Usuario) => void
  esPersonal: boolean
  esAdmin: boolean
}

const Contexto = createContext<ValorAuth | null>(null)

export function ProveedorAuth({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [cargando, setCargando] = useState(Boolean(tokenActual()))

  useEffect(() => {
    registrarAlExpirarSesion(() => setUsuario(null))
    if (!tokenActual()) return
    api<Usuario>('/auth/me')
      .then(setUsuario)
      .catch(() => guardarToken(null))
      .finally(() => setCargando(false))
  }, [])

  const iniciarSesion = useCallback((sesion: Sesion) => {
    guardarToken(sesion.token)
    setUsuario(sesion.usuario)
  }, [])

  const cerrarSesion = useCallback(() => {
    guardarToken(null)
    setUsuario(null)
  }, [])

  const valor = useMemo<ValorAuth>(
    () => ({
      usuario,
      cargando,
      iniciarSesion,
      cerrarSesion,
      actualizarUsuario: setUsuario,
      esPersonal: usuario?.rol === 'RECEPCIONISTA' || usuario?.rol === 'ADMINISTRADOR',
      esAdmin: usuario?.rol === 'ADMINISTRADOR',
    }),
    [usuario, cargando, iniciarSesion, cerrarSesion],
  )

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useAuth() {
  const valor = useContext(Contexto)
  if (!valor) throw new Error('useAuth fuera de ProveedorAuth')
  return valor
}
