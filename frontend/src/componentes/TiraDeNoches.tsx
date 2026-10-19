import type { Categoria, Noche } from '@/api/tipos'
import { aFecha, nombreCategoria, plata } from '@/util/formato'

const color: Record<Categoria, string> = {
  ALTA: 'bg-alba',
  MEDIA: 'bg-lavanda',
  BAJA: 'bg-neblina-300',
}

/**
 * La estadía noche por noche: cada bloque es una noche, coloreada por la temporada
 * en la que cae y con una altura proporcional a su precio. Así se ve de dónde sale el total.
 */
export function TiraDeNoches({ noches, compacta = false }: { noches: Noche[]; compacta?: boolean }) {
  const max = Math.max(...noches.map((n) => n.precio))
  const categorias = Array.from(new Set(noches.map((n) => n.categoria)))
  const alto = compacta ? 28 : 56
  return (
    <figure className="w-full">
      <div className="flex items-end gap-[3px]" style={{ height: alto }} role="img"
        aria-label={`Precio por noche: ${noches.map((n) => `${aFecha(n.fecha).getDate()} ${plata(n.precio)}`).join(', ')}`}>
        {noches.map((n) => (
          <div
            key={n.fecha}
            title={`${aFecha(n.fecha).toLocaleDateString('es-AR', { weekday: 'short', day: 'numeric', month: 'short' })} · temporada ${nombreCategoria[n.categoria].toLowerCase()} · ${plata(n.precio)}`}
            className={`min-w-[6px] flex-1 rounded-t-[4px] ${color[n.categoria]}`}
            style={{ height: `${Math.max(35, (n.precio / max) * 100)}%`, maxWidth: compacta ? 22 : 44 }}
          />
        ))}
      </div>
      {!compacta && (
        <div className="mt-1.5 flex gap-[3px] text-[11px] tabular-nums text-pizarra">
          {noches.map((n, i) => (
            <span key={n.fecha} className="min-w-[6px] flex-1 text-center" style={{ maxWidth: 44 }}>
              {noches.length <= 14 || i % 3 === 0 ? aFecha(n.fecha).getDate() : ''}
            </span>
          ))}
        </div>
      )}
      <figcaption className={`flex flex-wrap gap-x-4 gap-y-1 text-xs text-pizarra ${compacta ? 'mt-1.5' : 'mt-3'}`}>
        {categorias.map((c) => (
          <span key={c} className="inline-flex items-center gap-1.5">
            <span className={`h-2.5 w-2.5 rounded-sm ${color[c]}`} />
            Temporada {nombreCategoria[c].toLowerCase()}
          </span>
        ))}
      </figcaption>
    </figure>
  )
}
