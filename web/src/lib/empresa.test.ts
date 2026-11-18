import { describe, expect, it } from 'vitest'
import { validarEmpresa, validarNuevoPersonal } from './empresa'

describe('validarEmpresa', () => {
  const valida = { ruc: '20123456789', nombre: 'Empresa Demo', rubro: 'Servicios' }
  it('acepta una empresa completa', () => {
    expect(validarEmpresa(valida)).toEqual({})
  })
  it('exige un RUC de exactamente 11 dígitos', () => {
    expect(validarEmpresa({ ...valida, ruc: '1234567890' }).ruc).toBeDefined()
    expect(validarEmpresa({ ...valida, ruc: '123456789012' }).ruc).toBeDefined()
    expect(validarEmpresa({ ...valida, ruc: '2012345678a' }).ruc).toBeDefined()
    expect(validarEmpresa({ ...valida, ruc: ' 20123456789 ' }).ruc).toBeUndefined()
  })
  it('exige nombre y rubro', () => {
    const e = validarEmpresa({ ruc: '20123456789', nombre: ' ', rubro: '' })
    expect(e.nombre).toBeDefined()
    expect(e.rubro).toBeDefined()
  })
})

describe('validarNuevoPersonal', () => {
  const valido = { nombre: 'Ana Ríos', email: 'ana@empresa.pe', clave: 'una-clave-larga-1', rol: 'RRHH' }
  it('acepta RRHH y coordinación de bienestar', () => {
    expect(validarNuevoPersonal(valido)).toEqual({})
    expect(validarNuevoPersonal({ ...valido, rol: 'COORDINADOR_BIENESTAR' })).toEqual({})
  })
  it('rechaza un rol que no es de personal de empresa, como el de empleado o el de administración', () => {
    expect(validarNuevoPersonal({ ...valido, rol: 'EMPLEADO' }).rol).toBeDefined()
    expect(validarNuevoPersonal({ ...valido, rol: 'ADMIN' }).rol).toBeDefined()
    expect(validarNuevoPersonal({ ...valido, rol: '' }).rol).toBeDefined()
  })
  it('aplica las mismas reglas de nombre, correo y clave que el alta de un empleado', () => {
    const e = validarNuevoPersonal({ nombre: '', email: 'sin-arroba', clave: 'corta', rol: 'RRHH' })
    expect(e.nombre).toBeDefined()
    expect(e.email).toBeDefined()
    expect(e.clave).toBeDefined()
  })
})
