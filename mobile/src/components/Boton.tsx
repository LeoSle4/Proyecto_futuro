import { Pressable, StyleSheet, Text } from 'react-native'
import { alturaTactil, color, fuente, radio } from '../theme'

interface Props {
  texto: string
  alPulsar: () => void
  variante?: 'primario' | 'secundario'
  deshabilitado?: boolean
  etiquetaAccesible?: string
}

export function Boton({ texto, alPulsar, variante = 'primario', deshabilitado = false, etiquetaAccesible }: Props) {
  const primario = variante === 'primario'
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={etiquetaAccesible ?? texto}
      accessibilityState={{ disabled: deshabilitado }}
      disabled={deshabilitado}
      onPress={alPulsar}
      style={({ pressed }) => [
        estilos.base,
        primario ? estilos.primario : estilos.secundario,
        pressed && (primario ? estilos.primarioPulsado : estilos.secundarioPulsado),
        deshabilitado && estilos.apagado,
      ]}
    >
      <Text style={[estilos.texto, { color: primario ? color.blanco : color.tinta }]}>{texto}</Text>
    </Pressable>
  )
}

const estilos = StyleSheet.create({
  base: { minHeight: alturaTactil, borderRadius: radio.boton, paddingHorizontal: 20, paddingVertical: 12, justifyContent: 'center', borderWidth: 1 },
  primario: { backgroundColor: color.mar, borderColor: color.mar },
  primarioPulsado: { backgroundColor: color.marOscuro },
  secundario: { backgroundColor: color.blanco, borderColor: color.borde },
  secundarioPulsado: { borderColor: color.mar },
  apagado: { opacity: 0.55 },
  texto: { fontFamily: fuente.semi, fontSize: 18, textAlign: 'center' },
})
