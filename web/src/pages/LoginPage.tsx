import { useId, useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useSesion } from '../auth/SesionContext'
import { ErrorApi } from '../api/cliente'
import { FlujoRuido } from '../components/FlujoRuido'
import './LoginPage.css'

const retraso = (i: number) => ({ '--i': i }) as React.CSSProperties

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
      <section className="login-marca" aria-label="Qué es Kala">
        <svg className="login-campo" viewBox="0 0 800 740" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
          <defs>
            <filter id="difumina" x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur stdDeviation="80" /></filter>
            <filter id="grano" x="0" y="0" width="100%" height="100%">
              <feTurbulence type="fractalNoise" baseFrequency=".85" numOctaves="2" seed="7" stitchTiles="stitch" />
              <feColorMatrix type="matrix" values="0 0 0 0 1  0 0 0 0 1  0 0 0 0 1  0 0 0 .075 0" />
            </filter>
          </defs>
          <g filter="url(#difumina)">
            <ellipse cx="690" cy="150" rx="300" ry="220" fill="#0b5f73" opacity=".55" />
            <ellipse cx="560" cy="380" rx="260" ry="160" fill="#0891b2" opacity=".22" />
            <ellipse cx="120" cy="540" rx="330" ry="200" fill="#0891b2" opacity=".18" />
            <ellipse cx="420" cy="660" rx="380" ry="140" fill="#0b5f73" opacity=".5" />
          </g>
          <rect width="800" height="740" filter="url(#grano)" />
        </svg>
        <span className="login-logo sube" style={retraso(0)}>Kala</span>
        <div className="login-medio">
          <div>
            <h1>
              <span className="mascara"><span style={{ '--l': 0 } as React.CSSProperties}>Lo que respondes sale</span></span>
              <span className="mascara"><span style={{ '--l': 1 } as React.CSSProperties}>de tu teléfono mezclado</span></span>
              <span className="mascara"><span style={{ '--l': 2 } as React.CSSProperties}>con ruido.</span></span>
            </h1>
            <p className="login-lead sube" style={retraso(4)}>Tu equipo ve una estimación del conjunto. Nadie ve la respuesta de una persona.</p>
          </div>
          <div className="asienta"><FlujoRuido /></div>
        </div>
        <p className="login-prueba sube" style={retraso(6)}>
          <span><i />Sin datos individuales</span><span><i />Mínimo 30 respuestas por grupo</span>
        </p>
      </section>

      <section className="login-lado">
        <Link to="/ayuda" className="boton boton-contorno login-ayuda sube" style={retraso(3)}>Ayuda</Link>
        <form className="login-form" onSubmit={(e) => void enviar(e)} noValidate>
          <h2 className="sube" style={retraso(2)}>Entrar al panel</h2>
          <p className="login-sub sube" style={retraso(3)}>Para RRHH y coordinación de bienestar.</p>

          <div className="campo sube" style={retraso(4)}>
            <label htmlFor="usuario">Correo</label>
            <input
              id="usuario" name="username" type="email" autoComplete="username" required value={usuario}
              onChange={(e) => setUsuario(e.target.value)} aria-invalid={error ? true : undefined} aria-describedby={error ? idError : undefined}
            />
          </div>
          <div className="campo sube" style={retraso(5)}>
            <label htmlFor="clave">Contraseña</label>
            <input
              id="clave" name="password" type="password" autoComplete="current-password" required value={clave}
              onChange={(e) => setClave(e.target.value)} aria-invalid={error ? true : undefined} aria-describedby={error ? idError : undefined}
            />
          </div>
          {error && <p id={idError} role="alert" className="login-error">{error}</p>}
          <button type="submit" className="boton boton-primario sube" style={retraso(6)} disabled={enviando || !usuario || !clave}>
            {enviando ? 'Entrando…' : 'Entrar'}
            <i className={`login-barra${enviando ? ' cargando' : ''}`} aria-hidden="true" />
          </button>
        </form>
        <p className="login-pie sube" style={retraso(7)}>
          <span>¿Eres empleado? Usa la app en tu teléfono.</span>
          <Link to="/ayuda">Hablar con alguien</Link>
        </p>
      </section>
    </main>
  )
}
