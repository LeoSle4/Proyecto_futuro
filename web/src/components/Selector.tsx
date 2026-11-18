import { useEffect, useRef, useState } from 'react'
import { buscarPorPrefijo, moverIndice, type OpcionSelector } from '../lib/selector'
import './Selector.css'

interface Props {
  id: string
  valor: string
  opciones: OpcionSelector[]
  alCambiar: (valor: string) => void
  // Name of the list for screen readers; the visible <label htmlFor={id}> names the control itself.
  etiqueta?: string
  marcador?: string
  invalido?: boolean
  descritoPor?: string
  deshabilitado?: boolean
}

// A dropdown drawn with the panel's own design (the native menu cannot be styled), following the ARIA
// "select-only combobox" pattern: arrows, Home/End, Enter or Space to choose, Escape to close, type-ahead.
export function Selector({ id, valor, opciones, alCambiar, etiqueta = 'Opciones', marcador = 'Elige una opción', invalido, descritoPor, deshabilitado }: Props) {
  const [abierto, setAbierto] = useState(false)
  const [activo, setActivo] = useState(-1)
  const raiz = useRef<HTMLDivElement>(null)
  const boton = useRef<HTMLButtonElement>(null)
  const lista = useRef<HTMLUListElement>(null)
  const escrito = useRef({ texto: '', hasta: 0 })
  const idLista = `${id}-lista`
  const elegida = opciones.findIndex((o) => o.valor === valor)

  function abrir() {
    if (deshabilitado) return
    setAbierto(true)
    setActivo(elegida >= 0 ? elegida : 0)
  }
  function cerrar(devolverFoco: boolean) {
    setAbierto(false)
    if (devolverFoco) boton.current?.focus()
  }
  function elegir(i: number) {
    const o = opciones[i]
    if (!o) return
    alCambiar(o.valor)
    cerrar(true)
  }

  useEffect(() => {
    if (!abierto) return
    const fuera = (e: MouseEvent) => {
      if (!raiz.current?.contains(e.target as Node)) setAbierto(false)
    }
    document.addEventListener('mousedown', fuera)
    return () => document.removeEventListener('mousedown', fuera)
  }, [abierto])

  useEffect(() => {
    if (abierto && activo >= 0) lista.current?.children[activo]?.scrollIntoView({ block: 'nearest' })
  }, [abierto, activo])

  function alTeclear(e: React.KeyboardEvent) {
    const k = e.key
    if (k === 'ArrowDown' || k === 'ArrowUp' || k === 'Home' || k === 'End') {
      e.preventDefault()
      if (!abierto) abrir()
      else setActivo(moverIndice(activo, k, opciones.length))
    } else if (k === 'Enter' || k === ' ') {
      e.preventDefault()
      if (abierto) elegir(activo)
      else abrir()
    } else if (k === 'Escape') {
      if (abierto) {
        e.preventDefault()
        cerrar(true)
      }
    } else if (k === 'Tab') {
      if (abierto) setAbierto(false)
    } else if (k.length === 1 && !e.ctrlKey && !e.metaKey && !e.altKey) {
      const ahora = Date.now()
      escrito.current.texto = ahora > escrito.current.hasta ? k : escrito.current.texto + k
      escrito.current.hasta = ahora + 700
      const i = buscarPorPrefijo(opciones, escrito.current.texto, abierto ? activo : elegida)
      if (i >= 0) {
        if (abierto) setActivo(i)
        else alCambiar(opciones[i].valor)
      }
    }
  }

  return (
    <div className="selector" ref={raiz}>
      <button
        ref={boton}
        id={id}
        type="button"
        role="combobox"
        className={`sel-boton${elegida < 0 ? ' marcador' : ''}${invalido ? ' malo' : ''}`}
        aria-haspopup="listbox"
        aria-expanded={abierto}
        aria-controls={idLista}
        aria-activedescendant={abierto && activo >= 0 ? `${id}-op-${activo}` : undefined}
        aria-invalid={invalido ? true : undefined}
        aria-describedby={descritoPor}
        disabled={deshabilitado}
        onClick={() => (abierto ? cerrar(false) : abrir())}
        onKeyDown={alTeclear}
      >
        <span>{elegida >= 0 ? opciones[elegida].texto : marcador}</span>
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M3 5.5l4 4 4-4" /></svg>
      </button>
      {abierto && (
        <ul ref={lista} id={idLista} role="listbox" aria-label={etiqueta} className="sel-lista">
          {opciones.map((o, i) => (
            <li
              key={o.valor}
              id={`${id}-op-${i}`}
              role="option"
              aria-selected={i === elegida}
              className={i === activo ? 'activa' : undefined}
              onMouseEnter={() => setActivo(i)}
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => elegir(i)}
            >
              <span>{o.texto}</span>
              {i === elegida && (
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M3.5 8.5l3 3 6-7" /></svg>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
