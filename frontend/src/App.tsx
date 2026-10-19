import { lazy, Suspense, useEffect } from 'react'
import { Link, Route, Routes, useLocation } from 'react-router-dom'
import { LayoutPublico } from '@/componentes/LayoutPublico'
import { RutaProtegida } from '@/componentes/RutaProtegida'
import { Cargando } from '@/componentes/Estados'
import Inicio from '@/paginas/publico/Inicio'
import Habitaciones from '@/paginas/publico/Habitaciones'
import Buscar from '@/paginas/publico/Buscar'
import Pagar from '@/paginas/publico/Pagar'
import MisReservas from '@/paginas/publico/MisReservas'
import DetalleReserva from '@/paginas/publico/DetalleReserva'
import { Ingresar, MiCuenta, Recuperar, Registro, Restablecer } from '@/paginas/publico/Cuenta'

// El panel interno se carga aparte: el huésped no descarga gráficos ni pantallas de gestión.
const LayoutPanel = lazy(() => import('@/componentes/LayoutPanel').then((m) => ({ default: m.LayoutPanel })))
const Hoy = lazy(() => import('@/paginas/panel/Hoy'))
const Reservas = lazy(() => import('@/paginas/panel/Reservas'))
const DetalleReservaPanel = lazy(() => import('@/paginas/panel/DetalleReservaPanel'))
const NuevaReserva = lazy(() => import('@/paginas/panel/NuevaReserva'))
const HabitacionesPanel = lazy(() => import('@/paginas/panel/HabitacionesPanel'))
const Reportes = lazy(() => import('@/paginas/admin/Reportes'))
const TiposYHabitaciones = lazy(() => import('@/paginas/admin/TiposYHabitaciones'))
const Tarifas = lazy(() => import('@/paginas/admin/Tarifas'))
const Bloqueos = lazy(() => import('@/paginas/admin/Bloqueos'))
const Usuarios = lazy(() => import('@/paginas/admin/Usuarios'))
const Configuracion = lazy(() => import('@/paginas/admin/Configuracion'))
const Auditoria = lazy(() => import('@/paginas/admin/Auditoria'))

function SubirAlNavegar() {
  const { pathname } = useLocation()
  useEffect(() => window.scrollTo(0, 0), [pathname])
  return null
}

function NoEncontrada() {
  return (
    <div className="mx-auto max-w-xl px-4 py-24 text-center">
      <h1 className="text-4xl">Esta página no existe</h1>
      <p className="mt-3 text-pizarra">Puede que el enlace esté mal escrito o que la página se haya movido.</p>
      <Link to="/" className="boton-primario mt-6">Ir al inicio</Link>
    </div>
  )
}

const soloHuesped = (el: JSX.Element) => <RutaProtegida roles={['HUESPED']}>{el}</RutaProtegida>
const admin = (el: JSX.Element) => <RutaProtegida roles={['ADMINISTRADOR']}>{el}</RutaProtegida>

export default function App() {
  return (
    <>
      <SubirAlNavegar />
      <Suspense fallback={<div className="mx-auto max-w-5xl px-4"><Cargando /></div>}>
        <Routes>
          <Route element={<LayoutPublico />}>
            <Route index element={<Inicio />} />
            <Route path="habitaciones" element={<Habitaciones />} />
            <Route path="buscar" element={<Buscar />} />
            <Route path="pagar/:codigo" element={soloHuesped(<Pagar />)} />
            <Route path="mis-reservas" element={soloHuesped(<MisReservas />)} />
            <Route path="mis-reservas/:codigo" element={soloHuesped(<DetalleReserva />)} />
            <Route path="mi-cuenta" element={<RutaProtegida><MiCuenta /></RutaProtegida>} />
            <Route path="ingresar" element={<Ingresar />} />
            <Route path="registro" element={<Registro />} />
            <Route path="recuperar" element={<Recuperar />} />
            <Route path="restablecer" element={<Restablecer />} />
            <Route path="*" element={<NoEncontrada />} />
          </Route>
          <Route path="panel" element={<RutaProtegida roles={['RECEPCIONISTA', 'ADMINISTRADOR']}><LayoutPanel /></RutaProtegida>}>
            <Route index element={<Hoy />} />
            <Route path="reservas" element={<Reservas />} />
            <Route path="reservas/:codigo" element={<DetalleReservaPanel />} />
            <Route path="nueva-reserva" element={<NuevaReserva />} />
            <Route path="habitaciones" element={<HabitacionesPanel />} />
            <Route path="admin/reportes" element={admin(<Reportes />)} />
            <Route path="admin/habitaciones" element={admin(<TiposYHabitaciones />)} />
            <Route path="admin/tarifas" element={admin(<Tarifas />)} />
            <Route path="admin/bloqueos" element={admin(<Bloqueos />)} />
            <Route path="admin/usuarios" element={admin(<Usuarios />)} />
            <Route path="admin/configuracion" element={admin(<Configuracion />)} />
            <Route path="admin/auditoria" element={admin(<Auditoria />)} />
          </Route>
        </Routes>
      </Suspense>
    </>
  )
}
