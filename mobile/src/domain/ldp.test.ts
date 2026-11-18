import { aleatorizarRespuesta, perturbarItem, ruidoLaplace, valorLaplace, type ItemPendiente } from './ldp'

// Deterministic PRNG so the statistical tests never flake.
function mulberry32(semilla: number) {
  let a = semilla
  return () => {
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

const itemRR: ItemPendiente = {
  itemId: 3, configuracionLdpId: 2, mecanismo: 'RANDOMIZED_RESPONSE', probabilidadVerdad: 0.75, epsilon: null,
  escala: { valorMin: 0, valorMax: 1 },
}
const itemLaplace: ItemPendiente = {
  itemId: 4, configuracionLdpId: 3, mecanismo: 'LAPLACE', probabilidadVerdad: null, epsilon: 1,
  escala: { valorMin: 0, valorMax: 5 },
}

describe('aleatorizarRespuesta (Randomized Response)', () => {
  it('manda la verdad cuando el azar cae por debajo de p y la contraria si no', () => {
    expect(aleatorizarRespuesta(true, 0.75, () => 0.1)).toBe(true)
    expect(aleatorizarRespuesta(true, 0.75, () => 0.9)).toBe(false)
    expect(aleatorizarRespuesta(false, 0.75, () => 0.1)).toBe(false)
    expect(aleatorizarRespuesta(false, 0.75, () => 0.9)).toBe(true)
  })

  it('dice la verdad con probabilidad p y el estimador desesgado recupera la proporción real', () => {
    const u = mulberry32(42)
    const n = 100_000
    let verdad = 0
    let si = 0
    for (let i = 0; i < n; i++) {
      const real = i % 100 < 65 // 65 % de "sí"
      const enviado = aleatorizarRespuesta(real, 0.75, u)
      if (enviado === real) verdad++
      if (enviado) si++
    }
    expect(verdad / n).toBeCloseTo(0.75, 2)
    // Same unbiasing the server applies: (observed - (1 - p)) / (2p - 1).
    expect((si / n - 0.25) / 0.5).toBeCloseTo(0.65, 1)
  })

  it('rechaza una p fuera de (0.5, 1)', () => {
    expect(() => aleatorizarRespuesta(true, 0.5, () => 0.1)).toThrow()
    expect(() => aleatorizarRespuesta(true, 1, () => 0.1)).toThrow()
  })
})

describe('ruidoLaplace', () => {
  it('es simétrico y su magnitud media es igual a la escala', () => {
    const u = mulberry32(7)
    const escala = 5
    let suma = 0
    let absoluta = 0
    const n = 200_000
    for (let i = 0; i < n; i++) {
      const r = ruidoLaplace(escala, u)
      suma += r
      absoluta += Math.abs(r)
    }
    expect(suma / n).toBeCloseTo(0, 1)
    expect(absoluta / n / escala).toBeCloseTo(1, 1)
  })

  it('nunca devuelve un valor infinito aunque el azar sea exactamente 0', () => {
    expect(Number.isFinite(ruidoLaplace(5, () => 0))).toBe(true)
    expect(Number.isFinite(ruidoLaplace(5, () => 0.5))).toBe(true)
  })

  it('rechaza una escala no positiva', () => {
    expect(() => ruidoLaplace(0, () => 0.3)).toThrow()
  })
})

describe('valorLaplace', () => {
  it('suma el ruido sin recortar al rango de la escala (el servidor lo exige así)', () => {
    // u = 0.01 gives a large negative noise; the value must leave [0, 5].
    const v = valorLaplace(0, 0, 5, 1, () => 0.01)
    expect(v).toBeLessThan(0)
  })

  it('usa escala delta / epsilon', () => {
    // Same u, doubling epsilon halves the noise.
    const a = valorLaplace(2, 0, 5, 1, () => 0.9) - 2
    const b = valorLaplace(2, 0, 5, 2, () => 0.9) - 2
    expect(a / b).toBeCloseTo(2, 6)
  })
})

describe('perturbarItem', () => {
  it('en Randomized Response manda solo respuestaRuidosa', () => {
    const envio = perturbarItem(itemRR, 1, () => 0.1)
    expect(envio).toEqual({ itemId: 3, configuracionLdpId: 2, respuestaRuidosa: true })
  })

  it('en Randomized Response el "sí" es el valor máximo de la escala', () => {
    expect(perturbarItem(itemRR, 0, () => 0.1).respuestaRuidosa).toBe(false)
  })

  it('en Laplace manda solo valorRuidoso', () => {
    const envio = perturbarItem(itemLaplace, 4, () => 0.9)
    expect(envio.itemId).toBe(4)
    expect(envio.configuracionLdpId).toBe(3)
    expect(typeof envio.valorRuidoso).toBe('number')
    expect(envio.respuestaRuidosa).toBeUndefined()
  })

  it('rechaza un valor fuera de la escala o un ítem sin su parámetro', () => {
    expect(() => perturbarItem(itemLaplace, 9, () => 0.5)).toThrow()
    expect(() => perturbarItem({ ...itemRR, probabilidadVerdad: null }, 1, () => 0.5)).toThrow()
    expect(() => perturbarItem({ ...itemLaplace, epsilon: null }, 2, () => 0.5)).toThrow()
  })
})
