// Lima has no daylight saving: UTC-5 all year. The dashboard window starts on Monday.
const MS_DIA = 86_400_000

function aIso(fecha: Date): string {
  return fecha.toISOString().slice(0, 10)
}

export function lunesActual(ahora: number = Date.now()): string {
  const lima = new Date(ahora - 5 * 3_600_000)
  const dia = lima.getUTCDay() // 0 = Sunday
  const atras = (dia + 6) % 7
  return aIso(new Date(lima.getTime() - atras * MS_DIA))
}

export function moverSemanas(lunes: string, semanas: number): string {
  return aIso(new Date(new Date(`${lunes}T00:00:00Z`).getTime() + semanas * 7 * MS_DIA))
}

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

export function etiquetaSemana(lunes: string): string {
  const inicio = new Date(`${lunes}T00:00:00Z`)
  const fin = new Date(inicio.getTime() + 6 * MS_DIA)
  const f = (d: Date) => `${d.getUTCDate()} ${MESES[d.getUTCMonth()]}`
  return `${f(inicio)} – ${f(fin)} ${fin.getUTCFullYear()}`
}
