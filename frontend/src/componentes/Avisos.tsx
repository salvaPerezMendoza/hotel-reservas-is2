import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'

type Tipo = 'exito' | 'error' | 'info'
interface Aviso {
  id: number
  tipo: Tipo
  texto: string
}

const Contexto = createContext<(texto: string, tipo?: Tipo) => void>(() => {})

/** Mensajes breves que confirman una acción o explican un error. */
export function ProveedorAvisos({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([])
  const avisar = useCallback((texto: string, tipo: Tipo = 'exito') => {
    const id = Date.now() + Math.random()
    setAvisos((a) => [...a, { id, tipo, texto }])
    setTimeout(() => setAvisos((a) => a.filter((x) => x.id !== id)), 5000)
  }, [])
  return (
    <Contexto.Provider value={avisar}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 bottom-4 z-50 flex flex-col items-center gap-2 px-4" aria-live="polite">
        {avisos.map((a) => (
          <div
            key={a.id}
            className={`pointer-events-auto max-w-md rounded-xl px-4 py-3 text-sm font-medium shadow-flotante ${
              a.tipo === 'exito' ? 'bg-noche text-white' : a.tipo === 'error' ? 'bg-alerta text-white' : 'bg-white text-noche ring-1 ring-neblina-300'
            }`}
          >
            {a.texto}
          </div>
        ))}
      </div>
    </Contexto.Provider>
  )
}

export const useAvisos = () => useContext(Contexto)
