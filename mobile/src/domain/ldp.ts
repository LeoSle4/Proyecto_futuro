// Local differential privacy on the device. The server never sees a raw answer: it receives only what these
// functions return. The math mirrors the backend (RandomizedResponse and Laplace mechanisms).

export interface EscalaItem {
  valorMin: number
  valorMax: number
}

export interface ItemPendiente {
  itemId: number
  configuracionLdpId: number
  mecanismo: 'RANDOMIZED_RESPONSE' | 'LAPLACE' | string
  probabilidadVerdad: number | null
  epsilon: number | null
  escala: EscalaItem
}

export interface EnvioItem {
  itemId: number
  configuracionLdpId: number
  respuestaRuidosa?: boolean
  valorRuidoso?: number
}

// A source of uniform numbers in [0, 1). In production it must be cryptographically secure.
export type Uniforme = () => number

// Randomized Response: send the truth with probability p, the opposite with 1 - p. epsilon = ln(p / (1 - p)).
export function aleatorizarRespuesta(verdad: boolean, p: number, uniforme: Uniforme): boolean {
  if (!(p > 0.5 && p < 1)) {
    throw new RangeError('p debe ser mayor que 0.5 y menor que 1')
  }
  return uniforme() < p ? verdad : !verdad
}

// Laplace(0, scale) by inverse CDF. The open interval is enforced so the logarithm never sees 0.
export function ruidoLaplace(escala: number, uniforme: Uniforme): number {
  if (!(escala > 0) || !Number.isFinite(escala)) {
    throw new RangeError('La escala del ruido debe ser positiva')
  }
  let x = uniforme() - 0.5
  const limite = 0.5 - Number.EPSILON
  if (x > limite) x = limite
  if (x < -limite) x = -limite
  return -escala * Math.sign(x || 1) * Math.log(1 - 2 * Math.abs(x))
}

// Laplace mechanism: value + Lap(delta / epsilon), delta = max - min. Never clamped: the server expects raw noise.
export function valorLaplace(valor: number, min: number, max: number, epsilon: number, uniforme: Uniforme): number {
  return valor + ruidoLaplace((max - min) / epsilon, uniforme)
}

// Turns a real answer into the only thing allowed to leave the phone.
export function perturbarItem(item: ItemPendiente, valor: number, uniforme: Uniforme): EnvioItem {
  const { valorMin, valorMax } = item.escala
  if (!Number.isFinite(valor) || valor < valorMin || valor > valorMax) {
    throw new RangeError('La respuesta está fuera de la escala del ítem')
  }
  const base = { itemId: item.itemId, configuracionLdpId: item.configuracionLdpId }
  if (item.mecanismo === 'RANDOMIZED_RESPONSE') {
    if (item.probabilidadVerdad === null) throw new Error('El ítem no trae probabilidadVerdad')
    // "Yes" is the top of the scale, as the server counts true answers as 1.
    return { ...base, respuestaRuidosa: aleatorizarRespuesta(valor === valorMax, item.probabilidadVerdad, uniforme) }
  }
  if (item.mecanismo === 'LAPLACE') {
    if (item.epsilon === null || !(item.epsilon > 0)) throw new Error('El ítem no trae epsilon')
    return { ...base, valorRuidoso: valorLaplace(valor, valorMin, valorMax, item.epsilon, uniforme) }
  }
  throw new Error(`Mecanismo desconocido: ${item.mecanismo}`)
}
