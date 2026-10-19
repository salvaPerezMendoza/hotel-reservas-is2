import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { api, ErrorApi } from '@/api/cliente'
import type { Sesion, Usuario } from '@/api/tipos'
import { useAuth } from '@/auth/AuthContext'
import { CajaError } from '@/componentes/Estados'
import { useAvisos } from '@/componentes/Avisos'

function Tarjeta({ titulo, children, bajada }: { titulo: string; bajada?: ReactNode; children: ReactNode }) {
  return (
    <div className="mx-auto max-w-md px-4 py-14">
      <h1 className="text-4xl">{titulo}</h1>
      {bajada && <p className="mt-2 text-pizarra">{bajada}</p>}
      <div className="panel mt-8">{children}</div>
    </div>
  )
}

function Campo({ etiqueta, error, ...props }: { etiqueta: string; error?: string } & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="block">
      <span className="etiqueta">{etiqueta}</span>
      <input className={`campo ${error ? 'border-alerta' : ''}`} {...props} />
      {error && <span className="mt-1 block text-sm text-alerta">{error}</span>}
    </label>
  )
}

const camposDe = (e: unknown) => (e instanceof ErrorApi ? e.campos ?? {} : {})

const demos = [
  ['Huésped', 'huesped@demo.com', 'Huesped123!'],
  ['Recepción', 'recepcion@hotelaurora.com', 'Recepcion123!'],
  ['Administración', 'admin@hotelaurora.com', 'Admin123!'],
] as const

export function Ingresar() {
  const [params] = useSearchParams()
  const navegar = useNavigate()
  const { iniciarSesion } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const login = useMutation({
    mutationFn: () => api<Sesion>('/auth/login', { method: 'POST', json: { email, password } }),
    onSuccess: (s) => {
      iniciarSesion(s)
      const volver = params.get('volver')
      const personal = s.usuario.rol !== 'HUESPED'
      navegar(volver && !(personal && !volver.startsWith('/panel')) ? volver : personal ? '/panel' : '/', { replace: true })
    },
  })
  const enviar = (e: FormEvent) => { e.preventDefault(); login.mutate() }
  return (
    <Tarjeta titulo="Ingresar" bajada={params.get('motivo') === 'reservar' ? 'Para reservar necesitás una cuenta. Ingresá o creá una en un minuto.' : undefined}>
      <form onSubmit={enviar} className="space-y-4">
        <Campo etiqueta="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <Campo etiqueta="Contraseña" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        {login.error && <CajaError error={login.error} />}
        <button className="boton-primario w-full" disabled={login.isPending}>{login.isPending ? 'Ingresando…' : 'Ingresar'}</button>
      </form>
      <div className="mt-5 flex justify-between text-sm">
        <Link to="/recuperar" className="boton-texto">Olvidé mi contraseña</Link>
        <Link to={`/registro${params.toString() ? `?${params}` : ''}`} className="boton-texto">Crear cuenta</Link>
      </div>
      <div className="mt-6 border-t border-neblina-200 pt-5">
        <p className="text-sm text-pizarra">Cuentas de demostración</p>
        <div className="mt-2 flex flex-wrap gap-2">
          {demos.map(([nombre, e, p]) => (
            <button key={e} type="button" className="rounded-full bg-neblina px-3 py-1.5 text-sm font-medium hover:bg-neblina-200" onClick={() => { setEmail(e); setPassword(p) }}>
              {nombre}
            </button>
          ))}
        </div>
      </div>
    </Tarjeta>
  )
}

export function Registro() {
  const [params] = useSearchParams()
  const navegar = useNavigate()
  const { iniciarSesion } = useAuth()
  const [datos, setDatos] = useState({ nombre: '', apellido: '', email: '', password: '', telefono: '', documento: '' })
  const set = (k: keyof typeof datos) => (e: React.ChangeEvent<HTMLInputElement>) => setDatos({ ...datos, [k]: e.target.value })
  const registro = useMutation({
    mutationFn: () => api<Sesion>('/auth/registro', { method: 'POST', json: { ...datos, telefono: datos.telefono || null, documento: datos.documento || null } }),
    onSuccess: (s) => { iniciarSesion(s); navegar(params.get('volver') ?? '/', { replace: true }) },
  })
  const c = camposDe(registro.error)
  const enviar = (e: FormEvent) => { e.preventDefault(); registro.mutate() }
  return (
    <Tarjeta titulo="Crear cuenta" bajada="Con tu cuenta reservás, pagás y gestionás tus estadías.">
      <form onSubmit={enviar} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Campo etiqueta="Nombre" value={datos.nombre} onChange={set('nombre')} error={c.nombre} required autoComplete="given-name" />
          <Campo etiqueta="Apellido" value={datos.apellido} onChange={set('apellido')} error={c.apellido} required autoComplete="family-name" />
        </div>
        <Campo etiqueta="Email" type="email" value={datos.email} onChange={set('email')} error={c.email} required autoComplete="email" />
        <Campo etiqueta="Contraseña" type="password" value={datos.password} onChange={set('password')} error={c.password} required minLength={8} autoComplete="new-password" placeholder="Al menos 8 caracteres" />
        <div className="grid grid-cols-2 gap-3">
          <Campo etiqueta="Teléfono (opcional)" value={datos.telefono} onChange={set('telefono')} error={c.telefono} autoComplete="tel" />
          <Campo etiqueta="DNI (opcional)" value={datos.documento} onChange={set('documento')} error={c.documento} />
        </div>
        {registro.error && !Object.keys(c).length && <CajaError error={registro.error} />}
        <button className="boton-primario w-full" disabled={registro.isPending}>{registro.isPending ? 'Creando la cuenta…' : 'Crear cuenta'}</button>
      </form>
      <p className="mt-5 text-sm text-pizarra">¿Ya tenés cuenta? <Link to={`/ingresar${params.toString() ? `?${params}` : ''}`} className="boton-texto">Ingresá</Link></p>
    </Tarjeta>
  )
}

