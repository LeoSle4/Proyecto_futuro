import { useEffect, useMemo, useState } from 'react'
import { api, ErrorApi, MODO_DEMO } from '../api/cliente'
import type { Dashboard, Equipo } from '../api/tipos'
import { useSesion } from '../auth/SesionContext'
import { Indicador } from '../components/Indicador'
import { TarjetaEquipo } from '../components/TarjetaEquipo'
import { etiquetaSemana, lunesActual, moverSemanas } from '../lib/semana'
import './ResumenPage.css'

type Resumen = Dashboard | 'error'

export function ResumenPage() {
  const { cerrar } = useSesion()
  const [equipos, setEquipos] = useState<Equipo[] | null>(null)
  const [semana, setSemana] = useState(lunesActual)
  const [elegido, setElegido] = useState<number | null>(null)
  const [resumenes, setResumenes] = useState<Record<string, Resumen>>({})
  const [falloEquipos, setFalloEquipos] = useState<string | null>(null)

  useEffect(() => {
    let vigente = true
    api.equipos().then(
      (lista) => {
        if (!vigente) return
        setEquipos(lista)
        setElegido((actual) => actual ?? lista[0]?.id ?? null)
      },
      (e: unknown) => {
        if (!vigente) return
        if (e instanceof ErrorApi && e.estado === 401) void cerrar()
        else setFalloEquipos(e instanceof Error ? e.message : 'No pudimos cargar los equipos.')
      },
    )
    return () => {
      vigente = false
    }
  }, [cerrar])

  useEffect(() => {
    if (!equipos) return
    let vigente = true
    equipos.forEach((equipo) => {
      api.dashboard(equipo.id, semana).then(
        (d) => vigente && setResumenes((previos) => ({ ...previos, [`${semana}:${equipo.id}`]: d })),
        () => vigente && setResumenes((previos) => ({ ...previos, [`${semana}:${equipo.id}`]: 'error' })),
      )
    })
    return () => {
      vigente = false
    }
  }, [equipos, semana])

  const equipo = useMemo(() => equipos?.find((e) => e.id === elegido) ?? null, [equipos, elegido])
  const resumen = elegido !== null ? resumenes[`${semana}:${elegido}`] : undefined
  const esActual = semana === lunesActual()

  if (falloEquipos) return <p role="alert" className="resumen-aviso">{falloEquipos}</p>
  if (equipos && equipos.length === 0) {
    return (
      <section className="resumen-vacio">
        <h1>Todavía no hay equipos</h1>
        <p>Crea el primer equipo para empezar a recibir estimaciones.</p>
      </section>
    )
  }

  return (
    <div className="resumen">
      <aside className="resumen-equipos" aria-label="Equipos">
        <h2 className="resumen-lateral-titulo">Equipos</h2>
        {equipos?.map((e) => (
          <TarjetaEquipo
            key={e.id}
            equipo={e}
            resumen={resumenes[`${semana}:${e.id}`] ?? null}
            activo={e.id === elegido}
            alElegir={() => setElegido(e.id)}
          />
        ))}
      </aside>

      <div className="resumen-centro">
        <header className="resumen-cabecera">
          <div>
            <h1>{equipo?.nombre ?? 'Cargando…'}</h1>
            <p className="resumen-sub">
              {resumen && resumen !== 'error' && resumen.resultado === 'OK'
                ? 'Esta semana hay resultado. La neblina marca lo que todavía no podemos afirmar.'
                : 'Mira cuánto sabemos, y cuánto todavía no.'}
            </p>
          </div>
          <div className="resumen-semana" role="group" aria-label="Semana">
            <button type="button" className="boton" onClick={() => setSemana(moverSemanas(semana, -1))} aria-label="Semana anterior">←</button>
            <span className="resumen-semana-texto">{etiquetaSemana(semana)}</span>
            <button type="button" className="boton" onClick={() => setSemana(moverSemanas(semana, 1))} disabled={esActual} aria-label="Semana siguiente">→</button>
          </div>
        </header>

        {MODO_DEMO && <p className="resumen-aviso-demo">Empresa demo: todos los datos son sintéticos.</p>}

        <div aria-live="polite">
          {resumen === undefined && <p className="resumen-aviso">Cargando la semana…</p>}
          {resumen === 'error' && <p role="alert" className="resumen-aviso">No pudimos cargar esta semana. Inténtalo de nuevo en un momento.</p>}
          {resumen && resumen !== 'error' && (
            resumen.items.length === 0 ? (
              <p className="resumen-aviso">Este equipo no tiene cuestionarios activos esta semana.</p>
            ) : (
              <div className="resumen-indicadores">
                {resumen.items.map((item, i) => (
                  <Indicador key={item.itemId} item={item} respuestas={resumen.totalRespuestas} principal={i === 0} />
                ))}
              </div>
            )
          )}
        </div>

        <section className="resumen-saber" aria-label="Qué sabemos y qué no">
          <p><strong>Qué sabemos.</strong> Una estimación del equipo y su margen.</p>
          <p><strong>Qué no sabemos.</strong> Qué respondió cada persona, ni quién respondió: cada respuesta se mezcla con ruido en el teléfono antes de salir.</p>
        </section>
      </div>
    </div>
  )
}
