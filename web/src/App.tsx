import { Navigate, Route, Routes } from 'react-router-dom'
import { useSesion } from './auth/SesionContext'
import { Shell } from './components/Shell'
import { AyudaPage } from './pages/AyudaPage'
import { CuestionariosPage } from './pages/CuestionariosPage'
import { EmpleadosPage } from './pages/EmpleadosPage'
import { EquiposPage } from './pages/EquiposPage'
import { LoginPage } from './pages/LoginPage'
import { Pendiente } from './pages/Pendiente'
import { ResumenPage } from './pages/ResumenPage'

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
        <Route index element={<ResumenPage />} />
        <Route path="equipos" element={<EquiposPage />} />
        <Route path="empleados" element={<EmpleadosPage />} />
        <Route path="cuestionarios" element={<CuestionariosPage />} />
        <Route path="solicitudes" element={<Pendiente titulo="Solicitudes de ayuda" descripcion="Solicitudes que las personas dejan para hablar con alguien." />} />
        <Route path="ayuda" element={<AyudaPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
