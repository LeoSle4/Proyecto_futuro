import { useEffect } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useSesion } from '../auth/SesionContext'
import { MODO_DEMO } from '../api/cliente'
import './Shell.css'

// The administration manages companies, teams, and questionnaires; HR and wellbeing coordination work with their
// own company's teams, people, and requests.
interface Seccion {
  ruta: string
  texto: string
  fin?: boolean
}

const SECCIONES_ADMIN: Seccion[] = [
  { ruta: '/empresas', texto: 'Empresas' },
  { ruta: '/equipos', texto: 'Equipos' },
  { ruta: '/cuestionarios', texto: 'Cuestionarios' },
]
const SECCIONES_EMPRESA: Seccion[] = [
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
  const secciones = perfil?.roles.includes('ADMIN') ? SECCIONES_ADMIN : SECCIONES_EMPRESA
  const { pathname } = useLocation()
  // On a narrow screen the menu scrolls sideways: bring the current section into view so it is never hidden.
  useEffect(() => {
    document.querySelector('.shell-tabs a.active')?.scrollIntoView({ inline: 'center', block: 'nearest' })
  }, [pathname])
  return (
    <div className="shell">
      <a className="saltar" href="#contenido">Saltar al contenido</a>
      <header className="shell-cabecera">
        <span className="shell-marca sube" style={{ '--i': 0 } as React.CSSProperties}>Kala</span>
        <nav className="shell-tabs sube" style={{ '--i': 1 } as React.CSSProperties} aria-label="Secciones del panel">
          {secciones.map((s) => (
            <NavLink key={s.ruta} to={s.ruta} end={s.fin}>{s.texto}</NavLink>
          ))}
        </nav>
        <div className="shell-cuenta sube" style={{ '--i': 2 } as React.CSSProperties}>
          {MODO_DEMO && <span className="shell-etiqueta">Datos sintéticos</span>}
          <NavLink to="/cuenta" className="shell-cuenta-enlace">{rol} · Mi cuenta</NavLink>
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
