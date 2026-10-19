import { Link } from 'react-router-dom'

export function MarcaSol({ className = 'h-7 w-7' }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} aria-hidden="true">
      <path d="M12 42a20 20 0 0 1 40 0" fill="none" stroke="#E9A93B" strokeWidth="5" strokeLinecap="round" />
      <path d="M8 47h48" stroke="#B9AEDC" strokeWidth="4" strokeLinecap="round" />
    </svg>
  )
}

export function Logo({ claro = false, a = '/' }: { claro?: boolean; a?: string }) {
  return (
    <Link to={a} className={`flex items-center gap-2.5 ${claro ? 'text-white' : 'text-noche'}`}>
      <MarcaSol />
      <span className="font-titulo text-[22px] leading-none">Hotel Aurora</span>
    </Link>
  )
}
