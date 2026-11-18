// The privacy budget: how much noise per person per month a company allows (epsilon), and how much is used.
export const MAXIMO_PERMITIDO = 999.999

export type ResultadoMaximo = { valor: number; error?: undefined } | { valor?: undefined; error: string }

// Accepts "12,5" and "12.5". The backend requires a value above 0 and up to 999.999.
export function validarMaximo(texto: string): ResultadoMaximo {
  const limpio = texto.trim().replace(',', '.')
  if (!limpio) return { error: 'Escribe el máximo por persona.' }
  if (!/^\d+(\.\d+)?$/.test(limpio)) return { error: 'Escribe solo un número, por ejemplo 20 o 12,5.' }
  const valor = Number(limpio)
  if (valor <= 0) return { error: 'El máximo debe ser mayor que cero.' }
  if (valor > MAXIMO_PERMITIDO) return { error: `El máximo permitido es ${MAXIMO_PERMITIDO.toLocaleString('es-PE')}.` }
  return { valor }
}

export interface Uso {
  porcentaje: number
  // 85 % or more: worth saying, never an alarm.
  cerca: boolean
  agotado: boolean
}

export function usoDelPresupuesto(usado: number, maximo: number): Uso {
  const porcentaje = maximo > 0 ? Math.min(100, Math.round((usado / maximo) * 100)) : 0
  return { porcentaje, cerca: porcentaje >= 85, agotado: maximo > 0 && usado >= maximo }
}
