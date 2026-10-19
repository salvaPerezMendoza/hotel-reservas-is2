import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { HuespedResumen, Reserva, ResultadoBusqueda } from '@/api/tipos'
import { Encabezado } from '@/componentes/LayoutPanel'
import { CajaError, Cargando } from '@/componentes/Estados'
import { TiraDeNoches } from '@/componentes/TiraDeNoches'
import { useAvisos } from '@/componentes/Avisos'
import { hoyIso, noches, plata, sumarDias } from '@/util/formato'

const vacio = { nombre: '', apellido: '', email: '', telefono: '', documento: '' }

/** Reserva telefónica o presencial (RF de recepción): busca o da de alta al huésped. */
export default function NuevaReserva() {
  const navegar = useNavigate()
  const avisar = useAvisos()
  const hoy = hoyIso()
  const [checkIn, setCheckIn] = useState(hoy)
  const [checkOut, setCheckOut] = useState(sumarDias(hoy, 2))
  const [huespedes, setHuespedes] = useState(2)
  const [tipoId, setTipoId] = useState<number | null>(null)
  const [q, setQ] = useState('')
  const [huesped, setHuesped] = useState(vacio)
  const [observaciones, setObservaciones] = useState('')

  const busqueda = useQuery({
    queryKey: ['disponibilidad', checkIn, checkOut, huespedes],
    queryFn: () => api<ResultadoBusqueda>(`/disponibilidad?checkIn=${checkIn}&checkOut=${checkOut}&huespedes=${huespedes}`),
    enabled: checkOut > checkIn,
  })
  const coincidencias = useQuery({
    queryKey: ['huespedes', q],
    queryFn: () => api<HuespedResumen[]>(`/recepcion/huespedes?q=${encodeURIComponent(q)}`),
    enabled: q.trim().length >= 2,
  })
  const crear = useMutation({
    mutationFn: () => api<Reserva>('/recepcion/reservas', {
      method: 'POST',
      json: { huesped: { ...huesped, telefono: huesped.telefono || null, documento: huesped.documento || null }, tipoId, checkIn, checkOut, huespedes, observaciones: observaciones || null },
    }),
    onSuccess: (r) => { avisar(`Reserva ${r.codigo} creada.`); navegar(`/panel/reservas/${r.codigo}`) },
  })

  const set = (k: keyof typeof vacio) => (e: React.ChangeEvent<HTMLInputElement>) => setHuesped({ ...huesped, [k]: e.target.value })
  const elegir = (h: HuespedResumen) => { setHuesped({ nombre: h.nombre, apellido: h.apellido, email: h.email, telefono: h.telefono ?? '', documento: h.documento ?? '' }); setQ('') }
  const enviar = (e: FormEvent) => { e.preventDefault(); crear.mutate() }
  const opcion = busqueda.data?.opciones.find((o) => o.tipo.id === tipoId)

  return (
    <>
      <Encabezado titulo="Nueva reserva" bajada="Para reservas por teléfono o en el mostrador. Queda confirmada; el pago se registra después." />
      <form onSubmit={enviar} className="grid gap-6 lg:grid-cols-[1.3fr_1fr]">
        <section className="panel">
          <h2 className="text-xl">Estadía</h2>
          <div className="mt-4 grid grid-cols-3 gap-3">
            <label><span className="etiqueta">Ingreso</span><input type="date" className="campo" min={hoy} value={checkIn} onChange={(e) => { setCheckIn(e.target.value); if (checkOut <= e.target.value) setCheckOut(sumarDias(e.target.value, 1)) }} required /></label>
            <label><span className="etiqueta">Egreso</span><input type="date" className="campo" min={sumarDias(checkIn, 1)} value={checkOut} onChange={(e) => setCheckOut(e.target.value)} required /></label>
            <label><span className="etiqueta">Huéspedes</span><select className="campo" value={huespedes} onChange={(e) => setHuespedes(Number(e.target.value))}>{[1, 2, 3, 4].map((n) => <option key={n}>{n}</option>)}</select></label>
          </div>
          <div className="mt-5 space-y-2">
            {busqueda.isLoading && <Cargando texto="Buscando disponibilidad…" />}
            {busqueda.error && <CajaError error={busqueda.error} />}
            {busqueda.data?.opciones.length === 0 && <p className="text-sm text-alerta">No hay habitaciones libres para esas fechas y huéspedes.</p>}
            {busqueda.data?.opciones.map((o) => (
              <label key={o.tipo.id} className={`flex cursor-pointer items-center justify-between gap-4 rounded-xl p-3 ring-1 ${tipoId === o.tipo.id ? 'bg-neblina ring-noche' : 'ring-neblina-300 hover:ring-noche-600'}`}>
                <input type="radio" name="tipo" className="sr-only" checked={tipoId === o.tipo.id} onChange={() => setTipoId(o.tipo.id)} />
                <span><span className="font-semibold">{o.tipo.nombre}</span><span className="block text-xs text-pizarra">{o.habitacionesLibres === 1 ? "1 libre" : `${o.habitacionesLibres} libres`} · hasta {o.tipo.capacidad}</span></span>
                <span className="text-right tabular-nums"><span className="font-semibold">{plata(o.cotizacion.total)}</span><span className="block text-xs text-pizarra">{noches(o.cotizacion.noches)}</span></span>
              </label>
            ))}
          </div>
          {opcion && <div className="mt-5"><TiraDeNoches noches={opcion.cotizacion.detalle} compacta={opcion.cotizacion.detalle.length > 21} /></div>}
        </section>

        <section className="panel">
          <h2 className="text-xl">Huésped</h2>
          <label className="mt-4 block">
            <span className="etiqueta">Buscar un huésped existente</span>
            <input className="campo" placeholder="Nombre, email o DNI" value={q} onChange={(e) => setQ(e.target.value)} />
          </label>
          {coincidencias.data && q.trim().length >= 2 && (
            <ul className="mt-2 divide-y divide-neblina-200 rounded-xl ring-1 ring-neblina-300">
              {coincidencias.data.length === 0 && <li className="px-3 py-2 text-sm text-pizarra">Sin coincidencias: completá los datos abajo para darlo de alta.</li>}
              {coincidencias.data.slice(0, 6).map((h) => (
                <li key={h.id}><button type="button" className="w-full px-3 py-2 text-left text-sm hover:bg-neblina" onClick={() => elegir(h)}>{h.nombre} {h.apellido} <span className="text-pizarra">· {h.email}</span></button></li>
              ))}
            </ul>
          )}
          <div className="mt-4 grid grid-cols-2 gap-3">
            <label><span className="etiqueta">Nombre</span><input className="campo" value={huesped.nombre} onChange={set('nombre')} required /></label>
            <label><span className="etiqueta">Apellido</span><input className="campo" value={huesped.apellido} onChange={set('apellido')} required /></label>
            <label className="col-span-2"><span className="etiqueta">Email</span><input type="email" className="campo" value={huesped.email} onChange={set('email')} required /></label>
            <label><span className="etiqueta">Teléfono</span><input className="campo" value={huesped.telefono} onChange={set('telefono')} /></label>
            <label><span className="etiqueta">DNI</span><input className="campo" value={huesped.documento} onChange={set('documento')} /></label>
            <label className="col-span-2"><span className="etiqueta">Observaciones</span><input className="campo" value={observaciones} onChange={(e) => setObservaciones(e.target.value)} maxLength={500} /></label>
          </div>
          {crear.error && <div className="mt-4"><CajaError error={crear.error} /></div>}
          <button className="boton-primario mt-5 w-full" disabled={!tipoId || !opcion || crear.isPending}>
            {opcion ? `Crear reserva · ${plata(opcion.cotizacion.total)}` : 'Elegí una habitación'}
          </button>
        </section>
      </form>
    </>
  )
}
