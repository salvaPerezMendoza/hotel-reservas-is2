import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { TipoHabitacion } from '@/api/tipos'
import { BuscadorEstadia } from '@/componentes/BuscadorEstadia'
import { PortadaHabitacion } from '@/componentes/PortadaHabitacion'
import { useHotel } from '@/componentes/LayoutPublico'
import { plata, personas } from '@/util/formato'

export function useTipos() {
  return useQuery({ queryKey: ['catalogo'], queryFn: () => api<TipoHabitacion[]>('/catalogo/tipos'), staleTime: 300_000 })
}

export default function Inicio() {
  const { data: tipos } = useTipos()
  const { data: hotel } = useHotel()

  return (
    <>
      <section className="cielo-aurora relative overflow-hidden text-white">
        <div className="mx-auto max-w-6xl px-4 pb-24 pt-32 sm:px-6 sm:pb-32 sm:pt-40">
          <h1 className="max-w-3xl text-[44px] leading-[1.05] sm:text-[68px]">
            Despertate frente al río.
          </h1>
          <p className="mt-5 max-w-xl text-lg text-neblina-200">
            Dieciocho habitaciones sobre la Costanera de Buenos Aires. Elegí tus noches, mirá el precio de cada una y
            confirmá con una seña del {hotel?.porcentajeSena ?? 30}%.
          </p>
          <div className="relative z-10 mt-10 max-w-3xl text-noche">
            <BuscadorEstadia variante="portada" />
          </div>
        </div>
        <svg className="sol-saliendo pointer-events-none absolute -bottom-40 right-[-10rem] hidden h-[30rem] w-[30rem] opacity-90 lg:block xl:right-[-4rem]" viewBox="0 0 200 200" aria-hidden="true">
          <circle cx="100" cy="100" r="78" fill="none" stroke="#E9A93B" strokeWidth="2" opacity="0.7" />
          <circle cx="100" cy="100" r="58" fill="#E9A93B" opacity="0.18" />
        </svg>
      </section>

      <section className="mx-auto max-w-6xl px-4 pt-20 sm:px-6">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <h2 className="text-4xl">Las habitaciones</h2>
          <Link to="/habitaciones" className="boton-texto">Ver todo el detalle</Link>
        </div>
        <div className="mt-10 divide-y divide-neblina-300">
          {tipos?.map((t, i) => (
            <article key={t.id} className={`grid items-center gap-6 py-8 md:grid-cols-2 md:gap-12 ${i % 2 ? 'md:[&>*:first-child]:order-2' : ''}`}>
              <PortadaHabitacion nombre={t.nombre} imagenUrl={t.imagenUrl} className="aspect-[16/10] rounded-2xl" />
              <div>
                <h3 className="text-3xl">{t.nombre}</h3>
                <p className="mt-1 text-sm text-pizarra">Hasta {personas(t.capacidad)} · {t.cantidadHabitaciones} habitaciones</p>
                <p className="mt-4 max-w-prose text-pizarra">{t.descripcion}</p>
                <p className="mt-5">
                  <span className="text-sm text-pizarra">Desde </span>
                  <span className="cifra text-2xl">{plata(t.precioBase)}</span>
                  <span className="text-sm text-pizarra"> la noche</span>
                </p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="mx-auto mt-16 max-w-6xl px-4 sm:px-6">
        <div className="grid gap-10 rounded-3xl bg-white p-8 ring-1 ring-neblina-200 sm:p-12 md:grid-cols-3">
          <div>
            <h3 className="text-2xl">El precio, noche por noche</h3>
            <p className="mt-2 text-pizarra">Cada noche se cobra según la temporada en la que cae. Antes de pagar ves el detalle completo, sin cargos sorpresa.</p>
          </div>
          <div>
            <h3 className="text-2xl">Tu habitación, guardada</h3>
            <p className="mt-2 text-pizarra">Mientras completás el pago la habitación queda reservada para vos durante {hotel?.minutosRetencion ?? 10} minutos.</p>
          </div>
          <div>
            <h3 className="text-2xl">Cambios y cancelaciones</h3>
            <p className="mt-2 text-pizarra">{hotel?.descripcionPoliticaCancelacion ?? 'Podés modificar o cancelar desde Mis reservas.'}</p>
          </div>
        </div>
      </section>
    </>
  )
}
