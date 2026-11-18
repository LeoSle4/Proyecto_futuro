import type { Solicitud } from '../api/tipos'

export const TIPOS_CONTACTO: Array<{ valor: string; texto: string }> = [
  { valor: 'PSICOLOGO', texto: 'Psicólogo o psicóloga' },
  { valor: 'LINEA_CRISIS', texto: 'Línea de ayuda en crisis' },
  { valor: 'EPS', texto: 'EPS o seguro de salud' },
  { valor: 'OTRO', texto: 'Otro' },
]

const LARGO_MAXIMO = 150
const TELEFONO_MAXIMO = 20
const CORREO = /^[^@\s]+@[^@\s]+\.[^@\s]+$/

export interface NuevoContacto {
  nombre: string
  tipo: string
  telefono: string
  email: string
}

export type ErroresContacto = Partial<Record<keyof NuevoContacto, string>>

// The same rules as the backend: a name, a known type, and at least a phone or an email.
export function validarContacto(d: NuevoContacto): ErroresContacto {
  const e: ErroresContacto = {}
  const nombre = d.nombre.trim()
  const telefono = d.telefono.trim()
  const email = d.email.trim()
  if (!nombre) e.nombre = 'Escribe el nombre del contacto.'
  else if (nombre.length > LARGO_MAXIMO) e.nombre = `El nombre admite hasta ${LARGO_MAXIMO} caracteres.`
  if (!TIPOS_CONTACTO.some((t) => t.valor === d.tipo)) e.tipo = 'Elige qué tipo de contacto es.'
  if (!telefono && !email) {
    e.telefono = 'Escribe un teléfono o un correo.'
    e.email = 'Escribe un correo o un teléfono.'
  } else {
    if (telefono.length > TELEFONO_MAXIMO) e.telefono = `El teléfono admite hasta ${TELEFONO_MAXIMO} caracteres.`
    if (email && (email.length > LARGO_MAXIMO || !CORREO.test(email))) e.email = 'Ese correo no parece válido. Revisa que tenga @ y un dominio.'
  }
  return e
}

const ORDEN: Record<string, number> = { PENDIENTE: 0, EN_ATENCION: 1, CERRADA: 2 }

// Waiting first, then in attention, then closed; within a state, whoever has waited longest goes on top.
export function ordenarSolicitudes(lista: Solicitud[]): Solicitud[] {
  return [...lista].sort((a, b) => (ORDEN[a.estado] ?? 9) - (ORDEN[b.estado] ?? 9) || a.creadoEn.localeCompare(b.creadoEn))
}

export function etiquetaEstado(estado: string): string {
  if (estado === 'PENDIENTE') return 'Esperando'
  if (estado === 'EN_ATENCION') return 'En atención'
  if (estado === 'CERRADA') return 'Cerrada'
  return estado
}

const MS_DIA = 86_400_000
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

// Whole calendar days between a moment and now, never negative.
export function diasDesde(iso: string, ahora: number = Date.now()): number {
  const desde = new Date(iso)
  const a = new Date(desde.getFullYear(), desde.getMonth(), desde.getDate()).getTime()
  const b = new Date(new Date(ahora).getFullYear(), new Date(ahora).getMonth(), new Date(ahora).getDate()).getTime()
  return Math.max(0, Math.round((b - a) / MS_DIA))
}

export function textoEspera(iso: string, ahora: number = Date.now()): string {
  const d = diasDesde(iso, ahora)
  return d === 0 ? 'llegó hoy' : `lleva ${d} ${d === 1 ? 'día' : 'días'} esperando`
}

export function fechaCorta(iso: string): string {
  const f = new Date(iso)
  return `${f.getDate()} ${MESES[f.getMonth()]}`
}
