export type Rol = 'ADMIN' | 'RRHH' | 'COORDINADOR_BIENESTAR' | 'EMPLEADO'
export type TipoContactoAyuda = 'PSICOLOGO' | 'LINEA_CRISIS' | 'EPS' | 'OTRO'

export interface ContactoAyuda {
  id: number
  nombre: string
  tipo: TipoContactoAyuda
  telefono: string | null
  email: string | null
  activo: boolean
}

export interface CrearContactoAyuda {
  nombre: string
  tipo: TipoContactoAyuda
  telefono: string | null
  email: string | null
}

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

export interface ItemDashboard {
  itemId: number
  tema: string
  proporcionEstimada: number | null
  margenError95: number | null
  mediaEstimada: number | null
}

export interface Dashboard {
  equipoId: number
  semanaInicio: string
  resultado: 'OK' | 'DATOS_INSUFICIENTES'
  totalRespuestas: number
  items: ItemDashboard[]
}

export const N_MINIMO = 30
