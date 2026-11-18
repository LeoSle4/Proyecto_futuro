import { useState, type FormEvent } from 'react'
import { api, ErrorApi } from '../api/cliente'
import { useSesion } from '../auth/SesionContext'
import { validarCambioClave, type ErroresClave } from '../lib/clave'
import './CuentaPage.css'

const VACIO = { actual: '', nueva: '', repetida: '' }

export function CuentaPage() {
  const { perfil, cerrar } = useSesion()
  const [form, setForm] = useState(VACIO)
  const [errores, setErrores] = useState<ErroresClave>({})
  const [ver, setVer] = useState(false)
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function guardar(e: FormEvent) {
    e.preventDefault()
    const p = validarCambioClave(form)
    setErrores(p)
    setError(null)
    if (Object.keys(p).length > 0) return
    setOcupado(true)
    try {
      await api.cambiarClave(form.actual, form.nueva)
      // The server revokes every session on success, so go back to the login.
      await cerrar()
    } catch (err) {
      // A 401 after a valid session means the current password was wrong.
      if (err instanceof ErrorApi && err.estado === 401) setErrores({ actual: 'La contraseña actual no coincide.' })
      else setError(err instanceof Error ? err.message : 'No pudimos cambiar tu contraseña.')
    } finally {
      setOcupado(false)
    }
  }

  const campo = (clave: keyof typeof VACIO, etiqueta: string, auto: string) => (
    <div className="cu-campo">
      <label htmlFor={`cta-${clave}`}>{etiqueta}</label>
      <input
        id={`cta-${clave}`} type={ver ? 'text' : 'password'} autoComplete={auto} value={form[clave]}
        onChange={(e) => setForm({ ...form, [clave]: e.target.value })}
        aria-invalid={errores[clave] ? true : undefined} aria-describedby={errores[clave] ? `cta-err-${clave}` : undefined}
      />
      {errores[clave] && <p id={`cta-err-${clave}`} role="alert" className="cu-err">{errores[clave]}</p>}
    </div>
  )

  return (
    <section className="cta-pagina">
      <h1><span className="mascara"><span>Mi cuenta</span></span></h1>
      <p className="cta-intro sube" style={{ '--i': 2 } as React.CSSProperties}>
        {perfil?.username}. Cuando cambies tu contraseña, se cerrarán todas tus sesiones y tendrás que entrar de nuevo.
      </p>
      <form className="cta-form sube" style={{ '--i': 3 } as React.CSSProperties} onSubmit={(e) => void guardar(e)} noValidate>
        <h2>Cambiar mi contraseña</h2>
        {campo('actual', 'Contraseña actual', 'current-password')}
        {campo('nueva', 'Contraseña nueva', 'new-password')}
        <p className="cta-pista">Al menos 12 caracteres.</p>
        {campo('repetida', 'Repite la contraseña nueva', 'new-password')}
        <button type="button" className="cta-ver" aria-pressed={ver} onClick={() => setVer(!ver)}>{ver ? 'Ocultar contraseñas' : 'Mostrar contraseñas'}</button>
        {error && <p role="alert" className="cu-err">{error}</p>}
        <button type="submit" className="boton boton-primario cta-guardar" disabled={ocupado}>{ocupado ? 'Guardando…' : 'Guardar contraseña'}</button>
      </form>
    </section>
  )
}
