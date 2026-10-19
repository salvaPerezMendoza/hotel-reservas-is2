import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { hoyIso, sumarDias } from '@/util/formato'

interface Props {
  inicial?: { checkIn?: string; checkOut?: string; huespedes?: number }
  variante?: 'portada' | 'barra'
}

/** Formulario de búsqueda por fechas y cantidad de huéspedes (RF-01). */
export function BuscadorEstadia({ inicial, variante = 'barra' }: Props) {
  const navegar = useNavigate()
  const hoy = hoyIso()
  const [checkIn, setCheckIn] = useState(inicial?.checkIn ?? sumarDias(hoy, 7))
  const [checkOut, setCheckOut] = useState(inicial?.checkOut ?? sumarDias(hoy, 10))
  const [huespedes, setHuespedes] = useState(inicial?.huespedes ?? 2)
  const [error, setError] = useState<string | null>(null)

  function cambiarIngreso(valor: string) {
    setCheckIn(valor)
    if (valor && checkOut <= valor) setCheckOut(sumarDias(valor, 1))
  }

  function buscar(e: FormEvent) {
    e.preventDefault()
    if (!checkIn || !checkOut) return setError('Elegí las fechas de ingreso y egreso.')
    if (checkOut <= checkIn) return setError('El egreso tiene que ser después del ingreso.')
    setError(null)
    navegar(`/buscar?checkIn=${checkIn}&checkOut=${checkOut}&huespedes=${huespedes}`)
  }

  const portada = variante === 'portada'
  return (
    <form
      onSubmit={buscar}
      className={`grid gap-3 rounded-2xl bg-white p-3 sm:grid-cols-[1fr_1fr_0.7fr_auto] sm:items-end ${portada ? 'shadow-flotante sm:p-4' : 'ring-1 ring-neblina-200'}`}
    >
      <label className="block">
        <span className="etiqueta">Ingreso</span>
        <input type="date" className="campo" value={checkIn} min={hoy} onChange={(e) => cambiarIngreso(e.target.value)} required />
      </label>
      <label className="block">
        <span className="etiqueta">Egreso</span>
        <input type="date" className="campo" value={checkOut} min={checkIn ? sumarDias(checkIn, 1) : hoy} onChange={(e) => setCheckOut(e.target.value)} required />
      </label>
      <label className="block">
        <span className="etiqueta">Huéspedes</span>
        <select className="campo" value={huespedes} onChange={(e) => setHuespedes(Number(e.target.value))}>
          {[1, 2, 3, 4].map((n) => (
            <option key={n} value={n}>{n === 1 ? '1 persona' : `${n} personas`}</option>
          ))}
        </select>
      </label>
      <button type="submit" className={`${portada ? 'boton-reservar' : 'boton-primario'} h-[46px] px-6`}>
        Ver disponibilidad
      </button>
      {error && <p className="text-sm text-alerta sm:col-span-4" role="alert">{error}</p>}
    </form>
  )
}
