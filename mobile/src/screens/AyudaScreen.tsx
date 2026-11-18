import { useEffect, useState } from 'react'
import { ActivityIndicator, Linking, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native'
import { api } from '../api/cliente'
import type { Contacto, Solicitud } from '../api/tipos'
import { Boton } from '../components/Boton'
import { Fondo } from '../components/Fondo'
import { color, fuente, radio } from '../theme'

export function AyudaScreen({ alVolver }: { alVolver: () => void }) {
  const [contactos, setContactos] = useState<Contacto[] | null>(null)
  const [fallo, setFallo] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [errorSolicitud, setErrorSolicitud] = useState<string | null>(null)
  const [solicitudes, setSolicitudes] = useState<Solicitud[]>([])
  const [recienPedida, setRecienPedida] = useState(false)

  useEffect(() => {
    api.contactos().then(setContactos, () => setFallo(true))
    api.misSolicitudes().then(setSolicitudes, () => undefined)
  }, [])

  async function pedir() {
    setErrorSolicitud(null)
    setEnviando(true)
    try {
      const creada = await api.pedirAyuda(mensaje)
      setSolicitudes((previas) => [creada, ...previas])
      setMensaje('')
      setRecienPedida(true)
    } catch (e) {
      setErrorSolicitud(e instanceof Error ? e.message : 'No pudimos enviar tu solicitud.')
    } finally {
      setEnviando(false)
    }
  }

  const abierta = solicitudes.find((s) => s.estado !== 'CERRADA')

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
        <View style={estilos.tarjeta}>
          <Text style={estilos.nombre} accessibilityRole="header">Pedir que me escriban</Text>
          <Text style={estilos.texto}>
            A diferencia de tus respuestas del check-in, este mensaje sí llega con tu nombre, solo a la coordinación de bienestar. RRHH no lo ve.
          </Text>
          {abierta ? (
            <Text style={estilos.estado} accessibilityLiveRegion="polite">
              {abierta.estado === 'EN_ATENCION' ? 'Tu solicitud está siendo atendida.' : 'Tu solicitud llegó y está esperando.'}
            </Text>
          ) : (
            <>
              <TextInput
                style={estilos.campo} value={mensaje} onChangeText={setMensaje} multiline maxLength={2000}
                placeholder="Si quieres, cuéntanos algo (opcional)" placeholderTextColor={color.tintaTenue}
                accessibilityLabel="Mensaje opcional para la coordinación de bienestar"
              />
              {errorSolicitud && <Text style={estilos.error} accessibilityRole="alert">{errorSolicitud}</Text>}
              <Boton texto={enviando ? 'Enviando…' : 'Pedir que me escriban'} alPulsar={() => void pedir()} deshabilitado={enviando} />
            </>
          )}
          {recienPedida && <Text style={estilos.estado} accessibilityLiveRegion="polite">Listo. Alguien de bienestar lo verá y se pondrá en contacto contigo.</Text>}
        </View>
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
  campo: { minHeight: 110, borderRadius: radio.boton, borderWidth: 1, borderColor: color.tintaTenue, backgroundColor: color.blanco, padding: 14, fontFamily: fuente.regular, fontSize: 17, color: color.tinta, textAlignVertical: 'top' },
  estado: { fontFamily: fuente.semi, fontSize: 17, lineHeight: 24, color: color.marOscuro },
  error: { fontFamily: fuente.semi, fontSize: 16, color: color.error },
})
