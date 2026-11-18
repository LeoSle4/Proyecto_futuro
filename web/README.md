# Kala · Panel web de RRHH

React + TypeScript + Vite. Dirección visual "Orgánico": fondo hueso plano, Lora y Raleway, un solo verde profundo; la incertidumbre se dibuja como una banda simétrica y un grupo sin resultado es un velo rayado.

```bash
npm install
VITE_DEMO=1 npm run dev   # datos sintéticos, sin backend (VITE_DEMO_ROL=ADMIN para ver la vista de administración)
npm run dev               # contra el backend en localhost:8090 (proxy /api; KALA_API_URL lo cambia)
npx tsc -b && npm run lint && npm test && npm run build
```

- Tokens y base: `src/index.css`. Componentes: `src/components/`. API: `src/api/cliente.ts`.
- Pantalla de referencia: `src/pages/ResumenPage.tsx`. El contrato de diseño está en `.impeccable/surfaces/`.
- Reglas: sin rojo de alarma, sin tono clínico, sin gamificación; WCAG 2.2 AA.
