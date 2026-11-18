import { describe, expect, it } from 'vitest'
import { diasDesde, etiquetaEstado, fechaCorta, ordenarSolicitudes, textoEspera, validarContacto } from './ayuda'

const valido = { nombre: 'Dra. Ríos', tipo: 'PSICOLOGO', telefono: '987654321', email: '' }

describe('validarContacto', () => {
  it('acepta un contacto con teléfono y sin correo', () => {
    expect(validarContacto(valido)).toEqual({})
  })
  it('acepta un contacto solo con correo', () => {
    expect(validarContacto({ ...valido, telefono: '', email: 'ayuda@empresa.pe' })).toEqual({})
  })
  it('exige nombre y un tipo conocido', () => {
    const e = validarContacto({ nombre: ' ', tipo: 'OTRA_COSA', telefono: '1', email: '' })
    expect(e.nombre).toBeDefined()
    expect(e.tipo).toBeDefined()
  })
  it('exige al menos un teléfono o un correo, y lo dice en los dos campos', () => {
    const e = validarContacto({ ...valido, telefono: '  ', email: '' })
    expect(e.telefono).toBeDefined()
    expect(e.email).toBeDefined()
  })
  it('rechaza un correo mal formado y un teléfono de más de 20 caracteres', () => {
    expect(validarContacto({ ...valido, telefono: '', email: 'sin-arroba' }).email).toBeDefined()
    expect(validarContacto({ ...valido, telefono: '1'.repeat(21) }).telefono).toBeDefined()
  })
  it('limita el nombre y el correo a 150 caracteres', () => {
    expect(validarContacto({ ...valido, nombre: 'a'.repeat(151) }).nombre).toBeDefined()
    expect(validarContacto({ ...valido, telefono: '', email: `${'a'.repeat(146)}@x.pe` }).email).toBeDefined()
  })
})

describe('ordenarSolicitudes', () => {
  const s = (id: number, estado: string, creadoEn: string) => ({ id, estado, creadoEn, usuarioId: id, contactoId: null, mensaje: null, atendidoPor: null, atendidoEn: null })
  it('pone primero las pendientes, luego las que están en atención y al final las cerradas', () => {
    const r = ordenarSolicitudes([s(1, 'CERRADA', '2026-11-01T10:00:00'), s(2, 'EN_ATENCION', '2026-11-02T10:00:00'), s(3, 'PENDIENTE', '2026-11-03T10:00:00')])
    expect(r.map((x) => x.id)).toEqual([3, 2, 1])
  })
  it('dentro de un mismo estado deja arriba a la más antigua, que lleva más tiempo esperando', () => {
    const r = ordenarSolicitudes([s(1, 'PENDIENTE', '2026-11-05T10:00:00'), s(2, 'PENDIENTE', '2026-11-01T10:00:00')])
    expect(r.map((x) => x.id)).toEqual([2, 1])
  })
  it('no modifica la lista original', () => {
    const original = [s(1, 'CERRADA', '2026-11-01T10:00:00'), s(2, 'PENDIENTE', '2026-11-02T10:00:00')]
    ordenarSolicitudes(original)
    expect(original.map((x) => x.id)).toEqual([1, 2])
  })
})

describe('etiquetaEstado', () => {
  it('dice cada estado en palabras sencillas', () => {
    expect(etiquetaEstado('PENDIENTE')).toBe('Esperando')
    expect(etiquetaEstado('EN_ATENCION')).toBe('En atención')
    expect(etiquetaEstado('CERRADA')).toBe('Cerrada')
    expect(etiquetaEstado('RARA')).toBe('RARA')
  })
})

describe('diasDesde y textoEspera', () => {
  const ahora = new Date('2026-11-18T12:00:00').getTime()
  it('cuenta días completos', () => {
    expect(diasDesde('2026-11-18T09:00:00', ahora)).toBe(0)
    expect(diasDesde('2026-11-17T09:00:00', ahora)).toBe(1)
    expect(diasDesde('2026-11-11T09:00:00', ahora)).toBe(7)
  })
  it('nunca devuelve negativos si la fecha es del futuro', () => {
    expect(diasDesde('2026-11-30T09:00:00', ahora)).toBe(0)
  })
  it('dice la espera en palabras sin dramatizar', () => {
    expect(textoEspera('2026-11-18T09:00:00', ahora)).toBe('llegó hoy')
    expect(textoEspera('2026-11-17T09:00:00', ahora)).toBe('lleva 1 día esperando')
    expect(textoEspera('2026-11-14T09:00:00', ahora)).toBe('lleva 4 días esperando')
  })
})

describe('fechaCorta', () => {
  it('da el día y el mes en español', () => {
    expect(fechaCorta('2026-11-16T09:30:00')).toBe('16 nov')
    expect(fechaCorta('2026-01-05T09:30:00')).toBe('5 ene')
  })
})
