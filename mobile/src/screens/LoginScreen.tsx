import { useState } from 'react'
import { KeyboardAvoidingView, Platform, StyleSheet, Text, TextInput, View } from 'react-native'
import { api, ErrorApi } from '../api/cliente'
import { Boton } from '../components/Boton'
import { Fondo } from '../components/Fondo'
import { color, fuente, radio } from '../theme'

export function LoginScreen({ alEntrar }: { alEntrar: () => void }) {
  const [usuario, setUsuario] = useState('')
  const [clave, setClave] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar() {
    setError(null)
    setEnviando(true)
    try {
      await api.iniciarSesion(usuario.trim(), clave)
      alEntrar()
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 401) setError('El correo o la contraseña no coinciden. Revísalos e inténtalo de nuevo.')
      else setError(e instanceof Error ? e.message : 'No pudimos iniciar sesión.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Fondo>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={estilos.contenedor}>
        <Text style={estilos.marca} accessibilityRole="header">Kala</Text>
        <Text style={estilos.titulo}>Entra para responder</Text>
        <Text style={estilos.texto}>Lo que respondas sale de tu teléfono ya mezclado con ruido. Nadie verá tu respuesta.</Text>
        <View style={estilos.formulario}>
          <Text style={estilos.etiqueta}>Correo</Text>
          <TextInput
            style={[estilos.campo, error ? estilos.campoError : null]} value={usuario} onChangeText={setUsuario}
            autoCapitalize="none" autoComplete="username" keyboardType="email-address" textContentType="username"
            accessibilityLabel="Correo"
          />
          <Text style={estilos.etiqueta}>Contraseña</Text>
          <TextInput
            style={[estilos.campo, error ? estilos.campoError : null]} value={clave} onChangeText={setClave}
            secureTextEntry autoComplete="current-password" textContentType="password" accessibilityLabel="Contraseña"
          />
          {error && <Text style={estilos.error} accessibilityRole="alert">{error}</Text>}
          <Boton texto={enviando ? 'Entrando…' : 'Entrar'} alPulsar={() => void enviar()} deshabilitado={enviando || !usuario || !clave} />
        </View>
      </KeyboardAvoidingView>
    </Fondo>
  )
}

const estilos = StyleSheet.create({
  contenedor: { flex: 1, padding: 24, justifyContent: 'center', gap: 8 },
  marca: { fontFamily: fuente.negrita, fontSize: 34, color: color.mar },
  titulo: { fontFamily: fuente.negrita, fontSize: 28, color: color.tinta, marginTop: 8 },
  texto: { fontFamily: fuente.regular, fontSize: 17, lineHeight: 25, color: color.tintaSuave, marginBottom: 16 },
  formulario: { gap: 8 },
  etiqueta: { fontFamily: fuente.semi, fontSize: 16, color: color.tinta },
  campo: { minHeight: 52, borderRadius: radio.boton, borderWidth: 1, borderColor: color.tintaTenue, backgroundColor: color.blanco, paddingHorizontal: 14, fontFamily: fuente.regular, fontSize: 18, color: color.tinta },
  campoError: { borderColor: color.error, borderWidth: 2 },
  error: { fontFamily: fuente.semi, fontSize: 16, color: color.error },
})
