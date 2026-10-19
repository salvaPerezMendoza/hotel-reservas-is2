import type { EstadoHabitacion, EstadoReserva } from '@/api/tipos'
import { nombreEstado, nombreEstadoHabitacion } from '@/util/formato'

const estilosReserva: Record<EstadoReserva, string> = {
  PENDIENTE_PAGO: 'bg-alba-50 text-alba-600 ring-alba-300',
  CONFIRMADA: 'bg-exito-50 text-exito ring-exito/30',
  EN_CURSO: 'bg-lavanda-100 text-lavanda-600 ring-lavanda',
  FINALIZADA: 'bg-neblina text-pizarra ring-neblina-300',
  CANCELADA: 'bg-alerta-50 text-alerta ring-alerta/30',
  EXPIRADA: 'bg-neblina text-pizarra-400 ring-neblina-300',
}

export function InsigniaReserva({ estado }: { estado: EstadoReserva }) {
  return (
    <span className={`inline-flex shrink-0 items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ${estilosReserva[estado]}`}>
      {nombreEstado[estado]}
    </span>
  )
}

export const coloresHabitacion: Record<EstadoHabitacion, string> = {
  DISPONIBLE: 'bg-exito',
  OCUPADA: 'bg-noche',
  LIMPIEZA: 'bg-alba',
  MANTENIMIENTO: 'bg-alerta',
}

export function InsigniaHabitacion({ estado }: { estado: EstadoHabitacion }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-xs font-medium text-pizarra">
      <span className={`h-2 w-2 rounded-full ${coloresHabitacion[estado]}`} />
      {nombreEstadoHabitacion[estado]}
    </span>
  )
}
