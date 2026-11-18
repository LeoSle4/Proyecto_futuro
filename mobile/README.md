# Kala · App del empleado

Expo + React Native + TypeScript. Una pregunta por pantalla y la privacidad a la vista: la respuesta real se queda en el teléfono y lo que sale ya va mezclado con ruido (Randomized Response y Laplace, igual que el servidor).

```bash
npm install
EXPO_PUBLIC_DEMO=1 npx expo start      # datos sintéticos, sin backend (a = Android, i = iOS, w = web)
cp .env.example .env && npx expo start # contra el backend
npx jest && npx tsc --noEmit
```

- `src/domain/ldp.ts`: el ruido, puro y con pruebas. El azar sale de `expo-crypto` (`src/domain/azar.ts`), nunca de `Math.random`.
- El token de acceso vive en memoria; el refresh token, en `expo-secure-store`.
- Reglas: sin gamificación, sin tono clínico, sin rojo de alarma; la ayuda siempre en la misma esquina.
