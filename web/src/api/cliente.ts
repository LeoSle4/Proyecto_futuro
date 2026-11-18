import type { ContactoAyuda, CrearContactoAyuda, Dashboard, Equipo, Perfil, Sesion } from './tipos'
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
  if (estado === 404) return 'No encontramos ese equipo en tu empresa.'
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

async function conRenovacion<T>(ruta: string, init: RequestInit = {}): Promise<T> {
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
    return USAR_DEMO ? Promise.resolve(demo.equipos) : conRenovacion<Equipo[]>('/api/equipos')
  },
  dashboard(equipoId: number, semana: string): Promise<Dashboard> {
    return USAR_DEMO ? Promise.resolve(demo.dashboard(equipoId, semana)) : conRenovacion<Dashboard>(`/api/dashboard/${equipoId}?semana=${semana}`)
  },
  listarContactos(): Promise<ContactoAyuda[]> {
    return USAR_DEMO ? Promise.resolve(demo.listarContactos()) : conRenovacion<ContactoAyuda[]>('/api/ayuda/contactos')
  },
  crearContacto(contacto: CrearContactoAyuda): Promise<ContactoAyuda> {
    return USAR_DEMO ? Promise.resolve(demo.crearContacto(contacto)) : conRenovacion<ContactoAyuda>('/api/ayuda/contactos', { method: 'POST', body: JSON.stringify(contacto) })
  },
  async desactivarContacto(id: number): Promise<void> {
    if (USAR_DEMO) {
      demo.desactivarContacto(id)
      return
    }
    await conRenovacion<void>(`/api/ayuda/contactos/${id}/desactivar`, { method: 'PATCH' })
  },
}

export const MODO_DEMO = USAR_DEMO
