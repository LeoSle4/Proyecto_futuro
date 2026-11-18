import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { api, ErrorApi } from '../api/cliente'
import type { ContactoAyuda, Solicitud } from '../api/tipos'
import { Aviso } from '../components/Aviso'
import { useSesion } from '../auth/SesionContext'
import { etiquetaEstado, fechaCorta, ordenarSolicitudes, TIPOS_CONTACTO, textoEspera, validarContacto, type ErroresContacto } from '../lib/ayuda'
import { Selector } from '../components/Selector'
import './SolicitudesPage.css'

const retraso = (i: number) => ({ '--i': i }) as React.CSSProperties
const VACIO = { nombre: '', tipo: '', telefono: '', email: '' }
const FILTROS = [
  { valor: '', texto: 'Todas' },
  { valor: 'PENDIENTE', texto: 'Esperando' },
  { valor: 'EN_ATENCION', texto: 'En atención' },
  { valor: 'CERRADA', texto: 'Cerradas' },
]

export function SolicitudesPage() {
  const { perfil, cerrar } = useSesion()
  const esCoordinador = Boolean(perfil?.roles.includes('COORDINADOR_BIENESTAR'))
  const puedeContactos = esCoordinador || Boolean(perfil?.roles.includes('RRHH'))

  const [solicitudes, setSolicitudes] = useState<Solicitud[] | null>(null)
  const [contactos, setContactos] = useState<ContactoAyuda[] | null>(null)
  const [filtro, setFiltro] = useState('')
  const [derivar, setDerivar] = useState<Record<number, string>>({})
  const [cerrando, setCerrando] = useState<number | null>(null)
  const [desactivando, setDesactivando] = useState<number | null>(null)
  const [form, setForm] = useState(VACIO)
  const [errores, setErrores] = useState<ErroresContacto>({})
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState('')

  const manejar = useCallback((e: unknown, porDefecto: string) => {
    if (e instanceof ErrorApi && e.estado === 401) void cerrar()
    else setError(e instanceof Error ? e.message : porDefecto)
  }, [cerrar])

  const recargar = useCallback(async () => {
    const [s, c] = await Promise.all([esCoordinador ? api.solicitudesAyuda() : Promise.resolve([]), puedeContactos ? api.contactosAyuda() : Promise.resolve([])])
    setSolicitudes(s)
    setContactos(c)
  }, [esCoordinador, puedeContactos])

  useEffect(() => {
    let vigente = true
    Promise.all([esCoordinador ? api.solicitudesAyuda() : Promise.resolve([]), puedeContactos ? api.contactosAyuda() : Promise.resolve([])]).then(
      ([s, c]) => {
        if (!vigente) return
        setSolicitudes(s)
        setContactos(c)
      },
      (e: unknown) => vigente && manejar(e, 'No pudimos cargar la información.'),
    )
    return () => {
      vigente = false
    }
  }, [esCoordinador, puedeContactos, manejar])

  async function ejecutar(accion: () => Promise<unknown>, mensaje: string): Promise<boolean> {
    setError(null)
    setOcupado(true)
    try {
      await accion()
      await recargar()
      setAviso(mensaje)
      return true
    } catch (e) {
      manejar(e, 'No pudimos completar el cambio.')
      // A conflict means someone else changed it first: show how it is now.
      if (e instanceof ErrorApi && e.estado === 409) await recargar().catch(() => undefined)
      return false
    } finally {
      setOcupado(false)
    }
  }

  async function agregarContacto(e: FormEvent) {
    e.preventDefault()
    const problemas = validarContacto(form)
    setErrores(problemas)
    if (Object.keys(problemas).length > 0) return
    if (await ejecutar(() => api.crearContactoAyuda(form), `${form.nombre.trim()} agregado a los contactos.`)) setForm(VACIO)
  }

  const activos = useMemo(() => (contactos ?? []).filter((c) => c.activo), [contactos])
  const nombreDe = (id: number | null) => contactos?.find((c) => c.id === id)?.nombre
  const visibles = useMemo(() => ordenarSolicitudes((solicitudes ?? []).filter((s) => !filtro || s.estado === filtro)), [solicitudes, filtro])
  const esperando = (solicitudes ?? []).filter((s) => s.estado === 'PENDIENTE').length

  const campo = (nombre: keyof typeof VACIO) => ({
    value: form[nombre],
    'aria-invalid': errores[nombre] ? true : undefined,
    'aria-describedby': errores[nombre] ? `err-${nombre}` : undefined,
    onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setForm({ ...form, [nombre]: e.target.value }),
  })

  if (!puedeContactos) {
    return (
      <section className="so-pagina">
        <h1><span className="mascara"><span>Solicitudes de ayuda</span></span></h1>
        <p className="so-nota">Esta pantalla es para la coordinación de bienestar y RRHH, que cuidan de las personas de su empresa.</p>
      </section>
    )
  }

  return (
    <section className="so-pagina">
      <h1><span className="mascara"><span>Solicitudes de ayuda</span></span></h1>
      <p className="so-intro sube" style={retraso(2)}>
        Personas que pidieron que alguien les escriba. Responder con calma y a tiempo es lo más importante; aquí solo ves lo que ellas quisieron compartir.
      </p>
      {error && <p role="alert" className="so-error">{error}</p>}
      <Aviso mensaje={aviso} alCerrar={() => setAviso('')} />

      <div className="so-cuerpo">
        <div>
          {esCoordinador ? (
            <>
              <div className="so-filtros sube" style={retraso(3)} role="group" aria-label="Filtrar por estado">
                {FILTROS.map((f) => (
                  <button key={f.valor} type="button" className="chip" aria-pressed={filtro === f.valor} onClick={() => setFiltro(f.valor)}>
                    {f.texto}{f.valor === 'PENDIENTE' && esperando > 0 ? ` (${esperando})` : ''}
                  </button>
                ))}
              </div>
              {solicitudes === null && !error && <p className="so-nota">Cargando…</p>}
              {solicitudes?.length === 0 && <div className="so-vacio hatch"><h2>Todo en calma</h2><p>Nadie ha pedido ayuda. Cuando alguien lo haga, aparecerá aquí.</p></div>}
              {solicitudes && solicitudes.length > 0 && visibles.length === 0 && <p className="so-nota">No hay solicitudes con ese estado.</p>}
              <ul className="so-lista">
                {visibles.map((s) => (
                  <li key={s.id} className={`so-item estado-${s.estado.toLowerCase()}`}>
                    <div className="so-cabeza">
                      <b>Persona n.º {s.usuarioId}</b>
                      <span className="so-estado">{etiquetaEstado(s.estado)}</span>
                    </div>
                    <p className="so-cuando">
                      Pidió ayuda el {fechaCorta(s.creadoEn)}{s.estado === 'PENDIENTE' ? ` · ${textoEspera(s.creadoEn)}` : ''}
                      {s.estado !== 'PENDIENTE' && s.contactoId !== null && nombreDe(s.contactoId) ? ` · derivada a ${nombreDe(s.contactoId)}` : ''}
                    </p>
                    {s.mensaje ? <blockquote className="so-mensaje">{s.mensaje}</blockquote> : <p className="so-sin">No dejó ningún mensaje.</p>}
                    {s.estado === 'PENDIENTE' && (
                      <div className="so-acciones">
                        <label className="solo-lectores" htmlFor={`der-${s.id}`}>Derivar la solicitud {s.id} a</label>
                        <div className="so-derivar"><Selector id={`der-${s.id}`} etiqueta="Derivar a" valor={derivar[s.id] ?? ''} opciones={[{ valor: '', texto: 'Atender yo, sin derivar' }, ...activos.map((c) => ({ valor: String(c.id), texto: `Derivar a ${c.nombre}` }))]} alCambiar={(v) => setDerivar({ ...derivar, [s.id]: v })} /></div>
                        <button type="button" className="boton boton-primario" disabled={ocupado} onClick={() => void ejecutar(() => api.atenderSolicitud(s.id, derivar[s.id] ? Number(derivar[s.id]) : null), `Solicitud de la persona n.º ${s.usuarioId} en atención.`)}>Atender</button>
                      </div>
                    )}
                    {s.estado === 'EN_ATENCION' && (
                      <div className="so-acciones">
                        {cerrando === s.id ? (
                          <>
                            <p className="so-confirma"><b>¿Cerrar esta solicitud?</b> Ya no aparecerá entre las que esperan.</p>
                            <button type="button" className="boton boton-peligro" disabled={ocupado} onClick={() => void ejecutar(() => api.cerrarSolicitud(s.id), 'Solicitud cerrada.').then(() => setCerrando(null))}>Cerrar</button>
                            <button type="button" className="boton" disabled={ocupado} onClick={() => setCerrando(null)}>Cancelar</button>
                          </>
                        ) : (
                          <button type="button" className="boton boton-contorno" onClick={() => setCerrando(s.id)}>Marcar como cerrada</button>
                        )}
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            </>
          ) : (
            <div className="so-privado sube" style={retraso(3)}>
              <h2>Quién pidió ayuda lo ve solo la coordinación</h2>
              <p>Es a propósito: así RRHH no sabe quién la pidió, y las personas pueden hacerlo con confianza. Tú gestionas los contactos de ayuda a los que se puede acudir.</p>
            </div>
          )}
        </div>

        <aside className="so-contactos sube" style={retraso(4)} aria-label="Contactos de ayuda">
          <h2>Contactos de ayuda</h2>
          <p className="so-explica">A quién pueden acudir las personas desde la app. Lo ven todas.</p>
          {contactos === null && !error && <p className="so-nota">Cargando…</p>}
          {contactos?.length === 0 && <p className="so-nota">Todavía no hay contactos. Agrega el primero abajo.</p>}
          <ul className="so-cont-lista">
            {(contactos ?? []).map((c) => (
              <li key={c.id} className={c.activo ? '' : 'inactivo'}>
                <div>
                  <b>{c.nombre}</b>
                  <span>{TIPOS_CONTACTO.find((t) => t.valor === c.tipo)?.texto ?? c.tipo}{c.activo ? '' : ' · desactivado'}</span>
                  <span>
                    {c.telefono && <a href={`tel:${c.telefono}`}>{c.telefono}</a>}
                    {c.telefono && c.email ? ' · ' : ''}
                    {c.email && <a href={`mailto:${c.email}`}>{c.email}</a>}
                  </span>
                </div>
                {c.activo && (desactivando === c.id ? (
                  <span className="so-mini">
                    <button type="button" className="boton boton-peligro" disabled={ocupado} onClick={() => void ejecutar(() => api.desactivarContactoAyuda(c.id), `${c.nombre} ya no aparece en la app.`).then(() => setDesactivando(null))}>Quitar</button>
                    <button type="button" className="boton" onClick={() => setDesactivando(null)}>No</button>
                  </span>
                ) : (
                  <button type="button" className="boton so-quitar" onClick={() => setDesactivando(c.id)}>Desactivar</button>
                ))}
              </li>
            ))}
          </ul>

          <form className="so-alta" onSubmit={(e) => void agregarContacto(e)} noValidate>
            <h3>Agregar un contacto</h3>
            <div className="so-campo">
              <label htmlFor="co-nombre">Nombre</label>
              <input id="co-nombre" autoComplete="off" maxLength={150} {...campo('nombre')} />
              {errores.nombre && <p id="err-nombre" role="alert" className="so-err">{errores.nombre}</p>}
            </div>
            <div className="so-campo">
              <label htmlFor="co-tipo">Tipo</label>
              <Selector id="co-tipo" etiqueta="Tipos de contacto" marcador="Elige un tipo" valor={form.tipo} opciones={TIPOS_CONTACTO.map((t) => ({ valor: t.valor, texto: t.texto }))} alCambiar={(v) => setForm({ ...form, tipo: v })} invalido={Boolean(errores.tipo)} descritoPor={errores.tipo ? 'err-tipo' : undefined} />
              {errores.tipo && <p id="err-tipo" role="alert" className="so-err">{errores.tipo}</p>}
            </div>
            <div className="so-campo">
              <label htmlFor="co-tel">Teléfono</label>
              <input id="co-tel" inputMode="tel" autoComplete="off" maxLength={20} {...campo('telefono')} />
              {errores.telefono && <p id="err-telefono" role="alert" className="so-err">{errores.telefono}</p>}
            </div>
            <div className="so-campo">
              <label htmlFor="co-email">Correo</label>
              <input id="co-email" type="email" autoComplete="off" maxLength={150} {...campo('email')} />
              {errores.email && <p id="err-email" role="alert" className="so-err">{errores.email}</p>}
            </div>
            <button type="submit" className="boton boton-primario" disabled={ocupado}>Agregar contacto</button>
          </form>
        </aside>
      </div>
    </section>
  )
}
