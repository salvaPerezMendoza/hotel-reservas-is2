import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useAuth } from '@/auth/AuthContext'
import { api } from '@/api/cliente'
import type { Hotel } from '@/api/tipos'
import { Logo } from './Logo'

export function useHotel() {
  return useQuery({ queryKey: ['hotel'], queryFn: () => api<Hotel>('/hotel'), staleTime: 300_000 })
}

function Enlace({ a, children }: { a: string; children: string }) {
  return (
    <NavLink to={a} className={({ isActive }) => `rounded-full px-3 py-1.5 text-[15px] font-medium transition-colors ${isActive ? 'bg-white/15' : 'hover:bg-white/10'}`}>
      {children}
    </NavLink>
  )
}

export function LayoutPublico() {
  const { usuario, cerrarSesion, esPersonal } = useAuth()
  const { pathname } = useLocation()
  const [menu, setMenu] = useState(false)
  const { data: hotel } = useHotel()
  const sobreCielo = pathname === '/'

  return (
    <div className="flex min-h-screen flex-col">
      <header className={`${sobreCielo ? 'absolute inset-x-0 top-0 z-20' : 'bg-noche'} text-white`}>
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4 sm:px-6">
          <Logo claro />
          <button className="rounded-full p-2 hover:bg-white/10 md:hidden" onClick={() => setMenu(!menu)} aria-expanded={menu} aria-label="Menú">
            <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="2"><path d="M4 7h16M4 12h16M4 17h16" /></svg>
          </button>
          <nav className={`${menu ? 'flex' : 'hidden'} absolute inset-x-4 top-16 z-30 flex-col gap-1 rounded-2xl bg-noche p-3 shadow-flotante md:static md:flex md:flex-row md:items-center md:bg-transparent md:p-0 md:shadow-none`} onClick={() => setMenu(false)}>
            <Enlace a="/habitaciones">Habitaciones</Enlace>
            {usuario && !esPersonal && <Enlace a="/mis-reservas">Mis reservas</Enlace>}
            {esPersonal && <Enlace a="/panel">Panel del hotel</Enlace>}
            {usuario ? (
              <>
                <Enlace a="/mi-cuenta">{usuario.nombre}</Enlace>
                <button onClick={cerrarSesion} className="rounded-full px-3 py-1.5 text-left text-[15px] font-medium hover:bg-white/10">Salir</button>
              </>
            ) : (
              <>
                <Enlace a="/ingresar">Ingresar</Enlace>
                <Link to="/registro" className="rounded-full bg-white px-4 py-1.5 text-[15px] font-semibold text-noche hover:bg-neblina">Crear cuenta</Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="mt-20 bg-noche text-neblina-300">
        <div className="mx-auto grid max-w-6xl gap-8 px-4 py-12 sm:grid-cols-3 sm:px-6">
          <div>
            <Logo claro />
            <p className="mt-3 max-w-xs text-sm">Av. Costanera 1450, Buenos Aires. Reservas: +54 11 4000-1450.</p>
          </div>
          <div className="text-sm">
            <p className="font-semibold text-white">Horarios</p>
            <p className="mt-2">Check-in desde las {hotel?.horaCheckIn ?? '14:00'}</p>
            <p>Check-out hasta las {hotel?.horaCheckOut ?? '10:00'}</p>
          </div>
          <div className="text-sm">
            <p className="font-semibold text-white">Cancelaciones</p>
            <p className="mt-2 max-w-xs">{hotel?.descripcionPoliticaCancelacion}</p>
          </div>
        </div>
        <p className="border-t border-white/10 px-4 py-4 text-center text-xs text-pizarra-400">
          Proyecto académico · Ingeniería de Software II · FIUBA · Grupo 11. Hotel y datos ficticios.
        </p>
      </footer>
    </div>
  )
}
