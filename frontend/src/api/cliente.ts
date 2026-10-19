// Cliente HTTP mínimo: agrega el token y convierte los errores del backend en mensajes legibles.

const CLAVE_TOKEN = 'aurora.token'

export function guardarToken(token: string | null) {
  try {
    if (token) localStorage.setItem(CLAVE_TOKEN, token)
    else localStorage.removeItem(CLAVE_TOKEN)
  } catch {
    /* almacenamiento no disponible: la sesión dura lo que la pestaña */
  }
  tokenEnMemoria = token
}

let tokenEnMemoria: string | null = (() => {
  try {
    return localStorage.getItem(CLAVE_TOKEN)
  } catch {
    return null
  }
})()

export function tokenActual() {
  return tokenEnMemoria
}

export class ErrorApi extends Error {
  estado: number
  campos: Record<string, string> | null
  constructor(estado: number, mensaje: string, campos: Record<string, string> | null = null) {
    super(mensaje)
    this.estado = estado
    this.campos = campos
  }
}

let alExpirarSesion: (() => void) | null = null
export function registrarAlExpirarSesion(fn: () => void) {
  alExpirarSesion = fn
}

export async function api<T>(ruta: string, opciones: RequestInit & { json?: unknown } = {}): Promise<T> {
  const headers = new Headers(opciones.headers)
  if (opciones.json !== undefined) headers.set('Content-Type', 'application/json')
  if (tokenEnMemoria) headers.set('Authorization', `Bearer ${tokenEnMemoria}`)
  let respuesta: Response
  try {
    respuesta = await fetch(`/api${ruta}`, {
      ...opciones,
      headers,
      body: opciones.json !== undefined ? JSON.stringify(opciones.json) : opciones.body,
    })
  } catch {
    throw new ErrorApi(0, 'No pudimos conectarnos con el servidor. Revisá tu conexión e intentá de nuevo.')
  }
  if (respuesta.status === 401 && tokenEnMemoria) {
    guardarToken(null)
    alExpirarSesion?.()
    throw new ErrorApi(401, 'Tu sesión venció. Iniciá sesión de nuevo.')
  }
  if (!respuesta.ok) {
    let mensaje = 'No pudimos completar la operación.'
    let campos = null
    try {
      const cuerpo = await respuesta.json()
      mensaje = cuerpo.mensaje ?? mensaje
      campos = cuerpo.campos ?? null
    } catch {
      /* sin cuerpo */
    }
    if (respuesta.status === 401) mensaje = 'Iniciá sesión para continuar.'
    if (respuesta.status === 403) mensaje = 'No tenés permiso para hacer esto.'
    throw new ErrorApi(respuesta.status, mensaje, campos)
  }
  if (respuesta.status === 204) return undefined as T
  const tipo = respuesta.headers.get('Content-Type') ?? ''
  if (tipo.includes('application/json')) return (await respuesta.json()) as T
  return (await respuesta.blob()) as unknown as T
}

/** Descarga un archivo autenticado (por ejemplo, el Excel de reportes). */
export async function descargar(ruta: string, nombre: string) {
  const blob = await api<Blob>(ruta)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nombre
  a.click()
  URL.revokeObjectURL(url)
}
