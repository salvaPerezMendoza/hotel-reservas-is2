import { useEffect, useRef, type ReactNode } from 'react'

export function Modal({ titulo, abierto, alCerrar, children, ancho = 'max-w-lg' }: {
  titulo: string
  abierto: boolean
  alCerrar: () => void
  children: ReactNode
  ancho?: string
}) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!abierto) return
    const anterior = document.activeElement as HTMLElement | null
    ref.current?.focus()
    const alTeclear = (e: KeyboardEvent) => e.key === 'Escape' && alCerrar()
    document.addEventListener('keydown', alTeclear)
    return () => {
      document.removeEventListener('keydown', alTeclear)
      anterior?.focus()
    }
  }, [abierto, alCerrar])
  if (!abierto) return null
  return (
    <div className="fixed inset-0 z-40 flex items-end justify-center bg-noche/40 p-0 sm:items-center sm:p-4" onMouseDown={alCerrar}>
      <div
        ref={ref}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        className={`max-h-[92vh] w-full ${ancho} overflow-y-auto rounded-t-2xl bg-white p-6 shadow-flotante outline-none sm:rounded-2xl`}
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          <h2 className="text-2xl">{titulo}</h2>
          <button type="button" onClick={alCerrar} className="rounded-full p-1 text-pizarra hover:bg-neblina" aria-label="Cerrar">
            <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 6l12 12M18 6L6 18" /></svg>
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
