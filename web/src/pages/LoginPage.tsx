import { useId, useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { useSesion } from '../auth/SesionContext'
import { ErrorApi } from '../api/cliente'
import './LoginPage.css'

export function LoginPage() {
  const { perfil, cargando, iniciar } = useSesion()
  const [usuario, setUsuario] = useState('')
  const [clave, setClave] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const idError = useId()

  if (!cargando && perfil) return <Navigate to="/" replace />

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      await iniciar(usuario.trim(), clave)
    } catch (err) {
      if (err instanceof ErrorApi && err.estado === 401) setError('El correo o la contraseña no coinciden. Revísalos e inténtalo de nuevo.')
      else setError(err instanceof Error ? err.message : 'No pudimos iniciar sesión.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className="login">
      <form className="login-tarjeta" onSubmit={(e) => void enviar(e)} noValidate>
        <h1 className="login-marca">Kala</h1>
        <h2 className="login-titulo">Entrar al panel de bienestar</h2>
        <p className="login-texto">Verás estimaciones por equipo. Nunca respuestas de una persona.</p>

        <label htmlFor="usuario">Correo</label>
        <input
          id="usuario" name="username" type="email" autoComplete="username" required
          value={usuario} onChange={(e) => setUsuario(e.target.value)}
          aria-invalid={error ? true : undefined} aria-describedby={error ? idError : undefined}
        />
        <label htmlFor="clave">Contraseña</label>
        <input
          id="clave" name="password" type="password" autoComplete="current-password" required
          value={clave} onChange={(e) => setClave(e.target.value)}
          aria-invalid={error ? true : undefined} aria-describedby={error ? idError : undefined}
        />
        {error && <p id={idError} role="alert" className="login-error">{error}</p>}
        <button type="submit" className="boton boton-primario" disabled={enviando || !usuario || !clave}>
          {enviando ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </main>
  )
}
