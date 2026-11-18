# Kala

Bienestar corporativo con **privacidad diferencial local**. Cada persona responde desde su teléfono y la respuesta se
mezcla con ruido **antes de salir**; el servidor solo guarda sumas. RRHH ve estimaciones de un equipo con su margen de
error, nunca lo que respondió una persona, y un grupo con menos de 30 respuestas no muestra resultado.

| Carpeta | Qué es | Stack |
|---|---|---|
| [`backend/`](backend/README.md) | API, reglas de privacidad y base de datos | Java 25, Spring Boot 4, PostgreSQL (Neon), Flyway |
| [`web/`](web/README.md) | Panel de RRHH, coordinación y administración | React, TypeScript, Vite |
| [`mobile/`](mobile/README.md) | App del empleado: check-in y ayuda | Expo, React Native, TypeScript |
| `PRODUCT.md`, `DESIGN.md` | Para quién es Kala y su sistema visual | |

## Cómo funciona la privacidad

1. La app muestra una pregunta y la persona responde. Esa respuesta **no sale** del teléfono tal cual.
2. En el teléfono se aplica ruido: en una pregunta de sí o no, a 1 de cada 4 respuestas se le cambia el valor
   (Randomized Response); en una escala, se suma ruido Laplace.
3. Solo viaja la respuesta ya perturbada. El servidor suma, no guarda quién respondió qué.
4. El panel corrige el ruido con estadística y muestra una **estimación con su margen**. Con pocas respuestas el margen
   es grande, y por eso un grupo con menos de 30 respuestas no se publica.
5. Pedir ayuda es otro canal: es voluntario, lo ve solo la coordinación de bienestar y no se cruza con el check-in.

Los detalles (mecanismos, presupuesto de privacidad, retención) están en [`backend/README.md`](backend/README.md).

## Probarlo en cinco minutos, sin backend

No necesitas base de datos: los dos clientes traen datos sintéticos claramente etiquetados.

```bash
cd web && npm install && VITE_DEMO=1 npm run dev
cd mobile && npm install && EXPO_PUBLIC_DEMO=1 npx expo start
```

En el panel, `VITE_DEMO_ROL=ADMIN` o `VITE_DEMO_ROL=COORDINADOR_BIENESTAR` muestran las otras vistas. En la app, escanea el
QR con Expo Go.

## Probarlo contra el backend real

1. Arranca el backend (puerto **8090**) con sus variables de entorno: ver [`backend/README.md`](backend/README.md#arrancar).
2. Panel: `cd web && npm run dev` (el proxy apunta a `localhost:8090`; `KALA_API_URL` lo cambia).
3. App en un teléfono real: `localhost` es el propio teléfono. Copia `mobile/.env.example` a `mobile/.env` y pon la IP de
   tu computadora, por ejemplo `EXPO_PUBLIC_API_URL=http://192.168.0.10:8090`.

Quién entra a qué: la **administración** crea empresas y personal y activa cuestionarios; **RRHH** y la **coordinación de
bienestar** ven el panel; solo la coordinación ve las solicitudes de ayuda; el **empleado** usa la app.

## Pruebas

```bash
cd backend && ./mvnw test        # las pruebas con base de datos se omiten sin KALA_TEST_DB_URL
cd web && npm test               # lógica pura y cliente de la API
cd mobile && npx jest            # el ruido en el teléfono y el cliente de la API
```

## Lo que todavía no está resuelto

- La app móvil no se ha probado en un teléfono real contra el backend real.
- Una solicitud de ayuda llega al panel con el número de la persona, no con su nombre: falta decidir qué dato puede ver la
  coordinación para poder escribirle.
- El límite de intentos de login vive en memoria, así que vale para una sola instancia del servidor.

## Cómo se trabaja

- Commits pequeños con Conventional Commits, en español; el código y los identificadores, en inglés.
- Cada cambio entra por un pull request contra `main`. Quien publica corre un script de `.pr/` (local, no versionado).
