// The same rules the backend enforces on a new employee, so the form can say what is wrong before sending.
export const CLAVE_MINIMA = 12
export const CLAVE_MAXIMA_BYTES = 72 // bcrypt reads at most 72 bytes
const LARGO_MAXIMO = 150
const CORREO = /^[^@\s]+@[^@\s]+\.[^@\s]+$/

export interface NuevoEmpleado {
  nombre: string
  email: string
  clave: string
  equipoId: number | null
}

export type ErroresEmpleado = Partial<Record<keyof NuevoEmpleado, string>>

export function validarNuevoEmpleado(d: NuevoEmpleado): ErroresEmpleado {
  const errores: ErroresEmpleado = {}
  const nombre = d.nombre.trim()
  const email = d.email.trim()
  if (!nombre) errores.nombre = 'Escribe el nombre de la persona.'
  else if (nombre.length > LARGO_MAXIMO) errores.nombre = `El nombre admite hasta ${LARGO_MAXIMO} caracteres.`
  if (!email) errores.email = 'Escribe su correo.'
  else if (email.length > LARGO_MAXIMO || !CORREO.test(email)) errores.email = 'Ese correo no parece válido. Revisa que tenga @ y un dominio.'
  if (d.equipoId === null) errores.equipoId = 'Elige el equipo al que pertenece.'
  if ([...d.clave].length < CLAVE_MINIMA) errores.clave = `La clave inicial necesita al menos ${CLAVE_MINIMA} caracteres.`
  else if (new TextEncoder().encode(d.clave).length > CLAVE_MAXIMA_BYTES) errores.clave = `La clave inicial es demasiado larga (máximo ${CLAVE_MAXIMA_BYTES} bytes).`
  return errores
}
