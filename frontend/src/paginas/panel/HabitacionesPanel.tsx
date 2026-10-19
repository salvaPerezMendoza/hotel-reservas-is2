import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { EstadoHabitacion, Habitacion } from '@/api/tipos'
import { Encabezado } from '@/componentes/LayoutPanel'
import { Cargando, CajaError } from '@/componentes/Estados'
import { coloresHabitacion } from '@/componentes/InsigniaEstado'
import { useAvisos } from '@/componentes/Avisos'
import { nombreEstadoHabitacion } from '@/util/formato'

const estados: EstadoHabitacion[] = ['DISPONIBLE', 'OCUPADA', 'LIMPIEZA', 'MANTENIMIENTO']

/** Tablero de habitaciones por piso: recepción y limpieza marcan cuándo una habitación está lista. */
export default function HabitacionesPanel() {
  const cliente = useQueryClient()
  const avisar = useAvisos()
  const { data, isLoading, error } = useQuery({ queryKey: ['habitaciones-recepcion'], queryFn: () => api<Habitacion[]>('/recepcion/habitaciones'), refetchInterval: 30_000 })
  const cambiar = useMutation({
    mutationFn: ({ id, estado }: { id: number; estado: EstadoHabitacion }) => api<Habitacion>(`/recepcion/habitaciones/${id}/estado`, { method: 'PATCH', json: { estado } }),
    onSuccess: (h) => { cliente.invalidateQueries({ queryKey: ['habitaciones-recepcion'] }); cliente.invalidateQueries({ queryKey: ['tablero'] }); avisar(`Habitación ${h.numero}: ${nombreEstadoHabitacion[h.estado].toLowerCase()}.`) },
    onError: (e: Error) => avisar(e.message, 'error'),
  })
  const pisos = Array.from(new Set(data?.filter((h) => h.activa).map((h) => h.piso))).sort((a, b) => b - a)

  return (
    <>
      <Encabezado titulo="Habitaciones" bajada="Ocupada se marca sola con el check-in y pasa a limpieza con el check-out." />
      <div className="mb-5 flex flex-wrap gap-4 text-sm">
        {estados.map((e) => (
          <span key={e} className="inline-flex items-center gap-2">
            <span className={`h-3 w-3 rounded ${coloresHabitacion[e]}`} />{nombreEstadoHabitacion[e]}
            <span className="text-pizarra">{data?.filter((h) => h.activa && h.estado === e).length ?? ''}</span>
          </span>
        ))}
      </div>
      {isLoading && <Cargando />}
      {error && <CajaError error={error} />}
      <div className="space-y-4">
        {pisos.map((p) => (
          <section key={p} className="panel">
            <h2 className="text-lg">Piso {p}</h2>
            <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
              {data!.filter((h) => h.piso === p && h.activa).map((h) => (
                <div key={h.id} className="overflow-hidden rounded-xl ring-1 ring-neblina-300">
                  <div className={`h-1.5 ${coloresHabitacion[h.estado]}`} />
                  <div className="p-3">
                    <p className="cifra text-2xl">{h.numero}</p>
                    <p className="text-xs text-pizarra">{h.tipoNombre}</p>
                    <select
                      aria-label={`Estado de la habitación ${h.numero}`}
                      className="mt-2 w-full rounded-lg border border-neblina-300 bg-white px-2 py-1 text-sm"
                      value={h.estado}
                      disabled={h.estado === 'OCUPADA' || cambiar.isPending}
                      onChange={(e) => cambiar.mutate({ id: h.id, estado: e.target.value as EstadoHabitacion })}
                    >
                      {estados.filter((e) => e !== 'OCUPADA' || h.estado === 'OCUPADA').map((e) => <option key={e} value={e}>{nombreEstadoHabitacion[e]}</option>)}
                    </select>
                  </div>
                </div>
              ))}
            </div>
          </section>
        ))}
      </div>
    </>
  )
}
