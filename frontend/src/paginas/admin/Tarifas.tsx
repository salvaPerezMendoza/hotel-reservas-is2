import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { Categoria, TarifasTipo, Temporada } from '@/api/tipos'
import { Encabezado } from '@/componentes/LayoutPanel'
import { Cargando, CajaError } from '@/componentes/Estados'
import { Modal } from '@/componentes/Modal'
import { useAvisos } from '@/componentes/Avisos'
import { fechaCortaTexto, nombreCategoria, plata } from '@/util/formato'

const categorias: Categoria[] = ['ALTA', 'MEDIA', 'BAJA']
const punto: Record<Categoria, string> = { ALTA: 'bg-alba', MEDIA: 'bg-lavanda', BAJA: 'bg-neblina-300' }

export default function Tarifas() {
  const cliente = useQueryClient()
  const avisar = useAvisos()
  const tarifas = useQuery({ queryKey: ['admin-tarifas'], queryFn: () => api<TarifasTipo[]>('/admin/tarifas') })
  const temporadas = useQuery({ queryKey: ['admin-temporadas'], queryFn: () => api<Temporada[]>('/admin/temporadas') })
  const [editandoTarifa, setEditandoTarifa] = useState<TarifasTipo | null>(null)
  const [editandoTemporada, setEditandoTemporada] = useState<Temporada | 'nueva' | null>(null)

  const eliminar = useMutation({
    mutationFn: (t: Temporada) => api<void>(`/admin/temporadas/${t.id}`, { method: 'DELETE' }),
    onSuccess: () => { cliente.invalidateQueries({ queryKey: ['admin-temporadas'] }); avisar('Temporada eliminada.') },
    onError: (e: Error) => avisar(e.message, 'error'),
  })

  return (
    <>
      <Encabezado titulo="Tarifas y temporadas" bajada="Cada noche se cobra con la tarifa de la temporada en la que cae. Las fechas fuera de toda temporada cuentan como baja." />
      <section className="panel">
        <h2 className="text-xl">Precio por noche</h2>
        {tarifas.isLoading && <Cargando />}
        {tarifas.error && <CajaError error={tarifas.error} />}
        <div className="mt-4 overflow-x-auto">
          <table className="tabla min-w-[600px]">
            <thead><tr><th>Tipo</th>{categorias.map((c) => <th key={c} className="text-right"><span className="inline-flex items-center gap-1.5"><span className={`h-2.5 w-2.5 rounded-sm ${punto[c]}`} />{nombreCategoria[c]}</span></th>)}<th /></tr></thead>
            <tbody>
              {tarifas.data?.map((t) => (
                <tr key={t.tipoId}>
                  <td className="font-medium">{t.tipoNombre}<div className="text-xs font-normal text-pizarra">base {plata(t.precioBase)}</div></td>
                  {[t.alta, t.media, t.baja].map((v, i) => <td key={i} className={`text-right tabular-nums ${v == null ? 'text-pizarra' : ''}`}>{v == null ? 'base' : plata(v)}</td>)}
                  <td className="text-right"><button className="boton-texto" onClick={() => setEditandoTarifa(t)}>Editar</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel mt-6">
        <div className="flex items-center justify-between"><h2 className="text-xl">Temporadas</h2><button className="boton-secundario" onClick={() => setEditandoTemporada('nueva')}>Agregar temporada</button></div>
        {temporadas.isLoading && <Cargando />}
        <div className="mt-4 overflow-x-auto">
          <table className="tabla min-w-[560px]">
            <thead><tr><th>Nombre</th><th>Categoría</th><th>Desde</th><th>Hasta</th><th /></tr></thead>
            <tbody>
              {temporadas.data?.map((t) => (
                <tr key={t.id}>
                  <td className="font-medium">{t.nombre}</td>
                  <td><span className="inline-flex items-center gap-1.5"><span className={`h-2.5 w-2.5 rounded-sm ${punto[t.categoria]}`} />{nombreCategoria[t.categoria]}</span></td>
                  <td>{fechaCortaTexto(t.fechaDesde)}</td>
                  <td>{fechaCortaTexto(t.fechaHasta)}</td>
                  <td className="whitespace-nowrap text-right">
                    <button className="boton-texto mr-4" onClick={() => setEditandoTemporada(t)}>Editar</button>
                    <button className="boton-texto text-alerta" onClick={() => eliminar.mutate(t)}>Eliminar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {editandoTarifa && <ModalTarifa tarifa={editandoTarifa} alCerrar={() => setEditandoTarifa(null)} />}
      {editandoTemporada && <ModalTemporada temporada={editandoTemporada === 'nueva' ? null : editandoTemporada} alCerrar={() => setEditandoTemporada(null)} />}
    </>
  )
}

function ModalTarifa({ tarifa, alCerrar }: { tarifa: TarifasTipo; alCerrar: () => void }) {
  const cliente = useQueryClient()
  const avisar = useAvisos()
  const [valores, setValores] = useState<Record<Categoria, string>>({
    ALTA: String(tarifa.alta ?? ''), MEDIA: String(tarifa.media ?? ''), BAJA: String(tarifa.baja ?? ''),
  })
  const guardar = useMutation({
    mutationFn: () => api<TarifasTipo>(`/admin/tarifas/${tarifa.tipoId}`, {
      method: 'PUT',
      json: { tarifas: categorias.filter((c) => valores[c]).map((c) => ({ categoria: c, precioNoche: Number(valores[c]) })) },
    }),
    onSuccess: () => { cliente.invalidateQueries({ queryKey: ['admin-tarifas'] }); cliente.invalidateQueries({ queryKey: ['disponibilidad'] }); avisar(`Tarifas de ${tarifa.tipoNombre} guardadas.`); alCerrar() },
  })
  const enviar = (e: FormEvent) => { e.preventDefault(); guardar.mutate() }
  return (
    <Modal titulo={`Tarifas · ${tarifa.tipoNombre}`} abierto alCerrar={alCerrar}>
      <form onSubmit={enviar} className="space-y-4">
        {categorias.map((c) => (
          <label key={c} className="block"><span className="etiqueta">Temporada {nombreCategoria[c].toLowerCase()}</span>
            <input type="number" min={1} className="campo tabular-nums" placeholder={`Sin tarifa: se usa la base (${plata(tarifa.precioBase)})`} value={valores[c]} onChange={(e) => setValores({ ...valores, [c]: e.target.value })} />
          </label>
        ))}
        <p className="text-sm text-pizarra">Los cambios aplican a las reservas nuevas; las existentes conservan su precio.</p>
        {guardar.error && <CajaError error={guardar.error} />}
        <div className="flex justify-end gap-3"><button type="button" className="boton-secundario" onClick={alCerrar}>Volver</button><button className="boton-primario" disabled={guardar.isPending}>Guardar tarifas</button></div>
      </form>
    </Modal>
  )
}

function ModalTemporada({ temporada, alCerrar }: { temporada: Temporada | null; alCerrar: () => void }) {
  const cliente = useQueryClient()
  const avisar = useAvisos()
  const [d, setD] = useState({ nombre: temporada?.nombre ?? '', categoria: temporada?.categoria ?? ('ALTA' as Categoria), fechaDesde: temporada?.fechaDesde ?? '', fechaHasta: temporada?.fechaHasta ?? '' })
  const guardar = useMutation({
    mutationFn: () => api<Temporada>(temporada ? `/admin/temporadas/${temporada.id}` : '/admin/temporadas', { method: temporada ? 'PUT' : 'POST', json: d }),
    onSuccess: () => { cliente.invalidateQueries({ queryKey: ['admin-temporadas'] }); avisar('Temporada guardada.'); alCerrar() },
  })
  const enviar = (e: FormEvent) => { e.preventDefault(); guardar.mutate() }
  return (
    <Modal titulo={temporada ? `Editar ${temporada.nombre}` : 'Nueva temporada'} abierto alCerrar={alCerrar}>
      <form onSubmit={enviar} className="space-y-4">
        <label className="block"><span className="etiqueta">Nombre</span><input className="campo" value={d.nombre} onChange={(e) => setD({ ...d, nombre: e.target.value })} required placeholder="Fin de semana largo de noviembre" /></label>
        <label className="block"><span className="etiqueta">Categoría</span>
          <select className="campo" value={d.categoria} onChange={(e) => setD({ ...d, categoria: e.target.value as Categoria })}>{categorias.map((c) => <option key={c} value={c}>{nombreCategoria[c]}</option>)}</select>
        </label>
        <div className="grid grid-cols-2 gap-3">
          <label><span className="etiqueta">Desde</span><input type="date" className="campo" value={d.fechaDesde} onChange={(e) => setD({ ...d, fechaDesde: e.target.value })} required /></label>
          <label><span className="etiqueta">Hasta (inclusive)</span><input type="date" className="campo" min={d.fechaDesde} value={d.fechaHasta} onChange={(e) => setD({ ...d, fechaHasta: e.target.value })} required /></label>
        </div>
        <p className="text-sm text-pizarra">Dos temporadas no pueden superponerse.</p>
        {guardar.error && <CajaError error={guardar.error} />}
        <div className="flex justify-end gap-3"><button type="button" className="boton-secundario" onClick={alCerrar}>Volver</button><button className="boton-primario" disabled={guardar.isPending}>Guardar</button></div>
      </form>
    </Modal>
  )
}
