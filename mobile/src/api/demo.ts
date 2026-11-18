import type { Cuestionario, Contacto } from './tipos'

// Synthetic questionnaire so the app runs without a backend (EXPO_PUBLIC_DEMO=1).
export const cuestionariosDemo: Cuestionario[] = [
  {
    id: 1, codigo: 'PULSO', nombre: 'Pulso semanal', itemsPorEnvio: null,
    items: [
      {
        itemId: 1, orden: 1, tema: 'Pulso', texto: '¿Hoy te sentiste bien?',
        configuracionLdpId: 1, mecanismo: 'RANDOMIZED_RESPONSE', probabilidadVerdad: 0.75, epsilon: null,
        escala: { codigo: 'BINARIA', valorMin: 0, valorMax: 1, opciones: [{ valor: 0, etiqueta: 'No' }, { valor: 1, etiqueta: 'Sí' }] },
      },
      {
        itemId: 2, orden: 2, tema: 'Calma', texto: 'Me he sentido tranquilo(a) y relajado(a)',
        configuracionLdpId: 2, mecanismo: 'LAPLACE', probabilidadVerdad: null, epsilon: 1,
        escala: {
          codigo: 'WHO5', valorMin: 0, valorMax: 5,
          opciones: [
            { valor: 0, etiqueta: 'En ningún momento' }, { valor: 1, etiqueta: 'Pocas veces' }, { valor: 2, etiqueta: 'Menos de la mitad del tiempo' },
            { valor: 3, etiqueta: 'Más de la mitad del tiempo' }, { valor: 4, etiqueta: 'Casi todo el tiempo' }, { valor: 5, etiqueta: 'Todo el tiempo' },
          ],
        },
      },
    ],
  },
]

export const contactosDemo: Contacto[] = [
  { id: 1, nombre: 'Bienestar de tu empresa (ejemplo sintético)', tipo: 'COORDINADOR', telefono: null, email: 'bienestar@ejemplo.pe' },
]
