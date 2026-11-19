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

  it('acepta a un empleado cuyo rol llega con el prefijo ROLE_ del backend', async () => {
    simular((ruta) => (ruta === '/api/auth/login' ? { status: 200, cuerpo: sesion(1) } : { status: 200, cuerpo: { roles: ['ROLE_EMPLEADO'] } }))
    await expect(api.iniciarSesion('a@b.pe', 'clave')).resolves.toBeUndefined()
    expect(mockAlmacen.get('kala.refresh')).toBe('refresh-1')
  })

  it('sigue rechazando a RRHH aunque su rol llegue con prefijo', async () => {
    simular((ruta) => (ruta === '/api/auth/login' ? { status: 200, cuerpo: sesion(1) } : { status: 200, cuerpo: { roles: ['ROLE_RRHH'] } }))
    await expect(api.iniciarSesion('a@b.pe', 'clave')).rejects.toMatchObject({ estado: 403 })
    expect(mockAlmacen.has('kala.refresh')).toBe(false)
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
  it('manda todas las respuestas ya perturbadas en un solo lote a /api/checkin/lote', async () => {
    let cuerpo = ''
    const llamadas = simular((_ruta, init) => {
      cuerpo = String(init.body)
      return { status: 202 }
    })
    const lote = [
      { itemId: 1, configuracionLdpId: 2, valorRuidoso: 3.2 },
      { itemId: 2, configuracionLdpId: 2, valorRuidoso: 1.4 },
    ]
    await expect(api.enviar(lote)).resolves.toBeUndefined()
    expect(llamadas).toHaveLength(1)
    expect(llamadas[0].ruta).toBe('/api/checkin/lote')
    expect(JSON.parse(cuerpo)).toEqual({ respuestas: lote })
  })

  it('un solo ítem también viaja como lote de uno', async () => {
    let cuerpo = ''
    simular((_ruta, init) => {
      cuerpo = String(init.body)
      return { status: 202 }
    })
    await api.enviar([{ itemId: 1, configuracionLdpId: 2, respuestaRuidosa: true }])
    expect(JSON.parse(cuerpo)).toEqual({ respuestas: [{ itemId: 1, configuracionLdpId: 2, respuestaRuidosa: true }] })
  })

  it('propaga el 409 de "ya respondió" para que la pantalla lo muestre y no lo dé por enviado', async () => {
    simular(() => ({ status: 409 }))
    await expect(api.enviar([{ itemId: 1, configuracionLdpId: 2, respuestaRuidosa: true }])).rejects.toMatchObject({ estado: 409 })
  })
})

describe('cambio de clave', () => {
  it('manda la clave actual y la nueva y un 204 no rompe', async () => {
    let cuerpo = ''
    let metodo = ''
    simular((ruta, init) => {
      if (ruta === '/api/usuarios/me/clave') {
        cuerpo = String(init.body)
        metodo = String(init.method)
      }
      return { status: 204 }
    })
    await expect(api.cambiarClave('clave-actual-1', 'clave-nueva-larga')).resolves.toBeUndefined()
    expect(metodo).toBe('PUT')
    expect(JSON.parse(cuerpo)).toEqual({ claveActual: 'clave-actual-1', claveNueva: 'clave-nueva-larga' })
  })

  it('una clave actual incorrecta (401) llega como ErrorApi 401 aunque la sesión sea válida', async () => {
    mockAlmacen.set('kala.refresh', 'refresh-1')
    simular((ruta) => (ruta === '/api/auth/refresh' ? { status: 200, cuerpo: sesion(2) } : { status: 401 }))
    await expect(api.cambiarClave('mala', 'clave-nueva-larga')).rejects.toMatchObject({ estado: 401 })
  })

  it('un 400 (clave débil) se propaga como ErrorApi 400', async () => {
    simular(() => ({ status: 400 }))
    await expect(api.cambiarClave('actual', 'corta')).rejects.toMatchObject({ estado: 400 })
  })
})

describe('pedir ayuda', () => {
  it('manda solo el mensaje por POST y devuelve la solicitud creada', async () => {
    let cuerpo = ''
    let metodo = ''
    simular((_ruta, init) => {
      cuerpo = String(init.body)
      metodo = String(init.method)
      return { status: 201, cuerpo: { id: 7, estado: 'PENDIENTE', mensaje: 'hola' } }
    })
    const creada = await api.pedirAyuda('hola')
    expect(metodo).toBe('POST')
    expect(JSON.parse(cuerpo)).toEqual({ mensaje: 'hola' })
    expect(creada).toMatchObject({ id: 7, estado: 'PENDIENTE' })
  })

  it('permite pedir ayuda sin mensaje', async () => {
    let cuerpo = ''
    simular((_ruta, init) => {
      cuerpo = String(init.body)
      return { status: 201, cuerpo: { id: 8, estado: 'PENDIENTE', mensaje: null } }
    })
    await api.pedirAyuda('')
    expect(JSON.parse(cuerpo)).toEqual({ mensaje: null })
  })

  it('lista mis solicitudes', async () => {
    simular(() => ({ status: 200, cuerpo: [{ id: 7, estado: 'EN_ATENCION' }] }))
    await expect(api.misSolicitudes()).resolves.toEqual([{ id: 7, estado: 'EN_ATENCION' }])
  })
})
