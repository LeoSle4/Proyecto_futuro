import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api, ErrorApi } from '../api/cliente'
import type { Cuestionario, Empresa, Presupuesto } from '../api/tipos'
import { useSesion } from '../auth/SesionContext'
import { usoDelPresupuesto, validarMaximo } from '../lib/presupuesto'
import { Aviso } from '../components/Aviso'
import { Selector } from '../components/Selector'
import './CuestionariosPage.css'
import { cantidad } from '../lib/texto'

const retraso = (i: number) => ({ '--i': i }) as React.CSSProperties
const num = (n: number) => (Math.round(n * 100) / 100).toLocaleString('es-PE', { maximumFractionDigits: 2 }).replace('.', ',')

export function CuestionariosPage() {
  const { perfil, cerrar } = useSesion()
  const esAdmin = Boolean(perfil?.roles.includes('ADMIN'))
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [elegida, setElegida] = useState<number | null>(null)
  const empresaId = perfil?.empresaId ?? elegida

  const [lista, setLista] = useState<Cuestionario[] | null>(null)
  const [presu, setPresu] = useState<Presupuesto | null>(null)
  const [confirmando, setConfirmando] = useState<number | null>(null)
  const [maximo, setMaximo] = useState('')
  const [errorMaximo, setErrorMaximo] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState('')

  const manejar = useCallback((e: unknown, porDefecto: string) => {
    if (e instanceof ErrorApi && e.estado === 401) void cerrar()
    else setError(e instanceof Error ? e.message : porDefecto)
  }, [cerrar])

  const recargar = useCallback(async (id: number) => {
    const [c, p] = await Promise.all([api.cuestionarios(id), api.presupuesto(id)])
    setLista(c)
    setPresu(p)
    setMaximo(String(p.epsilonMaximo).replace('.', ','))
  }, [])

  useEffect(() => {
    if (!esAdmin || perfil?.empresaId !== undefined) return
    let vigente = true
    api.empresas().then(
      (e) => {
        if (!vigente) return
        setEmpresas(e)
        setElegida((a) => a ?? e[0]?.id ?? null)
      },
      (e: unknown) => vigente && manejar(e, 'No pudimos cargar las empresas.'),
    )
    return () => {
      vigente = false
    }
  }, [esAdmin, perfil?.empresaId, manejar])

  useEffect(() => {
    if (empresaId === null || empresaId === undefined) return
    let vigente = true
    Promise.all([api.cuestionarios(empresaId), api.presupuesto(empresaId)]).then(
      ([c, p]) => {
        if (!vigente) return
        setLista(c)
        setPresu(p)
        setMaximo(String(p.epsilonMaximo).replace('.', ','))
      },
      (e: unknown) => vigente && manejar(e, 'No pudimos cargar los cuestionarios.'),
    )
    return () => {
      vigente = false
    }
  }, [empresaId, manejar])

  // `junto` is where the error is shown: next to the budget field, or at the top for the rest.
  async function ejecutar(accion: () => Promise<unknown>, mensaje: string, junto: 'arriba' | 'maximo' = 'arriba'): Promise<boolean> {
    if (empresaId === null || empresaId === undefined) return false
    setError(null)
    setOcupado(true)
    try {
      await accion()
      await recargar(empresaId)
      setAviso(mensaje)
      return true
    } catch (e) {
      if (junto === 'maximo' && !(e instanceof ErrorApi && e.estado === 401)) setErrorMaximo(e instanceof Error ? e.message : 'No pudimos guardar el máximo.')
      else manejar(e, 'No pudimos completar el cambio.')
      return false
    } finally {
      setOcupado(false)
    }
  }

  async function guardarMaximo(e: FormEvent) {
    e.preventDefault()
    const r = validarMaximo(maximo)
    setErrorMaximo(r.error ?? null)
    if (r.valor === undefined || empresaId === null || empresaId === undefined) return
    await ejecutar(() => api.fijarPresupuesto(empresaId, r.valor), `Máximo por persona actualizado a ${num(r.valor)}.`, 'maximo')
  }

  const uso = presu ? usoDelPresupuesto(presu.epsilonMensual, presu.epsilonMaximo) : null
  const activos = lista?.filter((c) => c.activo).length ?? 0

  return (
    <section className="cu-pagina">
      <h1><span className="mascara"><span>Cuestionarios</span></span></h1>
      <p className="cu-intro sube" style={retraso(2)}>
        Qué preguntan las personas y cuánto ruido se permite sumar a lo que responden. {esAdmin ? 'Como administración, activas y desactivas cuestionarios.' : 'Solo administración activa y desactiva; tú ves el estado.'}
      </p>

      {esAdmin && perfil?.empresaId === undefined && (
        <div className="cu-empresa sube" style={retraso(3)}>
          <label htmlFor="cu-empresa">Empresa</label>
          <Selector id="cu-empresa" etiqueta="Empresas" valor={elegida === null ? '' : String(elegida)} opciones={empresas.map((e) => ({ valor: String(e.id), texto: e.nombre }))} alCambiar={(v) => { setLista(null); setPresu(null); setElegida(Number(v)) }} />
        </div>
      )}

      {error && <p role="alert" className="cu-error">{error}</p>}
      <Aviso mensaje={aviso} alCerrar={() => setAviso('')} />

      {empresaId === null || empresaId === undefined ? (
        <p className="cu-nota">{esAdmin ? 'Elige una empresa para ver sus cuestionarios.' : 'Tu cuenta no tiene una empresa asignada.'}</p>
      ) : (
        <div className="cu-cuerpo">
          <div>
            {lista === null && !error && <p className="cu-nota">Cargando cuestionarios…</p>}
            {lista?.length === 0 && <div className="cu-vacio hatch"><h2>No hay cuestionarios</h2><p>Todavía no existe ninguno en el catálogo.</p></div>}
            {lista && lista.length > 0 && (
              <ul className="cu-lista sube" style={retraso(4)}>
                {lista.map((c) => (
                  <li key={c.id} className={`cu-fila${c.activo ? '' : ' inactivo'}`}>
                    <div className="cu-quien">
                      <b>{c.nombre}</b>
                      <span>{c.codigo} · cada {cantidad(c.cadenciaDias, 'día', 'días')} · {c.itemsPorEnvio ? `${cantidad(c.itemsPorEnvio, 'pregunta', 'preguntas')} por envío` : 'todas las preguntas'}</span>
                    </div>
                    <span className={`cu-estado${c.activo ? '' : ' off'}`}>{c.activo ? 'Activo' : 'Inactivo'}</span>
                    {esAdmin && (
                      <span className="cu-acciones">
                        {confirmando === c.id ? (
                          <>
                            <button type="button" className="boton boton-peligro" disabled={ocupado} onClick={() => void ejecutar(() => api.desactivarCuestionario(empresaId, c.id), `${c.nombre} desactivado.`).then(() => setConfirmando(null))}>Desactivar</button>
                            <button type="button" className="boton" disabled={ocupado} onClick={() => setConfirmando(null)}>Cancelar</button>
                          </>
                        ) : c.activo ? (
                          <button type="button" className="boton" onClick={() => setConfirmando(c.id)}>Desactivar</button>
                        ) : (
                          <button type="button" className="boton boton-contorno" disabled={ocupado} onClick={() => void ejecutar(() => api.activarCuestionario(empresaId, c.id), `${c.nombre} activado.`)}>Activar</button>
                        )}
                      </span>
                    )}
                    {confirmando === c.id && <p className="cu-aviso-fila">Las personas dejarán de recibirlo desde el próximo envío. Puedes activarlo de nuevo cuando quieras.</p>}
                  </li>
                ))}
              </ul>
            )}
          </div>

          <aside className="cu-presupuesto sube" style={retraso(5)} aria-label="Presupuesto de privacidad">
            <h2>Presupuesto de privacidad</h2>
            {presu && uso ? (
              <>
                <p className="cu-cifra">{num(presu.epsilonMensual)} <small>de {num(presu.epsilonMaximo)}</small></p>
                <div className="cu-pista" role="img" aria-label={`${uso.porcentaje} % del presupuesto en uso`}>
                  <i style={{ transform: `scaleX(${uso.porcentaje / 100})` }} className={uso.cerca ? 'cerca' : ''} />
                </div>
                <p className="cu-explica">
                  Es cuánto ruido se permite sumar por persona al mes. Cuantos más cuestionarios activos, más se usa.
                </p>
                <p className="cu-estado-uso">
                  {uso.agotado ? 'Está agotado: para activar otro, sube el máximo o desactiva uno.' : uso.cerca ? `Cerca del límite: quedan ${num(presu.epsilonMaximo - presu.epsilonMensual)}.` : `Quedan ${num(presu.epsilonMaximo - presu.epsilonMensual)}.`}
                </p>
                <dl className="cu-libro">
                  <div><dt>Cuestionarios activos</dt><dd>{activos}</dd></div>
                  <div><dt>Mes</dt><dd>{presu.periodo}</dd></div>
                </dl>
                {esAdmin && (
                  <form className="cu-editar" onSubmit={(e) => void guardarMaximo(e)} noValidate>
                    <label htmlFor="cu-maximo">Máximo por persona</label>
                    <div className="cu-fila-edit">
                      <input id="cu-maximo" inputMode="decimal" value={maximo} onChange={(e) => setMaximo(e.target.value)} aria-invalid={errorMaximo ? true : undefined} aria-describedby={errorMaximo ? 'cu-err' : undefined} />
                      <button type="submit" className="boton boton-primario" disabled={ocupado}>Guardar</button>
                    </div>
                    {errorMaximo && <p id="cu-err" role="alert" className="cu-err">{errorMaximo}</p>}
                  </form>
                )}
              </>
            ) : (
              <p className="cu-nota">Cargando…</p>
            )}
          </aside>
        </div>
      )}
    </section>
  )
}
