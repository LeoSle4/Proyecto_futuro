import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ErrorApi } from './cliente'

type Respuesta = { status: number; cuerpo?: unknown }

function simular(manejador: (ruta: string, init: RequestInit) => Respuesta) {
  const llamadas: { ruta: string; metodo: string; cuerpo: string | undefined; auth: string | null }[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (ruta: string, init: RequestInit = {}) => {
      const cabeceras = new Headers(init.headers)
      llamadas.push({ ruta, metodo: String(init.method ?? 'GET'), cuerpo: init.body as string | undefined, auth: cabeceras.get('Authorization') })
      const r = manejador(ruta, init)
      return { ok: r.status >= 200 && r.status < 300, status: r.status, json: async () => r.cuerpo } as Response
    }),
  )
  return llamadas
}

const sesion = (n: number) => ({ accessToken: `acceso-${n}`, refreshToken: `refresh-${n}` })

beforeEach(() => {
  vi.unstubAllGlobals()
  const almacen = new Map<string, string>()
  vi.stubGlobal('sessionStorage', {
    getItem: (k: string) => almacen.get(k) ?? null,
    setItem: (k: string, v: string) => void almacen.set(k, v),
    removeItem: (k: string) => void almacen.delete(k),
  })
})

async function conSesion() {
  simular((ruta) => (ruta === '/api/auth/login' ? { status: 200, cuerpo: sesion(1) } : { status: 200, cuerpo: { username: 'a', uid: 1, roles: ['RRHH'] } }))
  await api.iniciarSesion('a@b.pe', 'clave')
}

describe('equipos', () => {
  it('crea un equipo con su nombre y la empresa del token por POST', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 201, cuerpo: { id: 9, nombre: 'Soporte', empresaId: 4 } }))
    const creado = await api.crearEquipo('Soporte', 4)
    expect(llamadas[0].metodo).toBe('POST')
    expect(llamadas[0].ruta).toBe('/api/equipos')
    expect(JSON.parse(llamadas[0].cuerpo ?? '{}')).toEqual({ nombre: 'Soporte', empresaId: 4 })
    expect(llamadas[0].auth).toBe('Bearer acceso-1')
    expect(creado).toMatchObject({ id: 9 })
  })

  it('edita un equipo por PUT con el mismo cuerpo', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 200, cuerpo: { id: 9, nombre: 'Atención', empresaId: 4 } }))
    await api.editarEquipo(9, 'Atención', 4)
    expect(llamadas[0].metodo).toBe('PUT')
    expect(llamadas[0].ruta).toBe('/api/equipos/9')
    expect(JSON.parse(llamadas[0].cuerpo ?? '{}')).toEqual({ nombre: 'Atención', empresaId: 4 })
  })

  it('elimina un equipo por DELETE y un 204 no rompe', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 204 }))
    await expect(api.eliminarEquipo(9)).resolves.toBeUndefined()
    expect(llamadas[0].metodo).toBe('DELETE')
    expect(llamadas[0].ruta).toBe('/api/equipos/9')
  })

  it('ante un 401 renueva la sesión una vez y reintenta la escritura', async () => {
    await conSesion()
    let vencido = true
    const llamadas = simular((ruta) => {
      if (ruta === '/api/auth/refresh') return { status: 200, cuerpo: sesion(2) }
      if (vencido) {
        vencido = false
        return { status: 401 }
      }
      return { status: 201, cuerpo: { id: 10, nombre: 'Ventas', empresaId: 4 } }
    })
    await api.crearEquipo('Ventas', 4)
    expect(llamadas.filter((l) => l.ruta === '/api/auth/refresh')).toHaveLength(1)
    expect(llamadas[llamadas.length - 1].auth).toBe('Bearer acceso-2')
  })

  it('traduce un conflicto 409 a un mensaje claro', async () => {
    await conSesion()
    simular(() => ({ status: 409 }))
    await expect(api.eliminarEquipo(9)).rejects.toMatchObject({ estado: 409 })
    await expect(api.eliminarEquipo(9)).rejects.toBeInstanceOf(ErrorApi)
  })
})

describe('empleados', () => {
  const empleado = { id: 5, email: 'ana@empresa.pe', nombre: 'Ana', activo: true, equipoId: 3, equipoNombre: 'Ventas' }

  it('lista los empleados de la empresa por GET', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 200, cuerpo: [empleado] }))
    await expect(api.empleados()).resolves.toEqual([empleado])
    expect(llamadas[0].metodo).toBe('GET')
    expect(llamadas[0].ruta).toBe('/api/empleados')
  })

  it('da de alta a un empleado por POST con equipo, correo, nombre y clave inicial', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 201, cuerpo: { id: 6, email: 'luis@empresa.pe', nombre: 'Luis' } }))
    await api.crearEmpleado({ equipoId: 3, email: 'luis@empresa.pe', nombre: 'Luis', clave: 'una-clave-larga-1' })
    expect(llamadas[0].metodo).toBe('POST')
    expect(llamadas[0].ruta).toBe('/api/empleados')
    expect(JSON.parse(llamadas[0].cuerpo ?? '{}')).toEqual({ equipoId: 3, email: 'luis@empresa.pe', nombre: 'Luis', clave: 'una-clave-larga-1' })
  })

  it('explica el 409 del alta como un correo repetido', async () => {
    await conSesion()
    simular(() => ({ status: 409 }))
    await expect(api.crearEmpleado({ equipoId: 3, email: 'ana@empresa.pe', nombre: 'Ana', clave: 'una-clave-larga-1' })).rejects.toMatchObject({
      estado: 409,
      message: expect.stringContaining('correo'),
    })
  })

  it('desactiva y reactiva una cuenta por PATCH y un 204 no rompe', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 204 }))
    await api.cambiarEstadoEmpleado(5, false)
    await api.cambiarEstadoEmpleado(5, true)
    expect(llamadas.map((l) => `${l.metodo} ${l.ruta}`)).toEqual(['PATCH /api/usuarios/5/desactivar', 'PATCH /api/usuarios/5/activar'])
  })
})
