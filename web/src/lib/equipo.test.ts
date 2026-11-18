import { describe, expect, it } from 'vitest'
import { validarEquipo } from './equipo'

describe('validarEquipo', () => {
  it('exige un nombre obligatorio', () => {
    expect(validarEquipo('   ').nombre).toBeDefined()
  })

  it('acepta nombres claros', () => {
    expect(validarEquipo(' Operaciones Lima ')).toEqual({})
  })

  it('rechaza nombres que exceden el límite', () => {
    expect(validarEquipo('A'.repeat(151)).nombre).toBeDefined()
  })
})
