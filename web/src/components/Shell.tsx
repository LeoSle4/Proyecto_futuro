import { NavLink, Outlet } from 'react-router-dom'
import { useSesion } from '../auth/SesionContext'
import { MODO_DEMO } from '../api/cliente'
import './Shell.css'

const SECCIONES = [
  { ruta: '/', texto: 'Resumen semanal', fin: true },
  { ruta: '/equipos', texto: 'Equipos' },
  { ruta: '/empleados', texto: 'Empleados' },
  { ruta: '/cuestionarios', texto: 'Cuestionarios' },
  { ruta: '/solicitudes', texto: 'Solicitudes de ayuda' },
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
        <span className="shell-marca">Kala</span>
        <nav className="shell-nav" aria-label="Secciones del panel">
          {SECCIONES.map((s) => (
            <NavLink key={s.ruta} to={s.ruta} end={s.fin} className="shell-enlace">
              {s.texto}
            </NavLink>
          ))}
        </nav>
        <div className="shell-cuenta">
          {MODO_DEMO && <span className="shell-etiqueta">Datos sintéticos</span>}
          <span className="shell-rol">{rol}</span>
          <button type="button" className="boton boton-suave" onClick={() => void cerrar()}>
            Salir
          </button>
          {/* Help always sits in the same corner, last in the header (WCAG 3.2.6). */}
          <NavLink to="/ayuda" className="boton boton-ayuda">Ayuda</NavLink>
        </div>
      </header>
      <main id="contenido" className="shell-contenido" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  )
}
