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

describe('cuestionarios y presupuesto', () => {
  it('pide el catálogo con sus estados', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 200, cuerpo: [{ id: 1, codigo: 'PULSO', nombre: 'Pulso', cadenciaDias: 7, itemsPorEnvio: null, activo: true }] }))
    const lista = await api.cuestionarios(4)
    expect(llamadas[0].ruta).toBe('/api/empresas/4/cuestionarios?incluirInactivos=true')
    expect(lista[0]).toMatchObject({ codigo: 'PULSO', activo: true })
  })

  it('activa y desactiva por POST', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 200, cuerpo: { empresaId: 4, cuestionarioId: 2, epsilonMensual: 7, epsilonMaximo: 20 } }))
    await api.activarCuestionario(4, 2)
    await api.desactivarCuestionario(4, 2)
    expect(llamadas.map((l) => `${l.metodo} ${l.ruta}`)).toEqual(['POST /api/empresas/4/cuestionarios/2/activar', 'POST /api/empresas/4/cuestionarios/2/desactivar'])
  })

  it('explica el 422 de activar como falta de presupuesto', async () => {
    await conSesion()
    simular(() => ({ status: 422 }))
    await expect(api.activarCuestionario(4, 2)).rejects.toMatchObject({ estado: 422, message: expect.stringContaining('presupuesto') })
  })

  it('lee el presupuesto y lo fija por PUT con el campo del backend', async () => {
    await conSesion()
    const llamadas = simular((_r, init) => ({ status: 200, cuerpo: { empresaId: 4, periodo: '2026-11', epsilonMaximo: init.method === 'PUT' ? 30 : 20, epsilonMensual: 7 } }))
    await expect(api.presupuesto(4)).resolves.toMatchObject({ epsilonMaximo: 20, epsilonMensual: 7 })
    await api.fijarPresupuesto(4, 30)
    expect(llamadas[1].metodo).toBe('PUT')
    expect(llamadas[1].ruta).toBe('/api/empresas/4/presupuesto')
    expect(JSON.parse(llamadas[1].cuerpo ?? '{}')).toEqual({ epsilonMaximoPersona: 30 })
  })

  it('explica el 422 de fijar el presupuesto como un máximo menor al uso actual', async () => {
    await conSesion()
    simular(() => ({ status: 422 }))
    await expect(api.fijarPresupuesto(4, 1)).rejects.toMatchObject({ message: expect.stringContaining('usan') })
  })

  it('lista las empresas para que el administrador elija una', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 200, cuerpo: [{ id: 4, nombre: 'Empresa Demo' }] }))
    await expect(api.empresas()).resolves.toEqual([{ id: 4, nombre: 'Empresa Demo' }])
    expect(llamadas[0].ruta).toBe('/api/empresas')
  })
})

describe('ayuda', () => {
  const contacto = { id: 3, nombre: 'Dra. Ríos', tipo: 'PSICOLOGO', telefono: '987654321', email: null, activo: true }
  const solicitud = { id: 7, usuarioId: 12, contactoId: null, mensaje: 'Hola', estado: 'PENDIENTE', creadoEn: '2026-11-01T10:00:00', atendidoPor: null, atendidoEn: null }

  it('lista los contactos de ayuda de la empresa', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 200, cuerpo: [contacto] }))
    await expect(api.contactosAyuda()).resolves.toEqual([contacto])
    expect(llamadas[0].ruta).toBe('/api/ayuda/contactos')
  })

  it('crea un contacto con teléfono y correo vacío como nulo', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 201, cuerpo: contacto }))
    await api.crearContactoAyuda({ nombre: 'Dra. Ríos', tipo: 'PSICOLOGO', telefono: '987654321', email: '' })
    expect(llamadas[0].metodo).toBe('POST')
    expect(JSON.parse(llamadas[0].cuerpo ?? '{}')).toEqual({ nombre: 'Dra. Ríos', tipo: 'PSICOLOGO', telefono: '987654321', email: null })
  })

  it('desactiva un contacto por PATCH', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 204 }))
    await api.desactivarContactoAyuda(3)
    expect(`${llamadas[0].metodo} ${llamadas[0].ruta}`).toBe('PATCH /api/ayuda/contactos/3/desactivar')
  })

  it('lista las solicitudes de ayuda', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 200, cuerpo: [solicitud] }))
    await expect(api.solicitudesAyuda()).resolves.toEqual([solicitud])
    expect(llamadas[0].ruta).toBe('/api/ayuda/solicitudes')
  })

  it('atiende una solicitud derivándola a un contacto, o sin derivar', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 200, cuerpo: { ...solicitud, estado: 'EN_ATENCION' } }))
    await api.atenderSolicitud(7, 3)
    await api.atenderSolicitud(7, null)
    expect(llamadas[0].metodo).toBe('PATCH')
    expect(llamadas[0].ruta).toBe('/api/ayuda/solicitudes/7/atender')
    expect(JSON.parse(llamadas[0].cuerpo ?? '{}')).toEqual({ contactoId: 3 })
    expect(JSON.parse(llamadas[1].cuerpo ?? '{}')).toEqual({ contactoId: null })
  })

  it('cierra una solicitud y explica el 409 de una que ya no está pendiente', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 200, cuerpo: { ...solicitud, estado: 'CERRADA' } }))
    await api.cerrarSolicitud(7)
    expect(`${llamadas[0].metodo} ${llamadas[0].ruta}`).toBe('PATCH /api/ayuda/solicitudes/7/cerrar')
    simular(() => ({ status: 409 }))
    await expect(api.atenderSolicitud(7, null)).rejects.toMatchObject({ estado: 409, message: expect.stringContaining('ya') })
  })
})

