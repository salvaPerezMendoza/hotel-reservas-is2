import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { OpcionDisponible, Reserva, ResultadoBusqueda } from '@/api/tipos'
import { useAuth } from '@/auth/AuthContext'
import { BuscadorEstadia } from '@/componentes/BuscadorEstadia'
import { Cargando, CajaError, Vacio } from '@/componentes/Estados'
import { PortadaHabitacion } from '@/componentes/PortadaHabitacion'
import { TiraDeNoches } from '@/componentes/TiraDeNoches'
import { useAvisos } from '@/componentes/Avisos'
import { fechaTexto, hoyIso, noches, personas, plata, sumarDias } from '@/util/formato'
import { useHotel } from '@/componentes/LayoutPublico'

export default function Buscar() {
  const [params] = useSearchParams()
  const checkIn = params.get('checkIn') ?? sumarDias(hoyIso(), 7)
  const checkOut = params.get('checkOut') ?? sumarDias(checkIn, 3)
  const huespedes = Number(params.get('huespedes') ?? 2)
  const tipoId = params.get('tipoId')
  const consulta = new URLSearchParams({ checkIn, checkOut, huespedes: String(huespedes) })
  if (tipoId) consulta.set('tipoId', tipoId)

  const { data, isLoading, error } = useQuery({
    queryKey: ['disponibilidad', consulta.toString()],
    queryFn: () => api<ResultadoBusqueda>(`/disponibilidad?${consulta}`),
  })

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <BuscadorEstadia key={consulta.toString()} inicial={{ checkIn, checkOut, huespedes }} />
      <div className="mt-10">
        <h1 className="text-4xl">{fechaTexto(checkIn)} al {fechaTexto(checkOut)}</h1>
        <p className="mt-2 text-pizarra">{data ? `${noches(data.noches)} · ${personas(huespedes)}` : personas(huespedes)}</p>
      </div>
      {isLoading && <Cargando texto="Buscando habitaciones libres…" />}
      {error && <div className="mt-6"><CajaError error={error} /></div>}
      {data && data.opciones.length === 0 && (
        <div className="mt-8">
          <Vacio titulo="No quedan habitaciones para esas fechas">
            Probá correr la estadía uno o dos días, o buscar con menos huéspedes.
          </Vacio>
        </div>
      )}
      <div className="mt-8 space-y-6">
        {data?.opciones.map((o) => <Opcion key={o.tipo.id} opcion={o} busqueda={data} />)}
      </div>
    </div>
  )
}

function Opcion({ opcion, busqueda }: { opcion: OpcionDisponible; busqueda: ResultadoBusqueda }) {
  const { tipo, cotizacion: c, habitacionesLibres } = opcion
  const { usuario, esPersonal } = useAuth()
  const { data: hotel } = useHotel()
  const navegar = useNavigate()
  const avisar = useAvisos()
  const [observaciones, setObservaciones] = useState('')
  const [abierta, setAbierta] = useState(false)

  const reservar = useMutation({
    mutationFn: () =>
      api<Reserva>('/reservas', {
        method: 'POST',
        json: { tipoId: tipo.id, checkIn: busqueda.checkIn, checkOut: busqueda.checkOut, huespedes: busqueda.huespedes, observaciones: observaciones || null },
      }),
    onSuccess: (r) => navegar(`/pagar/${r.codigo}`),
    onError: (e: Error) => avisar(e.message, 'error'),
  })

  function alReservar() {
    if (!usuario) {
      const volver = `/buscar?checkIn=${busqueda.checkIn}&checkOut=${busqueda.checkOut}&huespedes=${busqueda.huespedes}`
      navegar(`/ingresar?volver=${encodeURIComponent(volver)}&motivo=reservar`)
      return
    }
    if (!abierta) return setAbierta(true)
    reservar.mutate()
  }

  return (
    <article className="grid overflow-hidden rounded-2xl bg-white ring-1 ring-neblina-200 md:grid-cols-[280px_1fr]">
      <PortadaHabitacion nombre={tipo.nombre} imagenUrl={tipo.imagenUrl} className="aspect-[16/10] md:aspect-auto md:h-full" />
      <div className="grid gap-6 p-6 lg:grid-cols-[1fr_260px]">
        <div>
          <div className="flex flex-wrap items-baseline justify-between gap-2">
            <h2 className="text-3xl">{tipo.nombre}</h2>
            <span className={`text-sm ${habitacionesLibres <= 2 ? 'font-semibold text-alerta' : 'text-pizarra'}`}>
              {habitacionesLibres === 1 ? 'Queda 1 habitación' : `Quedan ${habitacionesLibres} habitaciones`}
            </span>
          </div>
          <p className="mt-1 text-sm text-pizarra">Hasta {personas(tipo.capacidad)} · {tipo.servicios.slice(0, 3).join(', ')}</p>
          <div className="mt-6">
            <TiraDeNoches noches={c.detalle} compacta={c.detalle.length > 21} />
          </div>
        </div>
        <div className="flex flex-col justify-between gap-4 border-t border-neblina-200 pt-5 lg:border-l lg:border-t-0 lg:pl-6 lg:pt-0">
          <dl className="space-y-1.5 text-sm">
            <div className="flex justify-between"><dt className="text-pizarra">{noches(c.noches)}</dt><dd className="tabular-nums">{plata(c.subtotal)}</dd></div>
            {c.descuento > 0 && (
              <div className="flex justify-between text-exito"><dt>{c.motivoDescuento}</dt><dd className="tabular-nums">−{plata(c.descuento)}</dd></div>
            )}
            <div className="flex justify-between border-t border-neblina-200 pt-2 text-base"><dt>Total</dt><dd className="cifra text-2xl tabular-nums">{plata(c.total)}</dd></div>
            <div className="flex justify-between text-pizarra"><dt>Seña para confirmar ({hotel?.porcentajeSena ?? 30}%)</dt><dd className="tabular-nums">{plata(c.sena)}</dd></div>
          </dl>
          {abierta && (
            <label className="block">
              <span className="etiqueta">Pedidos especiales (opcional)</span>
              <textarea className="campo min-h-[70px]" maxLength={500} value={observaciones} onChange={(e) => setObservaciones(e.target.value)} placeholder="Cuna, llegada tarde, piso alto…" />
            </label>
          )}
          {esPersonal ? (
            <p className="text-sm text-pizarra">Con una cuenta del hotel, las reservas se cargan desde el panel.</p>
          ) : (
            <button className="boton-reservar w-full" onClick={alReservar} disabled={reservar.isPending}>
              {reservar.isPending ? 'Guardando tu habitación…' : abierta ? 'Continuar al pago' : 'Reservar'}
            </button>
          )}
        </div>
      </div>
    </article>
  )
}
