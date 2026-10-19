import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { Reserva, Tablero } from '@/api/tipos'
import { Encabezado } from '@/componentes/LayoutPanel'
import { Cargando, CajaError } from '@/componentes/Estados'
import { TablaReservas } from '@/componentes/TablaReservas'
import { fechaTexto, hoyIso } from '@/util/formato'

function Cifra({ valor, nombre, detalle }: { valor: string | number; nombre: string; detalle?: string }) {
  return (
    <div className="rounded-2xl bg-white p-4 ring-1 ring-neblina-200">
      <p className="text-sm text-pizarra">{nombre}</p>
      <p className="mt-1 cifra text-3xl tabular-nums">{valor}</p>
      {detalle && <p className="text-xs text-pizarra">{detalle}</p>}
    </div>
  )
}

export default function Hoy() {
  const tablero = useQuery({ queryKey: ['tablero'], queryFn: () => api<Tablero>('/recepcion/tablero'), refetchInterval: 30_000 })
  const llegadas = useQuery({ queryKey: ['llegadas'], queryFn: () => api<Reserva[]>('/recepcion/llegadas') })
  const salidas = useQuery({ queryKey: ['salidas'], queryFn: () => api<Reserva[]>('/recepcion/salidas') })
  const t = tablero.data

  return (
    <>
      <Encabezado titulo="Hoy" bajada={fechaTexto(hoyIso())}>
        <Link to="/panel/nueva-reserva" className="boton-primario">Nueva reserva</Link>
      </Encabezado>
      {tablero.error && <CajaError error={tablero.error} />}
      {t && (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
          <Cifra nombre="Llegadas" valor={t.llegadasHoy} detalle="con ingreso hoy" />
          <Cifra nombre="Salidas" valor={t.salidasHoy} detalle="con egreso hoy" />
          <Cifra nombre="Ocupación" valor={`${Math.round(t.ocupacionHoy)}%`} detalle={`${t.enCurso} habitaciones ocupadas`} />
          <Cifra nombre="Listas para vender" valor={`${t.habitacionesDisponibles}/${t.habitacionesTotales}`} detalle={`${t.enLimpieza} en limpieza · ${t.enMantenimiento} en mantenimiento`} />
        </div>
      )}
      {t && t.pendientesDePago > 0 && (
        <p className="mt-3 text-sm text-pizarra">{t.pendientesDePago === 1 ? 'Hay 1 reserva online' : `Hay ${t.pendientesDePago} reservas online`} esperando el pago.</p>
      )}
      <section className="panel mt-6">
        <h2 className="mb-3 text-xl">Llegadas de hoy</h2>
        {llegadas.isLoading ? <Cargando /> : <TablaReservas reservas={llegadas.data ?? []} vacio="No hay llegadas pendientes para hoy." />}
      </section>
      <section className="panel mt-6">
        <h2 className="mb-3 text-xl">Salidas de hoy</h2>
        {salidas.isLoading ? <Cargando /> : <TablaReservas reservas={salidas.data ?? []} vacio="No quedan salidas pendientes para hoy." />}
      </section>
    </>
  )
}
