import { Navigate, Route, Routes } from 'react-router-dom'
import { useSesion } from './auth/SesionContext'
import { Shell } from './components/Shell'
import { AyudaPage } from './pages/AyudaPage'
import { CuestionariosPage } from './pages/CuestionariosPage'
import { CuentaPage } from './pages/CuentaPage'
import { EmpleadosPage } from './pages/EmpleadosPage'
import { EmpresasPage } from './pages/EmpresasPage'
import { EquiposPage } from './pages/EquiposPage'
import { LoginPage } from './pages/LoginPage'
import { SolicitudesPage } from './pages/SolicitudesPage'
import { ResumenPage } from './pages/ResumenPage'

// The administration has no company of its own, so its home is the list of companies.
function Inicio() {
  const { perfil } = useSesion()
  return perfil?.roles.includes('ADMIN') ? <Navigate to="/empresas" replace /> : <ResumenPage />
}

function Protegido() {
  const { perfil, cargando } = useSesion()
  if (cargando) return <p className="cargando-sesion" role="status">Cargando…</p>
  return perfil ? <Shell /> : <Navigate to="/entrar" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/entrar" element={<LoginPage />} />
      <Route element={<Protegido />}>
        <Route index element={<Inicio />} />
        <Route path="empresas" element={<EmpresasPage />} />
        <Route path="cuenta" element={<CuentaPage />} />
        <Route path="equipos" element={<EquiposPage />} />
        <Route path="empleados" element={<EmpleadosPage />} />
        <Route path="cuestionarios" element={<CuestionariosPage />} />
        <Route path="solicitudes" element={<SolicitudesPage />} />
        <Route path="ayuda" element={<AyudaPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
