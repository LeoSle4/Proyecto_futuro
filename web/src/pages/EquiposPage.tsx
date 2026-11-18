import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { api, ErrorApi } from '../api/cliente'
import type { Equipo } from '../api/tipos'
import { N_MINIMO } from '../api/tipos'
import { useSesion } from '../auth/SesionContext'
import { Aviso } from '../components/Aviso'
import { validarEquipo, type ErroresEquipo } from '../lib/equipo'
import './EquiposPage.css'

const retraso = (i: number) => ({ '--i': i }) as React.CSSProperties

export function EquiposPage() {
  const { perfil, cerrar } = useSesion()
  const empresaId = perfil?.empresaId
  const puedeEscribir = Boolean(perfil?.roles.some((r) => r === 'ADMIN' || r === 'RRHH')) && empresaId !== undefined

  const [equipos, setEquipos] = useState<Equipo[] | null>(null)
  const [nuevo, setNuevo] = useState('')
  const [errorNuevo, setErrorNuevo] = useState<ErroresEquipo>({})
  const [editando, setEditando] = useState<number | null>(null)
  const [texto, setTexto] = useState('')
  const [errorEdicion, setErrorEdicion] = useState<ErroresEquipo>({})
  const [borrando, setBorrando] = useState<number | null>(null)
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState('')
  const campoNuevo = useRef<HTMLInputElement>(null)
  const titulo = useRef<HTMLHeadingElement>(null)

  const cargar = useCallback(async () => {
    try {
      setEquipos(await api.equipos())
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 401) void cerrar()
      else setError(e instanceof Error ? e.message : 'No pudimos cargar los equipos.')
    }
  }, [cerrar])

  useEffect(() => {
    let vigente = true
    api.equipos().then(
      (lista) => vigente && setEquipos(lista),
      (e: unknown) => {
        if (!vigente) return
        if (e instanceof ErrorApi && e.estado === 401) void cerrar()
        else setError(e instanceof Error ? e.message : 'No pudimos cargar los equipos.')
      },
    )
    return () => {
      vigente = false
    }
  }, [cerrar])

  // Runs one change, refreshes the list and tells screen readers what happened.
  async function ejecutar(accion: () => Promise<unknown>, mensaje: string, alFallar?: (mensaje: string) => void) {
    setError(null)
    setOcupado(true)
    try {
      await accion()
      await cargar()
      setAviso(mensaje)
      return true
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 401) void cerrar()
      else {
        const mensajeError = e instanceof Error ? e.message : 'No pudimos completar el cambio.'
        if (alFallar) alFallar(mensajeError)
        else setError(mensajeError)
      }
      return false
    } finally {
      setOcupado(false)
    }
  }

  async function agregar(e: FormEvent) {
    e.preventDefault()
    const nombre = nuevo.trim()
    const problemas = validarEquipo(nombre)
    setErrorNuevo(problemas)
    if (Object.keys(problemas).length > 0) {
      campoNuevo.current?.focus()
      return
    }
    if (!nombre || empresaId === undefined) return
    setErrorNuevo({})
    if (await ejecutar(() => api.crearEquipo(nombre, empresaId), `Equipo ${nombre} agregado.`, (mensajeError) => setErrorNuevo({ nombre: mensajeError }))) {
      setNuevo('')
      campoNuevo.current?.focus()
    }
  }

  async function guardar(e: FormEvent, equipo: Equipo) {
    e.preventDefault()
    const nombre = texto.trim()
    const problemas = validarEquipo(nombre)
    setErrorEdicion(problemas)
    if (Object.keys(problemas).length > 0) return
    if (!nombre || empresaId === undefined) return
    setErrorEdicion({})
    if (await ejecutar(() => api.editarEquipo(equipo.id, nombre, empresaId), `Equipo renombrado a ${nombre}.`, (mensajeError) => setErrorEdicion({ nombre: mensajeError }))) {
      setEditando(null)
      requestAnimationFrame(() => document.querySelector<HTMLButtonElement>(`[data-editar-equipo="${equipo.id}"]`)?.focus())
    }
  }

  async function eliminar(equipo: Equipo) {
    if (await ejecutar(() => api.eliminarEquipo(equipo.id), `Equipo ${equipo.nombre} eliminado.`)) {
      setBorrando(null)
      requestAnimationFrame(() => (puedeEscribir ? campoNuevo.current?.focus() : titulo.current?.focus()))
    }
  }

  return (
    <section className="equipos-pagina">
      <h1 ref={titulo} tabIndex={-1}><span className="mascara"><span>Equipos</span></span></h1>
      <p className="equipos-intro sube" style={retraso(2)}>
        Los grupos de personas cuyas respuestas se estiman juntas. Un equipo muestra resultado cuando llega a 30 respuestas.
      </p>

      <div className="equipos-cuerpo">
      <div className="equipos-principal">
      {puedeEscribir ? (
        <form className="equipos-nuevo sube" style={retraso(3)} onSubmit={(e) => void agregar(e)} noValidate>
          <label htmlFor="nuevo-equipo">Nombre del equipo</label>
          <div className="equipos-fila">
            <input id="nuevo-equipo" ref={campoNuevo} value={nuevo} onChange={(e) => setNuevo(e.target.value)} maxLength={150} autoComplete="off" aria-invalid={errorNuevo.nombre ? true : undefined} aria-describedby={errorNuevo.nombre ? 'error-nuevo-equipo' : undefined} />
            <button type="submit" className="boton boton-primario" disabled={ocupado || !nuevo.trim()}>Agregar equipo</button>
          </div>
          {errorNuevo.nombre && <p id="error-nuevo-equipo" role="alert" className="equipos-campo-error">{errorNuevo.nombre}</p>}
        </form>
      ) : (
        <p className="equipos-nota sube" style={retraso(3)}>
          {empresaId === undefined
            ? 'Tu cuenta no tiene una empresa asignada, así que aquí solo puedes mirar.'
            : 'Tu rol puede ver los equipos, pero solo RRHH y administración pueden cambiarlos.'}
        </p>
      )}

      {error && <p role="alert" className="equipos-error">{error}</p>}
      <Aviso mensaje={aviso} alCerrar={() => setAviso('')} />

      {equipos === null && !error && <p className="equipos-nota">Cargando equipos…</p>}
      {equipos?.length === 0 && (
        <div className="equipos-vacio hatch">
          <h2>Todavía no hay equipos</h2>
          <p>{puedeEscribir ? 'Agrega el primero para empezar a recibir estimaciones.' : 'Cuando alguien agregue el primero, aparecerá aquí.'}</p>
        </div>
      )}

      {equipos && equipos.length > 0 && (
        <ul className="equipos-lista sube" style={retraso(4)}>
          {equipos.map((equipo) => (
            <li key={equipo.id} className="equipos-item">
              {editando === equipo.id ? (
                <form className="equipos-fila" onSubmit={(e) => void guardar(e, equipo)} noValidate>
                  <label className="solo-lectores" htmlFor={`editar-${equipo.id}`}>Nuevo nombre de {equipo.nombre}</label>
                  <input id={`editar-${equipo.id}`} value={texto} onChange={(e) => setTexto(e.target.value)} maxLength={150} autoFocus aria-invalid={errorEdicion.nombre ? true : undefined} aria-describedby={errorEdicion.nombre ? `error-editar-${equipo.id}` : undefined} />
                  <button type="submit" className="boton boton-primario" disabled={ocupado || !texto.trim()}>Guardar</button>
                  <button type="button" className="boton" onClick={() => { setEditando(null); setErrorEdicion({}) }} disabled={ocupado}>Cancelar</button>
                  {errorEdicion.nombre && <p id={`error-editar-${equipo.id}`} role="alert" className="equipos-campo-error">{errorEdicion.nombre}</p>}
                </form>
              ) : borrando === equipo.id ? (
                <div className="equipos-fila confirma">
                  <p><b>¿Eliminar {equipo.nombre}?</b> El equipo dejará de estar disponible y esta acción no se puede deshacer.</p>
                  <button type="button" className="boton boton-peligro" onClick={() => void eliminar(equipo)} disabled={ocupado}>Eliminar</button>
                  <button type="button" className="boton" onClick={() => setBorrando(null)} disabled={ocupado}>Cancelar</button>
                </div>
              ) : (
                <div className="equipos-fila">
                  <span className="equipos-nombre">{equipo.nombre}</span>
                  {puedeEscribir && (
                    <span className="equipos-acciones">
                      <button type="button" className="boton boton-contorno" data-editar-equipo={equipo.id} onClick={() => { setEditando(equipo.id); setTexto(equipo.nombre); setErrorEdicion({}); setBorrando(null) }}>Editar</button>
                      <button type="button" className="boton" onClick={() => { setBorrando(equipo.id); setEditando(null) }}>Eliminar</button>
                    </span>
                  )}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
      </div>
      <aside className="equipos-ficha sube" style={retraso(5)} aria-label="Resumen de equipos">
        <h2>Equipos</h2>
        <dl>
          <div><dt>En tu empresa</dt><dd>{equipos?.length ?? '—'}</dd></div>
          <div><dt>Mínimo para publicar</dt><dd>{N_MINIMO}</dd></div>
          <div><dt>Quién los cambia</dt><dd>RRHH</dd></div>
        </dl>
      </aside>
      </div>
    </section>
  )
}
