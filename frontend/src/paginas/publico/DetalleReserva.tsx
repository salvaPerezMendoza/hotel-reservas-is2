import { useState, type FormEvent } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { Reserva, SimulacionCancelacion } from '@/api/tipos'
import { Cargando, CajaError } from '@/componentes/Estados'
import { InsigniaReserva } from '@/componentes/InsigniaEstado'
import { Modal } from '@/componentes/Modal'
import { PortadaHabitacion } from '@/componentes/PortadaHabitacion'
import { useAvisos } from '@/componentes/Avisos'
import { useHotel } from '@/componentes/LayoutPublico'
import { fechaTexto, hoyIso, instanteTexto, nombreConcepto, nombreMedio, noches, personas, plata, sumarDias } from '@/util/formato'
import { useTipos } from './Inicio'

export default function DetalleReserva() {
  const { codigo = '' } = useParams()
  const [params] = useSearchParams()
  const { data: hotel } = useHotel()
  const { data: r, isLoading, error } = useQuery({ queryKey: ['reserva', codigo], queryFn: () => api<Reserva>(`/reservas/${codigo}`) })
  const [modificando, setModificando] = useState(false)
  const [cancelando, setCancelando] = useState(false)

  if (isLoading) return <div className="mx-auto max-w-4xl px-4"><Cargando /></div>
  if (error || !r) return <div className="mx-auto max-w-4xl px-4 py-10"><CajaError error={error} /></div>
  const recienConfirmada = params.get('confirmada') === '1' && r.estado === 'CONFIRMADA'

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <Link to="/mis-reservas" className="boton-texto text-sm">Mis reservas</Link>
      {recienConfirmada && (
        <div className="cielo-aurora mt-4 rounded-2xl p-6 text-white sm:p-8">
          <p className="font-titulo text-3xl">Listo, te esperamos el {fechaTexto(r.checkIn)}.</p>
          <p className="mt-2 text-neblina-200">Te mandamos la confirmación a {r.huesped.email}. El check-in es desde las {hotel?.horaCheckIn ?? '14:00'}.</p>
        </div>
      )}

      <div className="mt-6 overflow-hidden rounded-2xl bg-white ring-1 ring-neblina-200">
        <div className="grid md:grid-cols-[1fr_1.3fr]">
          <PortadaHabitacion nombre={r.tipoNombre} imagenUrl={r.tipoImagenUrl} className="aspect-[16/10] md:aspect-auto md:h-full" />
          <div className="p-6 sm:p-8">
            <div className="flex flex-wrap items-center gap-3">
              <InsigniaReserva estado={r.estado} />
              <span className="text-sm text-pizarra">Código {r.codigo}</span>
            </div>
            <h1 className="mt-3 text-4xl">{r.tipoNombre}</h1>
            <p className="text-pizarra">Habitación {r.habitacionNumero} · {personas(r.cantidadHuespedes)}</p>
            <dl className="mt-6 grid grid-cols-2 gap-4">
              <div><dt className="text-sm text-pizarra">Ingreso</dt><dd className="font-medium">{fechaTexto(r.checkIn)}</dd><dd className="text-sm text-pizarra">desde las {hotel?.horaCheckIn}</dd></div>
              <div><dt className="text-sm text-pizarra">Egreso</dt><dd className="font-medium">{fechaTexto(r.checkOut)}</dd><dd className="text-sm text-pizarra">hasta las {hotel?.horaCheckOut}</dd></div>
            </dl>
            <div className="mt-6 flex flex-wrap gap-3">
              {r.estado === 'PENDIENTE_PAGO' && <Link to={`/pagar/${r.codigo}`} className="boton-reservar">Completar el pago</Link>}
              {r.puedeModificarse && <button className="boton-secundario" onClick={() => setModificando(true)}>Cambiar fechas o habitación</button>}
              {r.puedeCancelarse && <button className="boton-texto px-2 text-alerta" onClick={() => setCancelando(true)}>Cancelar reserva</button>}
            </div>
          </div>
        </div>
      </div>

      <section className="mt-6 grid gap-6 md:grid-cols-2">
        <div className="panel">
          <h2 className="text-2xl">Cuenta</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between"><dt className="text-pizarra">Alojamiento ({noches(r.noches)})</dt><dd className="tabular-nums">{plata(r.precioTotal)}</dd></div>
            {r.totalConsumos > 0 && <div className="flex justify-between"><dt className="text-pizarra">Consumos</dt><dd className="tabular-nums">{plata(r.totalConsumos)}</dd></div>}
            <div className="flex justify-between"><dt className="text-pizarra">Pagado</dt><dd className="tabular-nums">−{plata(r.totalPagado)}</dd></div>
            <div className="flex justify-between border-t border-neblina-200 pt-2 text-base"><dt>Saldo a pagar en el hotel</dt><dd className="cifra text-xl tabular-nums">{plata(r.saldoPendiente)}</dd></div>
          </dl>
          {r.reembolso != null && r.estado === 'CANCELADA' && <p className="mt-4 rounded-xl bg-exito-50 px-3 py-2 text-sm text-exito">Reintegro de {plata(r.reembolso)} al medio de pago original.</p>}
        </div>
        <div className="panel">
          <h2 className="text-2xl">Movimientos</h2>
          {r.pagos.length === 0 ? (
            <p className="mt-4 text-sm text-pizarra">Todavía no hay pagos.</p>
          ) : (
            <ul className="mt-4 space-y-3 text-sm">
              {r.pagos.map((p) => (
                <li key={p.id} className="flex justify-between gap-4">
                  <div>
                    <p className="font-medium">{nombreConcepto[p.concepto]} · {nombreMedio[p.medio]}{p.estado === 'RECHAZADO' && <span className="text-alerta"> · rechazado</span>}</p>
                    <p className="text-pizarra">{instanteTexto(p.fecha)} · {p.referencia}</p>
                  </div>
                  <span className={`tabular-nums ${p.estado === 'RECHAZADO' ? 'text-pizarra line-through' : ''}`}>{p.concepto === 'REINTEGRO' ? '−' : ''}{plata(p.monto)}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <ModalModificar reserva={r} abierto={modificando} alCerrar={() => setModificando(false)} />
      <ModalCancelar reserva={r} abierto={cancelando} alCerrar={() => setCancelando(false)} />
    </div>
  )
}

function ModalModificar({ reserva, abierto, alCerrar }: { reserva: Reserva; abierto: boolean; alCerrar: () => void }) {
  const { data: tipos } = useTipos()
  const cliente = useQueryClient()
  const avisar = useAvisos()
  const [tipoId, setTipoId] = useState(reserva.tipoId)
  const [checkIn, setCheckIn] = useState(reserva.checkIn)
  const [checkOut, setCheckOut] = useState(reserva.checkOut)
  const [huespedes, setHuespedes] = useState(reserva.cantidadHuespedes)

  const modificar = useMutation({
    mutationFn: () => api<Reserva>(`/reservas/${reserva.codigo}`, { method: 'PUT', json: { tipoId, checkIn, checkOut, huespedes } }),
    onSuccess: (r) => {
      cliente.setQueryData(['reserva', r.codigo], r)
      cliente.invalidateQueries({ queryKey: ['mis-reservas'] })
      avisar('Reserva modificada. Te mandamos el detalle por mail.')
      alCerrar()
    },
  })

  function enviar(e: FormEvent) {
    e.preventDefault()
    modificar.mutate()
  }

  const tipo = tipos?.find((t) => t.id === tipoId)
  return (
    <Modal titulo="Cambiar la reserva" abierto={abierto} alCerrar={alCerrar}>
      <form onSubmit={enviar} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <label><span className="etiqueta">Ingreso</span><input type="date" className="campo" min={hoyIso()} value={checkIn} onChange={(e) => { setCheckIn(e.target.value); if (checkOut <= e.target.value) setCheckOut(sumarDias(e.target.value, 1)) }} required /></label>
          <label><span className="etiqueta">Egreso</span><input type="date" className="campo" min={sumarDias(checkIn, 1)} value={checkOut} onChange={(e) => setCheckOut(e.target.value)} required /></label>
        </div>
        <label className="block"><span className="etiqueta">Habitación</span>
          <select className="campo" value={tipoId} onChange={(e) => setTipoId(Number(e.target.value))}>
            {tipos?.map((t) => <option key={t.id} value={t.id}>{t.nombre} (hasta {t.capacidad})</option>)}
          </select>
        </label>
        <label className="block"><span className="etiqueta">Huéspedes</span>
          <select className="campo" value={huespedes} onChange={(e) => setHuespedes(Number(e.target.value))}>
            {Array.from({ length: tipo?.capacidad ?? 4 }, (_, i) => i + 1).map((n) => <option key={n} value={n}>{personas(n)}</option>)}
          </select>
        </label>
        <p className="text-sm text-pizarra">El precio se recalcula con las tarifas de las nuevas noches. Lo que ya pagaste queda a cuenta.</p>
        {modificar.error && <CajaError error={modificar.error} />}
        <div className="flex justify-end gap-3 pt-2">
          <button type="button" className="boton-secundario" onClick={alCerrar}>Volver</button>
          <button type="submit" className="boton-primario" disabled={modificar.isPending}>{modificar.isPending ? 'Guardando…' : 'Guardar cambios'}</button>
        </div>
      </form>
    </Modal>
  )
}

function ModalCancelar({ reserva, abierto, alCerrar }: { reserva: Reserva; abierto: boolean; alCerrar: () => void }) {
  const cliente = useQueryClient()
  const avisar = useAvisos()
  const { data: sim, isLoading, error } = useQuery({
    queryKey: ['cancelacion', reserva.codigo],
    queryFn: () => api<SimulacionCancelacion>(`/reservas/${reserva.codigo}/cancelacion`),
    enabled: abierto,
    gcTime: 0,
  })
  const cancelar = useMutation({
    mutationFn: () => api<Reserva>(`/reservas/${reserva.codigo}/cancelar`, { method: 'POST' }),
    onSuccess: (r) => {
      cliente.setQueryData(['reserva', r.codigo], r)
      cliente.invalidateQueries({ queryKey: ['mis-reservas'] })
      avisar('Reserva cancelada.')
      alCerrar()
    },
  })
  return (
    <Modal titulo="Cancelar la reserva" abierto={abierto} alCerrar={alCerrar}>
      {isLoading && <Cargando texto="Calculando el reintegro…" />}
      {error && <CajaError error={error} />}
      {sim && (
        <>
          <p className="text-pizarra">{sim.explicacion}</p>
          <dl className="mt-5 grid grid-cols-2 gap-4 rounded-xl bg-neblina p-4">
            <div><dt className="text-sm text-pizarra">Pagaste</dt><dd className="cifra text-2xl tabular-nums">{plata(sim.totalPagado)}</dd></div>
            <div><dt className="text-sm text-pizarra">Te devolvemos ({sim.porcentajeReintegro}%)</dt><dd className="cifra text-2xl tabular-nums">{plata(sim.montoReintegro)}</dd></div>
          </dl>
          {cancelar.error && <div className="mt-4"><CajaError error={cancelar.error} /></div>}
          <div className="mt-6 flex justify-end gap-3">
            <button className="boton-secundario" onClick={alCerrar}>Mantener la reserva</button>
            <button className="boton-peligro" onClick={() => cancelar.mutate()} disabled={cancelar.isPending}>{cancelar.isPending ? 'Cancelando…' : 'Cancelar reserva'}</button>
          </div>
        </>
      )}
    </Modal>
  )
}
