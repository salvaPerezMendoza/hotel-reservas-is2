import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { Reserva } from '@/api/tipos'
import { Cargando, CajaError, Vacio } from '@/componentes/Estados'
import { InsigniaReserva } from '@/componentes/InsigniaEstado'
import { PortadaHabitacion } from '@/componentes/PortadaHabitacion'
import { fechaCortaTexto, hoyIso, noches, plata } from '@/util/formato'

export default function MisReservas() {
  const { data, isLoading, error } = useQuery({ queryKey: ['mis-reservas'], queryFn: () => api<Reserva[]>('/reservas/mias') })
  const hoy = hoyIso()
  const proximas = data?.filter((r) => ['PENDIENTE_PAGO', 'CONFIRMADA', 'EN_CURSO'].includes(r.estado) && r.checkOut >= hoy) ?? []
  const pasadas = data?.filter((r) => !proximas.includes(r)) ?? []

  return (
    <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6">
      <h1 className="text-5xl">Mis reservas</h1>
      {isLoading && <Cargando />}
      {error && <div className="mt-6"><CajaError error={error} /></div>}
      {data && data.length === 0 && (
        <div className="mt-8">
          <Vacio titulo="Todavía no tenés reservas">
            <Link to="/" className="boton-reservar mt-4">Buscar fechas</Link>
          </Vacio>
        </div>
      )}
      {proximas.length > 0 && <Grupo titulo="Próximas" reservas={proximas} />}
      {pasadas.length > 0 && <Grupo titulo="Anteriores y canceladas" reservas={pasadas} />}
    </div>
  )
}

function Grupo({ titulo, reservas }: { titulo: string; reservas: Reserva[] }) {
  return (
    <section className="mt-10">
      <h2 className="text-2xl">{titulo}</h2>
      <ul className="mt-4 space-y-3">
        {reservas.map((r) => (
          <li key={r.codigo}>
            <Link to={`/mis-reservas/${r.codigo}`} className="grid grid-cols-[88px_1fr] items-center gap-4 rounded-2xl bg-white p-3 ring-1 ring-neblina-200 transition hover:ring-noche-600 sm:grid-cols-[120px_1fr_auto]">
              <PortadaHabitacion nombre={r.tipoNombre} imagenUrl={r.tipoImagenUrl} className="aspect-[4/3] rounded-xl" />
              <div>
                <p className="font-titulo text-xl">{r.tipoNombre}</p>
                <p className="text-sm text-pizarra">{fechaCortaTexto(r.checkIn)} – {fechaCortaTexto(r.checkOut)} · {noches(r.noches)}</p>
                <p className="mt-1 text-xs text-pizarra">{r.codigo}</p>
              </div>
              <div className="col-span-2 flex items-center justify-between gap-3 sm:col-span-1 sm:flex-col sm:items-end">
                <InsigniaReserva estado={r.estado} />
                <span className="tabular-nums">{plata(r.totalACobrar)}</span>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
