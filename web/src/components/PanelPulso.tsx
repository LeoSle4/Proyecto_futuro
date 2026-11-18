import { useState } from 'react'
import type { ItemDashboard } from '../api/tipos'
import { N_MINIMO } from '../api/tipos'
import { simulateInterval } from '../lib/gauge'
import { Medidor } from './Medidor'
import { MedidorSegmentos } from './MedidorSegmentos'
import { Odometro } from './Odometro'
import './PanelPulso.css'

const pct = (n: number) => `${Math.round(n)} %`

interface Props {
  item: ItemDashboard | undefined
  total: number
}

// The headline indicator of the week. Mount it with a key per team and week so its state starts fresh.
export function PanelPulso({ item, total }: Props) {
  if (!item || item.proporcionEstimada === null || item.margenError95 === null) return <SinResultado tema={item?.tema} total={total} propias={item?.respuestas ?? null} />
  return <ConResultado tema={item.tema} total={total} estimado={Math.round(item.proporcionEstimada * 100)} mitad={item.margenError95 * 100} />
}

function ConResultado({ tema, total, estimado, mitad }: { tema: string; total: number; estimado: number; mitad: number }) {
  const [n, setN] = useState(total)
  const intervalo = simulateInterval({ estimate: estimado, half: mitad }, total, n)
  // Round, easy-to-read sample sizes above today's, never below it.
  const opciones = [...new Set([total, Math.max(100, total * 2), Math.max(300, total * 5)])]
  const simulando = n !== total
  return (
    <section className={`panelcard asienta${simulando ? ' simulando' : ''}`}>
      <div className="pc-cabeza">
        <div>
          <div className="pc-pregunta">
            Pulso · {tema}
            <span className="pc-simulado">SIMULADO</span>
          </div>
          <div className="pc-cifra">
            <b><Odometro key={estimado} texto={`${estimado} %`} /></b>
            <span>dijo que sí</span>
          </div>
          <p className="pc-pie">Puede estar entre {intervalo.lo} % y {intervalo.hi} %.</p>
        </div>
        <dl className="libro">
          <div><dt>Respuestas</dt><dd>{n}</dd></div>
          <div><dt>Mínimo para publicar</dt><dd>{N_MINIMO}</dd></div>
          <div><dt>Margen al 95 %</dt><dd>±{Math.round((intervalo.hi - intervalo.lo) / 2)} puntos</dd></div>
        </dl>
      </div>
      <Medidor escala={{ min: 0, max: 100 }} paso={10} intervalo={intervalo} formato={pct} retardo={700} desliza={simulando} />
      <div className="pc-sim">
        <span><b>¿Y con más respuestas?</b> Simulación: el margen baja con la raíz de n.</span>
        <div className="chips">
          {opciones.map((o, i) => (
            <button key={o} type="button" className="chip" aria-pressed={n === o} onClick={() => setN(o)}>
              {o}{i === 0 ? ' (hoy)' : ''}
            </button>
          ))}
        </div>
      </div>
    </section>
  )
}

function SinResultado({ tema, total, propias }: { tema: string | undefined; total: number; propias: number | null }) {
  // This item's own count when the server sends it; else the team total, fair only when the whole team is short.
  const n = propias ?? (total < N_MINIMO ? total : null)
  return (
    <section className="panelcard asienta">
      <div className="pc-cabeza">
        <div>
          <div className="pc-pregunta">Pulso{tema ? ` · ${tema}` : ''}</div>
          <div className="pc-cifra"><b className="apagada">Sin resultado</b></div>
          <p className="pc-pie">
            {n !== null
              ? `Con menos de ${N_MINIMO} respuestas no se publica nada, para proteger al grupo.`
              : `Este indicador todavía no llegó al mínimo de ${N_MINIMO} respuestas.`}
          </p>
        </div>
        <dl className="libro">
          <div><dt>{n !== null && propias !== null ? 'Respuestas del indicador' : 'Respuestas del equipo'}</dt><dd>{n ?? total}</dd></div>
          <div><dt>Mínimo para publicar</dt><dd>{N_MINIMO}</dd></div>
          {n !== null
            ? <div><dt>Faltan</dt><dd>{Math.max(0, N_MINIMO - n)}</dd></div>
            : <div><dt>Este indicador</dt><dd>bajo el mínimo</dd></div>}
        </dl>
      </div>
      {n !== null && <MedidorSegmentos tengo={n} minimo={N_MINIMO} />}
    </section>
  )
}
