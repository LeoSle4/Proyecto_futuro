import { describe, expect, it } from 'vitest'
import { validarNuevoEmpleado } from './empleado'

const valido = { nombre: 'Ana Ríos', email: 'ana@empresa.pe', clave: 'una-clave-larga-1', equipoId: 3 }

describe('validarNuevoEmpleado', () => {
  it('acepta un empleado completo y no devuelve errores', () => {
    expect(validarNuevoEmpleado(valido)).toEqual({})
  })
  it('exige nombre, correo y equipo', () => {
    const e = validarNuevoEmpleado({ nombre: '  ', email: '', clave: valido.clave, equipoId: null })
    expect(e.nombre).toBeDefined()
    expect(e.email).toBeDefined()
    expect(e.equipoId).toBeDefined()
  })
  it('rechaza un correo mal formado', () => {
    expect(validarNuevoEmpleado({ ...valido, email: 'ana@empresa' }).email).toBeDefined()
    expect(validarNuevoEmpleado({ ...valido, email: 'ana empresa@x.pe' }).email).toBeDefined()
  })
  it('exige una clave de al menos 12 caracteres', () => {
    expect(validarNuevoEmpleado({ ...valido, clave: 'corta-1234' }).clave).toBeDefined()
    expect(validarNuevoEmpleado({ ...valido, clave: '123456789012' }).clave).toBeUndefined()
  })
  it('rechaza una clave de más de 72 bytes aunque tenga pocos caracteres', () => {
    expect(validarNuevoEmpleado({ ...valido, clave: 'é'.repeat(40) }).clave).toBeDefined() // 80 bytes
    expect(validarNuevoEmpleado({ ...valido, clave: 'a'.repeat(72) }).clave).toBeUndefined()
  })
  it('limita el nombre y el correo a 150 caracteres', () => {
    expect(validarNuevoEmpleado({ ...valido, nombre: 'a'.repeat(151) }).nombre).toBeDefined()
    expect(validarNuevoEmpleado({ ...valido, email: `${'a'.repeat(146)}@x.pe` }).email).toBeDefined()
    expect(validarNuevoEmpleado({ ...valido, email: `${'a'.repeat(145)}@x.pe` }).email).toBeUndefined() // exactamente 150
  })
})
