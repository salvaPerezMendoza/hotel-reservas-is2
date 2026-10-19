import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { ConceptoPago, Habitacion, MedioPago, Reserva } from '@/api/tipos'
import { Encabezado } from '@/componentes/LayoutPanel'
import { Cargando, CajaError } from '@/componentes/Estados'
import { InsigniaReserva } from '@/componentes/InsigniaEstado'
import { Modal } from '@/componentes/Modal'
import { useAvisos } from '@/componentes/Avisos'
import { fechaTexto, hoyIso, instanteTexto, nombreConcepto, nombreMedio, noches, personas, plata } from '@/util/formato'

function useActualizar(codigo: string) {
  const cliente = useQueryClient()
  return (r: Reserva) => {
    cliente.setQueryData(['panel-reserva', codigo], r)
    for (const k of ['tablero', 'llegadas', 'salidas', 'recepcion-reservas', 'habitaciones-recepcion']) cliente.invalidateQueries({ queryKey: [k] })
  }
}

export default function DetalleReservaPanel() {
  const { codigo = '' } = useParams()
  const { data: r, isLoading, error } = useQuery({ queryKey: ['panel-reserva', codigo], queryFn: () => api<Reserva>(`/reservas/${codigo}`) })
  const actualizar = useActualizar(codigo)
  const avisar = useAvisos()
  const [modal, setModal] = useState<'pago' | 'consumo' | 'checkin' | null>(null)

  const checkOut = useMutation({
    mutationFn: () => api<Reserva>(`/recepcion/reservas/${codigo}/check-out`, { method: 'POST' }),
    onSuccess: (x) => { actualizar(x); avisar(`Check-out hecho. La habitación ${x.habitacionNumero} pasó a limpieza.`) },
    onError: (e: Error) => avisar(e.message, 'error'),
  })

  if (isLoading) return <Cargando />
  if (error || !r) return <CajaError error={error} />
  const hoy = hoyIso()
  const puedeCheckIn = r.estado === 'CONFIRMADA' && r.checkIn <= hoy
  const puedeCheckOut = r.estado === 'EN_CURSO'
  const abierta = ['PENDIENTE_PAGO', 'CONFIRMADA', 'EN_CURSO'].includes(r.estado)

  return (
    <>
      <Link to="/panel/reservas" className="boton-texto text-sm">Reservas</Link>
      <Encabezado titulo={`${r.huesped.nombre} ${r.huesped.apellido}`} bajada={`${r.codigo} · ${r.canal === 'RECEPCION' ? 'cargada por recepción' : 'reserva online'} · creada ${instanteTexto(r.creadaEn)}`}>
        {puedeCheckIn && <button className="boton-primario" onClick={() => setModal('checkin')}>Hacer check-in</button>}
        {puedeCheckOut && <button className="boton-primario" onClick={() => checkOut.mutate()} disabled={checkOut.isPending}>Hacer check-out</button>}
        {abierta && r.saldoPendiente > 0 && <button className="boton-secundario" onClick={() => setModal('pago')}>Registrar pago</button>}
        {r.estado === 'EN_CURSO' && <button className="boton-secundario" onClick={() => setModal('consumo')}>Agregar consumo</button>}
      </Encabezado>

      <div className="grid gap-6 lg:grid-cols-[1.2fr_1fr]">
        <section className="panel">
          <div className="flex items-center justify-between"><h2 className="text-xl">Estadía</h2><InsigniaReserva estado={r.estado} /></div>
          <dl className="mt-4 grid grid-cols-2 gap-x-6 gap-y-4 text-sm">
            <div><dt className="text-pizarra">Habitación</dt><dd className="font-medium">{r.habitacionNumero} · {r.tipoNombre}</dd></div>
            <div><dt className="text-pizarra">Huéspedes</dt><dd className="font-medium">{personas(r.cantidadHuespedes)}</dd></div>
            <div><dt className="text-pizarra">Ingreso</dt><dd className="font-medium">{fechaTexto(r.checkIn)}</dd>{r.checkInReal && <dd className="text-pizarra">Llegó {instanteTexto(r.checkInReal)}</dd>}</div>
            <div><dt className="text-pizarra">Egreso</dt><dd className="font-medium">{fechaTexto(r.checkOut)}</dd>{r.checkOutReal && <dd className="text-pizarra">Se fue {instanteTexto(r.checkOutReal)}</dd>}</div>
            <div><dt className="text-pizarra">Duración</dt><dd className="font-medium">{noches(r.noches)}</dd></div>
            {r.retencionHasta && r.estado === 'PENDIENTE_PAGO' && <div><dt className="text-pizarra">Retenida hasta</dt><dd className="font-medium">{instanteTexto(r.retencionHasta)}</dd></div>}
          </dl>
          {r.observaciones && <p className="mt-5 rounded-xl bg-neblina p-3 text-sm"><span className="text-pizarra">Pedido del huésped: </span>{r.observaciones}</p>}
          <h3 className="mt-6 text-lg">Contacto</h3>
          <p className="text-sm">{r.huesped.email}{r.huesped.telefono && ` · ${r.huesped.telefono}`}{r.huesped.documento && ` · DNI ${r.huesped.documento}`}</p>
        </section>

        <section className="panel">
          <h2 className="text-xl">Cuenta</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between"><dt className="text-pizarra">Alojamiento</dt><dd className="tabular-nums">{plata(r.precioTotal)}</dd></div>
            <div className="flex justify-between"><dt className="text-pizarra">Consumos</dt><dd className="tabular-nums">{plata(r.totalConsumos)}</dd></div>
            <div className="flex justify-between"><dt className="text-pizarra">Pagado</dt><dd className="tabular-nums">−{plata(r.totalPagado)}</dd></div>
            <div className="flex justify-between border-t border-neblina-200 pt-2 text-base font-semibold"><dt>Saldo</dt><dd className="tabular-nums">{plata(r.saldoPendiente)}</dd></div>
          </dl>
          {r.consumos.length > 0 && (
            <>
              <h3 className="mt-6 text-lg">Consumos</h3>
              <ul className="mt-2 space-y-1.5 text-sm">
                {r.consumos.map((c) => <li key={c.id} className="flex justify-between"><span>{c.descripcion} <span className="text-pizarra">· {instanteTexto(c.fecha)}</span></span><span className="tabular-nums">{plata(c.monto)}</span></li>)}
              </ul>
            </>
          )}
          <h3 className="mt-6 text-lg">Pagos</h3>
          {r.pagos.length === 0 ? <p className="mt-2 text-sm text-pizarra">Sin pagos.</p> : (
            <ul className="mt-2 space-y-1.5 text-sm">
              {r.pagos.map((p) => (
                <li key={p.id} className="flex justify-between gap-3">
                  <span>{nombreConcepto[p.concepto]} · {nombreMedio[p.medio]}{p.estado === 'RECHAZADO' && <span className="text-alerta"> · rechazado</span>}<span className="block text-xs text-pizarra">{instanteTexto(p.fecha)} · {p.referencia}</span></span>
                  <span className={`tabular-nums ${p.estado === 'RECHAZADO' ? 'text-pizarra line-through' : ''}`}>{p.concepto === 'REINTEGRO' ? '−' : ''}{plata(p.monto)}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {modal === 'pago' && <ModalPago reserva={r} alCerrar={() => setModal(null)} />}
      {modal === 'consumo' && <ModalConsumo reserva={r} alCerrar={() => setModal(null)} />}
      {modal === 'checkin' && <ModalCheckIn reserva={r} alCerrar={() => setModal(null)} />}
    </>
  )
}

function ModalPago({ reserva, alCerrar }: { reserva: Reserva; alCerrar: () => void }) {
  const actualizar = useActualizar(reserva.codigo)
  const avisar = useAvisos()
  const [monto, setMonto] = useState(String(reserva.saldoPendiente))
  const [medio, setMedio] = useState<MedioPago>('EFECTIVO')
  const concepto: ConceptoPago = reserva.totalPagado === 0 ? (Number(monto) >= reserva.totalACobrar ? 'TOTAL' : 'SENA') : 'SALDO'
  const pagar = useMutation({
    mutationFn: () => api<Reserva>(`/recepcion/reservas/${reserva.codigo}/pagos`, { method: 'POST', json: { monto: Number(monto), medio, concepto } }),
    onSuccess: (r) => { actualizar(r); avisar(`Pago de ${plata(Number(monto))} registrado.`); alCerrar() },
  })
  const enviar = (e: FormEvent) => { e.preventDefault(); pagar.mutate() }
  return (
    <Modal titulo="Registrar pago" abierto alCerrar={alCerrar}>
      <form onSubmit={enviar} className="space-y-4">
        <p className="text-sm text-pizarra">Saldo pendiente: <strong className="text-noche">{plata(reserva.saldoPendiente)}</strong>{reserva.totalPagado === 0 && reserva.senaMinima ? ` · seña mínima ${plata(reserva.senaMinima)}` : ''}</p>
        <label className="block"><span className="etiqueta">Monto</span><input type="number" min="1" step="1" max={reserva.saldoPendiente} className="campo tabular-nums" value={monto} onChange={(e) => setMonto(e.target.value)} required /></label>
        <fieldset>
          <legend className="etiqueta">Medio de pago</legend>
          <div className="flex flex-wrap gap-2">
            {(['EFECTIVO', 'TARJETA', 'TRANSFERENCIA'] as MedioPago[]).map((m) => (
              <label key={m} className={`cursor-pointer rounded-full px-4 py-2 text-sm font-medium ring-1 ${medio === m ? 'bg-noche text-white ring-noche' : 'ring-neblina-300 hover:ring-noche-600'}`}>
                <input type="radio" className="sr-only" checked={medio === m} onChange={() => setMedio(m)} />{nombreMedio[m]}
              </label>
            ))}
          </div>
        </fieldset>
        <p className="text-sm text-pizarra">Se registra como {nombreConcepto[concepto].toLowerCase()}.</p>
        {pagar.error && <CajaError error={pagar.error} />}
        <div className="flex justify-end gap-3"><button type="button" className="boton-secundario" onClick={alCerrar}>Volver</button><button className="boton-primario" disabled={pagar.isPending}>Registrar pago</button></div>
      </form>
    </Modal>
  )
}

function ModalConsumo({ reserva, alCerrar }: { reserva: Reserva; alCerrar: () => void }) {
  const actualizar = useActualizar(reserva.codigo)
  const avisar = useAvisos()
  const [descripcion, setDescripcion] = useState('')
  const [monto, setMonto] = useState('')
  const agregar = useMutation({
    mutationFn: () => api<Reserva>(`/recepcion/reservas/${reserva.codigo}/consumos`, { method: 'POST', json: { descripcion, monto: Number(monto) } }),
    onSuccess: (r) => { actualizar(r); avisar('Consumo agregado a la cuenta.'); alCerrar() },
  })
  const enviar = (e: FormEvent) => { e.preventDefault(); agregar.mutate() }
  return (
    <Modal titulo="Agregar consumo" abierto alCerrar={alCerrar}>
      <form onSubmit={enviar} className="space-y-4">
        <div className="flex flex-wrap gap-2">
          {[['Frigobar', 8000], ['Room service', 18000], ['Lavandería', 12000], ['Estacionamiento', 9000]].map(([d, m]) => (
            <button type="button" key={d} className="rounded-full bg-neblina px-3 py-1.5 text-sm hover:bg-neblina-200" onClick={() => { setDescripcion(String(d)); setMonto(String(m)) }}>{d}</button>
          ))}
        </div>
        <label className="block"><span className="etiqueta">Descripción</span><input className="campo" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} required maxLength={200} /></label>
        <label className="block"><span className="etiqueta">Monto</span><input type="number" min="1" className="campo tabular-nums" value={monto} onChange={(e) => setMonto(e.target.value)} required /></label>
        {agregar.error && <CajaError error={agregar.error} />}
        <div className="flex justify-end gap-3"><button type="button" className="boton-secundario" onClick={alCerrar}>Volver</button><button className="boton-primario" disabled={agregar.isPending}>Agregar consumo</button></div>
      </form>
    </Modal>
  )
}

function ModalCheckIn({ reserva, alCerrar }: { reserva: Reserva; alCerrar: () => void }) {
  const actualizar = useActualizar(reserva.codigo)
  const avisar = useAvisos()
  const { data: libres, isLoading } = useQuery({
    queryKey: ['checkin-habitaciones', reserva.codigo],
    queryFn: () => api<Habitacion[]>(`/recepcion/reservas/${reserva.codigo}/habitaciones-disponibles`),
    gcTime: 0,
  })
  const [habitacionId, setHabitacionId] = useState(reserva.habitacionId)
  const asignadaLista = libres?.some((h) => h.id === reserva.habitacionId) ?? true
  // Si la asignada todavía está ocupada o en limpieza, se propone la primera lista del mismo tipo.
  useEffect(() => {
    if (libres && !asignadaLista && libres.length > 0) setHabitacionId(libres[0].id)
  }, [libres, asignadaLista])
  const checkIn = useMutation({
    mutationFn: () => api<Reserva>(`/recepcion/reservas/${reserva.codigo}/check-in`, { method: 'POST', json: { habitacionId } }),
    onSuccess: (r) => { actualizar(r); avisar(`Check-in hecho en la habitación ${r.habitacionNumero}.`); alCerrar() },
  })
  return (
    <Modal titulo="Check-in" abierto alCerrar={alCerrar}>
      <p className="text-sm text-pizarra">Confirmá la habitación que se le entrega a {reserva.huesped.nombre}.</p>
      {!asignadaLista && <p className="mt-3 rounded-xl bg-alba-50 px-3 py-2 text-sm text-noche">La {reserva.habitacionNumero} todavía no está lista (sigue ocupada o en limpieza). Te proponemos otra {reserva.tipoNombre} libre.</p>}
      {isLoading ? <Cargando /> : (
        <div className="mt-4 grid grid-cols-3 gap-2 sm:grid-cols-4">
          {libres?.map((h) => (
            <button key={h.id} type="button" onClick={() => setHabitacionId(h.id)}
              className={`rounded-xl px-3 py-3 text-center ring-1 ${habitacionId === h.id ? 'bg-noche text-white ring-noche' : 'bg-white ring-neblina-300 hover:ring-noche-600'}`}>
              <span className="block cifra text-xl">{h.numero}</span>
              <span className="text-xs opacity-75">{h.id === reserva.habitacionId ? 'asignada' : `piso ${h.piso}`}</span>
            </button>
          ))}
        </div>
      )}
      {libres && libres.length === 0 && <p className="mt-3 text-sm text-alerta">No hay habitaciones del tipo listas para entregar. Revisá el estado de limpieza.</p>}
      {reserva.saldoPendiente > 0 && <p className="mt-4 text-sm text-pizarra">Saldo pendiente: {plata(reserva.saldoPendiente)}. Se puede cobrar ahora o al check-out.</p>}
      {checkIn.error && <div className="mt-4"><CajaError error={checkIn.error} /></div>}
      <div className="mt-6 flex justify-end gap-3"><button className="boton-secundario" onClick={alCerrar}>Volver</button><button className="boton-primario" onClick={() => checkIn.mutate()} disabled={checkIn.isPending || !libres?.some((h) => h.id === habitacionId)}>Confirmar check-in</button></div>
    </Modal>
  )
}
