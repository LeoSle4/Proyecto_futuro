import type { Dashboard, Empleado, Equipo, NuevoEmpleadoSolicitud, Perfil } from './tipos'

// Synthetic data for working without the backend (VITE_DEMO=1). The UI labels it as synthetic.
// Mutable on purpose: the demo lets you create, rename and delete teams in memory.
const equipos: Equipo[] = [
  { id: 1, nombre: 'Operaciones Lima', empresaId: 1 },
  { id: 2, nombre: 'Ventas', empresaId: 1 },
  { id: 3, nombre: 'Administración', empresaId: 1 },
  { id: 4, nombre: 'Soporte', empresaId: 1 },
]

const respuestas: Record<number, number> = { 1: 38, 2: 12, 3: 44, 4: 27 }

function dashboard(equipoId: number, semana: string): Dashboard {
  const n = respuestas[equipoId] ?? 0
  const suficiente = n >= 30
  return {
    equipoId,
    semanaInicio: semana,
    resultado: suficiente ? 'OK' : 'DATOS_INSUFICIENTES',
    totalRespuestas: n,
    items: [
      { itemId: 1, tema: '¿Hoy te sentiste bien?', proporcionEstimada: suficiente ? 0.65 : null, margenError95: suficiente ? 0.31 : null, mediaEstimada: null, respuestas: n },
      { itemId: 2, tema: 'Me he sentido tranquilo(a) y relajado(a)', proporcionEstimada: null, margenError95: suficiente ? 2.2 : null, mediaEstimada: suficiente ? 3.2 : null, valorMin: 0, valorMax: 5, respuestas: n },
      { itemId: 3, tema: 'Poco interés o placer en hacer cosas', proporcionEstimada: null, margenError95: null, mediaEstimada: null, respuestas: Math.min(n, 19) },
    ],
  }
}

const perfil: Perfil = { username: 'rrhh@demo.kala', uid: 1, roles: ['RRHH'], empresaId: 1 }

function crearEquipo(nombre: string, empresaId: number): Equipo {
  const creado = { id: Math.max(0, ...equipos.map((e) => e.id)) + 1, nombre, empresaId }
  equipos.push(creado)
  return creado
}

function editarEquipo(id: number, nombre: string): Equipo {
  const equipo = equipos.find((e) => e.id === id)
  if (!equipo) throw new Error('Equipo no encontrado')
  equipo.nombre = nombre
  return { ...equipo }
}

function eliminarEquipo(id: number): void {
  const i = equipos.findIndex((e) => e.id === id)
  if (i >= 0) equipos.splice(i, 1)
}

const personas: Empleado[] = [
  { id: 101, email: 'ana.rios@demo.kala', nombre: 'Ana Ríos', activo: true, equipoId: 1, equipoNombre: 'Operaciones Lima' },
  { id: 102, email: 'luis.vega@demo.kala', nombre: 'Luis Vega', activo: true, equipoId: 1, equipoNombre: 'Operaciones Lima' },
  { id: 103, email: 'marta.paz@demo.kala', nombre: 'Marta Paz', activo: false, equipoId: 2, equipoNombre: 'Ventas' },
  { id: 104, email: 'jorge.sosa@demo.kala', nombre: 'Jorge Sosa', activo: true, equipoId: 3, equipoNombre: 'Administración' },
]

const empleados = (): Empleado[] => personas.map((p) => ({ ...p, equipoNombre: equipos.find((e) => e.id === p.equipoId)?.nombre ?? p.equipoNombre }))

function crearEmpleado(d: NuevoEmpleadoSolicitud): void {
  if (personas.some((p) => p.email.toLowerCase() === d.email.toLowerCase())) throw Object.assign(new Error('Ese correo ya tiene una cuenta.'), { estado: 409 })
  personas.push({ id: Math.max(0, ...personas.map((p) => p.id)) + 1, email: d.email, nombre: d.nombre, activo: true, equipoId: d.equipoId, equipoNombre: equipos.find((e) => e.id === d.equipoId)?.nombre ?? '' })
}

function cambiarEstadoEmpleado(id: number, activo: boolean): void {
  const p = personas.find((x) => x.id === id)
  if (p) p.activo = activo
}

export const demo = { empleados, crearEmpleado, cambiarEstadoEmpleado, equipos, dashboard, perfil, crearEquipo, editarEquipo, eliminarEquipo }
