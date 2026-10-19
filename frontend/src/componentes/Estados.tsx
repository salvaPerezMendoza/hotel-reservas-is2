import type { ReactNode } from 'react'
import { ErrorApi } from '@/api/cliente'

export function Cargando({ texto = 'Cargando…' }: { texto?: string }) {
  return (
    <div className="flex items-center gap-3 py-10 text-pizarra" role="status">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-neblina-300 border-t-noche" />
      {texto}
    </div>
  )
}

export function CajaError({ error, children }: { error: unknown; children?: ReactNode }) {
  const mensaje = error instanceof ErrorApi || error instanceof Error ? error.message : 'Algo salió mal.'
  return (
    <div className="rounded-xl bg-alerta-50 px-4 py-3 text-sm text-alerta" role="alert">
      {mensaje}
      {children}
    </div>
  )
}

export function Vacio({ titulo, children }: { titulo: string; children?: ReactNode }) {
  return (
    <div className="rounded-2xl border border-dashed border-neblina-300 px-6 py-10 text-center">
      <p className="font-titulo text-xl">{titulo}</p>
      {children && <div className="mx-auto mt-2 max-w-md text-pizarra">{children}</div>}
    </div>
  )
}
