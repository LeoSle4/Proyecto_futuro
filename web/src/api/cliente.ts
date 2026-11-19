import type { ContactoAyuda, Cuestionario, Dashboard, Empleado, Empresa, Equipo, NuevoEmpleadoSolicitud, Perfil, Presupuesto, Rol, Sesion, Solicitud } from './tipos'
import { demo } from './demo'

export class ErrorApi extends Error {
  readonly estado: number
  constructor(estado: number, mensaje: string) {
    super(mensaje)
    this.estado = estado
  }
}

const USAR_DEMO = import.meta.env.VITE_DEMO === '1'
const CLAVE_REFRESH = 'kala.refresh'

// The access token lives only in memory; the refresh token survives a reload in sessionStorage.
let accessToken: string | null = null

function leerRefresh(): string | null {
  try {
    return sessionStorage.getItem(CLAVE_REFRESH)
  } catch {
    return null
  }
}

function guardarRefresh(valor: string | null) {
  try {
    if (valor) sessionStorage.setItem(CLAVE_REFRESH, valor)
    else sessionStorage.removeItem(CLAVE_REFRESH)
  } catch {
    /* storage unavailable: the session just won't survive a reload */
  }
}

function guardar(sesion: Sesion | null) {
  accessToken = sesion?.accessToken ?? null
  guardarRefresh(sesion?.refreshToken ?? null)
}

