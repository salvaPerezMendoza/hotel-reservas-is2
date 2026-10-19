const moneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 })

export const plata = (n: number | null | undefined) => moneda.format(n ?? 0)

/** Las fechas del backend llegan como 'YYYY-MM-DD': se interpretan como fecha local, sin zona horaria. */
export function aFecha(iso: string) {
  const [a, m, d] = iso.split('-').map(Number)
  return new Date(a, m - 1, d)
}

export function isoDe(fecha: Date) {
  const m = String(fecha.getMonth() + 1).padStart(2, '0')
  const d = String(fecha.getDate()).padStart(2, '0')
  return `${fecha.getFullYear()}-${m}-${d}`
}

export function hoyIso() {
  return isoDe(new Date())
}

export function sumarDias(iso: string, dias: number) {
  const f = aFecha(iso)
  f.setDate(f.getDate() + dias)
  return isoDe(f)
}

const fechaLarga = new Intl.DateTimeFormat('es-AR', { weekday: 'short', day: 'numeric', month: 'short' })
const fechaCorta = new Intl.DateTimeFormat('es-AR', { day: 'numeric', month: 'short' })
const fechaHora = new Intl.DateTimeFormat('es-AR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })

export const fechaTexto = (iso: string) => fechaLarga.format(aFecha(iso))
export const fechaCortaTexto = (iso: string) => fechaCorta.format(aFecha(iso))
export const instanteTexto = (iso: string) => fechaHora.format(new Date(iso))

export const noches = (n: number) => (n === 1 ? '1 noche' : `${n} noches`)
export const personas = (n: number) => (n === 1 ? '1 huésped' : `${n} huéspedes`)

export const nombreEstado: Record<string, string> = {
  PENDIENTE_PAGO: 'Pendiente de pago',
  CONFIRMADA: 'Confirmada',
  EN_CURSO: 'En curso',
  FINALIZADA: 'Finalizada',
  CANCELADA: 'Cancelada',
  EXPIRADA: 'Vencida',
}

export const nombreMedio: Record<string, string> = {
  ONLINE: 'Online',
  EFECTIVO: 'Efectivo',
  TARJETA: 'Tarjeta',
  TRANSFERENCIA: 'Transferencia',
}

export const nombreConcepto: Record<string, string> = {
  SENA: 'Seña',
  TOTAL: 'Total',
  SALDO: 'Saldo',
  REINTEGRO: 'Reintegro',
}

export const nombreEstadoHabitacion: Record<string, string> = {
  DISPONIBLE: 'Lista',
  OCUPADA: 'Ocupada',
  LIMPIEZA: 'En limpieza',
  MANTENIMIENTO: 'Mantenimiento',
}

export const nombreRol: Record<string, string> = {
  HUESPED: 'Huésped',
  RECEPCIONISTA: 'Recepción',
  ADMINISTRADOR: 'Administración',
}

export const nombreCategoria: Record<string, string> = { ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja' }
