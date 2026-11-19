import { describe, expect, it } from 'vitest'
import type { ItemDashboard } from '../api/tipos'
import { separarPrincipal } from './principal'

const item = (id: number, extra: Partial<ItemDashboard> = {}): ItemDashboard => ({
  itemId: id,
  tema: `Tema ${id}`,
  proporcionEstimada: null,
  margenError95: null,
  mediaEstimada: null,
  ...extra,
})

describe('separarPrincipal', () => {
  it('un indicador de sí o no (rango 0 a 1) es el principal', () => {
    const si = item(1, { valorMin: 0, valorMax: 1, proporcionEstimada: 0.6, margenError95: 0.2 })
    const escala = item(2, { valorMin: 0, valorMax: 5, mediaEstimada: 3, margenError95: 1 })
    expect(separarPrincipal([escala, si])).toEqual({ principal: si, resto: [escala] })
  })

  it('una escala sola (GAD-7, 0 a 3) no se dibuja como proporción: no hay principal y va entre las tarjetas', () => {
    const gad = item(21, { valorMin: 0, valorMax: 3, mediaEstimada: 2.2, margenError95: 1.47 })
    expect(separarPrincipal([gad])).toEqual({ principal: null, resto: [gad] })
  })

  it('una escala sin resultado todavía tampoco es el principal', () => {
    const gad = item(21, { valorMin: 0, valorMax: 3, respuestas: 12 })
    expect(separarPrincipal([gad])).toEqual({ principal: null, resto: [gad] })
  })

  it('sin información de escala, el primero sigue siendo el principal si no trae una media', () => {
    const a = item(1)
    const b = item(2, { mediaEstimada: 3, margenError95: 1 })
    expect(separarPrincipal([a, b])).toEqual({ principal: a, resto: [b] })
  })

  it('sin indicadores no hay principal ni resto', () => {
    expect(separarPrincipal([])).toEqual({ principal: null, resto: [] })
  })
})
