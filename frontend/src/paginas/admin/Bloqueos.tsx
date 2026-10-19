import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { Bloqueo, Habitacion } from '@/api/tipos'
import { Encabezado } from '@/componentes/LayoutPanel'
import { Cargando, CajaError } from '@/componentes/Estados'
import { useAvisos } from '@/componentes/Avisos'
import { fechaCortaTexto, hoyIso, sumarDias } from '@/util/formato'

export default function Bloqueos() {
  const cliente = useQueryClient()
  const avisar = useAvisos()
  const bloqueos = useQuery({ queryKey: ['admin-bloqueos'], queryFn: () => api<Bloqueo[]>('/admin/bloqueos') })
  const habitaciones = useQuery({ queryKey: ['admin-habitaciones'], queryFn: () => api<Habitacion[]>('/admin/habitaciones') })
  const hoy = hoyIso()
  const [d, setD] = useState({ habitacionId: 0, desde: sumarDias(hoy, 1), hasta: sumarDias(hoy, 4), motivo: '' })
  const habitacionId = d.habitacionId || habitaciones.data?.find((h) => h.activa)?.id || 0

  const refrescar = () => ['admin-bloqueos', 'disponibilidad'].forEach((k) => cliente.invalidateQueries({ queryKey: [k] }))
  const crear = useMutation({
    mutationFn: () => api<Bloqueo>('/admin/bloqueos', { method: 'POST', json: { ...d, habitacionId } }),
    onSuccess: (b) => { refrescar(); avisar(`Habitación ${b.habitacionNumero} bloqueada.`); setD({ ...d, motivo: '' }) },
  })
  const eliminar = useMutation({
    mutationFn: (b: Bloqueo) => api<void>(`/admin/bloqueos/${b.id}`, { method: 'DELETE' }),
    onSuccess: () => { refrescar(); avisar('Bloqueo levantado.') },
    onError: (e: Error) => avisar(e.message, 'error'),
  })
  const enviar = (e: FormEvent) => { e.preventDefault(); crear.mutate() }

  return (
    <>
      <Encabezado titulo="Bloqueos" bajada="Sacan una habitación de la venta por un rango de fechas, por ejemplo para una obra. No se puede bloquear sobre reservas activas." />
      <form onSubmit={enviar} className="panel grid gap-3 md:grid-cols-[1fr_1fr_1fr_2fr_auto] md:items-end">
        <label><span className="etiqueta">Habitación</span>
          <select className="campo" value={habitacionId} onChange={(e) => setD({ ...d, habitacionId: Number(e.target.value) })}>
            {habitaciones.data?.filter((h) => h.activa).map((h) => <option key={h.id} value={h.id}>{h.numero} · {h.tipoNombre}</option>)}
          </select>
        </label>
        <label><span className="etiqueta">Desde</span><input type="date" className="campo" min={hoy} value={d.desde} onChange={(e) => setD({ ...d, desde: e.target.value })} required /></label>
        <label><span className="etiqueta">Hasta (se libera)</span><input type="date" className="campo" min={sumarDias(d.desde, 1)} value={d.hasta} onChange={(e) => setD({ ...d, hasta: e.target.value })} required /></label>
        <label><span className="etiqueta">Motivo</span><input className="campo" value={d.motivo} onChange={(e) => setD({ ...d, motivo: e.target.value })} required maxLength={200} placeholder="Pintura, cambio de aire acondicionado…" /></label>
        <button className="boton-primario" disabled={crear.isPending || !habitacionId}>Bloquear</button>
        {crear.error && <div className="md:col-span-5"><CajaError error={crear.error} /></div>}
      </form>
      <section className="panel mt-6">
        <h2 className="text-xl">Vigentes y próximos</h2>
        {bloqueos.isLoading && <Cargando />}
        {bloqueos.data?.length === 0 && <p className="mt-3 text-sm text-pizarra">No hay habitaciones bloqueadas.</p>}
        {bloqueos.data && bloqueos.data.length > 0 && (
          <table className="tabla mt-4">
            <thead><tr><th>Habitación</th><th>Desde</th><th>Hasta</th><th>Motivo</th><th /></tr></thead>
            <tbody>
              {bloqueos.data.map((b) => (
                <tr key={b.id}>
                  <td className="font-medium">{b.habitacionNumero}</td>
                  <td>{fechaCortaTexto(b.desde)}</td>
                  <td>{fechaCortaTexto(b.hasta)}</td>
                  <td>{b.motivo}</td>
                  <td className="text-right"><button className="boton-texto" onClick={() => eliminar.mutate(b)}>Levantar</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  )
}
