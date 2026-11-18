import { Pressable, StyleSheet, Text, View } from 'react-native'
import { color, fuente } from '../theme'

// Help sits in the same top-right corner on every screen (WCAG 3.2.6 consistent help).
export function Cabecera({ alAyuda }: { alAyuda: () => void }) {
  return (
    <View style={estilos.fila}>
      <Text style={estilos.marca} accessibilityRole="header">Kala</Text>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Hablar con alguien"
        onPress={alAyuda}
        style={estilos.ayuda}
      >
        <Text style={estilos.ayudaTexto}>Hablar con alguien</Text>
      </Pressable>
    </View>
  )
}

const estilos = StyleSheet.create({
  fila: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20, paddingTop: 8, paddingBottom: 12 },
  marca: { fontFamily: fuente.negrita, fontSize: 26, color: color.mar },
  ayuda: { minHeight: 44, justifyContent: 'center', paddingHorizontal: 16, borderRadius: 14, borderWidth: 1, borderColor: color.mar },
  ayudaTexto: { fontFamily: fuente.semi, fontSize: 15, color: color.marOscuro },
})
