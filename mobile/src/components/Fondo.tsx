import { LinearGradient } from 'expo-linear-gradient'
import type { ReactNode } from 'react'
import { StyleSheet } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { color } from '../theme'

export function Fondo({ children }: { children: ReactNode }) {
  return (
    <LinearGradient colors={[color.nieblaAlta, color.nieblaBaja]} style={estilos.fondo}>
      <SafeAreaView style={estilos.seguro}>{children}</SafeAreaView>
    </LinearGradient>
  )
}

const estilos = StyleSheet.create({ fondo: { flex: 1 }, seguro: { flex: 1 } })
