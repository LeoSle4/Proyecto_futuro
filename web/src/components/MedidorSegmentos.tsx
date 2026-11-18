import './Medidor.css'
import './MedidorSegmentos.css'

// Where a group stands against the publishing minimum: one segment per required response.
export function MedidorSegmentos({ tengo, minimo }: { tengo: number; minimo: number }) {
  return (
    <div className="medidor" role="img" aria-label={`${tengo} de ${minimo} respuestas`}>
      <div className="seg">
        {Array.from({ length: minimo }, (_, i) => (
          <i key={i} className={i < tengo ? 'lleno' : ''} />
        ))}
      </div>
      <span className="m-etiqueta inicio" style={{ left: 0 }}>0</span>
      <span className="m-etiqueta fin" style={{ left: '100%' }}>{minimo} (mínimo)</span>
    </div>
  )
}
