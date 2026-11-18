import { describe, expect, it } from 'vitest'
import { debias, expectedObserved, isFlipped, isYes, stableRandom, trackX } from './flow'

describe('trackX', () => {
  it('empieza en 0, pasa exactamente por la puerta en el centro y termina en el ancho', () => {
    expect(trackX(0, 640)).toBeCloseTo(0, 6)
    expect(trackX(0.5, 640)).toBeCloseTo(320, 6)
    expect(trackX(1, 640)).toBeCloseTo(640, 6)
  })
  it('nunca retrocede: un punto jamás se mueve hacia atrás', () => {
    let prev = -1
    for (let i = 0; i <= 2000; i++) {
      const x = trackX(i / 2000, 640)
      expect(x).toBeGreaterThan(prev)
      prev = x
    }
  })
  it('va más lento en la puerta que entre medias', () => {
    const slowAtGate = trackX(0.5 + 0.01, 640) - trackX(0.5, 640)
    const fastBetween = trackX(0.25 + 0.01, 640) - trackX(0.25, 640)
    expect(slowAtGate).toBeLessThan(fastBetween)
  })
})

describe('stableRandom', () => {
  it('es determinista y está en [0, 1)', () => {
    expect(stableRandom(3, 7)).toBe(stableRandom(3, 7))
    for (let i = 0; i < 500; i++) {
      const v = stableRandom(i, i * 3 + 1)
      expect(v).toBeGreaterThanOrEqual(0)
      expect(v).toBeLessThan(1)
    }
  })
})

describe('isFlipped', () => {
  it('cambia aproximadamente 1 de cada 4 respuestas', () => {
    let flips = 0
    const n = 8000
    for (let i = 0; i < n; i++) if (isFlipped(i, i % 13)) flips++
    expect(flips / n).toBeGreaterThan(0.22)
    expect(flips / n).toBeLessThan(0.28)
  })
})

describe('debias', () => {
  it('recupera el 65 % real a partir del 57,5 % observado tras cambiar 1 de cada 4', () => {
    const observado = 0.65 * 0.75 + 0.35 * 0.25
    expect(debias(observado)).toBeCloseTo(0.65, 6)
  })
  it('acota el resultado a 0–1', () => {
    expect(debias(0)).toBe(0)
    expect(debias(1)).toBe(1)
  })
})

describe('expectedObserved', () => {
  it('baja un 65 % real a 57,5 % cuando se cambia 1 de cada 4', () => {
    expect(expectedObserved(0.65)).toBeCloseTo(0.575, 6)
  })
  it('y el panel lo devuelve al valor real', () => {
    for (const real of [0.2, 0.5, 0.65, 0.9]) expect(debias(expectedObserved(real))).toBeCloseTo(real, 6)
  })
})

describe('isYes', () => {
  it('reparte los síes cerca del 65 % en los 64 puntos del diagrama', () => {
    let si = 0
    for (let id = 0; id < 64; id++) if (isYes(id)) si++
    expect(si / 64).toBeGreaterThan(0.62)
    expect(si / 64).toBeLessThan(0.68)
  })
})
