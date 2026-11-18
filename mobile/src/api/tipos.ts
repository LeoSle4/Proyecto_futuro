import type { ItemPendiente } from '../domain/ldp'

export interface Opcion {
  valor: number
  etiqueta: string
}

export interface ItemCuestionario extends ItemPendiente {
  orden: number
  tema: string
  texto: string
  escala: ItemPendiente['escala'] & { codigo: string; opciones: Opcion[] }
}

export interface Cuestionario {
  id: number
  codigo: string
  nombre: string
  itemsPorEnvio: number | null
  items: ItemCuestionario[]
}

export interface Contacto {
  id: number
  nombre: string
  tipo: string
  telefono?: string | null
  email?: string | null
}
