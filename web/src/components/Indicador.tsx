import type { ItemDashboard } from '../api/tipos'
import { N_MINIMO } from '../api/tipos'
import { Neblina } from './Neblina'
import { Velo } from './Velo'
import './Indicador.css'

const porcentaje = (v: number) => `${Math.round(v * 100)} %`
const decimal = (v: number) => v.toLocaleString('es-PE', { maximumFractionDigits: 1 })

interface Props {
  item: ItemDashboard
  respuestas: number
  principal?: boolean
}

export function Indicador({ item, respuestas, principal = false }: Props) {
  const esProporcion = item.proporcionEstimada !== null
  const estimado = item.proporcionEstimada ?? item.mediaEstimada
  const sinResultado = estimado === null || item.margenError95 === null

  if (sinResultado) {
    return (
      <Velo as="section" className="indicador">
        <h2 className="indicador-titulo">{item.tema}</h2>
        <p className="indicador-cifra indicador-cifra-apagada">Sin resultado</p>
        <p className="indicador-nota">
          {respuestas} de {N_MINIMO} respuestas. Con menos de {N_MINIMO} no se publica nada para proteger al grupo.
        </p>
      </Velo>
    )
  }

  const margen = item.margenError95 as number
  // The scale range comes from the server; without it (older backend) the axis is approximated around the estimate.
  const minimo = esProporcion ? 0 : (item.valorMin ?? Math.min(0, estimado - margen))
  const maximo = esProporcion ? 1 : (item.valorMax ?? Math.max(estimado + margen, 1))
  const formato = esProporcion ? porcentaje : decimal

  return (
    <section className="indicador">
      <h2 className="indicador-titulo">{item.tema}</h2>
      <p className={principal ? 'indicador-cifra indicador-cifra-grande' : 'indicador-cifra'}>
        {formato(estimado)}
        {esProporcion && <span className="indicador-unidad"> dijo que sí</span>}
      </p>
      <Neblina
        estimado={estimado}
        margen={margen}
        minimo={minimo}
        maximo={maximo}
        formato={formato}
        etiquetaEje={[formato(minimo), formato(maximo)]}
      />
      <p className="indicador-nota">
        Estimación con {respuestas} respuestas. La neblina marca lo que todavía no podemos afirmar.
      </p>
    </section>
  )
}
