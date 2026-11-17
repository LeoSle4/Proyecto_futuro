# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

React + TypeScript (confirmado). El panel web de RRHH se construye con Vite; la app móvil del empleado, después, con Expo (React Native). Un solo lenguaje para compartir el cliente de la API y el mecanismo de aleatorización LDP, que debe ser idéntico en los dos lados. Esta primera superficie es el panel web; la app móvil tendrá su propio contexto cuando se diseñe.

## Users

- **Primarios de esta superficie: personal de RRHH y coordinador de bienestar de una empresa cliente.** Entran con su cuenta, revisan semana a semana cómo está cada equipo, administran a los empleados de su empresa y atienden las solicitudes de ayuda (estas últimas, solo el coordinador). Trabajan con la API del backend de Kala (`backend/README.md`).
- **Administrador de la plataforma Kala (ADMIN):** da de alta empresas y personal, activa cuestionarios, fija el presupuesto de privacidad y desactiva cuentas. Es el operador, no un cliente.
- **Fuera de esta superficie, después:** el empleado, que responde desde la app móvil.
- Empresas medianas y grandes: cada grupo consultado necesita al menos 30 personas respondiendo para que haya resultado. Las empresas más chicas quedan fuera de alcance por el propio mecanismo.

## Product Purpose

Kala mide el bienestar de los equipos de una empresa sin saber qué respondió ninguna persona. Cada empleado responde en su teléfono; la respuesta se perturba allí mismo (privacidad diferencial local) antes de salir, y el servidor solo guarda sumas y hashes. RRHH ve estimaciones por equipo con su margen de error, nunca una respuesta individual, y no existe ningún puntaje individual. Éxito: que RRHH pueda actuar sobre la tendencia de un equipo y que los empleados confíen lo suficiente para responder con honestidad.

## Positioning

La privacidad no es una promesa de política, es una propiedad matemática y verificable: la respuesta se mezcla con ruido en el dispositivo, así que ni el servidor, ni RRHH, ni un administrador de base de datos pueden recuperarla. Una herramienta de encuestas "anónimas" de la competencia promete anonimato; Kala puede mostrarlo funcionando.

## Operating Context

- Ventanas semanales que empiezan el lunes (zona horaria de Lima). El pulso es semanal; las baterías WHO-5, PHQ-8 y GAD-7 son quincenales y cada envío trae solo 4 ítems elegidos al azar.
- El panel consulta **una semana por vez**. Con menos de 30 respuestas el resultado es `DATOS_INSUFICIENTES` y no se muestra ningún valor. No hay vista de tendencia de ocho semanas (decisión de producto actual).
- Las estimaciones traen un margen de error al 95 % que es grande con pocos datos (por ejemplo ±36 puntos con 30 personas y ±16 con 150). Pulso: proporción de "sí" estimada (0 a 1). Baterías: media en la escala del ítem.
- Presupuesto de privacidad: cuánto ε puede gastar una persona al mes. Lo fija el ADMIN (máximo 999.999, 20 por defecto) y no puede quedar por debajo de lo que ya gastan los cuestionarios activos.
- Sesión: JWT de 30 minutos con refresh de 30 días que se rota en cada uso.

## Capabilities and Constraints

- La API ya existe y está documentada: 40 endpoints con roles en `backend/README.md`. El panel solo la consume.
- Roles: `ADMIN`, `RRHH`, `COORDINADOR_BIENESTAR`, `EMPLEADO`. Un recurso de otra empresa responde 404; la empresa sale siempre del token.
- Solo el `COORDINADOR_BIENESTAR` ve y atiende las solicitudes de ayuda; RRHH no ve quién pidió ayuda.
- Los equipos pueden administrarse (ADMIN y RRHH escriben; el coordinador lee). Los empleados los crea RRHH o el coordinador en equipos de su empresa.
- Idioma de la interfaz: español (Perú).
- Indecisos y abiertos: nombre y marca visual de Kala más allá del nombre; logo; colores de marca. No existen todavía.

## Brand Commitments

- Nombre: **Kala**.
- Restricciones **vinculantes** confirmadas por el usuario: sin gamificación (rachas, puntos, insignias o ranking); sin tono clínico ni médico; sin rojo ni colores de alarma para resultados bajos.
- En la app móvil (después) la privacidad se muestra en acción en cada envío: la respuesta se ve mezclarse en el teléfono antes de salir. El panel debe sostener esa misma promesa (por ejemplo, explicar por qué un grupo pequeño no muestra datos).

## Evidence on Hand

- Backend funcionando, con API completa y pruebas (`backend/`), y un generador de datos de demostración sintético (`GeneradorDatosDemo`) con semilla fija.
- No hay logo, paleta, tipografía, capturas, testimonios, clientes ni cifras de uso. Todo dato visible en una maqueta debe etiquetarse como sintético; no se inventan clientes ni resultados reales.

## Product Principles

1. **Privacidad demostrable, no prometida.** Cada pantalla que muestra datos explica en una frase qué se sabe y qué no se puede saber.
2. **Honestidad sobre la incertidumbre.** Toda estimación va con su margen; un grupo sin datos suficientes dice exactamente por qué no hay resultado, en vez de mostrar una cifra falsa.
3. **Nada que vigile ni presione a una persona.** La interfaz de RRHH habla de equipos y tendencias, nunca de individuos ni de "quién falta por responder".
4. **Calma sin minimizar.** Baja carga mental y tono sereno, sin dramatizar resultados bajos ni esconderlos.
5. **La ayuda siempre a la vista y en el mismo lugar.** El acceso a "Hablar con alguien" no cambia de posición entre pantallas.

## Accessibility & Inclusion

- WCAG 2.2 nivel AA como mínimo: contraste de texto 4.5:1 (3:1 en texto grande y componentes), objetivos táctiles de al menos 24 px en web (44 px en móvil) y foco siempre visible y no tapado por barras fijas.
- Respetar "reducir movimiento"; no depender solo del color para comunicar un estado.
- Los gráficos y las estimaciones necesitan una alternativa de texto o tabla equivalente para lectores de pantalla.
- La ayuda ("Hablar con alguien") va en el mismo lugar relativo en todas las pantallas (criterio 3.2.6).
- Las personas pueden estar afectadas emocionalmente: lenguaje claro, formularios cortos y mensajes de error que no culpan.
