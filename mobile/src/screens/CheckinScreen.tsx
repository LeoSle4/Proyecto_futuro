import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native'
import { api, ErrorApi } from '../api/cliente'
import type { ItemCuestionario } from '../api/tipos'
import { Boton } from '../components/Boton'
import { Cabecera } from '../components/Cabecera'
import { Fondo } from '../components/Fondo'
import { MomentoPrivacidad } from '../components/MomentoPrivacidad'
import { uniformeSeguro } from '../domain/azar'
import { perturbarItem, type EnvioItem } from '../domain/ldp'
import { color, fuente, radio } from '../theme'

interface Props {
  alAyuda: () => void
  alSalir: () => void
  alCambiarClave: () => void
  alSesionVencida: () => void
}

type Fase = 'cargando' | 'elegir' | 'privacidad' | 'enviando' | 'listo' | 'yaRespondio' | 'vacio' | 'error'

// A closing state (done, nothing to answer, already answered, error): one centred composition, so the screen is never
// half empty. The ring-and-dot is the same mark the privacy moment uses; it is decoration, not a reward.
function Estado({ titulo, texto, conMarca = true, children }: { titulo: string; texto: string; conMarca?: boolean; children: ReactNode }) {
  return (
    <View style={estilos.centro}>
      {conMarca && (
        <View style={estilos.marca} accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
          <View style={estilos.marcaPunto} />
        </View>
      )}
      <Text style={estilos.titulo} accessibilityRole="header">{titulo}</Text>
      <Text style={estilos.texto}>{texto}</Text>
      <View style={estilos.acciones}>{children}</View>
    </View>
  )
}

