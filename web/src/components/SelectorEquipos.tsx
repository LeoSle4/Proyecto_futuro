import { useLayoutEffect, useRef, useState } from 'react'
import type { Dashboard, Equipo } from '../api/tipos'
import { N_MINIMO } from '../api/tipos'
import './SelectorEquipos.css'

type Resumen = Dashboard | 'error' | undefined

interface Props {
  equipos: Equipo[]
  resumenes: Record<string, Resumen>
  semana: string
  activo: number | null
  alElegir: (id: number) => void
}

function detalle(r: Resumen): { texto: string; sinResultado: boolean } {
  if (r === undefined) return { texto: 'Cargando…', sinResultado: false }
  if (r === 'error') return { texto: 'No se pudo cargar', sinResultado: false }
  return r.resultado === 'DATOS_INSUFICIENTES'
    ? { texto: `${r.totalRespuestas} de ${N_MINIMO} · sin resultado todavía`, sinResultado: true }
    : { texto: `${r.totalRespuestas} respuestas · con resultado`, sinResultado: false }
}

// The teams as a list with one teal indicator that slides to the chosen one (transform only).
export function SelectorEquipos({ equipos, resumenes, semana, activo, alElegir }: Props) {
  const botones = useRef(new Map<number, HTMLButtonElement>())
  const [pos, setPos] = useState<{ y: number; h: number } | null>(null)
  const [listo, setListo] = useState(false)

  useLayoutEffect(() => {
    const el = activo === null ? undefined : botones.current.get(activo)
    if (el) setPos({ y: el.offsetTop, h: el.offsetHeight })
  }, [activo, equipos.length])

  useLayoutEffect(() => {
    if (pos && !listo) {
      const cuadro = requestAnimationFrame(() => setListo(true))
      return () => cancelAnimationFrame(cuadro)
    }
  }, [pos, listo])

  return (
    <div className="equipos">
      {pos && (
        <i
          className={`ind${listo ? ' listo' : ''}`}
          style={{ height: pos.h, transform: `translateY(${pos.y}px)` }}
          aria-hidden="true"
        />
      )}
      {equipos.map((e) => {
        const d = detalle(resumenes[`${semana}:${e.id}`])
        const activa = e.id === activo
        return (
          <button
            key={e.id}
            ref={(el) => {
              if (el) botones.current.set(e.id, el)
              else botones.current.delete(e.id)
            }}
            type="button"
            className={`equipo eleva${activa ? ' activo' : ''}${d.sinResultado && !activa ? ' hatch' : ''}`}
            aria-pressed={activa}
            onClick={() => alElegir(e.id)}
          >
            <b>{e.nombre}</b>
            <span>{d.texto}</span>
          </button>
        )
      })}
    </div>
  )
}
