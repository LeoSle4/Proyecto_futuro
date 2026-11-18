import { useEffect, useState, type FormEvent } from 'react'
import { api, ErrorApi } from '../api/cliente'
import { useSesion } from '../auth/SesionContext'
import type { Presupuesto } from '../api/tipos'

export function PresupuestoPage() {
  const { perfil } = useSesion()
  const perfilUsuario = perfil as { empresaId?: number; roles?: string[] } | null
  const empresaId = perfilUsuario?.empresaId ?? 1
  const esAdmin = perfilUsuario?.roles?.includes('ADMIN') ?? false

  const [presupuesto, setPresupuesto] = useState<Presupuesto | null>(null)
  const [cargando, setCargando] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)
  
  const [nuevoMaximo, setNuevoMaximo] = useState<string>('')
  const [guardando, setGuardando] = useState<boolean>(false)
  const [mensajeExito, setMensajeExito] = useState<string | null>(null)
  const [errorValidacion, setErrorValidacion] = useState<string | null>(null)

  async function cargarPresupuesto() {
    setCargando(true)
    setError(null)
    try {
      const data = await api.presupuesto(empresaId)
      setPresupuesto(data)
      setNuevoMaximo(data.epsilonMaximo.toString())
    } catch (err: unknown) {
      if (err instanceof ErrorApi) {
        setError(err.message)
      } else {
        setError('No pudimos cargar el presupuesto de privacidad.')
      }
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    cargarPresupuesto()
  }, [empresaId])

  async function handleGuardar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setErrorValidacion(null)
    setMensajeExito(null)

    const valor = parseFloat(nuevoMaximo)
    if (isNaN(valor) || valor <= 0) {
      setErrorValidacion('Ingresa un número positivo válido.')
      return
    }
    if (valor > 999999) {
      setErrorValidacion('El límite máximo permitido es 999.999.')
      return
    }

    setGuardando(true)
    try {
      const actualizado = await api.fijarPresupuesto(empresaId, valor)
      setPresupuesto(actualizado)
      setMensajeExito('Presupuesto actualizado correctamente.')
    } catch (err: unknown) {
      if (err instanceof ErrorApi) {
        setErrorValidacion(err.message)
      } else {
        setErrorValidacion('Ocurrió un error al guardar el presupuesto.')
      }
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) {
    return <div className="cargando-pagina" role="status">Cargando presupuesto de privacidad…</div>
  }

  if (error) {
    return (
      <div className="contenedor-error" role="alert">
        <p>{error}</p>
        <button onClick={cargarPresupuesto} className="btn-secundario">Reintentar</button>
      </div>
    )
  }

  const maximo = presupuesto?.epsilonMaximo ?? 0
  const usado = presupuesto?.epsilonMensual ?? 0
  const porcentaje = maximo > 0 ? Math.min(Math.round((usado / maximo) * 100), 100) : 0

  return (
    <div className="pagina-presupuesto">
      <header className="cabecera-seccion">
        <h1>Presupuesto de privacidad de la empresa</h1>
        <p className="explicacion-amigable">
          Esto define cuánto ruido se permite sumar por persona al mes para proteger la privacidad de los colaboradores al responder cuestionarios.
        </p>
      </header>

      <section className="tarjeta-contenido">
        <div className="indicadores-presupuesto" style={{ display: 'flex', gap: '2rem', marginBottom: '1.5rem' }}>
          <div className="dato-metrica" style={{ display: 'flex', flexDirection: 'column' }}>
            <span className="etiqueta" style={{ color: 'var(--texto-secundario, #666)' }}>Máximo permitido por persona</span>
            <span className="valor" style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{maximo}</span>
          </div>
          <div className="dato-metrica" style={{ display: 'flex', flexDirection: 'column' }}>
            <span className="etiqueta" style={{ color: 'var(--texto-secundario, #666)' }}>Utilizado actualmente</span>
            <span className="valor" style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{usado}</span>
          </div>
        </div>

        <div className="contenedor-barra-uso">
          <div className="info-barra">
            <span>Nivel de uso actual</span>
            <span>{porcentaje}%</span>
          </div>
          <div className="barra-progreso-track">
            <div 
              className="barra-progreso-fill" 
              style={{ width: `${porcentaje}%` }}
            />
          </div>
        </div>

        {esAdmin ? (
          <form onSubmit={handleGuardar} className="formulario-presupuesto">
            <h2>Actualizar límite máximo</h2>
            <div className="grupo-input">
              <label htmlFor="epsilonMaximo">Nuevo límite máximo (Tope: 999.999)</label>
              <input 
                id="epsilonMaximo"
                type="number"
                step="any"
                min="0.001"
                max="999999"
                value={nuevoMaximo}
                onChange={(e) => setNuevoMaximo(e.target.value)}
                disabled={guardando}
              />
              {errorValidacion && (
                <span className="error-campo" role="alert">
                  {errorValidacion}
                </span>
              )}
            </div>

            {mensajeExito && (
              <p className="mensaje-exito" role="status">{mensajeExito}</p>
            )}

            <button type="submit" className="btn-primario" disabled={guardando}>
              {guardando ? 'Guardando...' : 'Guardar cambios'}
            </button>
          </form>
        ) : (
          <p className="aviso-lectura">
            Tu rol actual es de solo lectura. Solo los administradores pueden modificar este valor.
          </p>
        )}
      </section>
    </div>
  )
}