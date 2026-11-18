const mockAlmacen = new Map<string, string>()
jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(async (k: string) => mockAlmacen.get(k) ?? null),
  setItemAsync: jest.fn(async (k: string, v: string) => void mockAlmacen.set(k, v)),
  deleteItemAsync: jest.fn(async (k: string) => void mockAlmacen.delete(k)),
}))

import { api, ErrorApi } from './cliente'

type Respuesta = { status: number; cuerpo?: unknown }

function simular(manejador: (ruta: string, init: RequestInit) => Respuesta) {
  const llamadas: { ruta: string; auth: string | undefined }[] = []
  globalThis.fetch = jest.fn(async (url: string, init: RequestInit = {}) => {
    const ruta = url.replace(/^https?:\/\/[^/]+/, '')
    const auth = (init.headers as Record<string, string>)?.Authorization
    llamadas.push({ ruta, auth })
    const r = manejador(ruta, init)
    return { ok: r.status >= 200 && r.status < 300, status: r.status, json: async () => r.cuerpo } as Response
  }) as unknown as typeof fetch
  return llamadas
}

const sesion = (n: number) => ({ tokenType: 'Bearer', accessToken: `acceso-${n}`, refreshToken: `refresh-${n}` })

beforeEach(() => mockAlmacen.clear())

describe('login', () => {
  it('guarda el refresh token y acepta a un empleado', async () => {
    simular((ruta) => (ruta === '/api/auth/login' ? { status: 200, cuerpo: sesion(1) } : { status: 200, cuerpo: { roles: ['EMPLEADO'] } }))
    await api.iniciarSesion('a@b.pe', 'clave')
    expect(mockAlmacen.get('kala.refresh')).toBe('refresh-1')
  })

  it('rechaza una cuenta que no es de empleado y no guarda nada', async () => {
    simular((ruta) => (ruta === '/api/auth/login' ? { status: 200, cuerpo: sesion(1) } : { status: 200, cuerpo: { roles: ['RRHH'] } }))
    await expect(api.iniciarSesion('a@b.pe', 'clave')).rejects.toMatchObject({ estado: 403 })
    expect(mockAlmacen.has('kala.refresh')).toBe(false)
  })

  it('traduce un 401 a un ErrorApi sin filtrar texto del servidor', async () => {
    simular(() => ({ status: 401 }))
    await expect(api.iniciarSesion('a@b.pe', 'mala')).rejects.toBeInstanceOf(ErrorApi)
  })
})

describe('renovación de sesión', () => {
  async function conSesion() {
    simular((ruta) => (ruta === '/api/auth/login' ? { status: 200, cuerpo: sesion(1) } : { status: 200, cuerpo: { roles: ['EMPLEADO'] } }))
    await api.iniciarSesion('a@b.pe', 'clave')
  }

  it('ante un 401 renueva una vez y reintenta con el token nuevo', async () => {
    await conSesion()
    let vencido = true
    const llamadas = simular((ruta) => {
      if (ruta === '/api/auth/refresh') return { status: 200, cuerpo: sesion(2) }
      if (vencido) {
        vencido = false
        return { status: 401 }
      }
      return { status: 200, cuerpo: [] }
    })
    await expect(api.pendientes()).resolves.toEqual([])
    expect(llamadas.filter((l) => l.ruta === '/api/auth/refresh')).toHaveLength(1)
    expect(llamadas[llamadas.length - 1].auth).toBe('Bearer acceso-2')
    expect(mockAlmacen.get('kala.refresh')).toBe('refresh-2')
  })

  it('con varias peticiones vencidas a la vez renueva una sola vez (el servidor rota el token)', async () => {
    await conSesion()
    let renovado = false
    const llamadas = simular((ruta) => {
      if (ruta === '/api/auth/refresh') {
        renovado = true
        return { status: 200, cuerpo: sesion(2) }
      }
      return renovado ? { status: 200, cuerpo: [] } : { status: 401 }
    })
    await Promise.all([api.pendientes(), api.pendientes(), api.contactos()])
    expect(llamadas.filter((l) => l.ruta === '/api/auth/refresh')).toHaveLength(1)
  })

  it('si la renovación falla, borra la sesión y propaga el 401', async () => {
    await conSesion()
    simular((ruta) => (ruta === '/api/auth/refresh' ? { status: 401 } : { status: 401 }))
    await expect(api.pendientes()).rejects.toMatchObject({ estado: 401 })
    expect(mockAlmacen.has('kala.refresh')).toBe(false)
  })
})

describe('envío del check-in', () => {
  it('manda solo el envío ya perturbado y deja pasar el 409 de "ya respondió"', async () => {
    let cuerpo = ''
    simular((_ruta, init) => {
      cuerpo = String(init.body)
      return { status: 409 }
    })
    await expect(api.enviar({ itemId: 1, configuracionLdpId: 2, respuestaRuidosa: true })).rejects.toMatchObject({ estado: 409 })
    expect(JSON.parse(cuerpo)).toEqual({ itemId: 1, configuracionLdpId: 2, respuestaRuidosa: true })
  })

  it('un 202 sin cuerpo no rompe', async () => {
    simular(() => ({ status: 202 }))
    await expect(api.enviar({ itemId: 1, configuracionLdpId: 2, valorRuidoso: 3.2 })).resolves.toBeUndefined()
  })
})
