import type { Dashboard, Equipo, Perfil } from './tipos'

// Synthetic data for working without the backend (VITE_DEMO=1). The UI labels it as synthetic.
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
      { itemId: 1, tema: '¿Hoy te sentiste bien?', proporcionEstimada: suficiente ? 0.65 : null, margenError95: suficiente ? 0.31 : null, mediaEstimada: null },
      { itemId: 2, tema: 'Me he sentido tranquilo(a) y relajado(a)', proporcionEstimada: null, margenError95: suficiente ? 2.2 : null, mediaEstimada: suficiente ? 3.2 : null, valorMin: 0, valorMax: 5 },
      { itemId: 3, tema: 'Poco interés o placer en hacer cosas', proporcionEstimada: null, margenError95: null, mediaEstimada: null },
    ],
  }
}

const perfil: Perfil = { username: 'rrhh@demo.kala', uid: 1, roles: ['RRHH'], empresaId: 1 }

export const demo = { equipos, dashboard, perfil }