async function pedir<T>(ruta: string, init: RequestInit = {}, conAuth = true): Promise<T> {
  const cabeceras = new Headers(init.headers)
  if (init.body) cabeceras.set('Content-Type', 'application/json')
  if (conAuth && accessToken) cabeceras.set('Authorization', `Bearer ${accessToken}`)
  let respuesta: Response
  try {
    respuesta = await fetch(ruta, { ...init, headers: cabeceras })
  } catch {
    throw new ErrorApi(0, 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.')
  }
  if (!respuesta.ok) {
    let detalle: string | null = null
    try {
      const cuerpo = await respuesta.json() as { mensaje?: unknown }
      if (typeof cuerpo.mensaje === 'string' && cuerpo.mensaje.trim()) detalle = cuerpo.mensaje
    } catch {
      /* Some responses have no JSON body. */
    }
    throw new ErrorApi(respuesta.status, detalle ?? mensajePara(respuesta.status))
  }
  return respuesta.status === 204 ? (undefined as T) : ((await respuesta.json()) as T)
}

function mensajePara(estado: number): string {
  if (estado === 401) return 'Tu sesión terminó. Inicia sesión de nuevo.'
  if (estado === 403) return 'Tu cuenta no tiene permiso para ver esto.'
  if (estado === 400) return 'Revisa los datos e inténtalo de nuevo.'
  if (estado === 404) return 'No encontramos ese equipo en tu empresa.'
  if (estado === 409) return 'Ese cambio choca con datos que ya existen (por ejemplo, un equipo que todavía tiene empleados).'
  if (estado === 429) return 'Demasiados intentos. Espera unos minutos y vuelve a intentarlo.'
  return 'Algo salió mal de nuestro lado. Inténtalo de nuevo en un momento.'
}

// One in-flight refresh at a time: the backend rotates the token and treats reuse as theft.
let refrescando: Promise<boolean> | null = null

async function refrescar(): Promise<boolean> {
  const refreshToken = leerRefresh()
  if (!refreshToken) return false
  refrescando ??= pedir<Sesion>('/api/auth/refresh', { method: 'POST', body: JSON.stringify({ refreshToken }) }, false)
    .then((sesion) => {
      guardar(sesion)
      return true
    })
    .catch(() => {
      guardar(null)
      return false
    })
    .finally(() => {
      refrescando = null
    })
  return refrescando
}

async function conRenovacion<T>(ruta: string, init?: RequestInit): Promise<T> {
  try {
    return await pedir<T>(ruta, init)
  } catch (e) {
    if (e instanceof ErrorApi && e.estado === 401 && (await refrescar())) return pedir<T>(ruta, init)
    throw e
  }
}

// The backend sends Spring authorities ("ROLE_RRHH"); the panel works with the bare role name.
async function pedirPerfil(): Promise<Perfil> {
  const perfil = await pedir<Perfil>('/api/auth/me')
  return { ...perfil, roles: perfil.roles.map((r) => r.replace(/^ROLE_/, '') as Rol) }
}

export const api = {
  async iniciarSesion(username: string, password: string): Promise<Perfil> {
    if (USAR_DEMO) return demo.perfil
    const sesion = await pedir<Sesion>('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }, false)
    guardar(sesion)
    return pedirPerfil()
  },
  async restaurarSesion(): Promise<Perfil | null> {
    if (USAR_DEMO) return demo.perfil
    if (!leerRefresh() || !(await refrescar())) return null
    return pedirPerfil()
  },
  async cerrarSesion(): Promise<void> {
    const refreshToken = leerRefresh()
    guardar(null)
    if (USAR_DEMO || !refreshToken) return
    try {
      await pedir<void>('/api/auth/logout', { method: 'POST', body: JSON.stringify({ refreshToken }) }, false)
    } catch {
      /* the local session is already gone; the server expires it on its own */
    }
  },
  equipos(empresaId?: number): Promise<Equipo[]> {
    if (USAR_DEMO) return Promise.resolve([...demo.equipos].filter((equipo) => empresaId === undefined || equipo.empresaId === empresaId))
    const consulta = empresaId === undefined ? '' : `?empresaId=${empresaId}`
    return conRenovacion<Equipo[]>(`/api/equipos${consulta}`)
  },
  async crearEquipo(nombre: string, empresaId: number): Promise<Equipo> {
    if (USAR_DEMO) return demo.crearEquipo(nombre, empresaId)
    return conRenovacion<Equipo>('/api/equipos', { method: 'POST', body: JSON.stringify({ nombre, empresaId }) })
  },
  async editarEquipo(id: number, nombre: string, empresaId: number): Promise<Equipo> {
    if (USAR_DEMO) return demo.editarEquipo(id, nombre)
    return conRenovacion<Equipo>(`/api/equipos/${id}`, { method: 'PUT', body: JSON.stringify({ nombre, empresaId }) })
  },
  async eliminarEquipo(id: number): Promise<void> {
    if (USAR_DEMO) return demo.eliminarEquipo(id)
    await conRenovacion<void>(`/api/equipos/${id}`, { method: 'DELETE' })
  },
  empleados(): Promise<Empleado[]> {
    return USAR_DEMO ? Promise.resolve(demo.empleados()) : conRenovacion<Empleado[]>('/api/empleados')
  },
  async crearEmpleado(d: NuevoEmpleadoSolicitud): Promise<void> {
    if (USAR_DEMO) {
      try {
        return demo.crearEmpleado(d)
      } catch (e) {
        if (e instanceof Error && 'estado' in e && e.estado === 409) {
          throw new ErrorApi(409, 'Ese correo ya tiene una cuenta. Usa otro o pídele a esa persona que entre con la suya.')
        }
        throw e
      }
    }
    try {
      await conRenovacion<unknown>('/api/empleados', { method: 'POST', body: JSON.stringify(d) })
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 409) throw new ErrorApi(409, 'Ese correo ya tiene una cuenta. Usa otro o pídele a esa persona que entre con la suya.')
      throw e
    }
  },
  async cambiarEstadoEmpleado(id: number, activo: boolean): Promise<void> {
    if (USAR_DEMO) return demo.cambiarEstadoEmpleado(id, activo)
    await conRenovacion<void>(`/api/usuarios/${id}/${activo ? 'activar' : 'desactivar'}`, { method: 'PATCH' })
  },
  empresas(): Promise<Empresa[]> {
    return USAR_DEMO ? Promise.resolve(demo.empresas()) : conRenovacion<Empresa[]>('/api/empresas')
  },
  async crearEmpresa(d: { ruc: string; nombre: string; rubro: string }): Promise<void> {
    const cuerpo = { ruc: d.ruc.trim(), nombre: d.nombre.trim(), rubro: d.rubro.trim() }
    if (USAR_DEMO) return demo.crearEmpresa(cuerpo)
    try {
      await conRenovacion<unknown>('/api/empresas', { method: 'POST', body: JSON.stringify(cuerpo) })
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 409) throw new ErrorApi(409, 'Ya existe una empresa con esos datos. Revisa el RUC.')
      throw e
    }
  },
  async editarEmpresa(id: number, d: { ruc: string; nombre: string; rubro: string }): Promise<void> {
    const cuerpo = { ruc: d.ruc.trim(), nombre: d.nombre.trim(), rubro: d.rubro.trim() }
    if (USAR_DEMO) return demo.editarEmpresa(id, cuerpo)
    await conRenovacion<unknown>(`/api/empresas/${id}`, { method: 'PUT', body: JSON.stringify(cuerpo) })
  },
  async eliminarEmpresa(id: number): Promise<void> {
    if (USAR_DEMO) return demo.eliminarEmpresa(id)
    try {
      await conRenovacion<void>(`/api/empresas/${id}`, { method: 'DELETE' })
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 409) throw new ErrorApi(409, 'Esta empresa todavía tiene equipos. Elimina primero sus equipos y vuelve a intentarlo.')
      throw e
    }
  },
  async crearPersonal(empresaId: number, d: { email: string; nombre: string; clave: string; rol: string }): Promise<void> {
    const cuerpo = { email: d.email.trim(), nombre: d.nombre.trim(), clave: d.clave, rol: d.rol }
    if (USAR_DEMO) return demo.crearPersonal(empresaId, cuerpo)
    try {
      await conRenovacion<unknown>(`/api/admin/empresas/${empresaId}/personal`, { method: 'POST', body: JSON.stringify(cuerpo) })
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 409) throw new ErrorApi(409, 'Ese correo ya tiene una cuenta. Usa otro.')
      throw e
    }
  },
  // The server revokes every session when the password changes, so the caller must sign out afterwards.
  async cambiarClave(claveActual: string, claveNueva: string): Promise<void> {
    if (USAR_DEMO) return
    try {
      await conRenovacion<void>('/api/usuarios/me/clave', { method: 'PUT', body: JSON.stringify({ claveActual, claveNueva }) })
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 401) throw new ErrorApi(401, 'La contraseña actual no coincide.')
      throw e
    }
  },
  cuestionarios(empresaId: number): Promise<Cuestionario[]> {
    return USAR_DEMO ? Promise.resolve(demo.cuestionarios()) : conRenovacion<Cuestionario[]>(`/api/empresas/${empresaId}/cuestionarios?incluirInactivos=true`)
  },
  async activarCuestionario(empresaId: number, id: number): Promise<void> {
    if (USAR_DEMO) return demo.cambiarCuestionario(id, true)
    try {
      await conRenovacion<unknown>(`/api/empresas/${empresaId}/cuestionarios/${id}/activar`, { method: 'POST' })
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 422) throw new ErrorApi(422, 'Activarlo superaría el presupuesto de privacidad de la empresa. Sube el máximo o desactiva otro cuestionario.')
      throw e
    }
  },
  async desactivarCuestionario(empresaId: number, id: number): Promise<void> {
    if (USAR_DEMO) return demo.cambiarCuestionario(id, false)
    await conRenovacion<unknown>(`/api/empresas/${empresaId}/cuestionarios/${id}/desactivar`, { method: 'POST' })
  },
  presupuesto(empresaId: number): Promise<Presupuesto> {
    return USAR_DEMO ? Promise.resolve(demo.presupuesto()) : conRenovacion<Presupuesto>(`/api/empresas/${empresaId}/presupuesto`)
  },
  async fijarPresupuesto(empresaId: number, epsilonMaximoPersona: number): Promise<Presupuesto> {
    if (USAR_DEMO) return demo.fijarPresupuesto(epsilonMaximoPersona)
    try {
      return await conRenovacion<Presupuesto>(`/api/empresas/${empresaId}/presupuesto`, { method: 'PUT', body: JSON.stringify({ epsilonMaximoPersona }) })
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 422) throw new ErrorApi(422, 'Ese máximo es menor que lo que ya usan los cuestionarios activos. Desactiva alguno o elige un valor más alto.')
      throw e
    }
  },
  contactosAyuda(): Promise<ContactoAyuda[]> {
    return USAR_DEMO ? Promise.resolve(demo.contactos()) : conRenovacion<ContactoAyuda[]>('/api/ayuda/contactos')
  },
  async crearContactoAyuda(d: { nombre: string; tipo: string; telefono: string; email: string }): Promise<void> {
    const cuerpo = { nombre: d.nombre.trim(), tipo: d.tipo, telefono: d.telefono.trim() || null, email: d.email.trim() || null }
    if (USAR_DEMO) return demo.crearContacto(cuerpo)
    await conRenovacion<unknown>('/api/ayuda/contactos', { method: 'POST', body: JSON.stringify(cuerpo) })
  },
  async desactivarContactoAyuda(id: number): Promise<void> {
    if (USAR_DEMO) return demo.desactivarContacto(id)
    await conRenovacion<void>(`/api/ayuda/contactos/${id}/desactivar`, { method: 'PATCH' })
  },
  solicitudesAyuda(): Promise<Solicitud[]> {
    return USAR_DEMO ? Promise.resolve(demo.solicitudes()) : conRenovacion<Solicitud[]>('/api/ayuda/solicitudes')
  },
  async atenderSolicitud(id: number, contactoId: number | null): Promise<void> {
    if (USAR_DEMO) return demo.atender(id, contactoId)
    try {
      await conRenovacion<unknown>(`/api/ayuda/solicitudes/${id}/atender`, { method: 'PATCH', body: JSON.stringify({ contactoId }) })
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 409) throw new ErrorApi(409, 'Esta solicitud ya cambió de estado. Se actualizó la lista para que veas cómo está ahora.')
      throw e
    }
  },
  async cerrarSolicitud(id: number): Promise<void> {
    if (USAR_DEMO) return demo.cerrar(id)
    await conRenovacion<unknown>(`/api/ayuda/solicitudes/${id}/cerrar`, { method: 'PATCH' })
  },
  dashboard(equipoId: number, semana: string): Promise<Dashboard> {
    return USAR_DEMO ? Promise.resolve(demo.dashboard(equipoId, semana)) : conRenovacion<Dashboard>(`/api/dashboard/${equipoId}?semana=${semana}`)
  },
}

export const MODO_DEMO = USAR_DEMO
