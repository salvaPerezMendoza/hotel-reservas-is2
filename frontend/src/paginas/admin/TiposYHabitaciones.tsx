import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { Habitacion, TipoHabitacion } from '@/api/tipos'
import { Encabezado } from '@/componentes/LayoutPanel'
import { Cargando, CajaError } from '@/componentes/Estados'
import { InsigniaHabitacion } from '@/componentes/InsigniaEstado'
import { Modal } from '@/componentes/Modal'
import { useAvisos } from '@/componentes/Avisos'
import { plata } from '@/util/formato'

function useRefrescar() {
  const cliente = useQueryClient()
  return () => ['admin-tipos', 'admin-habitaciones', 'catalogo', 'habitaciones-recepcion', 'admin-tarifas'].forEach((k) => cliente.invalidateQueries({ queryKey: [k] }))
}

export default function TiposYHabitaciones() {
  const tipos = useQuery({ queryKey: ['admin-tipos'], queryFn: () => api<TipoHabitacion[]>('/admin/tipos') })
  const habitaciones = useQuery({ queryKey: ['admin-habitaciones'], queryFn: () => api<Habitacion[]>('/admin/habitaciones') })
  const refrescar = useRefrescar()
  const avisar = useAvisos()
  const [tipoEditado, setTipoEditado] = useState<TipoHabitacion | 'nuevo' | null>(null)
  const [habEditada, setHabEditada] = useState<Habitacion | 'nueva' | null>(null)

  const activarTipo = useMutation({
    mutationFn: (t: TipoHabitacion) => api<TipoHabitacion>(`/admin/tipos/${t.id}/activo?valor=${!t.activo}`, { method: 'PATCH' }),
    onSuccess: (t) => { refrescar(); avisar(t.activo ? `${t.nombre} vuelve a estar a la venta.` : `${t.nombre} dejó de mostrarse en el sitio.`) },
    onError: (e: Error) => avisar(e.message, 'error'),
  })
  const activarHab = useMutation({
    mutationFn: (h: Habitacion) => api<Habitacion>(`/admin/habitaciones/${h.id}/activa?valor=${!h.activa}`, { method: 'PATCH' }),
    onSuccess: (h) => { refrescar(); avisar(h.activa ? `Habitación ${h.numero} habilitada.` : `Habitación ${h.numero} dada de baja.`) },
    onError: (e: Error) => avisar(e.message, 'error'),
  })

  return (
    <>
      <Encabezado titulo="Tipos y habitaciones" />
      <section className="panel">
        <div className="flex items-center justify-between"><h2 className="text-xl">Tipos de habitación</h2><button className="boton-secundario" onClick={() => setTipoEditado('nuevo')}>Agregar tipo</button></div>
        {tipos.isLoading && <Cargando />}
        {tipos.error && <CajaError error={tipos.error} />}
        <div className="mt-4 overflow-x-auto">
          <table className="tabla min-w-[640px]">
            <thead><tr><th>Nombre</th><th>Capacidad</th><th className="text-right">Precio base</th><th className="text-right">Habitaciones</th><th>Estado</th><th /></tr></thead>
            <tbody>
              {tipos.data?.map((t) => (
                <tr key={t.id} className={t.activo ? '' : 'text-pizarra'}>
                  <td className="font-medium">{t.nombre}</td>
                  <td>{t.capacidad}</td>
                  <td className="text-right tabular-nums">{plata(t.precioBase)}</td>
                  <td className="text-right tabular-nums">{t.cantidadHabitaciones}</td>
                  <td>{t.activo ? 'A la venta' : 'Oculto'}</td>
                  <td className="whitespace-nowrap text-right">
                    <button className="boton-texto mr-4" onClick={() => setTipoEditado(t)}>Editar</button>
                    <button className="boton-texto" onClick={() => activarTipo.mutate(t)}>{t.activo ? 'Ocultar' : 'Poner a la venta'}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel mt-6">
        <div className="flex items-center justify-between"><h2 className="text-xl">Habitaciones</h2><button className="boton-secundario" onClick={() => setHabEditada('nueva')}>Agregar habitación</button></div>
        {habitaciones.isLoading && <Cargando />}
        <div className="mt-4 overflow-x-auto">
          <table className="tabla min-w-[560px]">
            <thead><tr><th>Número</th><th>Piso</th><th>Tipo</th><th>Estado</th><th /></tr></thead>
            <tbody>
              {habitaciones.data?.map((h) => (
                <tr key={h.id} className={h.activa ? '' : 'text-pizarra'}>
                  <td className="font-medium">{h.numero}</td>
                  <td>{h.piso}</td>
                  <td>{h.tipoNombre}</td>
                  <td>{h.activa ? <InsigniaHabitacion estado={h.estado} /> : 'Dada de baja'}</td>
                  <td className="whitespace-nowrap text-right">
                    <button className="boton-texto mr-4" onClick={() => setHabEditada(h)}>Editar</button>
                    <button className="boton-texto" onClick={() => activarHab.mutate(h)}>{h.activa ? 'Dar de baja' : 'Habilitar'}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {tipoEditado && <ModalTipo tipo={tipoEditado === 'nuevo' ? null : tipoEditado} alCerrar={() => setTipoEditado(null)} />}
      {habEditada && <ModalHabitacion habitacion={habEditada === 'nueva' ? null : habEditada} tipos={tipos.data ?? []} alCerrar={() => setHabEditada(null)} />}
    </>
  )
}

function ModalTipo({ tipo, alCerrar }: { tipo: TipoHabitacion | null; alCerrar: () => void }) {
  const refrescar = useRefrescar()
  const avisar = useAvisos()
  const [d, setD] = useState({
    nombre: tipo?.nombre ?? '', descripcion: tipo?.descripcion ?? '', capacidad: tipo?.capacidad ?? 2,
    precioBase: tipo?.precioBase ?? 100000, servicios: tipo?.servicios.join(', ') ?? '', imagenUrl: tipo?.imagenUrl ?? '',
  })
  const guardar = useMutation({
    mutationFn: () => api<TipoHabitacion>(tipo ? `/admin/tipos/${tipo.id}` : '/admin/tipos', {
      method: tipo ? 'PUT' : 'POST',
      json: { ...d, servicios: d.servicios.split(',').map((s) => s.trim()).filter(Boolean), imagenUrl: d.imagenUrl || null },
    }),
    onSuccess: (t) => { refrescar(); avisar(`${t.nombre} guardado.`); alCerrar() },
  })
  const enviar = (e: FormEvent) => { e.preventDefault(); guardar.mutate() }
  return (
    <Modal titulo={tipo ? `Editar ${tipo.nombre}` : 'Nuevo tipo de habitación'} abierto alCerrar={alCerrar} ancho="max-w-xl">
      <form onSubmit={enviar} className="space-y-4">
        <label className="block"><span className="etiqueta">Nombre</span><input className="campo" value={d.nombre} onChange={(e) => setD({ ...d, nombre: e.target.value })} required /></label>
        <label className="block"><span className="etiqueta">Descripción</span><textarea className="campo min-h-[90px]" value={d.descripcion} onChange={(e) => setD({ ...d, descripcion: e.target.value })} /></label>
        <div className="grid grid-cols-2 gap-3">
          <label><span className="etiqueta">Capacidad</span><input type="number" min={1} max={10} className="campo" value={d.capacidad} onChange={(e) => setD({ ...d, capacidad: Number(e.target.value) })} required /></label>
          <label><span className="etiqueta">Precio base por noche</span><input type="number" min={1} className="campo" value={d.precioBase} onChange={(e) => setD({ ...d, precioBase: Number(e.target.value) })} required /></label>
        </div>
        <label className="block"><span className="etiqueta">Servicios (separados por coma)</span><input className="campo" value={d.servicios} onChange={(e) => setD({ ...d, servicios: e.target.value })} /></label>
        <label className="block"><span className="etiqueta">URL de la foto (opcional)</span><input className="campo" value={d.imagenUrl} onChange={(e) => setD({ ...d, imagenUrl: e.target.value })} /></label>
        <p className="text-sm text-pizarra">El precio base se usa cuando no hay tarifa cargada para la temporada.</p>
        {guardar.error && <CajaError error={guardar.error} />}
        <div className="flex justify-end gap-3"><button type="button" className="boton-secundario" onClick={alCerrar}>Volver</button><button className="boton-primario" disabled={guardar.isPending}>Guardar</button></div>
      </form>
    </Modal>
  )
}

function ModalHabitacion({ habitacion, tipos, alCerrar }: { habitacion: Habitacion | null; tipos: TipoHabitacion[]; alCerrar: () => void }) {
  const refrescar = useRefrescar()
  const avisar = useAvisos()
  const [d, setD] = useState({ numero: habitacion?.numero ?? '', piso: habitacion?.piso ?? 1, tipoId: habitacion?.tipoId ?? tipos[0]?.id })
  const guardar = useMutation({
    mutationFn: () => api<Habitacion>(habitacion ? `/admin/habitaciones/${habitacion.id}` : '/admin/habitaciones', { method: habitacion ? 'PUT' : 'POST', json: d }),
    onSuccess: (h) => { refrescar(); avisar(`Habitación ${h.numero} guardada.`); alCerrar() },
  })
  const enviar = (e: FormEvent) => { e.preventDefault(); guardar.mutate() }
  return (
    <Modal titulo={habitacion ? `Editar habitación ${habitacion.numero}` : 'Nueva habitación'} abierto alCerrar={alCerrar}>
      <form onSubmit={enviar} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <label><span className="etiqueta">Número</span><input className="campo" value={d.numero} onChange={(e) => setD({ ...d, numero: e.target.value })} required maxLength={10} /></label>
          <label><span className="etiqueta">Piso</span><input type="number" min={0} max={100} className="campo" value={d.piso} onChange={(e) => setD({ ...d, piso: Number(e.target.value) })} required /></label>
        </div>
        <label className="block"><span className="etiqueta">Tipo</span>
          <select className="campo" value={d.tipoId} onChange={(e) => setD({ ...d, tipoId: Number(e.target.value) })}>{tipos.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}</select>
        </label>
        {guardar.error && <CajaError error={guardar.error} />}
        <div className="flex justify-end gap-3"><button type="button" className="boton-secundario" onClick={alCerrar}>Volver</button><button className="boton-primario" disabled={guardar.isPending}>Guardar</button></div>
      </form>
    </Modal>
  )
}
