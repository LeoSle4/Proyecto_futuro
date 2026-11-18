import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api, ErrorApi, MODO_DEMO } from '../api/cliente'
import type { ContactoAyuda, TipoContactoAyuda } from '../api/tipos'
import { useSesion } from '../auth/SesionContext'
import './Pendiente.css'
import './ContactosAyuda.css'

const TIPOS: { valor: TipoContactoAyuda; etiqueta: string }[] = [
  { valor: 'PSICOLOGO', etiqueta: 'Psicólogo/a' },
  { valor: 'LINEA_CRISIS', etiqueta: 'Línea de crisis' },
  { valor: 'EPS', etiqueta: 'EPS' },
  { valor: 'OTRO', etiqueta: 'Otro' },
]

const TIPO_ETIQUETA = Object.fromEntries(TIPOS.map(({ valor, etiqueta }) => [valor, etiqueta])) as Record<TipoContactoAyuda, string>

function mensajeError(error: unknown, accion: 'cargar' | 'guardar' | 'desactivar'): string {
  if (!(error instanceof ErrorApi)) return 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.'
  if (error.estado === 400) return accion === 'guardar' ? 'Revisa los datos: el servidor no pudo registrar el contacto.' : 'La solicitud no es válida. Inténtalo de nuevo.'
  if (error.estado === 401) return 'Tu sesión terminó. Inicia sesión de nuevo.'
  if (error.estado === 403) return 'Tu cuenta no tiene permiso para realizar esta acción.'
  if (error.estado >= 500) return 'El servidor tuvo un problema. Inténtalo de nuevo más tarde.'
  return accion === 'desactivar' ? 'No pudimos desactivar el contacto. Actualiza la página e inténtalo de nuevo.' : 'No pudimos cargar los contactos. Inténtalo de nuevo.'
}

