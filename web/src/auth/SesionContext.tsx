import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from '../api/cliente'
import type { Perfil } from '../api/tipos'

interface Valor {
  perfil: Perfil | null
  cargando: boolean
  iniciar: (username: string, password: string) => Promise<void>
  cerrar: () => Promise<void>
}

const SesionContext = createContext<Valor | null>(null)

const ROLES_PANEL = ['ADMIN', 'RRHH', 'COORDINADOR_BIENESTAR']

export function SesionProvider({ children }: { children: ReactNode }) {
  const [perfil, setPerfil] = useState<Perfil | null>(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    let vigente = true
    api.restaurarSesion().then((p) => {
      if (vigente) {
        setPerfil(p)
        setCargando(false)
      }
    })
    return () => {
      vigente = false
    }
  }, [])

  const iniciar = useCallback(async (username: string, password: string) => {
    const p = await api.iniciarSesion(username, password)
    if (!p.roles.some((r) => ROLES_PANEL.includes(r))) {
      await api.cerrarSesion()
      throw new Error('Esta cuenta es de empleado. El panel es solo para RRHH y coordinación de bienestar.')
    }
    setPerfil(p)
  }, [])

  const cerrar = useCallback(async () => {
    await api.cerrarSesion()
    setPerfil(null)
  }, [])

  const valor = useMemo(() => ({ perfil, cargando, iniciar, cerrar }), [perfil, cargando, iniciar, cerrar])
  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>
}

export function useSesion(): Valor {
  const valor = useContext(SesionContext)
  if (!valor) throw new Error('useSesion must be used inside SesionProvider')
  return valor
}
