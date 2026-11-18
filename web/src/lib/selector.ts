// Keyboard behaviour of the custom select, kept free of the DOM so it can be tested.

export interface OpcionSelector {
  valor: string
  texto: string
}

// Next active index for a navigation key. It stops at the ends instead of wrapping.
export function moverIndice(actual: number, tecla: string, total: number): number {
  if (total <= 0) return -1
  if (tecla === 'Home') return 0
  if (tecla === 'End') return total - 1
  if (tecla === 'ArrowDown') return actual < 0 ? 0 : Math.min(total - 1, actual + 1)
  if (tecla === 'ArrowUp') return actual < 0 ? total - 1 : Math.max(0, actual - 1)
  return actual
}

const normalizar = (t: string) => t.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()

// Type-ahead: the first option starting with what was typed. A single repeated letter cycles through the options
// that start with it, beginning after the current one.
export function buscarPorPrefijo(opciones: OpcionSelector[], escrito: string, desde: number): number {
  const q = normalizar(escrito)
  if (!q || opciones.length === 0) return -1
  const n = opciones.length
  const inicio = q.length === 1 ? desde + 1 : desde
  for (let k = 0; k < n; k++) {
    const i = (((inicio + k) % n) + n) % n
    if (normalizar(opciones[i].texto).startsWith(q)) return i
  }
  return -1
}
