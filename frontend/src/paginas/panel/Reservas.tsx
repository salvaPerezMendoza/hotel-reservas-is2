import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { EstadoReserva, Reserva } from '@/api/tipos'
import { Encabezado } from '@/componentes/LayoutPanel'
import { Cargando, CajaError } from '@/componentes/Estados'
import { TablaReservas } from '@/componentes/TablaReservas'
import { nombreEstado } from '@/util/formato'

export default function Reservas() {
  const [q, setQ] = useState('')
  const [estado, setEstado] = useState<EstadoReserva | ''>('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const params = new URLSearchParams()
  if (q.trim()) params.set('q', q.trim())
  if (estado) params.set('estado', estado)
  if (desde) params.set('desde', desde)
  if (hasta) params.set('hasta', hasta)
  const { data, isLoading, error } = useQuery({
    queryKey: ['recepcion-reservas', params.toString()],
    queryFn: () => api<Reserva[]>(`/recepcion/reservas?${params}`),
    placeholderData: (anterior) => anterior,
  })
  return (
    <>
      <Encabezado titulo="Reservas">
        <Link to="/panel/nueva-reserva" className="boton-primario">Nueva reserva</Link>
      </Encabezado>
      <div className="panel">
        <div className="grid gap-3 md:grid-cols-[2fr_1fr_1fr_1fr]">
          <label><span className="etiqueta">Buscar</span><input className="campo" placeholder="Código, nombre, email o DNI" value={q} onChange={(e) => setQ(e.target.value)} /></label>
          <label><span className="etiqueta">Estado</span>
            <select className="campo" value={estado} onChange={(e) => setEstado(e.target.value as EstadoReserva | '')}>
              <option value="">Todos</option>
              {Object.entries(nombreEstado).map(([v, n]) => <option key={v} value={v}>{n}</option>)}
            </select>
          </label>
          <label><span className="etiqueta">Estadía desde</span><input type="date" className="campo" value={desde} onChange={(e) => setDesde(e.target.value)} /></label>
          <label><span className="etiqueta">hasta</span><input type="date" className="campo" value={hasta} onChange={(e) => setHasta(e.target.value)} /></label>
        </div>
        <div className="mt-5">
          {isLoading && <Cargando />}
          {error && <CajaError error={error} />}
          {data && <><p className="mb-2 text-sm text-pizarra">{data.length === 1 ? '1 reserva' : `${data.length} reservas`}</p><TablaReservas reservas={data} vacio="Ninguna reserva coincide con la búsqueda." /></>}
        </div>
      </div>
    </>
  )
}
