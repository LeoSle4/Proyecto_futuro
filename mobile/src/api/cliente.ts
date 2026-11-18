import * as SecureStore from 'expo-secure-store'
import type { EnvioItem } from '../domain/ldp'
import { contactosDemo, cuestionariosDemo } from './demo'
import type { Contacto, Cuestionario, Solicitud } from './tipos'

export class ErrorApi extends Error {
  readonly estado: number
  constructor(estado: number, mensaje: string) {
    super(mensaje)
    this.estado = estado
  }
}

export const MODO_DEMO = process.env.EXPO_PUBLIC_DEMO === '1'
// On a real phone "localhost" is the phone itself: set EXPO_PUBLIC_API_URL to your computer's address.
// The backend listens on 8090 (server.port in application.properties).
const BASE = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8090'
const CLAVE_REFRESH = 'kala.refresh'

let accessToken: string | null = null

interface Sesion {
  accessToken: string
  refreshToken: string
}

async function pedir<T>(ruta: string, init: RequestInit = {}, conAuth = true): Promise<T> {
  const cabeceras: Record<string, string> = { 'Content-Type': 'application/json' }
  if (conAuth && accessToken) cabeceras.Authorization = `Bearer ${accessToken}`
  let respuesta: Response
  try {
    respuesta = await fetch(`${BASE}${ruta}`, { ...init, headers: cabeceras })
  } catch {
    throw new ErrorApi(0, 'No hay conexión. Inténtalo de nuevo en un momento.')
  }
  if (!respuesta.ok) throw new ErrorApi(respuesta.status, mensajePara(respuesta.status))
  return respuesta.status === 204 || respuesta.status === 202 ? (undefined as T) : ((await respuesta.json()) as T)
}

function mensajePara(estado: number): string {
  if (estado === 400) return 'Revisa los datos e inténtalo de nuevo.'
  if (estado === 401) return 'Tu sesión terminó. Inicia sesión de nuevo.'
  if (estado === 403) return 'Tu cuenta no puede hacer esto.'
  if (estado === 409) return 'Ya respondiste esta pregunta.'
  if (estado === 422) return 'Este cuestionario cambió. Vuelve a abrirlo para continuar.'
  if (estado === 429) return 'Demasiados intentos. Espera unos minutos.'
  return 'Algo salió mal de nuestro lado. Inténtalo de nuevo.'
}

async function guardar(sesion: Sesion | null) {
  accessToken = sesion?.accessToken ?? null
  if (sesion) await SecureStore.setItemAsync(CLAVE_REFRESH, sesion.refreshToken)
  else await SecureStore.deleteItemAsync(CLAVE_REFRESH)
}

// One refresh at a time: the backend rotates the token and treats reuse as theft.
let refrescando: Promise<boolean> | null = null

async function refrescar(): Promise<boolean> {
  const refreshToken = await SecureStore.getItemAsync(CLAVE_REFRESH)
  if (!refreshToken) return false
  refrescando ??= pedir<Sesion>('/api/auth/refresh', { method: 'POST', body: JSON.stringify({ refreshToken }) }, false)
    .then(async (s) => {
      await guardar(s)
      return true
    })
    .catch(async () => {
      await guardar(null)
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
  async iniciarSesion(username: string, password: string): Promise<void> {
    if (MODO_DEMO) return
    const sesion = await pedir<Sesion>('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }, false)
    const perfil = await (async () => {
      accessToken = sesion.accessToken
      return pedir<{ roles: string[] }>('/api/auth/me')
    })()
    if (!perfil.roles.includes('EMPLEADO')) {
      accessToken = null
      throw new ErrorApi(403, 'Esta app es para empleados. Usa el panel web de RRHH.')
    }
    await guardar(sesion)
  },
  async restaurarSesion(): Promise<boolean> {
    return MODO_DEMO ? true : refrescar()
  },
  async cerrarSesion(): Promise<void> {
    const refreshToken = await SecureStore.getItemAsync(CLAVE_REFRESH)
    await guardar(null)
    if (MODO_DEMO || !refreshToken) return
    try {
      await pedir<void>('/api/auth/logout', { method: 'POST', body: JSON.stringify({ refreshToken }) }, false)
    } catch {
      /* the local session is gone; the server expires its own */
    }
  },
  pendientes(): Promise<Cuestionario[]> {
    return MODO_DEMO ? Promise.resolve(cuestionariosDemo) : conRenovacion<Cuestionario[]>('/api/cuestionarios/pendientes')
  },
  async enviar(envio: EnvioItem): Promise<void> {
    if (MODO_DEMO) return
    await conRenovacion<void>('/api/checkin', { method: 'POST', body: JSON.stringify(envio) })
  },
  // The server revokes every session on success, so the caller must sign out afterwards.
  async cambiarClave(claveActual: string, claveNueva: string): Promise<void> {
    if (MODO_DEMO) return
    await conRenovacion<void>('/api/usuarios/me/clave', { method: 'PUT', body: JSON.stringify({ claveActual, claveNueva }) })
  },
  // Unlike a check-in, this message is deliberate and reaches the wellbeing coordinator with the person's identity.
  async pedirAyuda(mensaje: string): Promise<Solicitud> {
    if (MODO_DEMO) return { id: 1, estado: 'PENDIENTE', mensaje: mensaje.trim() || null }
    const texto = mensaje.trim()
    return conRenovacion<Solicitud>('/api/ayuda/solicitudes', { method: 'POST', body: JSON.stringify({ mensaje: texto === '' ? null : texto }) })
  },
  misSolicitudes(): Promise<Solicitud[]> {
    return MODO_DEMO ? Promise.resolve([]) : conRenovacion<Solicitud[]>('/api/ayuda/solicitudes/mias')
  },
  contactos(): Promise<Contacto[]> {
    return MODO_DEMO ? Promise.resolve(contactosDemo) : conRenovacion<Contacto[]>('/api/ayuda/contactos')
  },
}
