import { NavLink, Outlet, Link } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '@/auth/AuthContext'
import { MarcaSol } from './Logo'
import { nombreRol } from '@/util/formato'

const recepcion = [
  ['/panel', 'Hoy'],
  ['/panel/reservas', 'Reservas'],
  ['/panel/nueva-reserva', 'Nueva reserva'],
  ['/panel/habitaciones', 'Habitaciones'],
] as const

const administracion = [
  ['/panel/admin/reportes', 'Reportes'],
  ['/panel/admin/habitaciones', 'Tipos y habitaciones'],
  ['/panel/admin/tarifas', 'Tarifas y temporadas'],
  ['/panel/admin/bloqueos', 'Bloqueos'],
  ['/panel/admin/usuarios', 'Usuarios'],
  ['/panel/admin/configuracion', 'Configuración'],
  ['/panel/admin/auditoria', 'Auditoría'],
] as const

function Grupo({ titulo, enlaces, alNavegar }: { titulo: string; enlaces: readonly (readonly [string, string])[]; alNavegar: () => void }) {
  return (
    <div>
      <p className="px-3 text-xs font-medium text-pizarra-400">{titulo}</p>
      <ul className="mt-1.5 space-y-0.5">
        {enlaces.map(([a, nombre]) => (
          <li key={a}>
            <NavLink to={a} end={a === '/panel'} onClick={alNavegar}
              className={({ isActive }) => `block rounded-lg px-3 py-2 text-[15px] ${isActive ? 'bg-noche text-white' : 'text-noche hover:bg-neblina'}`}>
              {nombre}
            </NavLink>
          </li>
        ))}
      </ul>
    </div>
  )
}

/** Panel interno: sobrio y denso, pensado para trabajar todo el día. */
export function LayoutPanel() {
  const { usuario, esAdmin, cerrarSesion } = useAuth()
  const [abierto, setAbierto] = useState(false)
  return (
    <div className="min-h-screen bg-neblina lg:grid lg:grid-cols-[240px_1fr]">
      <aside className={`${abierto ? 'block' : 'hidden'} fixed inset-0 z-30 overflow-y-auto border-r border-neblina-200 bg-white p-4 lg:static lg:block`}>
        <div className="flex items-center justify-between">
          <Link to="/panel" className="flex items-center gap-2 px-2">
            <MarcaSol className="h-6 w-6" />
            <span className="font-titulo text-lg">Aurora · Panel</span>
          </Link>
          <button className="rounded-full p-2 hover:bg-neblina lg:hidden" onClick={() => setAbierto(false)} aria-label="Cerrar menú">
            <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 6l12 12M18 6L6 18" /></svg>
          </button>
        </div>
        <nav className="mt-8 space-y-6">
          <Grupo titulo="Recepción" enlaces={recepcion} alNavegar={() => setAbierto(false)} />
          {esAdmin && <Grupo titulo="Administración" enlaces={administracion} alNavegar={() => setAbierto(false)} />}
        </nav>
        <div className="mt-10 border-t border-neblina-200 px-3 pt-4 text-sm">
          <p className="font-medium">{usuario?.nombre} {usuario?.apellido}</p>
          <p className="text-pizarra">{usuario && nombreRol[usuario.rol]}</p>
          <div className="mt-3 flex gap-4">
            <Link to="/" className="boton-texto">Ver el sitio</Link>
            <button className="boton-texto" onClick={cerrarSesion}>Salir</button>
          </div>
        </div>
      </aside>
      <div className="min-w-0">
        <div className="flex items-center gap-3 border-b border-neblina-200 bg-white px-4 py-3 lg:hidden">
          <button className="rounded-full p-2 hover:bg-neblina" onClick={() => setAbierto(true)} aria-label="Abrir menú">
            <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M4 7h16M4 12h16M4 17h16" /></svg>
          </button>
          <span className="font-titulo text-lg">Aurora · Panel</span>
        </div>
        <main className="mx-auto max-w-6xl px-4 py-8 sm:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export function Encabezado({ titulo, children, bajada }: { titulo: string; bajada?: string; children?: React.ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-3xl">{titulo}</h1>
        {bajada && <p className="mt-1 text-pizarra">{bajada}</p>}
      </div>
      {children && <div className="flex flex-wrap gap-2">{children}</div>}
    </div>
  )
}