describe('administración: empresas y personal', () => {
  const empresa = { id: 4, ruc: '20123456789', nombre: 'Empresa Demo', rubro: 'Servicios' }

  it('lista las empresas', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 200, cuerpo: [empresa] }))
    await expect(api.empresas()).resolves.toEqual([empresa])
    expect(llamadas[0].ruta).toBe('/api/empresas')
  })

  it('crea una empresa por POST con RUC, nombre y rubro recortados', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 201, cuerpo: empresa }))
    await api.crearEmpresa({ ruc: ' 20123456789 ', nombre: ' Empresa Demo ', rubro: ' Servicios ' })
    expect(llamadas[0].metodo).toBe('POST')
    expect(llamadas[0].ruta).toBe('/api/empresas')
    expect(JSON.parse(llamadas[0].cuerpo ?? '{}')).toEqual({ ruc: '20123456789', nombre: 'Empresa Demo', rubro: 'Servicios' })
  })

  it('edita por PUT y elimina por DELETE', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 200, cuerpo: empresa }))
    await api.editarEmpresa(4, { ruc: '20123456789', nombre: 'Otra', rubro: 'Servicios' })
    simular(() => ({ status: 204 }))
    await api.eliminarEmpresa(4)
    expect(`${llamadas[0].metodo} ${llamadas[0].ruta}`).toBe('PUT /api/empresas/4')
  })

  it('explica el 409 de eliminar como una empresa que todavía tiene equipos', async () => {
    await conSesion()
    simular(() => ({ status: 409 }))
    await expect(api.eliminarEmpresa(4)).rejects.toMatchObject({ estado: 409, message: expect.stringContaining('equipos') })
  })

  it('da de alta a una persona de RRHH o coordinación en una empresa', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 201, cuerpo: { id: 9, email: 'ana@empresa.pe', nombre: 'Ana' } }))
    await api.crearPersonal(4, { email: 'ana@empresa.pe', nombre: 'Ana', clave: 'una-clave-larga-1', rol: 'RRHH' })
    expect(llamadas[0].metodo).toBe('POST')
    expect(llamadas[0].ruta).toBe('/api/admin/empresas/4/personal')
    expect(JSON.parse(llamadas[0].cuerpo ?? '{}')).toEqual({ email: 'ana@empresa.pe', nombre: 'Ana', clave: 'una-clave-larga-1', rol: 'RRHH' })
  })

  it('explica el 409 del alta de personal como un correo repetido', async () => {
    await conSesion()
    simular(() => ({ status: 409 }))
    await expect(api.crearPersonal(4, { email: 'ana@empresa.pe', nombre: 'Ana', clave: 'una-clave-larga-1', rol: 'RRHH' })).rejects.toMatchObject({ message: expect.stringContaining('correo') })
  })

  it('cambia la propia clave con la actual y la nueva, y no manda nada más', async () => {
    await conSesion()
    const llamadas = simular(() => ({ status: 204 }))
    await api.cambiarClave('actual-larga-123', 'nueva-larga-12345')
    expect(llamadas[0].metodo).toBe('PUT')
    expect(llamadas[0].ruta).toBe('/api/usuarios/me/clave')
    expect(JSON.parse(llamadas[0].cuerpo ?? '{}')).toEqual({ claveActual: 'actual-larga-123', claveNueva: 'nueva-larga-12345' })
  })
})
