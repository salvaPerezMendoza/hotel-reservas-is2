import { Link } from 'react-router-dom'
import type { Reserva } from '@/api/tipos'
import { InsigniaReserva } from './InsigniaEstado'
import { fechaCortaTexto, noches, plata } from '@/util/formato'

export function TablaReservas({ reservas, vacio = 'No hay reservas.' }: { reservas: Reserva[]; vacio?: string }) {
  if (reservas.length === 0) return <p className="py-6 text-sm text-pizarra">{vacio}</p>
  return (
    <div className="-mx-5 overflow-x-auto px-5 sm:-mx-6 sm:px-6">
      <table className="tabla min-w-[720px]">
        <thead>
          <tr><th>Código</th><th>Huésped</th><th>Habitación</th><th>Estadía</th><th>Estado</th><th className="text-right">Saldo</th></tr>
        </thead>
        <tbody>
          {reservas.map((r) => (
            <tr key={r.codigo} className="hover:bg-neblina/60">
              <td><Link to={`/panel/reservas/${r.codigo}`} className="font-semibold text-lavanda-600 hover:underline">{r.codigo}</Link></td>
              <td>{r.huesped.nombre} {r.huesped.apellido}<div className="text-xs text-pizarra">{r.canal === 'RECEPCION' ? 'Por recepción' : 'Online'}</div></td>
              <td>{r.habitacionNumero}<div className="text-xs text-pizarra">{r.tipoNombre}</div></td>
              <td className="whitespace-nowrap">{fechaCortaTexto(r.checkIn)} – {fechaCortaTexto(r.checkOut)}<div className="text-xs text-pizarra">{noches(r.noches)}</div></td>
              <td><InsigniaReserva estado={r.estado} /></td>
              <td className={`text-right tabular-nums ${r.saldoPendiente > 0 && r.estado !== 'CANCELADA' ? 'font-semibold' : 'text-pizarra'}`}>{plata(r.saldoPendiente)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
