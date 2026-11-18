import { Lora_600SemiBold } from '@expo-google-fonts/lora'
import { Raleway_400Regular, Raleway_600SemiBold } from '@expo-google-fonts/raleway'
import { useFonts } from 'expo-font'
import { StatusBar } from 'expo-status-bar'
import { useCallback, useEffect, useState } from 'react'
import { View } from 'react-native'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import { api } from './src/api/cliente'
import { AyudaScreen } from './src/screens/AyudaScreen'
import { CambioClaveScreen } from './src/screens/CambioClaveScreen'
import { CheckinScreen } from './src/screens/CheckinScreen'
import { LoginScreen } from './src/screens/LoginScreen'

type Pantalla = 'iniciando' | 'login' | 'checkin' | 'ayuda' | 'clave'

export default function App() {
  const [fuentesListas] = useFonts({ Raleway_400Regular, Raleway_600SemiBold, Lora_600SemiBold })
  const [pantalla, setPantalla] = useState<Pantalla>('iniciando')

  useEffect(() => {
    api.restaurarSesion().then((ok) => setPantalla(ok ? 'checkin' : 'login'))
  }, [])

  const salir = useCallback(async () => {
    await api.cerrarSesion()
    setPantalla('login')
  }, [])

  const sesionVencida = useCallback(() => setPantalla('login'), [])

  if (!fuentesListas || pantalla === 'iniciando') return null

  return (
    <SafeAreaProvider>
      <StatusBar style="dark" />
      {pantalla === 'login' && <LoginScreen alEntrar={() => setPantalla('checkin')} />}
      {/* Kept mounted behind the help screen so a half-answered check-in is not lost. */}
      {(pantalla === 'checkin' || pantalla === 'ayuda' || pantalla === 'clave') && (
        <View style={{ flex: 1, display: pantalla === 'checkin' ? 'flex' : 'none' }}>
          <CheckinScreen alAyuda={() => setPantalla('ayuda')} alSalir={() => void salir()} alCambiarClave={() => setPantalla('clave')} alSesionVencida={sesionVencida} />
        </View>
      )}
      {pantalla === 'clave' && <CambioClaveScreen alVolver={() => setPantalla('checkin')} alTerminar={() => void salir()} />}
      {pantalla === 'ayuda' && <AyudaScreen alVolver={() => setPantalla('checkin')} />}
    </SafeAreaProvider>
  )
}
