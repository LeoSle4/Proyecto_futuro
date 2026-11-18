import { describe, expect, it } from 'vitest'
import { usoDelPresupuesto, validarMaximo } from './presupuesto'

describe('validarMaximo', () => {
  it('acepta un número positivo y lo devuelve como número', () => {
    expect(validarMaximo('20')).toEqual({ valor: 20 })
    expect(validarMaximo('12,5')).toEqual({ valor: 12.5 })
    expect(validarMaximo('0.5')).toEqual({ valor: 0.5 })
  })
  it('exige un valor', () => {
    expect(validarMaximo('  ').error).toBeDefined()
  })
  it('rechaza lo que no es un número', () => {
    expect(validarMaximo('mucho').error).toBeDefined()
    expect(validarMaximo('1e3').error).toBeDefined()
  })
  it('rechaza cero y negativos, porque el mínimo permitido es mayor que cero', () => {
    expect(validarMaximo('0').error).toBeDefined()
    expect(validarMaximo('-3').error).toBeDefined()
  })
  it('admite hasta 999.999 y rechaza más', () => {
    expect(validarMaximo('999.999')).toEqual({ valor: 999.999 })
    expect(validarMaximo('1000').error).toBeDefined()
  })
})

describe('usoDelPresupuesto', () => {
  it('calcula el porcentaje usado', () => {
    expect(usoDelPresupuesto(5, 20)).toMatchObject({ porcentaje: 25, cerca: false, agotado: false })
  })
  it('avisa cuando se está cerca del límite sin llamarlo alarma', () => {
    expect(usoDelPresupuesto(17, 20)).toMatchObject({ porcentaje: 85, cerca: true, agotado: false })
  })
  it('marca el presupuesto agotado cuando se usa todo', () => {
    expect(usoDelPresupuesto(20, 20)).toMatchObject({ porcentaje: 100, cerca: true, agotado: true })
  })
  it('no pasa de 100 % aunque el dato venga pasado, ni se rompe con un máximo en cero', () => {
    expect(usoDelPresupuesto(30, 20).porcentaje).toBe(100)
    expect(usoDelPresupuesto(3, 0).porcentaje).toBe(0)
  })
})
