import type { ItemDashboard } from '../api/tipos'
import { N_MINIMO } from '../api/tipos'
import { Medidor } from './Medidor'
import { MedidorSegmentos } from './MedidorSegmentos'
import './TarjetaIndicador.css'

// Decimal comma, as in the rest of the product copy.
const dec = (n: number) => String(Math.round(n * 10) / 10).replace('.', ',')
const pct = (n: number) => `${Math.round(n)} %`
const redondea = (n: number) => Math.round(n * 10) / 10

interface Props {
  item: ItemDashboard
  total: number
  retardo: number
}

// Any indicator other than the headline: a mean on its scale, or a group still short of the minimum.
export function TarjetaIndicador({ item, total, retardo }: Props) {
  const margen = item.margenError95
  if (margen !== null && item.mediaEstimada !== null) {
    const min = item.valorMin ?? 0
    const max = item.valorMax ?? Math.max(Math.ceil(item.mediaEstimada + margen), 5)
    const lo = redondea(Math.max(min, item.mediaEstimada - margen))
    const hi = redondea(Math.min(max, item.mediaEstimada + margen))
    return (
      <article className="tarjeta eleva sube">
        <h3>{item.tema}</h3>
        <p className="t-valor">{dec(item.mediaEstimada)} <small>de {max}</small></p>
        <Medidor escala={{ min, max }} paso={1} intervalo={{ estimate: item.mediaEstimada, lo, hi }} formato={dec} retardo={retardo} />
        <p className="t-nota">Entre {dec(lo)} y {dec(hi)} con {total} respuestas. Más respuestas, menos margen.</p>
      </article>
    )
  }
  if (margen !== null && item.proporcionEstimada !== null) {
    const est = item.proporcionEstimada * 100
    return (
      <article className="tarjeta eleva sube">
        <h3>{item.tema}</h3>
        <p className="t-valor">{pct(est)} <small>dijo que sí</small></p>
        <Medidor
          escala={{ min: 0, max: 100 }} paso={10} formato={pct} retardo={retardo}
          intervalo={{ estimate: est, lo: Math.max(0, est - margen * 100), hi: Math.min(100, est + margen * 100) }}
        />
        <p className="t-nota">Con {total} respuestas. Más respuestas, menos margen.</p>
      </article>
    )
  }
  // The server sends the team's total, not this item's own count. Only when the whole team is under the minimum
  // is the total a fair measure; otherwise we say the item fell short without inventing a number.
  const equipoBajo = total < N_MINIMO
  return (
    <article className="tarjeta hatch sube">
      <h3>{item.tema}</h3>
      <p className="t-valor apagado">Sin resultado</p>
      {equipoBajo && <MedidorSegmentos tengo={total} minimo={N_MINIMO} />}
      <p className="t-nota">
        {equipoBajo
          ? `${Math.max(0, N_MINIMO - total)} respuestas para llegar al mínimo. Con menos de ${N_MINIMO} no se publica nada.`
          : `Este indicador todavía no llegó al mínimo de ${N_MINIMO} respuestas. Con menos no se publica nada.`}
      </p>
    </article>
  )
}
