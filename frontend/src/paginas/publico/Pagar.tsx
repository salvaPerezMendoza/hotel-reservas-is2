import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ErrorApi } from '@/api/cliente'
import type { ConceptoPago, Reserva, ResultadoPago } from '@/api/tipos'
import { useAuth } from '@/auth/AuthContext'
import { Cargando, CajaError } from '@/componentes/Estados'
import { formatoReloj, useTiempoRestante } from '@/componentes/CuentaRegresiva'
import { PortadaHabitacion } from '@/componentes/PortadaHabitacion'
import { fechaTexto, noches, personas, plata } from '@/util/formato'

export default function Pagar() {
  const { codigo = '' } = useParams()
  const { data: reserva, isLoading, error } = useQuery({
    queryKey: ['reserva', codigo],
    queryFn: () => api<Reserva>(`/reservas/${codigo}`),
  })
  if (isLoading) return <div className="mx-auto max-w-5xl px-4"><Cargando /></div>
  if (error || !reserva) return <div className="mx-auto max-w-5xl px-4 py-10"><CajaError error={error} /></div>
  if (reserva.estado !== 'PENDIENTE_PAGO') {
    return (
      <div className="mx-auto max-w-xl px-4 py-16 text-center">
        <h1 className="text-4xl">{reserva.estado === 'EXPIRADA' ? 'Se liberó la habitación' : 'Esta reserva ya no espera pago'}</h1>
        <p className="mt-3 text-pizarra">
          {reserva.estado === 'EXPIRADA'
            ? 'Pasó el tiempo para completar el pago y la habitación volvió a quedar disponible. Podés buscarla de nuevo.'
            : 'Podés ver su estado en Mis reservas.'}
        </p>
        <div className="mt-6 flex justify-center gap-3">
          <Link to={`/buscar?checkIn=${reserva.checkIn}&checkOut=${reserva.checkOut}&huespedes=${reserva.cantidadHuespedes}`} className="boton-primario">Buscar de nuevo</Link>
          <Link to={`/mis-reservas/${reserva.codigo}`} className="boton-secundario">Ver la reserva</Link>
        </div>
      </div>
    )
  }
  return <Checkout reserva={reserva} />
}

