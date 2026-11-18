import { describe, expect, it } from 'vitest'
import { validarCambioClave } from './clave'

const valido = { actual: 'actual-larga-123', nueva: 'nueva-larga-12345', repetida: 'nueva-larga-12345' }

describe('validarCambioClave', () => {
  it('acepta un cambio correcto', () => {
    expect(validarCambioClave(valido)).toEqual({})
  })
  it('exige la contraseña actual', () => {
    expect(validarCambioClave({ ...valido, actual: '' }).actual).toBeDefined()
  })
  it('exige una nueva de al menos 12 caracteres', () => {
    expect(validarCambioClave({ ...valido, nueva: 'corta-1234', repetida: 'corta-1234' }).nueva).toBeDefined()
  })
  it('rechaza una nueva de más de 72 bytes aunque tenga pocos caracteres', () => {
    const larga = 'é'.repeat(40)
    expect(validarCambioClave({ ...valido, nueva: larga, repetida: larga }).nueva).toBeDefined()
  })
  it('exige que las dos nuevas coincidan', () => {
    expect(validarCambioClave({ ...valido, repetida: 'otra-distinta-123' }).repetida).toBeDefined()
  })
  it('rechaza una nueva igual a la actual, que es lo que el servidor también rechaza', () => {
    expect(validarCambioClave({ actual: 'misma-clave-1234', nueva: 'misma-clave-1234', repetida: 'misma-clave-1234' }).nueva).toBeDefined()
  })
})
