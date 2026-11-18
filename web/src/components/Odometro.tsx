import { useEffect, useState } from 'react'
import './Odometro.css'

// Three stacked 0-9 strips: the units digit makes two full turns and the tens one, like a real odometer.
const FRANJA = Array.from({ length: 30 }, (_, i) => i % 10)

const sinMovimiento = () => typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches

// Mount it with `key={texto}` so a new value rolls again.
export function Odometro({ texto, animar = true }: { texto: string; animar?: boolean }) {
  const quieto = !animar || sinMovimiento()
  const [listo, setListo] = useState(quieto)
  const [rodando, setRodando] = useState(!quieto)

  useEffect(() => {
    if (quieto) return
    const cuadro = requestAnimationFrame(() => setListo(true))
    const fin = setTimeout(() => setRodando(false), 1400)
    return () => {
      cancelAnimationFrame(cuadro)
      clearTimeout(fin)
    }
  }, [quieto])

  const total = (texto.match(/\d/g) ?? []).length
  let visto = 0
  return (
    <span className={`odo${listo ? ' on' : ''}${rodando ? ' rodando' : ''}`} role="img" aria-label={texto}>
      {[...texto].map((c, i) => {
        if (!/\d/.test(c)) {
          return (
            <span key={i} className="ch" aria-hidden="true">
              {c === ' ' ? ' ' : c}
            </span>
          )
        }
        const desdeLaDerecha = total - 1 - visto
        const vueltas = desdeLaDerecha === 0 ? 2 : desdeLaDerecha === 1 ? 1 : 0
        const estilo = { '--t': vueltas * 10 + Number(c), '--k': visto } as React.CSSProperties
        visto += 1
        return (
          <span key={i} className="dg" aria-hidden="true">
            <span className="st" style={estilo}>
              {FRANJA.map((n, j) => (
                <i key={j}>{n}</i>
              ))}
            </span>
          </span>
        )
      })}
    </span>
  )
}
