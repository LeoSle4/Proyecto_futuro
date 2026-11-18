import { CLAVE_MAXIMA_BYTES, CLAVE_MINIMA } from './empleado'

export interface CambioClave {
  actual: string
  nueva: string
  repetida: string
}

export type ErroresClave = Partial<Record<keyof CambioClave, string>>

// The rules the server applies when someone changes their own password.
export function validarCambioClave(d: CambioClave): ErroresClave {
  const e: ErroresClave = {}
  if (!d.actual) e.actual = 'Escribe tu contraseña actual.'
  if ([...d.nueva].length < CLAVE_MINIMA) e.nueva = `La contraseña nueva necesita al menos ${CLAVE_MINIMA} caracteres.`
  else if (new TextEncoder().encode(d.nueva).length > CLAVE_MAXIMA_BYTES) e.nueva = `La contraseña nueva es demasiado larga (máximo ${CLAVE_MAXIMA_BYTES} bytes).`
  else if (d.nueva === d.actual) e.nueva = 'La contraseña nueva debe ser distinta de la actual.'
  if (d.repetida !== d.nueva) e.repetida = 'Las dos contraseñas nuevas no coinciden.'
  return e
}