export function CheckinScreen({ alAyuda, alSalir, alCambiarClave, alSesionVencida }: Props) {
  const [items, setItems] = useState<ItemCuestionario[]>([])
  const [indice, setIndice] = useState(0)
  const [fase, setFase] = useState<Fase>('cargando')
  const [valor, setValor] = useState<number | null>(null)
  const [envio, setEnvio] = useState<EnvioItem | null>(null)
  const [envios, setEnvios] = useState<EnvioItem[]>([])
  const [mensaje, setMensaje] = useState<string | null>(null)

  const cargar = useCallback(async () => {
    setFase('cargando')
    setMensaje(null)
    setEnvios([])
    try {
      const cuestionarios = await api.pendientes()
      const primero = cuestionarios[0]
      const lista = primero ? [...primero.items].sort((a, b) => a.orden - b.orden).slice(0, primero.itemsPorEnvio ?? undefined) : []
      setItems(lista)
      setIndice(0)
      setFase(lista.length ? 'elegir' : 'vacio')
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 401) alSesionVencida()
      else {
        setMensaje(e instanceof Error ? e.message : 'No pudimos cargar tus preguntas.')
        setFase('error')
      }
    }
  }, [alSesionVencida])

  useEffect(() => {
    void cargar()
  }, [cargar])

  const item = items[indice]
  const restantes = items.length - indice

  // The noise is drawn once, at selection time, so the value shown is exactly the value sent.
  function elegir(v: number) {
    if (!item) return
    setValor(v)
    setEnvio(perturbarItem(item, v, uniformeSeguro))
    setFase('privacidad')
  }

  // The questionnaire is one submission: each answer waits on the phone and the last one sends them all together.
  async function enviar() {
    if (!envio) return
    const lote = [...envios.slice(0, indice), envio]
    setMensaje(null)
    if (indice + 1 < items.length) {
      setEnvios(lote)
      setValor(null)
      setEnvio(null)
      setIndice(indice + 1)
      setFase('elegir')
      return
    }
    setFase('enviando')
    try {
      await api.enviar(lote)
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 401) return alSesionVencida()
      if (e instanceof ErrorApi && e.estado === 409) {
        setFase('yaRespondio')
        return
      }
      setMensaje(e instanceof Error ? e.message : 'No pudimos enviar tus respuestas.')
      setFase('privacidad')
      return
    }
    setEnvios([])
    setValor(null)
    setEnvio(null)
    setFase('listo')
  }

  // Going back keeps the earlier answers; only the one being changed is chosen again.
  function cambiarRespuesta() {
    setValor(null)
    setEnvio(null)
    setFase('elegir')
  }

  const opciones = useMemo(() => item?.escala.opciones ?? [], [item])

  return (
    <Fondo>
      <Cabecera alAyuda={alAyuda} />
      <ScrollView contentContainerStyle={estilos.cuerpo}>
        {fase === 'cargando' && <ActivityIndicator color={color.mar} size="large" accessibilityLabel="Cargando tus preguntas" />}

        {fase === 'error' && (
          <Estado titulo="No pudimos cargar tus preguntas" texto={mensaje ?? ''} conMarca={false}>
            <Boton texto="Intentar de nuevo" alPulsar={() => void cargar()} />
          </Estado>
        )}

        {fase === 'vacio' && (
          <Estado titulo="Hoy no hay nada que responder" texto="Cuando haya una pregunta nueva, la verás aquí.">
            <Boton texto="Cambiar mi contraseña" variante="secundario" alPulsar={alCambiarClave} />
            <Boton texto="Salir" variante="secundario" alPulsar={alSalir} />
          </Estado>
        )}

        {fase === 'listo' && (
          <Estado titulo="Listo por hoy" texto="Gracias por responder. Tus respuestas salieron mezcladas con ruido: nadie puede ver las tuyas.">
            <Boton texto="Cambiar mi contraseña" variante="secundario" alPulsar={alCambiarClave} />
            <Boton texto="Salir" variante="secundario" alPulsar={alSalir} />
          </Estado>
        )}

        {fase === 'yaRespondio' && (
          <Estado titulo="Ya respondiste esta vez" texto="Este cuestionario se responde una vez por periodo. Tus respuestas anteriores siguen guardadas y las de ahora no se enviaron.">
            <Boton texto="Cambiar mi contraseña" variante="secundario" alPulsar={alCambiarClave} />
            <Boton texto="Salir" variante="secundario" alPulsar={alSalir} />
          </Estado>
        )}

        {item && (fase === 'elegir' || fase === 'privacidad' || fase === 'enviando') && (
          <>
            <Text style={estilos.restantes}>{restantes === 1 ? 'Última pregunta' : `Quedan ${restantes} preguntas`}</Text>
            <View style={estilos.pregunta}>
              <Text style={estilos.preguntaTexto} accessibilityRole="header">{item.texto}</Text>
            </View>

            {fase === 'elegir' && (
              <View style={estilos.opciones}>
                {opciones.map((o) => (
                  <Boton key={o.valor} texto={o.etiqueta} variante="secundario" alPulsar={() => elegir(o.valor)} />
                ))}
              </View>
            )}

            {(fase === 'privacidad' || fase === 'enviando') && valor !== null && envio && (
              <View style={estilos.opciones}>
                <MomentoPrivacidad item={item} valorReal={valor} envio={envio} />
                {mensaje && <Text style={estilos.error} accessibilityRole="alert">{mensaje}</Text>}
                <Boton texto={fase === 'enviando' ? 'Enviando…' : restantes === 1 ? 'Enviar' : 'Siguiente'} alPulsar={() => void enviar()} deshabilitado={fase === 'enviando'} />
                <Boton texto="Cambiar mi respuesta" variante="secundario" alPulsar={cambiarRespuesta} deshabilitado={fase === 'enviando'} />
              </View>
            )}
          </>
        )}
      </ScrollView>
    </Fondo>
  )
}

const estilos = StyleSheet.create({
  cuerpo: { flexGrow: 1, padding: 20, gap: 16, paddingBottom: 40 },
  centro: { flexGrow: 1, justifyContent: 'center', gap: 14, paddingBottom: 48 },
  marca: { alignSelf: 'center', width: 72, height: 72, borderRadius: 36, borderWidth: 3, borderColor: color.turquesa, alignItems: 'center', justifyContent: 'center', marginBottom: 10 },
  marcaPunto: { width: 24, height: 24, borderRadius: 12, backgroundColor: color.turquesa },
  titulo: { fontFamily: fuente.negrita, fontSize: 30, color: color.tinta, textAlign: 'center' },
  texto: { fontFamily: fuente.regular, fontSize: 18, lineHeight: 26, color: color.tintaSuave, textAlign: 'center', maxWidth: 320, alignSelf: 'center' },
  acciones: { gap: 12, alignSelf: 'stretch', marginTop: 14 },
  restantes: { fontFamily: fuente.semi, fontSize: 16, color: color.tintaSuave },
  pregunta: { backgroundColor: color.superficie, borderColor: color.borde, borderWidth: 1, borderRadius: radio.tarjeta, padding: 24 },
  preguntaTexto: { fontFamily: fuente.negrita, fontSize: 28, lineHeight: 34, color: color.tinta },
  opciones: { gap: 12 },
  error: { fontFamily: fuente.semi, fontSize: 16, color: color.error },
})
