# Kala · Panel web de RRHH

React + TypeScript + Vite. Dirección visual "Garúa limeña": la incertidumbre se dibuja como neblina y un grupo sin resultado es un velo.

```bash
npm install
VITE_DEMO=1 npm run dev   # datos sintéticos, sin backend
npm run dev               # contra el backend en localhost:8080 (proxy /api)
npx tsc -b && npm run lint && npm run build
```

- Tokens y base: `src/index.css`. Componentes: `src/components/`. API: `src/api/cliente.ts`.
- Pantalla de referencia: `src/pages/ResumenPage.tsx`. El contrato de diseño está en `.impeccable/surfaces/`.
- Reglas: sin rojo de alarma, sin tono clínico, sin gamificación; WCAG 2.2 AA.