export function Recuperar() {
  const [email, setEmail] = useState('')
  const recuperar = useMutation({ mutationFn: () => api<void>('/auth/recuperar', { method: 'POST', json: { email } }) })
  const enviar = (e: FormEvent) => { e.preventDefault(); recuperar.mutate() }
  return (
    <Tarjeta titulo="Recuperar la contraseña" bajada="Te mandamos un enlace para elegir una nueva.">
      {recuperar.isSuccess ? (
        <p>Si {email} tiene una cuenta, en unos minutos te llega el enlace. Vence en una hora.</p>
      ) : (
        <form onSubmit={enviar} className="space-y-4">
          <Campo etiqueta="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
          {recuperar.error && <CajaError error={recuperar.error} />}
          <button className="boton-primario w-full" disabled={recuperar.isPending}>Enviar enlace</button>
        </form>
      )}
    </Tarjeta>
  )
}

export function Restablecer() {
  const [params] = useSearchParams()
  const navegar = useNavigate()
  const avisar = useAvisos()
  const [password, setPassword] = useState('')
  const restablecer = useMutation({
    mutationFn: () => api<void>('/auth/restablecer', { method: 'POST', json: { token: params.get('token'), password } }),
    onSuccess: () => { avisar('Contraseña actualizada. Ya podés ingresar.'); navegar('/ingresar') },
  })
  const enviar = (e: FormEvent) => { e.preventDefault(); restablecer.mutate() }
  return (
    <Tarjeta titulo="Elegí una contraseña nueva">
      <form onSubmit={enviar} className="space-y-4">
        <Campo etiqueta="Contraseña nueva" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} autoComplete="new-password" />
        {restablecer.error && <CajaError error={restablecer.error} />}
        <button className="boton-primario w-full" disabled={restablecer.isPending || !params.get('token')}>Guardar contraseña</button>
      </form>
    </Tarjeta>
  )
}

export function MiCuenta() {
  const { usuario, actualizarUsuario } = useAuth()
  const avisar = useAvisos()
  const [datos, setDatos] = useState({
    nombre: usuario?.nombre ?? '', apellido: usuario?.apellido ?? '', telefono: usuario?.telefono ?? '', documento: usuario?.documento ?? '',
  })
  const set = (k: keyof typeof datos) => (e: React.ChangeEvent<HTMLInputElement>) => setDatos({ ...datos, [k]: e.target.value })
  const guardar = useMutation({
    mutationFn: () => api<Usuario>('/auth/me', { method: 'PUT', json: { ...datos, telefono: datos.telefono || null, documento: datos.documento || null } }),
    onSuccess: (u) => { actualizarUsuario(u); avisar('Datos guardados.') },
  })
  const c = camposDe(guardar.error)
  const enviar = (e: FormEvent) => { e.preventDefault(); guardar.mutate() }
  return (
    <Tarjeta titulo="Mi cuenta" bajada={usuario?.email}>
      <form onSubmit={enviar} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Campo etiqueta="Nombre" value={datos.nombre} onChange={set('nombre')} error={c.nombre} required />
          <Campo etiqueta="Apellido" value={datos.apellido} onChange={set('apellido')} error={c.apellido} required />
        </div>
        <Campo etiqueta="Teléfono" value={datos.telefono} onChange={set('telefono')} error={c.telefono} />
        <Campo etiqueta="DNI" value={datos.documento} onChange={set('documento')} error={c.documento} />
        {guardar.error && !Object.keys(c).length && <CajaError error={guardar.error} />}
        <button className="boton-primario w-full" disabled={guardar.isPending}>Guardar cambios</button>
      </form>
    </Tarjeta>
  )
}
