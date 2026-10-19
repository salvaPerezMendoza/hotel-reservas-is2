import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/cliente'
import type { Rol, Usuario } from '@/api/tipos'
import { useAuth } from '@/auth/AuthContext'
import { Encabezado } from '@/componentes/LayoutPanel'
import { Cargando, CajaError } from '@/componentes/Estados'
import { Modal } from '@/componentes/Modal'
import { useAvisos } from '@/componentes/Avisos'
import { nombreRol } from '@/util/formato'

const roles: Rol[] = ['HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR']

export default function Usuarios() {
  const cliente = useQueryClient()
  const avisar = useAvisos()
  const { usuario: yo } = useAuth()
  const [filtro, setFiltro] = useState<'personal' | 'todos'>('personal')
  const [busqueda, setBusqueda] = useState('')
  const [nuevo, setNuevo] = useState(false)
  const { data, isLoading, error } = useQuery({ queryKey: ['admin-usuarios'], queryFn: () => api<Usuario[]>('/admin/usuarios') })
  const refrescar = () => cliente.invalidateQueries({ queryKey: ['admin-usuarios'] })

  const cambiarRol = useMutation({
    mutationFn: ({ id, rol }: { id: number; rol: Rol }) => api<Usuario>(`/admin/usuarios/${id}/rol`, { method: 'PATCH', json: { rol } }),
    onSuccess: (u) => { refrescar(); avisar(`${u.nombre} ahora es ${nombreRol[u.rol].toLowerCase()}.`) },
    onError: (e: Error) => avisar(e.message, 'error'),
  })
  const activar = useMutation({
    mutationFn: (u: Usuario) => api<Usuario>(`/admin/usuarios/${u.id}/activo?valor=${!u.activo}`, { method: 'PATCH' }),
    onSuccess: (u) => { refrescar(); avisar(u.activo ? `Cuenta de ${u.nombre} reactivada.` : `Cuenta de ${u.nombre} desactivada.`) },
    onError: (e: Error) => avisar(e.message, 'error'),
  })

  const t = busqueda.trim().toLowerCase()
  const visibles = (data ?? [])
    .filter((u) => filtro === 'todos' || u.rol !== 'HUESPED')
    .filter((u) => !t || `${u.nombre} ${u.apellido} ${u.email}`.toLowerCase().includes(t))

  return (
    <>
      <Encabezado titulo="Usuarios"><button className="boton-primario" onClick={() => setNuevo(true)}>Agregar usuario</button></Encabezado>
      <div className="panel">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex rounded-full bg-neblina p-1 text-sm">
            {(['personal', 'todos'] as const).map((f) => (
              <button key={f} className={`rounded-full px-4 py-1.5 ${filtro === f ? 'bg-white font-semibold shadow-sm' : 'text-pizarra'}`} onClick={() => setFiltro(f)}>{f === 'personal' ? 'Personal del hotel' : 'Todos'}</button>
            ))}
          </div>
          <input className="campo max-w-xs" placeholder="Buscar por nombre o email" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
        </div>
        {isLoading && <Cargando />}
        {error && <CajaError error={error} />}
        <div className="mt-4 overflow-x-auto">
          <table className="tabla min-w-[640px]">
            <thead><tr><th>Nombre</th><th>Email</th><th>Rol</th><th>Estado</th><th /></tr></thead>
            <tbody>
              {visibles.map((u) => (
                <tr key={u.id} className={u.activo ? '' : 'text-pizarra'}>
                  <td className="font-medium">{u.nombre} {u.apellido}</td>
                  <td>{u.email}</td>
                  <td>
                    <select className="rounded-lg border border-neblina-300 bg-white px-2 py-1 text-sm" value={u.rol} disabled={u.id === yo?.id} aria-label={`Rol de ${u.nombre}`}
                      onChange={(e) => cambiarRol.mutate({ id: u.id, rol: e.target.value as Rol })}>
                      {roles.map((r) => <option key={r} value={r}>{nombreRol[r]}</option>)}
                    </select>
                  </td>
                  <td>{u.activo ? 'Activa' : 'Desactivada'}</td>
                  <td className="text-right">{u.id !== yo?.id && <button className="boton-texto" onClick={() => activar.mutate(u)}>{u.activo ? 'Desactivar' : 'Reactivar'}</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      {nuevo && <ModalNuevo alCerrar={() => setNuevo(false)} alGuardar={refrescar} />}
    </>
  )
}

function ModalNuevo({ alCerrar, alGuardar }: { alCerrar: () => void; alGuardar: () => void }) {
  const avisar = useAvisos()
  const [d, setD] = useState({ nombre: '', apellido: '', email: '', password: '', rol: 'RECEPCIONISTA' as Rol })
  const crear = useMutation({
    mutationFn: () => api<Usuario>('/admin/usuarios', { method: 'POST', json: d }),
    onSuccess: (u) => { alGuardar(); avisar(`Cuenta de ${u.nombre} creada.`); alCerrar() },
  })
  const enviar = (e: FormEvent) => { e.preventDefault(); crear.mutate() }
  return (
    <Modal titulo="Nuevo usuario" abierto alCerrar={alCerrar}>
      <form onSubmit={enviar} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <label><span className="etiqueta">Nombre</span><input className="campo" value={d.nombre} onChange={(e) => setD({ ...d, nombre: e.target.value })} required /></label>
          <label><span className="etiqueta">Apellido</span><input className="campo" value={d.apellido} onChange={(e) => setD({ ...d, apellido: e.target.value })} required /></label>
        </div>
        <label className="block"><span className="etiqueta">Email</span><input type="email" className="campo" value={d.email} onChange={(e) => setD({ ...d, email: e.target.value })} required /></label>
        <label className="block"><span className="etiqueta">Contraseña inicial</span><input type="text" className="campo" value={d.password} onChange={(e) => setD({ ...d, password: e.target.value })} required minLength={8} autoComplete="off" /></label>
        <label className="block"><span className="etiqueta">Rol</span>
          <select className="campo" value={d.rol} onChange={(e) => setD({ ...d, rol: e.target.value as Rol })}>{roles.map((r) => <option key={r} value={r}>{nombreRol[r]}</option>)}</select>
        </label>
        {crear.error && <CajaError error={crear.error} />}
        <div className="flex justify-end gap-3"><button type="button" className="boton-secundario" onClick={alCerrar}>Volver</button><button className="boton-primario" disabled={crear.isPending}>Crear usuario</button></div>
      </form>
    </Modal>
  )
}
