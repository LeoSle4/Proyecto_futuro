import { NavLink, Outlet } from 'react-router-dom'
import { useSesion } from '../auth/SesionContext'
import { MODO_DEMO } from '../api/cliente'
import './Shell.css'

const SECCIONES = [
  { ruta: '/', texto: 'Resumen', fin: true },
  { ruta: '/equipos', texto: 'Equipos' },
  { ruta: '/empleados', texto: 'Empleados' },
  { ruta: '/cuestionarios', texto: 'Cuestionarios' },
  { ruta: '/solicitudes', texto: 'Solicitudes' },
]

const ROL_TEXTO: Record<string, string> = {
  ADMIN: 'Administración',
  RRHH: 'RRHH',
  COORDINADOR_BIENESTAR: 'Coordinación de bienestar',
}

export function Shell() {
  const { perfil, cerrar } = useSesion()
  const rol = perfil?.roles.map((r) => ROL_TEXTO[r]).find(Boolean) ?? ''
  return (
    <div className="shell">
      <a className="saltar" href="#contenido">Saltar al contenido</a>
      <header className="shell-cabecera">
        <span className="shell-marca sube" style={{ '--i': 0 } as React.CSSProperties}>Kala</span>
        <nav className="shell-tabs sube" style={{ '--i': 1 } as React.CSSProperties} aria-label="Secciones del panel">
          {SECCIONES.map((s) => (
            <NavLink key={s.ruta} to={s.ruta} end={s.fin}>{s.texto}</NavLink>
          ))}
        </nav>
        <div className="shell-cuenta sube" style={{ '--i': 2 } as React.CSSProperties}>
          {MODO_DEMO && <span className="shell-etiqueta">Datos sintéticos</span>}
          <span>{rol}</span>
          <button type="button" className="shell-salir" onClick={() => void cerrar()}>Salir</button>
          {/* Help always sits in the same corner, last in the header (WCAG 3.2.6). */}
          <NavLink to="/ayuda" className="boton boton-contorno">Ayuda</NavLink>
        </div>
      </header>
      <main id="contenido" className="shell-contenido" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  )
}