function ContactosAyudaPage() {
  const { perfil } = useSesion()
  const puedeGestionar = Boolean(perfil?.roles.some((rol) => rol === 'RRHH' || rol === 'COORDINADOR_BIENESTAR'))
  const [contactos, setContactos] = useState<ContactoAyuda[]>([])
  const [cargando, setCargando] = useState(true)
  const [enviando, setEnviando] = useState(false)
  const [desactivandoId, setDesactivandoId] = useState<number | null>(null)
  const [formularioAbierto, setFormularioAbierto] = useState(false)
  const [nombre, setNombre] = useState('')
  const [tipo, setTipo] = useState<TipoContactoAyuda | ''>('')
  const [telefono, setTelefono] = useState('')
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [exito, setExito] = useState('')

  const cargarContactos = useCallback(async () => {
    try {
      const resultado = await api.listarContactos()
      setContactos(resultado.filter((contacto) => contacto.activo))
    } catch (e) {
      setError(mensajeError(e, 'cargar'))
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => { void cargarContactos() }, [cargarContactos])

  async function registrar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setError('')
    setExito('')
    const nombreLimpio = nombre.trim()
    const telefonoLimpio = telefono.trim()
    const emailLimpio = email.trim()
    if (!nombreLimpio) return setError('Escribe el nombre del contacto.')
    if (nombreLimpio.length > 150) return setError('El nombre no puede superar los 150 caracteres.')
    if (!TIPOS.some((opcion) => opcion.valor === tipo)) return setError('Selecciona un tipo de contacto válido.')
    if (telefonoLimpio.length > 20) return setError('El teléfono no puede superar los 20 caracteres.')
    if (emailLimpio.length > 150) return setError('El correo no puede superar los 150 caracteres.')
    if (emailLimpio && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailLimpio)) return setError('Escribe un correo electrónico válido.')
    if (!telefonoLimpio && !emailLimpio) return setError('Añade al menos un medio de contacto: teléfono o correo.')
    setEnviando(true)
    try {
      await api.crearContacto({ nombre: nombreLimpio, tipo: tipo as TipoContactoAyuda, telefono: telefonoLimpio || null, email: emailLimpio || null })
      setNombre('')
      setTipo('')
      setTelefono('')
      setEmail('')
      setFormularioAbierto(false)
      setExito('El contacto se registró correctamente.')
      await cargarContactos()
    } catch (e) {
      setError(mensajeError(e, 'guardar'))
    } finally {
      setEnviando(false)
    }
  }

  async function desactivar(contacto: ContactoAyuda) {
    if (!window.confirm(`¿Desactivar a ${contacto.nombre}?`)) return
    setError('')
    setExito('')
    setDesactivandoId(contacto.id)
    try {
      await api.desactivarContacto(contacto.id)
      setExito('El contacto se desactivó correctamente.')
      await cargarContactos()
    } catch (e) {
      setError(mensajeError(e, 'desactivar'))
    } finally {
      setDesactivandoId(null)
    }
  }

  return (
    <section className="pendiente contactos-ayuda">
      <header className="contactos-cabecera">
        <div>
          <h1>Contactos de ayuda</h1>
          <p>Encuentra apoyo y orientación disponible en tu empresa.</p>
        </div>
        {puedeGestionar && <button className="boton boton-primario" type="button" onClick={() => { setFormularioAbierto((abierto) => !abierto); setError('') }}>
          {formularioAbierto ? 'Cerrar formulario' : 'Registrar contacto'}
        </button>}
      </header>

      {MODO_DEMO && <p className="contactos-demo">Datos sintéticos de demostración</p>}
      {error && <p className="contactos-mensaje contactos-error" role="alert">{error}</p>}
      {exito && <p className="contactos-mensaje contactos-exito" role="status">{exito}</p>}

      {formularioAbierto && puedeGestionar && <form className="contactos-formulario" onSubmit={(evento) => void registrar(evento)}>
        <h2>Nuevo contacto</h2>
        <label>Nombre <span aria-hidden="true">*</span>
          <input required maxLength={150} value={nombre} onChange={(evento) => setNombre(evento.target.value)} autoComplete="name" />
        </label>
        <label>Tipo <span aria-hidden="true">*</span>
          <select required value={tipo} onChange={(evento) => setTipo(evento.target.value as TipoContactoAyuda | '')}>
            <option value="">Selecciona un tipo</option>
            {TIPOS.map((opcion) => <option key={opcion.valor} value={opcion.valor}>{opcion.etiqueta}</option>)}
          </select>
        </label>
        <label>Teléfono
          <input maxLength={20} value={telefono} onChange={(evento) => setTelefono(evento.target.value)} type="tel" autoComplete="tel" />
        </label>
        <label>Correo electrónico
          <input maxLength={150} value={email} onChange={(evento) => setEmail(evento.target.value)} type="email" autoComplete="email" />
        </label>
        <p className="contactos-ayuda-form">Agrega al menos un teléfono o correo electrónico.</p>
        <button className="boton boton-primario" type="submit" disabled={enviando}>{enviando ? 'Guardando…' : 'Guardar contacto'}</button>
      </form>}

      {cargando ? <p className="contactos-estado" role="status">Cargando contactos…</p> : contactos.length === 0 ? (
        <p className="contactos-vacio">Aún no hay contactos de ayuda activos registrados.</p>
      ) : (
        <ul className="contactos-lista">
          {contactos.map((contacto) => <li className="contacto-tarjeta" key={contacto.id}>
            <div className="contacto-detalle">
              <h2>{contacto.nombre}</h2>
              <span className="contacto-tipo">{TIPO_ETIQUETA[contacto.tipo] ?? contacto.tipo}</span>
              <span className="contacto-estado">Activo</span>
              <div className="contacto-medios">
                {contacto.telefono && <a href={`tel:${contacto.telefono}`}>{contacto.telefono}</a>}
                {contacto.email && <a href={`mailto:${contacto.email}`}>{contacto.email}</a>}
              </div>
            </div>
            {puedeGestionar && <button className="boton boton-suave" type="button" disabled={desactivandoId === contacto.id} onClick={() => void desactivar(contacto)}>
              {desactivandoId === contacto.id ? 'Desactivando…' : 'Desactivar'}
            </button>}
          </li>)}
        </ul>
      )}
    </section>
  )
}

export function Pendiente({ titulo, descripcion }: { titulo: string; descripcion: string }) {
  if (titulo === 'Solicitudes de ayuda') return <ContactosAyudaPage />
  return (
    <section className="pendiente">
      <h1>{titulo}</h1>
      <p>{descripcion}</p>
      <p className="pendiente-nota">Esta pantalla todavía no está construida. Usa los componentes y tokens de la base visual.</p>
    </section>
  )
}
