export type Rol = 'ADMIN' | 'RRHH' | 'COORDINADOR_BIENESTAR' | 'EMPLEADO'

export interface Sesion {
  accessToken: string
  refreshToken: string
}

export interface Perfil {
  username: string
  uid: number
  roles: Rol[]
  empresaId?: number
  equipoId?: number
}

export interface Equipo {
  id: number
  nombre: string
  empresaId: number
}

export interface Empleado {
  id: number
  email: string
  nombre: string
  activo: boolean
  equipoId: number
  equipoNombre: string
}

export interface NuevoEmpleadoSolicitud {
  equipoId: number
  email: string
  nombre: string
  clave: string
}

export interface Cuestionario {
  id: number
  codigo: string
  nombre: string
  cadenciaDias: number
  itemsPorEnvio: number | null
  activo: boolean
}

export interface Presupuesto {
  empresaId: number
  periodo: string
  epsilonMaximo: number
  epsilonMensual: number
}

export interface Empresa {
  id: number
  nombre: string
}

export interface ContactoAyuda {
  id: number
  nombre: string
  tipo: string
  telefono: string | null
  email: string | null
  activo: boolean
}

export interface Solicitud {
  id: number
  usuarioId: number
  contactoId: number | null
  mensaje: string | null
  estado: 'PENDIENTE' | 'EN_ATENCION' | 'CERRADA' | string
  creadoEn: string
  atendidoPor: number | null
  atendidoEn: string | null
}

export interface ItemDashboard {
  itemId: number
  tema: string
  proporcionEstimada: number | null
  margenError95: number | null
  mediaEstimada: number | null
  // Range of the item's answer scale; the axis of a mean is drawn from it.
  valorMin?: number | null
  valorMax?: number | null
  // How many responses this item had in the window. Older servers do not send it.
  respuestas?: number | null
}

export interface Dashboard {
  equipoId: number
  semanaInicio: string
  resultado: 'OK' | 'DATOS_INSUFICIENTES'
  totalRespuestas: number
  items: ItemDashboard[]
}

export const N_MINIMO = 30
