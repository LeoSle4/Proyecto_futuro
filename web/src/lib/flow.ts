// The login diagram: answers travel along a track, slow down through the "noise" gate, and about one
// in four changes value there. All of it is deterministic so it renders the same on every machine.

// Stable pseudo-random in [0, 1) from two integers.
export function stableRandom(a: number, b: number): number {
  const x = Math.sin(a * 127.1 + b * 311.7) * 43758.5453
  return x - Math.floor(x)
}

export const FLIP_SHARE = 0.25

// Whether dot `id` changes its value on lap `lap` (about FLIP_SHARE of them do).
export function isFlipped(id: number, lap: number): boolean {
  return stableRandom(id, lap + 17) < FLIP_SHARE
}

// Position along a track of the given width for progress u in [0, 1]. Strictly increasing, slow at the
// ends and at the gate (u = 0.5), quick in between, so dots look like they squeeze through it.
export function trackX(u: number, width: number): number {
  return width * (u - 0.045 * Math.sin(4 * Math.PI * u))
}

// What the panel does with the noisy share: with truth probability p = 1 - FLIP_SHARE, an observed share o of
// "sí" comes from a real share (o - (1 - p)) / (2p - 1). Clamped to [0, 1].
export function debias(observed: number, flipShare = FLIP_SHARE): number {
  const p = 1 - flipShare
  return Math.min(1, Math.max(0, (observed - (1 - p)) / (2 * p - 1)))
}

// Expected share of "sí" after the noise, on average: truth stays with probability 1 - FLIP_SHARE.
export function expectedObserved(real: number, flipShare = FLIP_SHARE): number {
  return real * (1 - flipShare) + (1 - real) * flipShare
}

// Whether dot `id` truly answered "sí", spread so that any run of consecutive ids holds close to `share` of them
// (a plain random draw over 64 dots can miss it by several points).
export function isYes(id: number, share = 0.65): boolean {
  const v = id * 0.7548776662 + 0.13
  return v - Math.floor(v) < share
}
