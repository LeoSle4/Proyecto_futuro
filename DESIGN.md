---
name: Kala
description: Panel de bienestar donde la incertidumbre se dibuja como neblina y un grupo pequeño es un velo, no un hueco.
colors:
  niebla-alta: "#f0f3f5"
  niebla-baja: "#dde5ea"
  tinta: "#25343f"
  tinta-suave: "#4a5c68"
  tinta-tenue: "#566873"
  borde: "#cdd8de"
  mar: "#1d6a73"
  mar-oscuro: "#14535b"
  sol: "#f0b44c"
  superficie-solida: "#ffffff"
  velo-claro: "#e9eff2"
  error: "#8a3b12"
typography:
  display:
    fontFamily: "Albert Sans, system-ui, sans-serif"
    fontSize: "4.5rem"
    fontWeight: 800
    lineHeight: 1.1
  headline:
    fontFamily: "Albert Sans, system-ui, sans-serif"
    fontSize: "2.25rem"
    fontWeight: 800
    lineHeight: 1.15
  title:
    fontFamily: "Albert Sans, system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 600
    lineHeight: 1.15
  body:
    fontFamily: "Albert Sans, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "Albert Sans, system-ui, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.5
rounded:
  sm: "12px"
  md: "16px"
  lg: "24px"
  pill: "999px"
spacing:
  sm: "8px"
  md: "18px"
  lg: "32px"
components:
  button-primary:
    backgroundColor: "{colors.mar}"
    textColor: "{colors.superficie-solida}"
    rounded: "{rounded.sm}"
    height: "44px"
    padding: "0 20px"
  button-primary-hover:
    backgroundColor: "{colors.mar-oscuro}"
  button-secondary:
    backgroundColor: "{colors.superficie-solida}"
    textColor: "{colors.tinta}"
    rounded: "{rounded.sm}"
    height: "44px"
    padding: "0 20px"
  card-indicador:
    backgroundColor: "{colors.superficie-solida}"
    textColor: "{colors.tinta}"
    rounded: "{rounded.lg}"
    padding: "26px 32px"
  card-equipo-activo:
    backgroundColor: "{colors.mar}"
    textColor: "{colors.superficie-solida}"
    rounded: "{rounded.md}"
    padding: "16px 18px"
---

# Design System: Kala

## Overview

**Creative North Star: "La Garúa"**

Kala se lee como la luz difusa de Lima: niebla perlada, un solo verde mar y un único destello de sol. La incertidumbre no se esconde, se dibuja: cada estimación lleva su neblina alrededor y un grupo con menos de 30 respuestas es un velo rayado que explica por qué no hay cifra. El tono es calmo y cálido, nunca clínico.

Es una herramienta de trabajo que se abre una vez por semana, así que prima la lectura tranquila sobre la expresión: una sola idea por tarjeta, cifras grandes y su margen siempre a la vista.

**Key Characteristics:**
- Fondo de niebla fría en degradado vertical, nunca crema.
- Superficies casi blancas con borde de 1px y esquinas generosas.
- El sol aparece solo como el punto de la estimación.
- Un velo rayado marca todo lo que todavía no se puede afirmar.

## Colors

Paleta de niebla fría con un verde mar como única voz y un acento cálido reservado.

