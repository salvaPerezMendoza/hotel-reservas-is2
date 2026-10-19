// Tipos que reflejan los DTO del backend (paquete web.dto).

export type Rol = 'HUESPED' | 'RECEPCIONISTA' | 'ADMINISTRADOR'
export type EstadoReserva = 'PENDIENTE_PAGO' | 'CONFIRMADA' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA' | 'EXPIRADA'
export type CanalReserva = 'ONLINE' | 'RECEPCION'
export type MedioPago = 'ONLINE' | 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA'
export type ConceptoPago = 'SENA' | 'TOTAL' | 'SALDO' | 'REINTEGRO'
export type EstadoPago = 'APROBADO' | 'RECHAZADO'
export type Categoria = 'ALTA' | 'MEDIA' | 'BAJA'
export type EstadoHabitacion = 'DISPONIBLE' | 'OCUPADA' | 'LIMPIEZA' | 'MANTENIMIENTO'
export type PoliticaCancelacion = 'FLEXIBLE' | 'MODERADA' | 'ESTRICTA'
export type EstrategiaPrecio = 'POR_TEMPORADA' | 'TEMPORADA_CON_DESCUENTO'

export interface Usuario {
  id: number
  nombre: string
  apellido: string
  email: string
  rol: Rol
  telefono: string | null
  documento: string | null
  activo: boolean
}

export interface Sesion {
  token: string
  usuario: Usuario
}

export interface Hotel {
  nombre: string
  horaCheckIn: string
  horaCheckOut: string
  porcentajeSena: number
  minutosRetencion: number
  politicaCancelacion: string
  descripcionPoliticaCancelacion: string
  descripcionPoliticaPrecio: string
}

export interface TipoHabitacion {
  id: number
  nombre: string
  descripcion: string
  capacidad: number
  precioBase: number
  servicios: string[]
  imagenUrl: string | null
  activo: boolean
  cantidadHabitaciones: number
}

export interface Noche {
  fecha: string
  categoria: Categoria
  precio: number
}

export interface Cotizacion {
  noches: number
  detalle: Noche[]
  subtotal: number
  descuento: number
  motivoDescuento: string | null
  total: number
  sena: number
}

export interface OpcionDisponible {
  tipo: TipoHabitacion
  habitacionesLibres: number
  cotizacion: Cotizacion
}

export interface ResultadoBusqueda {
  checkIn: string
  checkOut: string
  huespedes: number
  noches: number
  opciones: OpcionDisponible[]
}

export interface Pago {
  id: number
  monto: number
  medio: MedioPago
  concepto: ConceptoPago
  estado: EstadoPago
  referencia: string
  fecha: string
}

export interface Consumo {
  id: number
  descripcion: string
  monto: number
  fecha: string
}

export interface HuespedResumen {
  id: number
  nombre: string
  apellido: string
  email: string
  telefono: string | null
  documento: string | null
}

export interface Reserva {
  id: number
  codigo: string
  estado: EstadoReserva
  canal: CanalReserva
  huesped: HuespedResumen
  tipoId: number
  tipoNombre: string
  tipoImagenUrl: string | null
  habitacionId: number
  habitacionNumero: string
  checkIn: string
  checkOut: string
  noches: number
  cantidadHuespedes: number
  precioTotal: number
  totalConsumos: number
  totalACobrar: number
  totalPagado: number
  saldoPendiente: number
  senaMinima: number | null
  retencionHasta: string | null
  creadaEn: string
  canceladaEn: string | null
  reembolso: number | null
  checkInReal: string | null
  checkOutReal: string | null
  observaciones: string | null
  pagos: Pago[]
  consumos: Consumo[]
  puedeModificarse: boolean
  puedeCancelarse: boolean
}

export interface SimulacionCancelacion {
  porcentajeReintegro: number
  montoReintegro: number
  totalPagado: number
  explicacion: string
}

export interface ResultadoPago {
  aprobado: boolean
  mensaje: string
  pago: Pago
  reserva: Reserva
}

export interface Habitacion {
  id: number
  numero: string
  piso: number
  tipoId: number
  tipoNombre: string
  estado: EstadoHabitacion
  activa: boolean
}

export interface Tablero {
  llegadasHoy: number
  salidasHoy: number
  enCurso: number
  habitacionesDisponibles: number
  habitacionesTotales: number
  enLimpieza: number
  enMantenimiento: number
  pendientesDePago: number
  ocupacionHoy: number
}

export interface Temporada {
  id: number
  nombre: string
  categoria: Categoria
  fechaDesde: string
  fechaHasta: string
}

export interface TarifasTipo {
  tipoId: number
  tipoNombre: string
  precioBase: number
  alta: number | null
  media: number | null
  baja: number | null
}

export interface Opcion {
  valor: string
  nombre: string
  descripcion: string
}

export interface Configuracion {
  nombreHotel: string
  politicaCancelacion: PoliticaCancelacion
  porcentajeSena: number
  minutosRetencion: number
  horaCheckIn: string
  horaCheckOut: string
  estrategiaPrecio: EstrategiaPrecio
  politicasCancelacion: Opcion[]
  estrategiasPrecio: Opcion[]
}

export interface Bloqueo {
  id: number
  habitacionId: number
  habitacionNumero: string
  desde: string
  hasta: string
  motivo: string
}

export interface RegistroAuditoria {
  id: number
  fecha: string
  usuario: string
  accion: string
  entidad: string
  entidadId: string | null
  detalle: string | null
}

export interface ReporteOcupacion {
  desde: string
  hasta: string
  nochesDisponibles: number
  nochesOcupadas: number
  porcentaje: number
  porTipo: { tipo: string; habitaciones: number; nochesDisponibles: number; nochesOcupadas: number; porcentaje: number }[]
  porDia: { fecha: string; ocupadas: number; disponibles: number; porcentaje: number }[]
}

export interface Monto {
  clave: string
  monto: number
  operaciones: number
}

export interface ReporteIngresos {
  desde: string
  hasta: string
  cobrado: number
  reintegrado: number
  neto: number
  porTipo: Monto[]
  porMedio: Monto[]
  porDia: { fecha: string; monto: number }[]
}
