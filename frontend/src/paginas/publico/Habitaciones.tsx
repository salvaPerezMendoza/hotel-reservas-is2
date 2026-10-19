import { Link } from 'react-router-dom'
import { PortadaHabitacion } from '@/componentes/PortadaHabitacion'
import { BuscadorEstadia } from '@/componentes/BuscadorEstadia'
import { Cargando, CajaError } from '@/componentes/Estados'
import { plata, personas } from '@/util/formato'
import { useTipos } from './Inicio'

export default function Habitaciones() {
  const { data: tipos, isLoading, error } = useTipos()
  return (
    <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
      <h1 className="text-5xl">Habitaciones</h1>
      <p className="mt-3 max-w-2xl text-pizarra">Todas tienen vista al río, ropa de cama de algodón y desayuno incluido. El precio de la noche cambia según la temporada.</p>
      <div className="mt-8"><BuscadorEstadia /></div>
      {isLoading && <Cargando />}
      {error && <div className="mt-6"><CajaError error={error} /></div>}
      <div className="mt-12 space-y-14">
        {tipos?.map((t) => (
          <article key={t.id} className="grid gap-8 md:grid-cols-[1.1fr_1fr]">
            <PortadaHabitacion nombre={t.nombre} imagenUrl={t.imagenUrl} className="aspect-[4/3] rounded-2xl" />
            <div className="flex flex-col">
              <h2 className="text-4xl">{t.nombre}</h2>
              <p className="mt-1 text-pizarra">Hasta {personas(t.capacidad)}</p>
              <p className="mt-4 max-w-prose">{t.descripcion}</p>
              {t.servicios.length > 0 && (
                <ul className="mt-5 grid grid-cols-2 gap-x-6 gap-y-1.5 text-sm text-pizarra">
                  {t.servicios.map((s) => (
                    <li key={s} className="flex items-center gap-2">
                      <span className="h-1.5 w-1.5 rounded-full bg-lavanda-600" />{s}
                    </li>
                  ))}
                </ul>
              )}
              <div className="mt-auto flex flex-wrap items-center justify-between gap-4 pt-6">
                <p><span className="cifra text-3xl">{plata(t.precioBase)}</span> <span className="text-sm text-pizarra">por noche en temporada baja</span></p>
                <Link to={`/buscar?tipoId=${t.id}`} className="boton-primario">Ver fechas</Link>
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  )
}
