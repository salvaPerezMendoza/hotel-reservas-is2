import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Area, AreaChart, Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api, descargar } from '@/api/cliente'
import type { ReporteIngresos, ReporteOcupacion } from '@/api/tipos'
import { Encabezado } from '@/componentes/LayoutPanel'
import { Cargando, CajaError } from '@/componentes/Estados'
import { useAvisos } from '@/componentes/Avisos'
import { aFecha, hoyIso, nombreMedio, plata, sumarDias } from '@/util/formato'

const NOCHE = '#16213A'
const LAVANDA = '#7466A8'
const GRILLA = '#E2E7EE'
const ejeX = { tick: { fontSize: 12, fill: '#5B6478' }, tickLine: false, axisLine: { stroke: GRILLA } }
const ejeY = { tick: { fontSize: 12, fill: '#5B6478' }, tickLine: false, axisLine: false, width: 56 }
const diaCorto = (iso: string) => aFecha(iso).toLocaleDateString('es-AR', { day: 'numeric', month: 'short' })
const milesCorto = (n: number) => (n === 0 ? '$0' : n >= 1_000_000 ? `$${(n / 1_000_000).toFixed(1)} M` : `$${Math.round(n / 1000)} mil`)

const rangos = [
  ['Últimos 30 días', -30, 0],
  ['Próximos 30 días', 0, 30],
  ['Últimos 90 días', -90, 0],
] as const

function Cifra({ nombre, valor, detalle }: { nombre: string; valor: string; detalle?: string }) {
  return (
    <div>
      <p className="text-sm text-pizarra">{nombre}</p>
      <p className="cifra text-3xl tabular-nums">{valor}</p>
      {detalle && <p className="text-xs text-pizarra">{detalle}</p>}
    </div>
  )
}

