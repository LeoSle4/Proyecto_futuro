import type { WeekRow } from '../lib/gauge'
import { summarizeWeek } from '../lib/gauge'
import { N_MINIMO } from '../api/tipos'
import './ResumenSemana.css'

// The week at a glance. Its title and numbers follow the week that is selected.
export function ResumenSemana({ rotulo, filas }: { rotulo: string; filas: Array<WeekRow | null> }) {
  const r = summarizeWeek(filas)
  return (
    <section className="semana-resumen sube" style={{ '--i': 6 } as React.CSSProperties}>
      <h2>{rotulo}</h2>
      <dl>
        <div><dt>Equipos con resultado</dt><dd>{r.conResultado} de {r.total}</dd></div>
        <div><dt>Respuestas en total</dt><dd>{r.respuestas}</dd></div>
        <div><dt>Mínimo para publicar</dt><dd>{N_MINIMO}</dd></div>
      </dl>
    </section>
  )
}
