import type { Cuestionario, Dashboard, Empleado, Empresa, Equipo, NuevoEmpleadoSolicitud, Perfil, Presupuesto, Sesion } from './tipos'
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
    throw new ErrorApi(respuesta.status, mensajePara(respuesta.status))
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

export const api = {
  async iniciarSesion(username: string, password: string): Promise<Perfil> {
    if (USAR_DEMO) return demo.perfil
    const sesion = await pedir<Sesion>('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }, false)
    guardar(sesion)
    return pedir<Perfil>('/api/auth/me')
  },
  async restaurarSesion(): Promise<Perfil | null> {
    if (USAR_DEMO) return demo.perfil
    if (!leerRefresh() || !(await refrescar())) return null
    return pedir<Perfil>('/api/auth/me')
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
  equipos(): Promise<Equipo[]> {
    return USAR_DEMO ? Promise.resolve([...demo.equipos]) : conRenovacion<Equipo[]>('/api/equipos')
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
    if (USAR_DEMO) return demo.crearEmpleado(d)
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
    return USAR_DEMO ? Promise.resolve([{ id: 1, nombre: 'Empresa demo' }]) : conRenovacion<Empresa[]>('/api/empresas')
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
  dashboard(equipoId: number, semana: string): Promise<Dashboard> {
    return USAR_DEMO ? Promise.resolve(demo.dashboard(equipoId, semana)) : conRenovacion<Dashboard>(`/api/dashboard/${equipoId}?semana=${semana}`)
  },
}

export const MODO_DEMO = USAR_DEMO