function Checkout({ reserva }: { reserva: Reserva }) {
  const { usuario } = useAuth()
  const navegar = useNavigate()
  const cliente = useQueryClient()
  const restante = useTiempoRestante(reserva.retencionHasta)
  const [concepto, setConcepto] = useState<ConceptoPago>('SENA')
  const [titular, setTitular] = useState(usuario ? `${usuario.nombre} ${usuario.apellido}` : '')
  const [tarjeta, setTarjeta] = useState('4242 4242 4242 4242')
  const [vence, setVence] = useState('12/29')
  const [cvv, setCvv] = useState('123')
  const [rechazar, setRechazar] = useState(false)
  const [rechazo, setRechazo] = useState<string | null>(null)

  const pagar = useMutation({
    mutationFn: () => api<ResultadoPago>(`/reservas/${reserva.codigo}/pagos`, { method: 'POST', json: { concepto, titular, simularRechazo: rechazar } }),
    onSuccess: (r) => {
      cliente.setQueryData(['reserva', reserva.codigo], r.reserva)
      cliente.invalidateQueries({ queryKey: ['mis-reservas'] })
      if (r.aprobado) navegar(`/mis-reservas/${reserva.codigo}?confirmada=1`)
      else setRechazo(r.mensaje)
    },
    onError: (e) => {
      if (e instanceof ErrorApi && e.estado === 422) cliente.invalidateQueries({ queryKey: ['reserva', reserva.codigo] })
    },
  })

  const vencida = restante === 0
  const monto = concepto === 'SENA' ? reserva.senaMinima ?? reserva.precioTotal : reserva.precioTotal

  function enviar(e: FormEvent) {
    e.preventDefault()
    setRechazo(null)
    pagar.mutate()
  }

  return (
    <div className="mx-auto grid max-w-5xl gap-8 px-4 py-10 sm:px-6 lg:grid-cols-[1fr_360px]">
      <form onSubmit={enviar} className="panel order-2 lg:order-1">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-3xl">Pagar y confirmar</h1>
          {restante !== null && (
            <p className={`rounded-full px-3 py-1 text-sm font-semibold tabular-nums ${restante < 120 ? 'bg-alerta-50 text-alerta' : 'bg-lavanda-100 text-lavanda-600'}`} aria-live="polite">
              {vencida ? 'Tiempo agotado' : `Guardamos tu habitación ${formatoReloj(restante)}`}
            </p>
          )}
        </div>

        <fieldset className="mt-6">
          <legend className="etiqueta">¿Cuánto querés pagar ahora?</legend>
          <div className="grid gap-3 sm:grid-cols-2">
            {([
              ['SENA', 'Seña', reserva.senaMinima ?? 0, 'El resto lo abonás al llegar al hotel.'],
              ['TOTAL', 'Total', reserva.precioTotal, 'No tenés que pagar nada más en el hotel.'],
            ] as const).map(([valor, nombre, importe, ayuda]) => (
              <label key={valor} className={`cursor-pointer rounded-xl border p-4 transition-colors ${concepto === valor ? 'border-noche bg-neblina' : 'border-neblina-300 hover:border-noche-600'}`}>
                <input type="radio" name="concepto" className="sr-only" checked={concepto === valor} onChange={() => setConcepto(valor)} />
                <span className="flex items-baseline justify-between">
                  <span className="font-semibold">{nombre}</span>
                  <span className="cifra text-xl tabular-nums">{plata(importe)}</span>
                </span>
                <span className="mt-1 block text-sm text-pizarra">{ayuda}</span>
              </label>
            ))}
          </div>
        </fieldset>

        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          <label className="block sm:col-span-2">
            <span className="etiqueta">Nombre en la tarjeta</span>
            <input className="campo" value={titular} onChange={(e) => setTitular(e.target.value)} required maxLength={80} autoComplete="off" />
          </label>
          <label className="block sm:col-span-2">
            <span className="etiqueta">Número de tarjeta</span>
            <input className="campo tabular-nums" value={tarjeta} onChange={(e) => setTarjeta(e.target.value)} inputMode="numeric" autoComplete="off" />
          </label>
          <label className="block">
            <span className="etiqueta">Vencimiento</span>
            <input className="campo" value={vence} onChange={(e) => setVence(e.target.value)} autoComplete="off" />
          </label>
          <label className="block">
            <span className="etiqueta">Código de seguridad</span>
            <input className="campo" value={cvv} onChange={(e) => setCvv(e.target.value)} inputMode="numeric" autoComplete="off" />
          </label>
        </div>
        <p className="mt-3 text-sm text-pizarra">Pasarela de pago simulada: no se cobra nada ni se guardan los datos de la tarjeta.</p>

        {rechazo && <div className="mt-5"><CajaError error={new Error(`${rechazo} Revisá los datos o probá con otra tarjeta.`)} /></div>}
        {pagar.error && <div className="mt-5"><CajaError error={pagar.error} /></div>}

        <button type="submit" className="boton-reservar relative mt-6 w-full overflow-hidden py-3.5 text-base" disabled={pagar.isPending || vencida}>
          {pagar.isPending && <span className="barra-pago absolute inset-y-0 left-0 w-1/3 bg-white/40" aria-hidden="true" />}
          <span className="relative">{pagar.isPending ? 'Procesando el pago…' : `Pagar ${plata(monto)}`}</span>
        </button>

        <details className="mt-6 text-sm text-pizarra">
          <summary className="cursor-pointer select-none">Opciones de demostración</summary>
          <label className="mt-3 flex items-center gap-2">
            <input type="checkbox" checked={rechazar} onChange={(e) => setRechazar(e.target.checked)} />
            Simular que el banco rechaza el pago
          </label>
        </details>
      </form>

      <aside className="order-1 lg:order-2">
        <div className="overflow-hidden rounded-2xl bg-white ring-1 ring-neblina-200">
          <PortadaHabitacion nombre={reserva.tipoNombre} imagenUrl={reserva.tipoImagenUrl} className="aspect-[16/9]" />
          <div className="p-5">
            <p className="text-sm text-pizarra">Reserva {reserva.codigo}</p>
            <h2 className="mt-1 text-2xl">{reserva.tipoNombre}</h2>
            <dl className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between"><dt className="text-pizarra">Ingreso</dt><dd>{fechaTexto(reserva.checkIn)}</dd></div>
              <div className="flex justify-between"><dt className="text-pizarra">Egreso</dt><dd>{fechaTexto(reserva.checkOut)}</dd></div>
              <div className="flex justify-between"><dt className="text-pizarra">Estadía</dt><dd>{noches(reserva.noches)} · {personas(reserva.cantidadHuespedes)}</dd></div>
              <div className="flex justify-between border-t border-neblina-200 pt-3 text-base"><dt>Total</dt><dd className="cifra text-xl tabular-nums">{plata(reserva.precioTotal)}</dd></div>
            </dl>
          </div>
        </div>
      </aside>
    </div>
  )
}
