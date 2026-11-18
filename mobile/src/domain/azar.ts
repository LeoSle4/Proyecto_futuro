import * as Crypto from 'expo-crypto'
import type { Uniforme } from './ldp'

// Cryptographically secure uniform in [0, 1) with 53 bits of precision. Math.random is never used for privacy noise.
export const uniformeSeguro: Uniforme = () => {
  const [alto, bajo] = Crypto.getRandomValues(new Uint32Array(2))
  return ((alto >>> 5) * 67108864 + (bajo >>> 6)) / 9007199254740992
}
