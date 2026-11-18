import type { ContactoAyuda, Cuestionario, Dashboard, Empleado, Empresa, Equipo, NuevoEmpleadoSolicitud, Perfil, Presupuesto, Solicitud } from './tipos'

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

// VITE_DEMO_ROL=ADMIN (or COORDINADOR_BIENESTAR) shows the panel as that role; an admin has no company of their own.
const rolDemo = (import.meta.env.VITE_DEMO_ROL ?? 'RRHH') as Perfil['roles'][number]
const perfil: Perfil = rolDemo === 'ADMIN' ? { username: 'admin@demo.kala', uid: 1, roles: ['ADMIN'] } : { username: 'rrhh@demo.kala', uid: 1, roles: [rolDemo], empresaId: 1 }

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

// Epsilon each questionnaire adds to the monthly spend (illustrative demo values).
const GASTO: Record<number, number> = { 1: 1.1, 2: 5, 3: 6, 4: 7 }
const catalogo: Cuestionario[] = [
  { id: 1, codigo: 'PULSO', nombre: 'Pulso semanal', cadenciaDias: 7, itemsPorEnvio: null, activo: true },
  { id: 2, codigo: 'WHO5', nombre: 'Bienestar (WHO-5)', cadenciaDias: 14, itemsPorEnvio: 5, activo: true },
  { id: 3, codigo: 'PHQ8', nombre: 'Ánimo (PHQ-8)', cadenciaDias: 14, itemsPorEnvio: 8, activo: false },
  { id: 4, codigo: 'GAD7', nombre: 'Ansiedad (GAD-7)', cadenciaDias: 14, itemsPorEnvio: 7, activo: false },
]
let maximo = 20

const cuestionarios = (): Cuestionario[] => catalogo.map((c) => ({ ...c }))
const usado = () => catalogo.filter((c) => c.activo).reduce((s, c) => s + GASTO[c.id], 0)
const presupuesto = (): Presupuesto => ({ empresaId: 1, periodo: '2026-11', epsilonMaximo: maximo, epsilonMensual: Math.round(usado() * 100) / 100 })

function cambiarCuestionario(id: number, activo: boolean): void {
  const c = catalogo.find((x) => x.id === id)
  if (!c) return
  if (activo && !c.activo && usado() + GASTO[id] > maximo) throw Object.assign(new Error('Activarlo superaría el presupuesto de privacidad de la empresa.'), { estado: 422 })
  c.activo = activo
}

function fijarPresupuesto(valor: number): Presupuesto {
  if (valor < usado()) throw Object.assign(new Error('Ese máximo es menor que lo que ya usan los cuestionarios activos.'), { estado: 422 })
  maximo = valor
  return presupuesto()
}

const contactosAyuda: ContactoAyuda[] = [
  { id: 1, nombre: 'Dra. Lucía Paredes', tipo: 'PSICOLOGO', telefono: '987654321', email: 'lucia@bienestar.demo', activo: true },
  { id: 2, nombre: 'Línea 113 salud', tipo: 'LINEA_CRISIS', telefono: '113', email: null, activo: true },
  { id: 3, nombre: 'EPS de la empresa', tipo: 'EPS', telefono: null, email: 'eps@demo.kala', activo: false },
]
const solicitudesDemo: Solicitud[] = [
  { id: 11, usuarioId: 204, contactoId: null, mensaje: 'Me gustaría que alguien me escriba esta semana.', estado: 'PENDIENTE', creadoEn: '2026-11-16T09:30:00', atendidoPor: null, atendidoEn: null },
  { id: 12, usuarioId: 117, contactoId: null, mensaje: null, estado: 'PENDIENTE', creadoEn: '2026-11-17T14:10:00', atendidoPor: null, atendidoEn: null },
  { id: 13, usuarioId: 150, contactoId: 1, mensaje: 'Gracias por la ayuda.', estado: 'EN_ATENCION', creadoEn: '2026-11-12T11:00:00', atendidoPor: 1, atendidoEn: '2026-11-13T08:00:00' },
  { id: 14, usuarioId: 99, contactoId: 2, mensaje: null, estado: 'CERRADA', creadoEn: '2026-11-05T16:00:00', atendidoPor: 1, atendidoEn: '2026-11-06T09:00:00' },
]

const contactos = (): ContactoAyuda[] => contactosAyuda.map((c) => ({ ...c }))
const solicitudes = (): Solicitud[] => solicitudesDemo.map((s) => ({ ...s }))

function crearContacto(d: { nombre: string; tipo: string; telefono: string | null; email: string | null }): void {
  contactosAyuda.push({ id: Math.max(0, ...contactosAyuda.map((c) => c.id)) + 1, activo: true, ...d })
}

function desactivarContacto(id: number): void {
  const c = contactosAyuda.find((x) => x.id === id)
  if (c) c.activo = false
}

function atender(id: number, contactoId: number | null): void {
  const s = solicitudesDemo.find((x) => x.id === id)
  if (!s || s.estado !== 'PENDIENTE') throw Object.assign(new Error('Esta solicitud ya cambió de estado.'), { estado: 409 })
  s.estado = 'EN_ATENCION'
  s.contactoId = contactoId
  s.atendidoPor = 1
}

function cerrar(id: number): void {
  const s = solicitudesDemo.find((x) => x.id === id)
  if (s) s.estado = 'CERRADA'
}

const empresasDemo: Empresa[] = [
  { id: 1, ruc: '20123456789', nombre: 'Empresa demo', rubro: 'Servicios' },
  { id: 2, ruc: '20987654321', nombre: 'Textiles del Sur', rubro: 'Manufactura' },
]

const empresas = (): Empresa[] => empresasDemo.map((e) => ({ ...e }))

function crearEmpresa(d: { ruc: string; nombre: string; rubro: string }): void {
  if (empresasDemo.some((e) => e.ruc === d.ruc)) throw Object.assign(new Error('Ya existe una empresa con ese RUC.'), { estado: 409 })
  empresasDemo.push({ id: Math.max(0, ...empresasDemo.map((e) => e.id)) + 1, ...d })
}

function editarEmpresa(id: number, d: { ruc: string; nombre: string; rubro: string }): void {
  const e = empresasDemo.find((x) => x.id === id)
  if (e) Object.assign(e, d)
}

function eliminarEmpresa(id: number): void {
  if (id === 1) throw Object.assign(new Error('Esta empresa todavía tiene equipos.'), { estado: 409 })
  const i = empresasDemo.findIndex((x) => x.id === id)
  if (i >= 0) empresasDemo.splice(i, 1)
}

function crearPersonal(_empresaId: number, d: { email: string; nombre: string; clave: string; rol: string }): void {
  void d
}

export const demo = { empresas, crearEmpresa, editarEmpresa, eliminarEmpresa, crearPersonal, contactos, solicitudes, crearContacto, desactivarContacto, atender, cerrar, cuestionarios, presupuesto, cambiarCuestionario, fijarPresupuesto, empleados, crearEmpleado, cambiarEstadoEmpleado, equipos, dashboard, perfil, crearEquipo, editarEquipo, eliminarEquipo }
