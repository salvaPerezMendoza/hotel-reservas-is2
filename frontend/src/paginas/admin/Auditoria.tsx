import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { RegistroAuditoria } from '@/api/tipos'
import { Encabezado } from '@/componentes/LayoutPanel'
import { Cargando, CajaError } from '@/componentes/Estados'
import { instanteTexto } from '@/util/formato'

export default function Auditoria() {
  const [limite, setLimite] = useState(100)
  const [texto, setTexto] = useState('')
  const { data, isLoading, error } = useQuery({ queryKey: ['admin-auditoria', limite], queryFn: () => api<RegistroAuditoria[]>(`/admin/auditoria?limite=${limite}`), placeholderData: (a) => a })
  const t = texto.trim().toLowerCase()
  const filas = (data ?? []).filter((r) => !t || `${r.usuario} ${r.accion} ${r.entidad} ${r.entidadId ?? ''} ${r.detalle ?? ''}`.toLowerCase().includes(t))
  return (
    <>
      <Encabezado titulo="Auditoría" bajada="Quién hizo qué y cuándo. Los registros no se pueden editar ni borrar." />
      <div className="panel">
        <input className="campo max-w-sm" placeholder="Filtrar por usuario, acción o código" value={texto} onChange={(e) => setTexto(e.target.value)} />
        {isLoading && <Cargando />}
        {error && <CajaError error={error} />}
        <div className="mt-4 overflow-x-auto">
          <table className="tabla min-w-[720px]">
            <thead><tr><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Sobre</th><th>Detalle</th></tr></thead>
            <tbody>
              {filas.map((r) => (
                <tr key={r.id}>
                  <td className="whitespace-nowrap text-pizarra">{instanteTexto(r.fecha)}</td>
                  <td>{r.usuario}</td>
                  <td className="font-medium">{r.accion}</td>
                  <td>{r.entidad}{r.entidadId && <span className="text-pizarra"> {r.entidadId}</span>}</td>
                  <td className="text-pizarra">{r.detalle}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {data && data.length >= limite && <button className="boton-secundario mt-4" onClick={() => setLimite(limite + 200)}>Ver más registros</button>}
      </div>
    </>
  )
}
