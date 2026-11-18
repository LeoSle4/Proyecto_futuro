import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api, ErrorApi } from '../api/cliente'
import type { Empresa } from '../api/tipos'
import { Aviso } from '../components/Aviso'
import { Selector } from '../components/Selector'
import { useSesion } from '../auth/SesionContext'
import { ROLES_PERSONAL, validarEmpresa, validarNuevoPersonal, type ErroresEmpresa, type ErroresPersonal } from '../lib/empresa'
import './EmpresasPage.css'

const retraso = (i: number) => ({ '--i': i }) as React.CSSProperties
const EMPRESA_VACIA = { ruc: '', nombre: '', rubro: '' }
const PERSONAL_VACIO = { nombre: '', email: '', clave: '', rol: '' }

type Modo = { tipo: 'editar' | 'borrar' | 'personal'; id: number } | null

export function EmpresasPage() {
  const { perfil, cerrar } = useSesion()
  const esAdmin = Boolean(perfil?.roles.includes('ADMIN'))
  const [empresas, setEmpresas] = useState<Empresa[] | null>(null)
  const [nueva, setNueva] = useState(EMPRESA_VACIA)
  const [erroresNueva, setErroresNueva] = useState<ErroresEmpresa>({})
  const [modo, setModo] = useState<Modo>(null)
  const [edicion, setEdicion] = useState(EMPRESA_VACIA)
  const [erroresEdicion, setErroresEdicion] = useState<ErroresEmpresa>({})
  const [personal, setPersonal] = useState(PERSONAL_VACIO)
  const [erroresPersonal, setErroresPersonal] = useState<ErroresPersonal>({})
  const [verClave, setVerClave] = useState(false)
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState('')

  const manejar = useCallback((e: unknown, porDefecto: string) => {
    if (e instanceof ErrorApi && e.estado === 401) void cerrar()
    else setError(e instanceof Error ? e.message : porDefecto)
  }, [cerrar])

  useEffect(() => {
    if (!esAdmin) return
    let vigente = true
    api.empresas().then((l) => vigente && setEmpresas(l), (e: unknown) => vigente && manejar(e, 'No pudimos cargar las empresas.'))
    return () => {
      vigente = false
    }
  }, [esAdmin, manejar])

  async function ejecutar(accion: () => Promise<unknown>, mensaje: string): Promise<boolean> {
    setError(null)
    setOcupado(true)
    try {
      await accion()
      setEmpresas(await api.empresas())
      setAviso(mensaje)
      return true
    } catch (e) {
      manejar(e, 'No pudimos completar el cambio.')
      return false
    } finally {
      setOcupado(false)
    }
  }

  async function crear(e: FormEvent) {
    e.preventDefault()
    const p = validarEmpresa(nueva)
    setErroresNueva(p)
    if (Object.keys(p).length > 0) return
    if (await ejecutar(() => api.crearEmpresa(nueva), `Empresa ${nueva.nombre.trim()} creada.`)) setNueva(EMPRESA_VACIA)
  }

  async function guardar(e: FormEvent, id: number) {
    e.preventDefault()
    const p = validarEmpresa(edicion)
    setErroresEdicion(p)
    if (Object.keys(p).length > 0) return
    if (await ejecutar(() => api.editarEmpresa(id, edicion), `Empresa ${edicion.nombre.trim()} actualizada.`)) setModo(null)
  }

  async function altaPersonal(e: FormEvent, empresa: Empresa) {
    e.preventDefault()
    const p = validarNuevoPersonal(personal)
    setErroresPersonal(p)
    if (Object.keys(p).length > 0) return
    const rol = ROLES_PERSONAL.find((r) => r.valor === personal.rol)?.texto ?? personal.rol
    const ok = await ejecutar(
      () => api.crearPersonal(empresa.id, personal),
      `${personal.nombre.trim()} ya tiene cuenta de ${rol} en ${empresa.nombre}. Compártele su correo y la clave inicial: la clave no se vuelve a mostrar.`,
    )
    if (ok) {
      setPersonal(PERSONAL_VACIO)
      setVerClave(false)
      setModo(null)
    }
  }

  const abrir = (tipo: 'editar' | 'borrar' | 'personal', e: Empresa) => {
    setError(null)
    setModo({ tipo, id: e.id })
    if (tipo === 'editar') {
      setEdicion({ ruc: e.ruc ?? '', nombre: e.nombre, rubro: e.rubro ?? '' })
      setErroresEdicion({})
    }
    if (tipo === 'personal') {
      setPersonal(PERSONAL_VACIO)
      setErroresPersonal({})
    }
  }

  if (!esAdmin) {
    return (
      <section className="em-pagina">
        <h1><span className="mascara"><span>Empresas</span></span></h1>
        <p className="em-nota">Esta pantalla es para la administración de la plataforma, que crea las empresas y a su personal.</p>
      </section>
    )
  }

  return (
    <section className="em-pagina">
      <h1><span className="mascara"><span>Empresas</span></span></h1>
      <p className="em-intro sube" style={retraso(2)}>
        Las empresas que usan Kala. Aquí las creas y das de alta a su personal de RRHH y de coordinación de bienestar, que luego gestionan a sus empleados.
      </p>
      {error && <p role="alert" className="em-error">{error}</p>}
      <Aviso mensaje={aviso} alCerrar={() => setAviso('')} />

      <div className="em-cuerpo">
        <div>
          <form className="em-nueva sube" style={retraso(3)} onSubmit={(e) => void crear(e)} noValidate>
            <h2>Crear una empresa</h2>
            <div className="em-rejilla">
              <div className="em-campo">
                <label htmlFor="em-ruc">RUC</label>
                <input id="em-ruc" inputMode="numeric" maxLength={11} autoComplete="off" value={nueva.ruc} onChange={(e) => setNueva({ ...nueva, ruc: e.target.value })} aria-invalid={erroresNueva.ruc ? true : undefined} aria-describedby={erroresNueva.ruc ? 'err-ruc' : undefined} />
                {erroresNueva.ruc && <p id="err-ruc" role="alert" className="em-err">{erroresNueva.ruc}</p>}
              </div>
              <div className="em-campo">
                <label htmlFor="em-nombre">Nombre</label>
                <input id="em-nombre" autoComplete="off" value={nueva.nombre} onChange={(e) => setNueva({ ...nueva, nombre: e.target.value })} aria-invalid={erroresNueva.nombre ? true : undefined} aria-describedby={erroresNueva.nombre ? 'err-nombre' : undefined} />
                {erroresNueva.nombre && <p id="err-nombre" role="alert" className="em-err">{erroresNueva.nombre}</p>}
              </div>
              <div className="em-campo">
                <label htmlFor="em-rubro">Rubro</label>
                <input id="em-rubro" autoComplete="off" value={nueva.rubro} onChange={(e) => setNueva({ ...nueva, rubro: e.target.value })} aria-invalid={erroresNueva.rubro ? true : undefined} aria-describedby={erroresNueva.rubro ? 'err-rubro' : undefined} />
                {erroresNueva.rubro && <p id="err-rubro" role="alert" className="em-err">{erroresNueva.rubro}</p>}
              </div>
            </div>
            <button type="submit" className="boton boton-primario em-enviar" disabled={ocupado}>Crear empresa</button>
          </form>

          {empresas === null && !error && <p className="em-nota">Cargando empresas…</p>}
          {empresas?.length === 0 && <div className="em-vacio hatch"><h2>Todavía no hay empresas</h2><p>Crea la primera para poder dar de alta a su personal.</p></div>}
          {empresas && empresas.length > 0 && (
            <ul className="em-lista sube" style={retraso(4)}>
              {empresas.map((e) => (
                <li key={e.id} className="em-item">
                  {modo?.id === e.id && modo.tipo === 'editar' ? (
                    <form className="em-edicion" onSubmit={(ev) => void guardar(ev, e.id)} noValidate>
                      <div className="em-rejilla">
                        <div className="em-campo"><label htmlFor={`ed-ruc-${e.id}`}>RUC</label><input id={`ed-ruc-${e.id}`} inputMode="numeric" maxLength={11} value={edicion.ruc} onChange={(ev) => setEdicion({ ...edicion, ruc: ev.target.value })} aria-invalid={erroresEdicion.ruc ? true : undefined} />{erroresEdicion.ruc && <p role="alert" className="em-err">{erroresEdicion.ruc}</p>}</div>
                        <div className="em-campo"><label htmlFor={`ed-nom-${e.id}`}>Nombre</label><input id={`ed-nom-${e.id}`} value={edicion.nombre} onChange={(ev) => setEdicion({ ...edicion, nombre: ev.target.value })} aria-invalid={erroresEdicion.nombre ? true : undefined} />{erroresEdicion.nombre && <p role="alert" className="em-err">{erroresEdicion.nombre}</p>}</div>
                        <div className="em-campo"><label htmlFor={`ed-rub-${e.id}`}>Rubro</label><input id={`ed-rub-${e.id}`} value={edicion.rubro} onChange={(ev) => setEdicion({ ...edicion, rubro: ev.target.value })} aria-invalid={erroresEdicion.rubro ? true : undefined} />{erroresEdicion.rubro && <p role="alert" className="em-err">{erroresEdicion.rubro}</p>}</div>
                      </div>
                      <div className="em-botones">
                        <button type="submit" className="boton boton-primario" disabled={ocupado}>Guardar</button>
                        <button type="button" className="boton" onClick={() => setModo(null)} disabled={ocupado}>Cancelar</button>
                      </div>
                    </form>
                  ) : (
                    <div className="em-fila">
                      <div className="em-quien"><b>{e.nombre}</b><span>RUC {e.ruc ?? '—'} · {e.rubro ?? '—'}</span></div>
                      {modo?.id === e.id && modo.tipo === 'borrar' ? (
                        <span className="em-botones">
                          <span className="em-confirma"><b>¿Eliminar {e.nombre}?</b> No se puede deshacer.</span>
                          <button type="button" className="boton boton-peligro" disabled={ocupado} onClick={() => void ejecutar(() => api.eliminarEmpresa(e.id), `Empresa ${e.nombre} eliminada.`).then((ok) => ok && setModo(null))}>Eliminar</button>
                          <button type="button" className="boton" onClick={() => setModo(null)} disabled={ocupado}>Cancelar</button>
                        </span>
                      ) : (
                        <span className="em-botones">
                          <button type="button" className="boton boton-contorno" onClick={() => abrir('personal', e)} aria-expanded={modo?.id === e.id && modo.tipo === 'personal'}>Agregar personal</button>
                          <button type="button" className="boton" onClick={() => abrir('editar', e)}>Editar</button>
                          <button type="button" className="boton" onClick={() => abrir('borrar', e)}>Eliminar</button>
                        </span>
                      )}
                    </div>
                  )}

                  {modo?.id === e.id && modo.tipo === 'personal' && (
                    <form className="em-personal" onSubmit={(ev) => void altaPersonal(ev, e)} noValidate>
                      <h3>Dar de alta a una persona en {e.nombre}</h3>
                      <div className="em-rejilla dos">
                        <div className="em-campo"><label htmlFor="pe-nombre">Nombre</label><input id="pe-nombre" autoComplete="off" value={personal.nombre} onChange={(ev) => setPersonal({ ...personal, nombre: ev.target.value })} aria-invalid={erroresPersonal.nombre ? true : undefined} aria-describedby={erroresPersonal.nombre ? 'pe-err-nombre' : undefined} />{erroresPersonal.nombre && <p id="pe-err-nombre" role="alert" className="em-err">{erroresPersonal.nombre}</p>}</div>
                        <div className="em-campo"><label htmlFor="pe-email">Correo</label><input id="pe-email" type="email" autoComplete="off" value={personal.email} onChange={(ev) => setPersonal({ ...personal, email: ev.target.value })} aria-invalid={erroresPersonal.email ? true : undefined} aria-describedby={erroresPersonal.email ? 'pe-err-email' : undefined} />{erroresPersonal.email && <p id="pe-err-email" role="alert" className="em-err">{erroresPersonal.email}</p>}</div>
                        <div className="em-campo">
                          <label htmlFor="pe-rol">Rol</label>
                          <Selector id="pe-rol" etiqueta="Roles" marcador="Elige un rol" valor={personal.rol} opciones={ROLES_PERSONAL} alCambiar={(v) => setPersonal({ ...personal, rol: v })} invalido={Boolean(erroresPersonal.rol)} descritoPor={erroresPersonal.rol ? 'pe-err-rol' : undefined} />
                          {erroresPersonal.rol && <p id="pe-err-rol" role="alert" className="em-err">{erroresPersonal.rol}</p>}
                        </div>
                        <div className="em-campo">
                          <label htmlFor="pe-clave">Clave inicial</label>
                          <div className="em-clave">
                            <input id="pe-clave" type={verClave ? 'text' : 'password'} autoComplete="new-password" value={personal.clave} onChange={(ev) => setPersonal({ ...personal, clave: ev.target.value })} aria-invalid={erroresPersonal.clave ? true : undefined} aria-describedby={erroresPersonal.clave ? 'pe-err-clave' : undefined} />
                            <button type="button" className="em-ver" onClick={() => setVerClave(!verClave)} aria-pressed={verClave}>{verClave ? 'Ocultar' : 'Mostrar'}</button>
                          </div>
                          {erroresPersonal.clave ? <p id="pe-err-clave" role="alert" className="em-err">{erroresPersonal.clave}</p> : <p className="em-pista">Al menos 12 caracteres. No se vuelve a mostrar.</p>}
                        </div>
                      </div>
                      <div className="em-botones">
                        <button type="submit" className="boton boton-primario" disabled={ocupado}>Dar de alta</button>
                        <button type="button" className="boton" onClick={() => setModo(null)} disabled={ocupado}>Cancelar</button>
                      </div>
                    </form>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>

        <aside className="em-ficha sube" style={retraso(5)} aria-label="Resumen de empresas">
          <h2>Plataforma</h2>
          <dl>
            <div><dt>Empresas</dt><dd>{empresas?.length ?? '—'}</dd></div>
            <div><dt>Roles que crea</dt><dd>RRHH y coordinación</dd></div>
          </dl>
        </aside>
      </div>
    </section>
  )
}
