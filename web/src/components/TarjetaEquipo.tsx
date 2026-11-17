import type { Dashboard, Equipo } from '../api/tipos'
import { N_MINIMO } from '../api/tipos'
import './TarjetaEquipo.css'

interface Props {
  equipo: Equipo
  resumen: Dashboard | null | 'error'
  activo: boolean
  alElegir: () => void
}

export function TarjetaEquipo({ equipo, resumen, activo, alElegir }: Props) {
  const sinResultado = resumen !== null && resumen !== 'error' && resumen.resultado === 'DATOS_INSUFICIENTES'
  let detalle = 'Cargando…'
  if (resumen === 'error') detalle = 'No se pudo cargar'
  else if (resumen) {
    detalle = sinResultado
      ? `${resumen.totalRespuestas} de ${N_MINIMO} · sin resultado todavía`
      : `${resumen.totalRespuestas} respuestas · con resultado`
  }
  return (
    <button
      type="button"
      className={`tarjeta-equipo${activo ? ' activo' : ''}${sinResultado ? ' velo-equipo' : ''}`}
      aria-pressed={activo}
      onClick={alElegir}
    >
      <span className="tarjeta-equipo-nombre">{equipo.nombre}</span>
      <span className="tarjeta-equipo-detalle">{detalle}</span>
    </button>
  )
}
