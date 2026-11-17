import './Neblina.css'

interface Props {
  estimado: number
  margen: number
  // Axis shown for the scale, e.g. 0 to 1 for a proportion.
  minimo: number
  maximo: number
  formato: (valor: number) => string
  etiquetaEje: [string, string]
}

const acotar = (v: number, a: number, b: number) => Math.min(b, Math.max(a, v))

// The estimate's uncertainty drawn as fog: the wider the haze, the less we can claim.
export function Neblina({ estimado, margen, minimo, maximo, formato, etiquetaEje }: Props) {
  const rango = maximo - minimo
  const pos = (v: number) => ((acotar(v, minimo, maximo) - minimo) / rango) * 100
  const desde = pos(estimado - margen)
  const hasta = pos(estimado + margen)
  const bajo = formato(acotar(estimado - margen, minimo, maximo))
  const alto = formato(acotar(estimado + margen, minimo, maximo))
  return (
    <div className="neblina">
      <div
        className="neblina-pista"
        role="img"
        aria-label={`Estimación ${formato(estimado)}. Puede estar entre ${bajo} y ${alto}.`}
      >
        <div className="neblina-bruma" style={{ left: `${desde}%`, width: `${hasta - desde}%` }} />
        <div className="neblina-nucleo" style={{ left: `${desde}%`, width: `${hasta - desde}%` }} />
        <div className="neblina-punto" style={{ left: `${pos(estimado)}%` }} />
      </div>
      <div className="neblina-eje" aria-hidden="true">
        <span>{etiquetaEje[0]}</span>
        <span>puede estar entre {bajo} y {alto}</span>
        <span>{etiquetaEje[1]}</span>
      </div>
    </div>
  )
}
