import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { api, ErrorApi } from '../api/cliente'
import type { Empleado, Equipo } from '../api/tipos'
import { useSesion } from '../auth/SesionContext'
import { erroresEmpleadoDelServidor, validarNuevoEmpleado, type ErroresEmpleado } from '../lib/empleado'
import { Aviso } from '../components/Aviso'
import { Selector } from '../components/Selector'
import './EmpleadosPage.css'

const retraso = (i: number) => ({ '--i': i }) as React.CSSProperties
const VACIO = { nombre: '', email: '', clave: '', equipoId: '' }

export function EmpleadosPage() {
  const { perfil, cerrar } = useSesion()
  // Listing employees is for HR and wellbeing coordination; an admin has no company of their own here.
  const permitido = Boolean(perfil?.roles.some((r) => r === 'RRHH' || r === 'COORDINADOR_BIENESTAR'))

  const [empleados, setEmpleados] = useState<Empleado[] | null>(null)
  const [equipos, setEquipos] = useState<Equipo[]>([])
  const [form, setForm] = useState(VACIO)
  const [errores, setErrores] = useState<ErroresEmpleado>({})
  const [verClave, setVerClave] = useState(false)
  const [busqueda, setBusqueda] = useState('')
  const [filtroEquipo, setFiltroEquipo] = useState('')
  const [confirmando, setConfirmando] = useState<number | null>(null)
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState('')

  const recargar = useCallback(async () => setEmpleados(await api.empleados()), [])

  useEffect(() => {
    if (!permitido) return
    let vigente = true
    Promise.all([api.empleados(), api.equipos()]).then(
      ([lista, eq]) => {
        if (!vigente) return
        setEmpleados(lista)
        setEquipos(eq)
      },
      (e: unknown) => {
        if (!vigente) return
        if (e instanceof ErrorApi && e.estado === 401) void cerrar()
        else setError(e instanceof Error ? e.message : 'No pudimos cargar a las personas.')
      },
    )
    return () => {
      vigente = false
    }
  }, [permitido, cerrar])

  async function ejecutar(accion: () => Promise<unknown>, mensaje: string, alFallar?: (e: unknown) => boolean): Promise<boolean> {
    setError(null)
    setOcupado(true)
    try {
      await accion()
      setAviso(mensaje)
      try {
        await recargar()
      } catch (e) {
        if (e instanceof ErrorApi && e.estado === 401) void cerrar()
        else setError('El cambio se guardó, pero no pudimos actualizar la lista. Inténtalo de nuevo.')
      }
      return true
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 401) void cerrar()
      else if (!alFallar || !alFallar(e)) setError(e instanceof Error ? e.message : 'No pudimos completar el cambio.')
      return false
    } finally {
      setOcupado(false)
    }
  }

  async function darDeAlta(e: FormEvent) {
    e.preventDefault()
    const equipoId = form.equipoId === '' ? null : Number(form.equipoId)
    const problemas = validarNuevoEmpleado({ ...form, equipoId })
    setErrores(problemas)
    if (Object.keys(problemas).length > 0 || equipoId === null) return
    const nombre = form.nombre.trim()
    const ok = await ejecutar(
      () => api.crearEmpleado({ equipoId, email: form.email.trim(), nombre, clave: form.clave }),
      `Cuenta de ${nombre} creada. Comparte con esa persona la clave inicial que acabas de definir; no volverás a verla aquí.`,
      (e) => {
        if (!(e instanceof ErrorApi)) return false
        const problemasServidor = erroresEmpleadoDelServidor(e.estado, e.message)
        if (Object.keys(problemasServidor).length === 0) return false
        setErrores((actuales) => ({ ...actuales, ...problemasServidor }))
        return true
      },
    )
    if (ok) {
      setForm(VACIO)
      setVerClave(false)
    }
  }

  const visibles = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return (empleados ?? []).filter(
      (p) => (!filtroEquipo || String(p.equipoId) === filtroEquipo) && (!q || p.nombre.toLowerCase().includes(q) || p.email.toLowerCase().includes(q)),
    )
  }, [empleados, busqueda, filtroEquipo])
  const activos = (empleados ?? []).filter((p) => p.activo).length

  const campo = (nombre: keyof typeof VACIO) => ({
    value: form[nombre],
    'aria-invalid': errores[nombre] ? true : undefined,
    'aria-describedby': errores[nombre] ? `err-${nombre}` : undefined,
    onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((actual) => ({ ...actual, [nombre]: e.target.value }))
      setErrores((actuales) => ({ ...actuales, [nombre]: undefined }))
    },
  })

  if (!permitido) {
    return (
      <section className="emp-pagina">
        <h1><span className="mascara"><span>Empleados</span></span></h1>
        <p className="emp-nota">Esta pantalla es para RRHH y coordinación de bienestar, que gestionan a las personas de su empresa.</p>
      </section>
    )
  }

  return (
    <section className="emp-pagina">
      <h1><span className="mascara"><span>Empleados</span></span></h1>
      <p className="emp-intro sube" style={retraso(2)}>
        Las personas que responden desde la app. Aquí das de alta una cuenta o la desactivas; nunca ves lo que respondió cada una.
      </p>

      <div className="emp-cuerpo">
        <div>
          <form className="emp-alta sube" style={retraso(3)} onSubmit={(e) => void darDeAlta(e)} noValidate>
            <h2>Dar de alta a una persona</h2>
            <div className="emp-rejilla">
              <div className="emp-campo">
                <label htmlFor="emp-nombre">Nombre</label>
                <input id="emp-nombre" autoComplete="off" maxLength={150} {...campo('nombre')} />
                {errores.nombre && <p id="err-nombre" role="alert" className="emp-err">{errores.nombre}</p>}
              </div>
              <div className="emp-campo">
                <label htmlFor="emp-email">Correo</label>
                <input id="emp-email" type="email" autoComplete="off" maxLength={150} {...campo('email')} />
                {errores.email && <p id="err-email" role="alert" className="emp-err">{errores.email}</p>}
              </div>
              <div className="emp-campo">
                <label htmlFor="emp-equipo">Equipo</label>
                <Selector id="emp-equipo" etiqueta="Equipos" marcador="Elige un equipo" valor={form.equipoId} opciones={equipos.map((e) => ({ valor: String(e.id), texto: e.nombre }))} alCambiar={(v) => { setForm((actual) => ({ ...actual, equipoId: v })); setErrores((actuales) => ({ ...actuales, equipoId: undefined })) }} invalido={Boolean(errores.equipoId)} descritoPor={errores.equipoId ? 'err-equipoId' : undefined} />
                {errores.equipoId && <p id="err-equipoId" role="alert" className="emp-err">{errores.equipoId}</p>}
              </div>
              <div className="emp-campo">
                <label htmlFor="emp-clave">Clave inicial</label>
                <div className="emp-clave">
                  <input id="emp-clave" type={verClave ? 'text' : 'password'} autoComplete="new-password" {...campo('clave')} />
                  <button type="button" className="emp-ver" onClick={() => setVerClave(!verClave)} aria-pressed={verClave}>{verClave ? 'Ocultar' : 'Mostrar'}</button>
                </div>
                {errores.clave ? <p id="err-clave" role="alert" className="emp-err">{errores.clave}</p> : <p className="emp-pista">Al menos 12 caracteres. No se vuelve a mostrar.</p>}
              </div>
            </div>
            <button type="submit" className="boton boton-primario emp-enviar" disabled={ocupado}>Dar de alta</button>
          </form>

          {error && <p role="alert" className="emp-error">{error}</p>}
      <Aviso mensaje={aviso} alCerrar={() => setAviso('')} />

          <div className="emp-filtros sube" style={retraso(4)}>
            <div className="emp-campo">
              <label htmlFor="emp-buscar">Buscar</label>
              <input id="emp-buscar" type="search" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Nombre o correo" />
            </div>
            <div className="emp-campo">
              <label htmlFor="emp-filtro">Equipo</label>
              <Selector id="emp-filtro" etiqueta="Filtrar por equipo" valor={filtroEquipo} opciones={[{ valor: '', texto: 'Todos los equipos' }, ...equipos.map((e) => ({ valor: String(e.id), texto: e.nombre }))]} alCambiar={setFiltroEquipo} />
            </div>
          </div>

          {empleados === null && !error && <p className="emp-nota">Cargando personas…</p>}
          {empleados?.length === 0 && (
            <div className="emp-vacio hatch"><h2>Todavía no hay personas</h2><p>Da de alta a la primera para que pueda responder desde la app.</p></div>
          )}
          {empleados && empleados.length > 0 && visibles.length === 0 && <p className="emp-nota">Nadie coincide con esa búsqueda.</p>}

          {visibles.length > 0 && (
            <ul className="emp-lista">
              {visibles.map((p) => (
                <li key={p.id} className={`emp-fila${p.activo ? '' : ' inactiva'}`}>
                  <div className="emp-quien"><b>{p.nombre}</b><span>{p.email}</span></div>
                  <span className="emp-equipo">{p.equipoNombre}</span>
                  <span className={`emp-estado${p.activo ? '' : ' off'}`}>{p.activo ? 'Cuenta activa' : 'Cuenta desactivada'}</span>
                  {confirmando === p.id ? (
                    <span className="emp-acciones">
                      <button type="button" className="boton boton-peligro" disabled={ocupado} onClick={() => void ejecutar(() => api.cambiarEstadoEmpleado(p.id, false), `Cuenta de ${p.nombre} desactivada: ya no puede entrar.`).then(() => setConfirmando(null))}>Desactivar</button>
                      <button type="button" className="boton" disabled={ocupado} onClick={() => setConfirmando(null)}>Cancelar</button>
                    </span>
                  ) : (
                    <span className="emp-acciones">
                      {p.activo
                        ? <button type="button" className="boton" onClick={() => setConfirmando(p.id)}>Desactivar</button>
                        : <button type="button" className="boton boton-contorno" disabled={ocupado} onClick={() => void ejecutar(() => api.cambiarEstadoEmpleado(p.id, true), `Cuenta de ${p.nombre} reactivada.`)}>Reactivar</button>}
                    </span>
                  )}
                  {confirmando === p.id && <p className="emp-aviso-fila">Esta persona dejará de poder entrar a la app. Puedes reactivarla cuando quieras.</p>}
                </li>
              ))}
            </ul>
          )}
        </div>

        <aside className="emp-ficha sube" style={retraso(5)} aria-label="Resumen de personas">
          <h2>Personas</h2>
          <dl>
            <div><dt>En tu empresa</dt><dd>{empleados?.length ?? '—'}</dd></div>
            <div><dt>Con cuenta activa</dt><dd>{empleados ? activos : '—'}</dd></div>
            <div><dt>Desactivadas</dt><dd>{empleados ? empleados.length - activos : '—'}</dd></div>
          </dl>
        </aside>
      </div>
    </section>
  )
}
