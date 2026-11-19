import { useEffect, useMemo, useState } from 'react'
import { api, ErrorApi, MODO_DEMO } from '../api/cliente'
import type { Dashboard, Equipo } from '../api/tipos'
import { N_MINIMO } from '../api/tipos'
import { useSesion } from '../auth/SesionContext'
import { PanelPulso } from '../components/PanelPulso'
import { ResumenSemana } from '../components/ResumenSemana'
import { SelectorEquipos } from '../components/SelectorEquipos'
import { TarjetaIndicador } from '../components/TarjetaIndicador'
import { separarPrincipal } from '../lib/principal'
import { etiquetaSemana, lunesActual, moverSemanas } from '../lib/semana'
import './ResumenPage.css'

type Resumen = Dashboard | 'error' | undefined

function textoIntroductorio(r: Dashboard, esActual: boolean): string {
  const cuando = esActual ? 'esta semana' : 'esa semana'
  if (r.resultado === 'OK') return `${r.totalRespuestas} respuestas ${cuando}: hay resultado. El margen marca lo que todavía no podemos afirmar.`
  const faltan = N_MINIMO - r.totalRespuestas
  return faltan <= 3
    ? `${r.totalRespuestas} respuestas ${cuando}: faltan ${faltan} para llegar al mínimo de ${N_MINIMO}.`
    : `${r.totalRespuestas} respuestas ${cuando}: todavía no alcanza el mínimo de ${N_MINIMO}. No se muestra nada para proteger al grupo.`
}

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
  const filas = (equipos ?? []).map((e) => {
    const r = resumenes[`${semana}:${e.id}`]
    return r && r !== 'error' ? r : null
  })

  if (falloEquipos) return <p role="alert" className="resumen-aviso">{falloEquipos}</p>
  if (equipos && equipos.length === 0) {
    return (
      <section className="resumen-vacio">
        <h1>Todavía no hay equipos</h1>
        <p>Crea el primer equipo para empezar a recibir estimaciones.</p>
      </section>
    )
  }

  const datos = resumen && resumen !== 'error' ? resumen : null
  const { principal, resto } = separarPrincipal(datos?.items ?? [])
  return (
    <div className="resumen">
      <aside className="resumen-lateral" aria-label="Equipos">
        <h2 className="rotulo">EQUIPOS</h2>
        {equipos && <SelectorEquipos equipos={equipos} resumenes={resumenes} semana={semana} activo={elegido} alElegir={setElegido} />}
        <ResumenSemana rotulo={`Semana ${etiquetaSemana(semana)}`} filas={filas} />
      </aside>

      <div className="resumen-centro">
        <header className="resumen-cabecera">
          <h1 key={equipo?.nombre ?? 'cargando'}>
            <span className="mascara"><span>{equipo?.nombre ?? 'Cargando…'}</span></span>
          </h1>
          <p className="resumen-intro sube" style={{ '--i': 3 } as React.CSSProperties} key={`${semana}:${elegido}:i`}>
            {datos ? textoIntroductorio(datos, esActual) : 'Mira cuánto sabemos, y cuánto todavía no.'}
          </p>
          <div className="semana sube" style={{ '--i': 4 } as React.CSSProperties} role="group" aria-label="Semana">
            <button type="button" onClick={() => setSemana(moverSemanas(semana, -1))} aria-label="Semana anterior">←</button>
            <span>{etiquetaSemana(semana)}</span>
            <button type="button" onClick={() => setSemana(moverSemanas(semana, 1))} disabled={esActual} aria-label="Semana siguiente">→</button>
          </div>
        </header>

        {MODO_DEMO && <p className="resumen-demo">Empresa demo: todos los datos son sintéticos.</p>}

        <div aria-live="polite" key={`${semana}:${elegido}`} className="resumen-contenido">
          {resumen === undefined && <p className="resumen-aviso">Cargando la semana…</p>}
          {resumen === 'error' && <p role="alert" className="resumen-aviso">No pudimos cargar esta semana. Inténtalo de nuevo en un momento.</p>}
          {datos && (datos.items.length === 0 ? (
            <p className="resumen-aviso">Este equipo no tiene cuestionarios activos esta semana.</p>
          ) : (
            <>
              {principal && <PanelPulso item={principal} total={datos.totalRespuestas} />}
              {resto.length > 0 && (
                <div className="resumen-pares">
                  {resto.map((item, i) => (
                    <TarjetaIndicador key={item.itemId} item={item} total={datos.totalRespuestas} retardo={900 + i * 100} />
                  ))}
                </div>
              )}
            </>
          ))}
        </div>

        <p className="resumen-notas sube" style={{ '--i': 8 } as React.CSSProperties}>
          <b>Cómo leer esto.</b> Cada cifra es una estimación del equipo con su margen: la respuesta de cada persona se mezcla con ruido
          en su teléfono antes de salir, y el panel no puede deshacerlo.
        </p>
      </div>
    </div>
  )
}
