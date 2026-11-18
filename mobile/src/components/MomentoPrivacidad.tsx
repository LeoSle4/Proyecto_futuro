import { useEffect, useRef, useState } from 'react'
import { AccessibilityInfo, Animated, StyleSheet, Text, View } from 'react-native'
import type { EnvioItem } from '../domain/ldp'
import type { ItemCuestionario } from '../api/tipos'
import { color, fuente, radio } from '../theme'

interface Props {
  item: ItemCuestionario
  valorReal: number
  envio: EnvioItem
}

const decimal = (n: number) => n.toLocaleString('es-PE', { maximumFractionDigits: 1 })

// Privacy made visible: the real answer stays on the phone; this is what actually leaves it.
export function MomentoPrivacidad({ item, valorReal, envio }: Props) {
  const etiquetaReal = item.escala.opciones.find((o) => o.valor === valorReal)?.etiqueta ?? String(valorReal)
  const esRR = envio.respuestaRuidosa !== undefined
  const enviado = esRR
    ? item.escala.opciones.find((o) => o.valor === (envio.respuestaRuidosa ? item.escala.valorMax : item.escala.valorMin))?.etiqueta ?? ''
    : decimal(envio.valorRuidoso ?? 0)

  const [sinMovimiento, setSinMovimiento] = useState(false)
  const aparece = useRef(new Animated.Value(0)).current

  useEffect(() => {
    AccessibilityInfo.isReduceMotionEnabled().then(setSinMovimiento)
  }, [])

  useEffect(() => {
    if (sinMovimiento) {
      aparece.setValue(1)
      return
    }
    aparece.setValue(0)
    Animated.timing(aparece, { toValue: 1, duration: 700, delay: 500, useNativeDriver: true }).start()
  }, [aparece, sinMovimiento, envio])

  const cambiadas = Math.round((1 - (item.probabilidadVerdad ?? 0.75)) * 100)
  const explicacion = esRR
    ? `Para que nadie pueda saber qué respondiste de verdad, la app cambia a propósito unas ${cambiadas} de cada 100 respuestas. Quien lo recibe solo ve el conjunto del equipo.`
    : `La app le suma un poco de ruido a tu respuesta antes de enviarla, así que el número que sale no es el tuyo exacto. Quien lo recibe solo ve el conjunto del equipo.`

  return (
    <View style={estilos.tarjeta} accessibilityLiveRegion="polite">
      <View style={estilos.bloque}>
        <Text style={estilos.titulo}>Tu respuesta</Text>
        <Text style={estilos.valor}>{etiquetaReal}</Text>
        <Text style={estilos.nota}>Se queda en tu teléfono.</Text>
      </View>
      <View style={estilos.separador} />
      <Animated.View style={[estilos.bloque, { opacity: aparece, transform: [{ translateY: aparece.interpolate({ inputRange: [0, 1], outputRange: [8, 0] }) }] }]}>
        <Text style={estilos.titulo}>Lo que se envía</Text>
        <View style={estilos.envio}>
          <View style={estilos.punto} />
          <Text style={estilos.valor}>{enviado}</Text>
        </View>
        <Text style={estilos.nota}>Ya mezclado con ruido.</Text>
      </Animated.View>
      <Text style={estilos.explicacion}>{explicacion}</Text>
    </View>
  )
}

const estilos = StyleSheet.create({
  tarjeta: { backgroundColor: color.superficie, borderColor: color.borde, borderWidth: 1, borderRadius: radio.tarjeta, padding: 24, gap: 14 },
  bloque: { gap: 4 },
  titulo: { fontFamily: fuente.semi, fontSize: 16, color: color.tintaSuave },
  valor: { fontFamily: fuente.negrita, fontSize: 30, color: color.tinta },
  nota: { fontFamily: fuente.regular, fontSize: 15, color: color.tintaSuave },
  separador: { height: 1, backgroundColor: color.borde },
  envio: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  punto: { width: 22, height: 22, borderRadius: 11, backgroundColor: color.sol, borderWidth: 3, borderColor: color.tinta },
  explicacion: { fontFamily: fuente.regular, fontSize: 16, lineHeight: 24, color: color.tintaSuave },
})