export default function Reportes() {
  const hoy = hoyIso()
  const avisar = useAvisos()
  const [desde, setDesde] = useState(sumarDias(hoy, -30))
  const [hasta, setHasta] = useState(hoy)
  const [bajando, setBajando] = useState(false)
  const rango = `desde=${desde}&hasta=${hasta}`
  const ocupacion = useQuery({ queryKey: ['rep-ocupacion', rango], queryFn: () => api<ReporteOcupacion>(`/admin/reportes/ocupacion?${rango}`), enabled: hasta >= desde })
  const ingresos = useQuery({ queryKey: ['rep-ingresos', rango], queryFn: () => api<ReporteIngresos>(`/admin/reportes/ingresos?${rango}`), enabled: hasta >= desde })

  async function bajarExcel() {
    setBajando(true)
    try { await descargar(`/admin/reportes/excel?${rango}`, `reporte-hotel-${desde}-${hasta}.xlsx`) }
    catch (e) { avisar((e as Error).message, 'error') }
    finally { setBajando(false) }
  }

  const o = ocupacion.data
  const i = ingresos.data
  return (
    <>
      <Encabezado titulo="Reportes" bajada="Ocupación por noche vendida e ingresos por fecha de cobro.">
        <button className="boton-secundario" onClick={bajarExcel} disabled={bajando}>{bajando ? 'Generando…' : 'Descargar Excel'}</button>
      </Encabezado>
      <div className="panel flex flex-wrap items-end gap-3">
        <label><span className="etiqueta">Desde</span><input type="date" className="campo" value={desde} onChange={(e) => setDesde(e.target.value)} /></label>
        <label><span className="etiqueta">Hasta</span><input type="date" className="campo" value={hasta} min={desde} onChange={(e) => setHasta(e.target.value)} /></label>
        <div className="flex flex-wrap gap-2">
          {rangos.map(([n, a, b]) => (
            <button key={n} className="rounded-full bg-neblina px-3 py-2 text-sm hover:bg-neblina-200" onClick={() => { setDesde(sumarDias(hoy, a)); setHasta(sumarDias(hoy, b)) }}>{n}</button>
          ))}
        </div>
      </div>

      {(ocupacion.error || ingresos.error) && <div className="mt-4"><CajaError error={ocupacion.error ?? ingresos.error} /></div>}
      {(ocupacion.isLoading || ingresos.isLoading) && <Cargando />}

      {o && (
        <section className="panel mt-6">
          <div className="flex flex-wrap items-end justify-between gap-6">
            <h2 className="text-xl">Ocupación</h2>
            <div className="flex gap-10">
              <Cifra nombre="Promedio del período" valor={`${Math.round(o.porcentaje)}%`} />
              <Cifra nombre="Noches vendidas" valor={o.nochesOcupadas.toLocaleString('es-AR')} detalle={`de ${o.nochesDisponibles.toLocaleString('es-AR')} disponibles`} />
            </div>
          </div>
          <div className="mt-6 h-64">
            <ResponsiveContainer>
              <AreaChart data={o.porDia} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid vertical={false} stroke={GRILLA} />
                <XAxis dataKey="fecha" tickFormatter={diaCorto} minTickGap={28} {...ejeX} />
                <YAxis domain={[0, 100]} tickFormatter={(v) => `${v}%`} {...ejeY} />
                <Tooltip formatter={(v: number) => [`${Math.round(v)}%`, 'Ocupación']} labelFormatter={(l: string) => diaCorto(l)} />
                <Area type="monotone" dataKey="porcentaje" stroke={NOCHE} strokeWidth={2} fill={LAVANDA} fillOpacity={0.18} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
          <table className="tabla mt-6">
            <thead><tr><th>Tipo</th><th className="text-right">Habitaciones</th><th className="text-right">Noches vendidas</th><th>Ocupación</th></tr></thead>
            <tbody>
              {o.porTipo.map((t) => (
                <tr key={t.tipo}>
                  <td>{t.tipo}</td>
                  <td className="text-right tabular-nums">{t.habitaciones}</td>
                  <td className="text-right tabular-nums">{t.nochesOcupadas} / {t.nochesDisponibles}</td>
                  <td className="w-1/3">
                    <div className="flex items-center gap-3">
                      <div className="h-2 flex-1 rounded-full bg-neblina"><div className="h-2 rounded-full bg-noche" style={{ width: `${Math.min(100, t.porcentaje)}%` }} /></div>
                      <span className="w-10 text-right tabular-nums">{Math.round(t.porcentaje)}%</span>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {i && (
        <section className="panel mt-6">
          <div className="flex flex-wrap items-end justify-between gap-6">
            <h2 className="text-xl">Ingresos</h2>
            <div className="flex flex-wrap gap-10">
              <Cifra nombre="Cobrado" valor={plata(i.cobrado)} />
              <Cifra nombre="Reintegrado" valor={plata(i.reintegrado)} />
              <Cifra nombre="Neto" valor={plata(i.neto)} />
            </div>
          </div>
          <div className="mt-6 h-64">
            <ResponsiveContainer>
              <BarChart data={i.porDia} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid vertical={false} stroke={GRILLA} />
                <XAxis dataKey="fecha" tickFormatter={diaCorto} minTickGap={28} {...ejeX} />
                <YAxis tickFormatter={milesCorto} {...ejeY} width={72} />
                <Tooltip formatter={(v: number) => [plata(v), 'Neto cobrado']} labelFormatter={(l: string) => diaCorto(l)} cursor={{ fill: '#EEF1F5' }} />
                <Bar dataKey="monto" fill={NOCHE} radius={[3, 3, 0, 0]} maxBarSize={22} />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-6 grid gap-8 md:grid-cols-2">
            <Desglose titulo="Por tipo de habitación" filas={i.porTipo.map((m) => ({ nombre: m.clave, monto: m.monto, operaciones: m.operaciones }))} />
            <Desglose titulo="Por medio de pago" filas={i.porMedio.map((m) => ({ nombre: nombreMedio[m.clave] ?? m.clave, monto: m.monto, operaciones: m.operaciones }))} />
          </div>
        </section>
      )}
    </>
  )
}

function Desglose({ titulo, filas }: { titulo: string; filas: { nombre: string; monto: number; operaciones: number }[] }) {
  const max = Math.max(1, ...filas.map((f) => f.monto))
  return (
    <div>
      <h3 className="text-lg">{titulo}</h3>
      <ul className="mt-3 space-y-3">
        {filas.map((f) => (
          <li key={f.nombre}>
            <div className="flex justify-between text-sm"><span>{f.nombre} <span className="text-pizarra">· {f.operaciones} {f.operaciones === 1 ? 'pago' : 'pagos'}</span></span><span className="tabular-nums">{plata(f.monto)}</span></div>
            <div className="mt-1 h-2 rounded-full bg-neblina"><div className="h-2 rounded-full bg-lavanda-600" style={{ width: `${(Math.max(0, f.monto) / max) * 100}%` }} /></div>
          </li>
        ))}
        {filas.length === 0 && <li className="text-sm text-pizarra">Sin cobros en el período.</li>}
      </ul>
    </div>
  )
}
