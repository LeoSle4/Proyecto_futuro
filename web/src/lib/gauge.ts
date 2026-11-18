// Pure geometry for the uncertainty gauge: every number the drawing needs, with no DOM.

export interface Scale {
  min: number
  max: number
}

export interface Interval {
  estimate: number
  lo: number
  hi: number
}

const clamp = (v: number, a: number, b: number) => Math.min(b, Math.max(a, v))

// Position of a value on its scale, 0 to 100, clamped to the track.
export function percentOf(value: number, scale: Scale): number {
  return ((clamp(value, scale.min, scale.max) - scale.min) / (scale.max - scale.min)) * 100
}

export interface BandGeometry {
  left: number
  width: number
  estimate: number
  haloLeft: number
  haloWidth: number
}

// The band sits exactly on the interval; the halo is the same shape a little wider, symmetric around it
// and never past the ends of the track.
export function bandGeometry(interval: Interval, scale: Scale, halo = 3): BandGeometry {
  const left = percentOf(interval.lo, scale)
  const right = percentOf(interval.hi, scale)
  const pad = Math.min(halo, left, 100 - right)
  return {
    left,
    width: right - left,
    estimate: percentOf(interval.estimate, scale),
    haloLeft: left - pad,
    haloWidth: right - left + 2 * pad,
  }
}

// Illustrative only: the half-width of a 95 % interval shrinks with the square root of the sample size.
export function simulateInterval(base: { estimate: number; half: number }, n0: number, n: number): Interval {
  if (!(n > 0) || !(n0 > 0)) throw new RangeError('n debe ser positiva')
  const half = base.half * Math.sqrt(n0 / n)
  return {
    estimate: base.estimate,
    lo: clamp(Math.round(base.estimate - half), 0, 100),
    hi: clamp(Math.round(base.estimate + half), 0, 100),
  }
}

export interface WeekRow {
  resultado: 'OK' | 'DATOS_INSUFICIENTES'
  totalRespuestas: number
}

// The week card: how many teams have a result out of all the teams, and the responses of those loaded.
export function summarizeWeek(rows: Array<WeekRow | null | undefined>) {
  const loaded = rows.filter((r): r is WeekRow => Boolean(r))
  return {
    conResultado: loaded.filter((r) => r.resultado === 'OK').length,
    total: rows.length,
    respuestas: loaded.reduce((sum, r) => sum + r.totalRespuestas, 0),
  }
}
