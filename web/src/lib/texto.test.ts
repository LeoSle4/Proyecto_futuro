import { describe, expect, it } from 'vitest'
import { cantidad } from './texto'

describe('cantidad', () => {
  it('usa el singular con 1', () => {
    expect(cantidad(1, 'pregunta', 'preguntas')).toBe('1 pregunta')
    expect(cantidad(1, 'día', 'días')).toBe('1 día')
  })

  it('usa el plural con cualquier otro número', () => {
    expect(cantidad(7, 'pregunta', 'preguntas')).toBe('7 preguntas')
    expect(cantidad(14, 'día', 'días')).toBe('14 días')
    expect(cantidad(0, 'pregunta', 'preguntas')).toBe('0 preguntas')
  })
})
