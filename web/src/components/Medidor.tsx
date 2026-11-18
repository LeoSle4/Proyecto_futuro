import { bandGeometry, percentOf, type Interval, type Scale } from '../lib/gauge'
import './Medidor.css'

interface Props {
  escala: Scale
  paso: number
  intervalo: Interval
  formato: (n: number) => string
  // Delay in ms before the band opens, so several gauges on one screen do not open together.
  retardo?: number
  // True while the interval is being simulated: the band glides instead of re-opening.
  desliza?: boolean
}

// One instrument for every indicator: a scale, a symmetric band for the 95 % interval, brackets with their
// values and the estimate. Hovering or focusing it explains what the band means.
export function Medidor({ escala, paso, intervalo, formato, retardo = 0, desliza = false }: Props) {
  const g = bandGeometry(intervalo, escala)
  const marcas = Math.round((escala.max - escala.min) / paso)
  const medio = (escala.min + escala.max) / 2
  const centro = g.left + g.width / 2
  return (
    <div
      className={`medidor${desliza ? ' desliza' : ''}`}
      tabIndex={0}
      role="img"
      aria-label={`Estimación ${formato(intervalo.estimate)}. El valor real puede estar entre ${formato(intervalo.lo)} y ${formato(intervalo.hi)}.`}
      style={{ '--bd': `${retardo}ms` } as React.CSSProperties}
    >
      <div className="m-eje" />
      {Array.from({ length: marcas + 1 }, (_, i) => {
        const mayor = i === 0 || i === marcas || i === Math.round(marcas / 2)
        return <i key={i} className={`m-marca${mayor ? ' mayor' : ''}`} style={{ left: `${(i / marcas) * 100}%` }} />
      })}
      <div className="m-banda halo" style={{ left: `${g.haloLeft}%`, width: `${g.haloWidth}%` }} />
      <div className="m-banda nucleo" style={{ left: `${g.left}%`, width: `${g.width}%` }} />
      <i className="m-corchete" style={{ left: `${g.left}%` }} />
      <i className="m-corchete" style={{ left: `${g.left + g.width}%` }} />
      <span className="m-valor" style={{ left: `${g.left}%` }}>{formato(intervalo.lo)}</span>
      <span className="m-valor" style={{ left: `${g.left + g.width}%` }}>{formato(intervalo.hi)}</span>
      <div className="m-estimado" style={{ left: `${g.estimate}%` }} />
      <span className="m-etiqueta inicio" style={{ left: 0 }}>{formato(escala.min)}</span>
      <span className="m-etiqueta" style={{ left: `${percentOf(medio, escala)}%` }}>{formato(medio)}</span>
      <span className="m-etiqueta fin" style={{ left: '100%' }}>{formato(escala.max)}</span>
      <div className="m-pista" style={{ left: `${centro}%` }}>El valor real puede estar en cualquier punto de esta banda</div>
    </div>
  )
}
