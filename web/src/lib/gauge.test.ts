import { describe, expect, it } from 'vitest'
import { bandGeometry, percentOf, simulateInterval, summarizeWeek } from './gauge'

describe('percentOf', () => {
  it('ubica un valor dentro de su escala', () => {
    expect(percentOf(65, { min: 0, max: 100 })).toBe(65)
    expect(percentOf(2.5, { min: 0, max: 5 })).toBe(50)
  })
  it('acota los valores que se salen de la escala', () => {
    expect(percentOf(-4, { min: 0, max: 100 })).toBe(0)
    expect(percentOf(130, { min: 0, max: 100 })).toBe(100)
  })
})

describe('bandGeometry', () => {
  it('centra la banda en su intervalo y deja el halo simétrico', () => {
    const g = bandGeometry({ estimate: 65, lo: 34, hi: 96 }, { min: 0, max: 100 })
    expect(g.left).toBe(34)
    expect(g.width).toBe(62)
    expect(g.estimate).toBe(65)
    expect(g.haloLeft + g.haloWidth / 2).toBeCloseTo(g.left + g.width / 2, 6)
  })
  it('no deja que el halo se salga de la escala', () => {
    const g = bandGeometry({ estimate: 5, lo: 0, hi: 10 }, { min: 0, max: 100 })
    expect(g.haloLeft).toBeGreaterThanOrEqual(0)
    expect(g.haloLeft + g.haloWidth).toBeLessThanOrEqual(100)
  })
})

describe('simulateInterval', () => {
  it('con la misma n no cambia el intervalo', () => {
    expect(simulateInterval({ estimate: 65, half: 31 }, 38, 38)).toEqual({ estimate: 65, lo: 34, hi: 96 })
  })
  it('con más respuestas el margen baja con la raíz de n', () => {
    const r = simulateInterval({ estimate: 50, half: 20 }, 100, 400) // half / 2
    expect(r.lo).toBe(40)
    expect(r.hi).toBe(60)
  })
  it('acota el intervalo a 0–100', () => {
    const r = simulateInterval({ estimate: 90, half: 30 }, 30, 30)
    expect(r.hi).toBe(100)
    expect(r.lo).toBe(60)
  })
  it('rechaza una n no positiva', () => {
    expect(() => simulateInterval({ estimate: 50, half: 20 }, 38, 0)).toThrow()
  })
})

describe('summarizeWeek', () => {
  it('cuenta los equipos con resultado y suma todas las respuestas', () => {
    const s = summarizeWeek([
      { resultado: 'OK', totalRespuestas: 38 },
      { resultado: 'DATOS_INSUFICIENTES', totalRespuestas: 12 },
      { resultado: 'OK', totalRespuestas: 44 },
      { resultado: 'DATOS_INSUFICIENTES', totalRespuestas: 27 },
    ])
    expect(s).toEqual({ conResultado: 2, total: 4, respuestas: 121 })
  })
  it('ignora los equipos que todavía no cargaron', () => {
    expect(summarizeWeek([null, { resultado: 'OK', totalRespuestas: 30 }])).toEqual({ conResultado: 1, total: 2, respuestas: 30 })
  })
})
