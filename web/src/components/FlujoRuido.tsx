import { useEffect, useRef } from 'react'
import { debias, expectedObserved, FLIP_SHARE, isFlipped, isYes, trackX } from '../lib/flow'
import './FlujoRuido.css'

const W = 640
const H = 128
const COLS = 16
const ROWS = [16, 48, 80, 112]
const GATE = W / 2
const SPEED = 26 // px per second, linear in progress
const SHARE_YES = 0.65

interface Punto { fila: number; desfase: number; si: boolean; id: number }

const puntos: Punto[] = ROWS.flatMap((_, fila) =>
  Array.from({ length: COLS }, (_, i) => {
    const id = fila * COLS + i
    return { fila, id, desfase: i * (W / COLS) + (fila % 2) * (W / COLS / 2), si: isYes(id, SHARE_YES) }
  }),
)

function estado(p: Punto, t: number) {
  const avance = p.desfase + SPEED * t
  const vuelta = Math.floor(avance / W)
  const u = (avance - vuelta * W) / W
  return { vuelta, u, x: trackX(u, W) }
}

function pintar(ctx: CanvasRenderingContext2D, t: number) {
  ctx.clearRect(0, 0, W, H)
  const luz = ctx.createLinearGradient(GATE - 34, 0, GATE + 34, 0)
  luz.addColorStop(0, 'rgba(207,230,236,0)')
  luz.addColorStop(0.5, 'rgba(207,230,236,0.13)')
  luz.addColorStop(1, 'rgba(207,230,236,0)')
  ctx.fillStyle = luz
  ctx.fillRect(GATE - 34, 0, 68, H)
  ctx.strokeStyle = 'rgba(207,230,236,0.4)'
  ctx.setLineDash([3, 5])
  ctx.beginPath()
  ctx.moveTo(GATE, 2)
  ctx.lineTo(GATE, H - 2)
  ctx.stroke()
  ctx.setLineDash([])

  for (const p of puntos) {
    const { vuelta, u, x } = estado(p, t)
    const cambia = isFlipped(p.id, vuelta)
    const despues = u > 0.5
    const si = despues && cambia ? !p.si : p.si
    const borde = Math.min(1, x / 46, (W - x) / 46)
    const dx = x - GATE
    const y = ROWS[p.fila]

    // A ring leaves the spot where a value changes, then fades: after the gate you cannot tell which ones did.
    if (despues && cambia && dx > 0 && dx < 90) {
      const q = dx / 90
      ctx.globalAlpha = 0.7 * (1 - q) * (1 - q)
      ctx.strokeStyle = '#7fd0e3'
      ctx.lineWidth = 1.5
      ctx.beginPath()
      ctx.arc(x, y, 5 + q * 11, 0, Math.PI * 2)
      ctx.stroke()
    }
    const escala = despues && cambia && dx < 34 ? 1 + 0.45 * (1 - dx / 34) : 1
    ctx.globalAlpha = 0.95 * borde
    ctx.strokeStyle = '#cfe6ec'
    ctx.fillStyle = '#cfe6ec'
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.arc(x, y, 5 * escala, 0, Math.PI * 2)
    if (si) ctx.fill()
    else ctx.stroke()
  }
  ctx.globalAlpha = 1
}

// The numbers shown under the diagram are expected values, not a sample of 64 dots (a sample that small swings
// by ten points, which is the lesson of the panel, not of this diagram): the real share of "sí" among the dots,
// what is seen on average after the noise, and what the panel recovers from it.
const REAL = puntos.filter((p) => p.si).length / puntos.length
const VISTO = expectedObserved(REAL)
const PORCENTAJES = { real: Math.round(REAL * 100), visto: Math.round(VISTO * 100), panel: Math.round(debias(VISTO) * 100) }

// Answers travel left to right, slow down through the noise gate and about one in four changes value there.
// The two percentages are counted from the dots on screen: individuals change, the total hardly moves.
export function FlujoRuido() {
  const lienzo = useRef<HTMLCanvasElement>(null)
  
  useEffect(() => {
    const canvas = lienzo.current
    const ctx = canvas?.getContext('2d')
    if (!canvas || !ctx) return
    const quieto = matchMedia('(prefers-reduced-motion: reduce)').matches
    let cuadro = 0
    let inicio = 0
    const ajustar = () => {
      const dpr = window.devicePixelRatio || 1
      const ancho = canvas.clientWidth || W
      canvas.width = Math.round(ancho * dpr)
      canvas.height = Math.round((ancho * H) / W * dpr)
      ctx.setTransform((canvas.width / W), 0, 0, (canvas.width / W), 0, 0)
      if (quieto) pintar(ctx, 7)
    }
    ajustar()
    const observador = new ResizeObserver(ajustar)
    observador.observe(canvas)

    const bucle = (ahora: number) => {
      if (!inicio) inicio = ahora
      pintar(ctx, (ahora - inicio) / 1000 + 3)
      cuadro = requestAnimationFrame(bucle)
    }
    if (!quieto) cuadro = requestAnimationFrame(bucle)

    return () => {
      cancelAnimationFrame(cuadro)
      observador.disconnect()
    }
  }, [])

  return (
    <div className="flujo">
      <p className="solo-lectores">
        Diagrama: cada punto es una respuesta, llena si es sí y vacía si es no. Al pasar por la línea de ruido, a una de cada cuatro
        se le cambia el valor. El porcentaje de síes casi no cambia ({Math.round(FLIP_SHARE * 100)} % de las respuestas se alteran).
      </p>
      <canvas ref={lienzo} className="flujo-lienzo" aria-hidden="true" />
      <div className="flujo-pie"><span>Tu respuesta</span><em>ruido: 1 de cada 4 cambia</em><span>Lo que sale</span></div>
      <div className="flujo-total" aria-hidden="true">
        <span>Sí: <em>{PORCENTAJES.real} %</em></span><span /><span>Sí, en promedio: <em>≈ {PORCENTAJES.visto} %</em></span>
      </div>
      <p className="flujo-corrige" aria-hidden="true">Quien recibe lo corrige con matemática: ≈ {PORCENTAJES.visto} % → <em>{PORCENTAJES.panel} %</em></p>
      <div className="flujo-leyenda" aria-hidden="true">
        <span><i className="si" />Sí</span><span><i className="no" />No</span>
      </div>
    </div>
  )
}
