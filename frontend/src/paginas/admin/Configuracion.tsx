import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { Configuracion as Config, Opcion } from '@/api/tipos'
import { Encabezado } from '@/componentes/LayoutPanel'
import { Cargando, CajaError } from '@/componentes/Estados'
import { useAvisos } from '@/componentes/Avisos'

function Opciones({ nombre, opciones, valor, alCambiar }: { nombre: string; opciones: Opcion[]; valor: string; alCambiar: (v: string) => void }) {
  return (
    <div className="grid gap-3 md:grid-cols-3">
      {opciones.map((o) => (
        <label key={o.valor} className={`cursor-pointer rounded-xl p-4 ring-1 transition ${valor === o.valor ? 'bg-neblina ring-noche' : 'ring-neblina-300 hover:ring-noche-600'}`}>
          <input type="radio" name={nombre} className="sr-only" checked={valor === o.valor} onChange={() => alCambiar(o.valor)} />
          <span className="font-semibold">{o.nombre}</span>
          <span className="mt-1 block text-sm text-pizarra">{o.descripcion}</span>
        </label>
      ))}
    </div>
  )
}

/** Acá se eligen las estrategias de precio y cancelación vigentes (patrón Strategy en el backend). */
export default function Configuracion() {
  const cliente = useQueryClient()
  const avisar = useAvisos()
  const { data, isLoading, error } = useQuery({ queryKey: ['admin-config'], queryFn: () => api<Config>('/admin/configuracion') })
  const [d, setD] = useState<Config | null>(null)
  useEffect(() => { if (data) setD(data) }, [data])

  const guardar = useMutation({
    mutationFn: () => api<Config>('/admin/configuracion', {
      method: 'PUT',
      json: { nombreHotel: d!.nombreHotel, politicaCancelacion: d!.politicaCancelacion, porcentajeSena: d!.porcentajeSena, minutosRetencion: d!.minutosRetencion, horaCheckIn: d!.horaCheckIn, horaCheckOut: d!.horaCheckOut, estrategiaPrecio: d!.estrategiaPrecio },
    }),
    onSuccess: (c) => { cliente.setQueryData(['admin-config'], c); ['hotel', 'disponibilidad'].forEach((k) => cliente.invalidateQueries({ queryKey: [k] })); avisar('Configuración guardada. Aplica a las reservas nuevas.') },
  })
  const enviar = (e: FormEvent) => { e.preventDefault(); guardar.mutate() }

  if (isLoading || !d) return error ? <CajaError error={error} /> : <Cargando />
  return (
    <form onSubmit={enviar}>
      <Encabezado titulo="Configuración">
        <button className="boton-primario" disabled={guardar.isPending}>{guardar.isPending ? 'Guardando…' : 'Guardar cambios'}</button>
      </Encabezado>
      {guardar.error && <div className="mb-4"><CajaError error={guardar.error} /></div>}
      <section className="panel">
        <h2 className="text-xl">Hotel</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-5">
          <label className="md:col-span-2"><span className="etiqueta">Nombre</span><input className="campo" value={d.nombreHotel} onChange={(e) => setD({ ...d, nombreHotel: e.target.value })} required /></label>
          <label><span className="etiqueta">Check-in desde</span><input type="time" className="campo" value={d.horaCheckIn} onChange={(e) => setD({ ...d, horaCheckIn: e.target.value })} required /></label>
          <label><span className="etiqueta">Check-out hasta</span><input type="time" className="campo" value={d.horaCheckOut} onChange={(e) => setD({ ...d, horaCheckOut: e.target.value })} required /></label>
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-5">
          <label><span className="etiqueta">Seña para confirmar (%)</span><input type="number" min={0} max={100} className="campo" value={d.porcentajeSena} onChange={(e) => setD({ ...d, porcentajeSena: Number(e.target.value) })} required /></label>
          <label><span className="etiqueta">Retención durante el pago (min)</span><input type="number" min={1} max={120} className="campo" value={d.minutosRetencion} onChange={(e) => setD({ ...d, minutosRetencion: Number(e.target.value) })} required /></label>
        </div>
      </section>
      <section className="panel mt-6">
        <h2 className="text-xl">Cálculo del precio</h2>
        <p className="mt-1 text-sm text-pizarra">Cómo se calcula el total de una estadía.</p>
        <div className="mt-4"><Opciones nombre="precio" opciones={d.estrategiasPrecio} valor={d.estrategiaPrecio} alCambiar={(v) => setD({ ...d, estrategiaPrecio: v as Config['estrategiaPrecio'] })} /></div>
      </section>
      <section className="panel mt-6">
        <h2 className="text-xl">Política de cancelación</h2>
        <p className="mt-1 text-sm text-pizarra">Cuánto se reintegra según la anticipación con la que se cancela.</p>
        <div className="mt-4"><Opciones nombre="cancelacion" opciones={d.politicasCancelacion} valor={d.politicaCancelacion} alCambiar={(v) => setD({ ...d, politicaCancelacion: v as Config['politicaCancelacion'] })} /></div>
      </section>
    </form>
  )
}
