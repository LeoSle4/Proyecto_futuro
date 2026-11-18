import type { ReactNode } from 'react'
import { StyleSheet, View } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { color } from '../theme'

// A flat bone ground: no gradient, so the content carries the screen.
export function Fondo({ children }: { children: ReactNode }) {
  return (
    <View style={estilos.fondo}>
      <SafeAreaView style={estilos.seguro}>{children}</SafeAreaView>
    </View>
  )
}

const estilos = StyleSheet.create({ fondo: { flex: 1, backgroundColor: color.nieblaAlta }, seguro: { flex: 1 } })
