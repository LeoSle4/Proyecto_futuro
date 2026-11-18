import { useEffect, useState } from 'react'
import { ActivityIndicator, Linking, ScrollView, StyleSheet, Text, View } from 'react-native'
import { api } from '../api/cliente'
import type { Contacto } from '../api/tipos'
import { Boton } from '../components/Boton'
import { Fondo } from '../components/Fondo'
import { color, fuente, radio } from '../theme'

export function AyudaScreen({ alVolver }: { alVolver: () => void }) {
  const [contactos, setContactos] = useState<Contacto[] | null>(null)
  const [fallo, setFallo] = useState(false)

  useEffect(() => {
    api.contactos().then(setContactos, () => setFallo(true))
  }, [])

  return (
    <Fondo>
      <ScrollView contentContainerStyle={estilos.cuerpo}>
        <Text style={estilos.titulo} accessibilityRole="header">Hablar con alguien</Text>
        <Text style={estilos.texto}>Estas personas de tu empresa pueden escucharte. Puedes escribirles o llamarles cuando quieras.</Text>
        {!contactos && !fallo && <ActivityIndicator color={color.mar} />}
        {fallo && <Text style={estilos.texto}>No pudimos cargar los contactos. Inténtalo de nuevo en un momento.</Text>}
        {contactos?.length === 0 && <Text style={estilos.texto}>Tu empresa todavía no agregó contactos. Puedes hablar con tu coordinación de bienestar.</Text>}
        {contactos?.map((c) => (
          <View key={c.id} style={estilos.tarjeta}>
            <Text style={estilos.nombre}>{c.nombre}</Text>
            {c.telefono ? <Boton texto={`Llamar al ${c.telefono}`} alPulsar={() => void Linking.openURL(`tel:${c.telefono}`)} /> : null}
            {c.email ? <Boton texto={`Escribir a ${c.email}`} variante="secundario" alPulsar={() => void Linking.openURL(`mailto:${c.email}`)} /> : null}
          </View>
        ))}
        <Boton texto="Volver" variante="secundario" alPulsar={alVolver} />
      </ScrollView>
    </Fondo>
  )
}

const estilos = StyleSheet.create({
  cuerpo: { padding: 20, gap: 14 },
  titulo: { fontFamily: fuente.negrita, fontSize: 30, color: color.tinta, marginTop: 8 },
  texto: { fontFamily: fuente.regular, fontSize: 18, lineHeight: 26, color: color.tintaSuave },
  tarjeta: { backgroundColor: color.superficie, borderColor: color.borde, borderWidth: 1, borderRadius: radio.tarjeta, padding: 20, gap: 12 },
  nombre: { fontFamily: fuente.negrita, fontSize: 20, color: color.tinta },
})
