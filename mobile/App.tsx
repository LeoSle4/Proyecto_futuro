import { AlbertSans_400Regular, AlbertSans_600SemiBold, AlbertSans_800ExtraBold, useFonts } from '@expo-google-fonts/albert-sans'
import { StatusBar } from 'expo-status-bar'
import { useCallback, useEffect, useState } from 'react'
import { View } from 'react-native'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import { api } from './src/api/cliente'
import { AyudaScreen } from './src/screens/AyudaScreen'
import { CheckinScreen } from './src/screens/CheckinScreen'
import { LoginScreen } from './src/screens/LoginScreen'

type Pantalla = 'iniciando' | 'login' | 'checkin' | 'ayuda'

export default function App() {
  const [fuentesListas] = useFonts({ AlbertSans_400Regular, AlbertSans_600SemiBold, AlbertSans_800ExtraBold })
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
      {(pantalla === 'checkin' || pantalla === 'ayuda') && (
        <View style={{ flex: 1, display: pantalla === 'checkin' ? 'flex' : 'none' }}>
          <CheckinScreen alAyuda={() => setPantalla('ayuda')} alSalir={() => void salir()} alSesionVencida={sesionVencida} />
        </View>
      )}
      {pantalla === 'ayuda' && <AyudaScreen alVolver={() => setPantalla('checkin')} />}
    </SafeAreaProvider>
  )
}
