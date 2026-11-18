import { useEffect } from 'react'
import './Aviso.css'

// A confirmation that floats in a corner and leaves on its own, so the page never jumps. It is also a live region.
export function Aviso({ mensaje, alCerrar, ms = 6000 }: { mensaje: string; alCerrar: () => void; ms?: number }) {
  useEffect(() => {
    if (!mensaje) return
    const t = setTimeout(alCerrar, ms)
    return () => clearTimeout(t)
  }, [mensaje, alCerrar, ms])
  return (
    <div className="aviso-zona" role="status" aria-live="polite">
      {mensaje && (
        <p key={mensaje} className="aviso">
          <span>{mensaje}</span>
          <button type="button" onClick={alCerrar} aria-label="Cerrar aviso">Cerrar</button>
        </p>
      )}
    </div>
  )
}
