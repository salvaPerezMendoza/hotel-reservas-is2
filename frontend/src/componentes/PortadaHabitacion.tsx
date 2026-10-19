const paletas = [
  ['#22304F', '#4A5884', '#B9AEDC'],
  ['#2B3A5E', '#7466A8', '#F3CB82'],
  ['#1E2E46', '#3E6A78', '#E9A93B'],
  ['#2A2448', '#5B4E8C', '#F3CB82'],
]

function indice(nombre: string) {
  let h = 0
  for (const c of nombre) h = (h * 31 + c.charCodeAt(0)) >>> 0
  return h % paletas.length
}

/**
 * Imagen de la habitación. Si el administrador cargó una foto, se usa; si no,
 * se dibuja una ventana al amanecer con los colores del hotel.
 */
export function PortadaHabitacion({ nombre, imagenUrl, className = '' }: { nombre: string; imagenUrl?: string | null; className?: string }) {
  if (imagenUrl) {
    return <img src={imagenUrl} alt={`Habitación ${nombre}`} className={`h-full w-full object-cover ${className}`} loading="lazy" />
  }
  const [cielo, medio, luz] = paletas[indice(nombre)]
  const id = `g-${nombre.replace(/\W/g, '')}`
  return (
    <svg viewBox="0 0 400 260" preserveAspectRatio="xMidYMid slice" className={`h-full w-full ${className}`} role="img" aria-label={`Ilustración de la habitación ${nombre}`}>
      <defs>
        <linearGradient id={`${id}-c`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor={cielo} />
          <stop offset="0.65" stopColor={medio} />
          <stop offset="1" stopColor={luz} />
        </linearGradient>
      </defs>
      <rect width="400" height="260" fill="#E2E7EE" />
      {/* Ventana con arco */}
      <path d="M120 230V95a80 80 0 0 1 160 0v135z" fill={`url(#${id}-c)`} />
      <circle cx="200" cy="208" r="34" fill={luz} opacity="0.9" />
      <path d="M120 200h160" stroke="#E2E7EE" strokeWidth="2" opacity="0.5" />
      <path d="M200 15v215M120 135h160" stroke="#E2E7EE" strokeWidth="5" />
      <path d="M120 230V95a80 80 0 0 1 160 0v135" fill="none" stroke="#FFFFFF" strokeWidth="8" />
      {/* Cama */}
      <rect x="40" y="215" width="320" height="45" rx="6" fill="#FFFFFF" />
      <rect x="60" y="200" width="70" height="22" rx="8" fill="#ECE8F7" />
      <rect x="270" y="200" width="70" height="22" rx="8" fill="#ECE8F7" />
    </svg>
  )
}
