import { validarNuevoEmpleado } from './empleado'

export interface DatosEmpresa {
  ruc: string
  nombre: string
  rubro: string
}

export type ErroresEmpresa = Partial<Record<keyof DatosEmpresa, string>>

// A Peruvian RUC is exactly 11 digits, the rule the backend enforces.
export function validarEmpresa(d: DatosEmpresa): ErroresEmpresa {
  const e: ErroresEmpresa = {}
  if (!/^\d{11}$/.test(d.ruc.trim())) e.ruc = 'El RUC debe tener exactamente 11 dígitos, sin espacios ni guiones.'
  if (!d.nombre.trim()) e.nombre = 'Escribe el nombre de la empresa.'
  if (!d.rubro.trim()) e.rubro = 'Escribe a qué se dedica la empresa.'
  return e
}

export const ROLES_PERSONAL: Array<{ valor: string; texto: string }> = [
  { valor: 'RRHH', texto: 'RRHH' },
  { valor: 'COORDINADOR_BIENESTAR', texto: 'Coordinación de bienestar' },
]

export interface NuevoPersonal {
  nombre: string
  email: string
  clave: string
  rol: string
}

export type ErroresPersonal = Partial<Record<keyof NuevoPersonal, string>>

// HR and wellbeing coordination follow the same name, email and password rules as an employee.
export function validarNuevoPersonal(d: NuevoPersonal): ErroresPersonal {
  const { equipoId: _equipo, ...base } = validarNuevoEmpleado({ nombre: d.nombre, email: d.email, clave: d.clave, equipoId: 0 })
  void _equipo
  const errores: ErroresPersonal = { ...base }
  if (!ROLES_PERSONAL.some((r) => r.valor === d.rol)) errores.rol = 'Elige si la persona es de RRHH o de coordinación de bienestar.'
  return errores
}