### Primary
- **Verde Mar** (#1d6a73): cifras, equipo activo, botón primario, enlaces. Contraste ≥ 4.5:1 sobre blanco.
- **Verde Mar Profundo** (#14535b): hover del botón y enlaces.

### Secondary
- **Sol de Garúa** (#f0b44c): únicamente el punto de la estimación y la etiqueta "Datos sintéticos".

### Neutral
- **Niebla Alta / Baja** (#f0f3f5 → #dde5ea): fondo en degradado vertical.
- **Tinta** (#25343f), **Tinta Suave** (#4a5c68), **Tinta Tenue** (#566873): texto principal, secundario y notas.
- **Borde de Bruma** (#cdd8de): bordes de 1px de superficies.
- **Marrón Quemado** (#8a3b12): solo errores de formulario; nunca para resultados bajos.

### Named Rules
**The One Sun Rule.** El amarillo sol marca la estimación y nada más. Su rareza es el punto.
**The No Alarm Rule.** Ningún resultado bajo, incompleto o ausente se pinta de rojo ni de color de alarma.

## Typography

**Display, Headline y Body:** Albert Sans (con system-ui como respaldo).

**Character:** humanista y abierta, de peso 800 en cifras y títulos y 400 en texto corrido, con numerales tabulares.

### Hierarchy
- **Display** (800, 4.5rem, 1.1): la cifra principal de una semana.
- **Headline** (800, 2.25rem, 1.15): nombre de equipo y de pantalla.
- **Title** (600, 1.125rem): nombre de un indicador.
- **Body** (400, 1rem, 1.5): texto, con medida máxima de 65ch.
- **Label** (400, 0.875rem): ejes, notas y etiquetas.

### Named Rules
**The Tabular Rule.** Toda cifra usa numerales tabulares para que las semanas se comparen sin trampas.

## Layout

Cabecera horizontal con marca, navegación y cuenta; la ayuda es siempre el último elemento, en la misma esquina. Debajo, columna lateral de 300px con los equipos y centro fluido (máximo 1280px). Bajo 900px se apila y los equipos pasan a una rejilla. Espaciado en pasos de 8, 18 y 32px. Todo objetivo interactivo mide al menos 44px.

## Elevation & Depth

Casi plano. La profundidad viene de superficies blancas translúcidas sobre la niebla y bordes de 1px. La única sombra es la del punto solar (`0 3px 8px rgb(37 52 63 / 0.25)`). Con "reducir transparencia" las superficies pasan a blanco sólido y la bruma pierde el desenfoque.

### Named Rules
**The Flat-By-Default Rule.** Las tarjetas no llevan sombra; solo el punto de la estimación se despega.

## Shapes

Esquinas generosas: 24px en tarjetas de contenido, 16px en tarjetas de equipo, 12px en botones y campos, y píldora en etiquetas. La neblina es una franja redondeada y difuminada; el velo, un rayado diagonal a 135° de 10px.

## Components

### Buttons
- **Shape:** esquina de 12px, 44px de alto.
- **Primary:** verde mar con texto blanco; hover a verde mar profundo.
- **Secondary:** blanco con borde de bruma; el borde pasa a verde mar al hover.
- **Ayuda:** borde verde mar, siempre en la misma esquina de la cabecera.

### Cards / Containers
- **Indicador:** blanco translúcido, borde de 1px, 24px de esquina, relleno 26×32px; la cifra, la neblina y una nota de una línea.
- **Velo:** mismo contenedor con rayado diagonal; dice "Sin resultado" y cuántas respuestas faltan.

### Inputs / Fields
- Alto 48px, borde de 1px en tinta tenue, foco con anillo verde mar de 3px, error con borde marrón de 2px y mensaje con `role="alert"`.

### Navigation
- Enlaces de 44px; el activo es una píldora blanca con borde y texto verde mar profundo.

### Neblina (signature component)
Un riel tenue de escala completa, una bruma verde mar difuminada que cubre el intervalo al 95 %, un hilo central y el punto solar con aro de tinta. Debajo, el rango escrito: "puede estar entre X y Y".

## Do's and Don'ts

### Do:
- **Do** mostrar siempre el margen de cada estimación junto a la cifra.
- **Do** usar el velo rayado y explicar por qué no hay resultado, con el conteo "12 de 30".
- **Do** dejar cada estado con texto además del color.
- **Do** respetar "reducir movimiento" y "reducir transparencia".

### Don't:
- **Don't** usar rojo ni colores de alarma para resultados bajos.
- **Don't** usar tono clínico o médico ni gamificación (puntos, rachas, insignias).
- **Don't** mostrar nunca información de una persona individual.
- **Don't** dar un hueco vacío a un grupo sin resultado.
