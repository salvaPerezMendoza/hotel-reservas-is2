import { useEffect, useState } from 'react'

/** Muestra cuánto falta para que venza la retención de la habitación (RF-07). */
export function useTiempoRestante(hasta: string | null) {
  const [ahora, setAhora] = useState(Date.now())
  useEffect(() => {
    if (!hasta) return
    const t = setInterval(() => setAhora(Date.now()), 1000)
    return () => clearInterval(t)
  }, [hasta])
  if (!hasta) return null
  return Math.max(0, Math.floor((new Date(hasta).getTime() - ahora) / 1000))
}

export function formatoReloj(segundos: number) {
  const m = Math.floor(segundos / 60)
  const s = segundos % 60
  return `${m}:${String(s).padStart(2, '0')}`
}
