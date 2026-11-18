---
name: Kala
description: Panel y app de bienestar donde la incertidumbre se dibuja como una banda simétrica y un grupo pequeño es un velo rayado, no un hueco.
colors:
  hueso: "#f6f6f1"
  hueso-oscuro: "#ecece5"
  tinta: "#0f172a"
  tinta-suave: "#475569"
  tinta-tenue: "#566272"
  linea: "#dcddd4"
  turquesa: "#0891b2"
  mar: "#0b5f73"
  noche: "#03222c"
  bruma: "#cfe6ec"
  salvia: "#e7e9e2"
  error: "#8a3b12"
typography:
  display:
    fontFamily: "Lora, Georgia, serif"
    fontSize: "6rem"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "-0.04em"
  headline:
    fontFamily: "Lora, Georgia, serif"
    fontSize: "3.25rem"
    fontWeight: 600
    lineHeight: 1.05
    letterSpacing: "-0.025em"
  title:
    fontFamily: "Lora, Georgia, serif"
    fontSize: "1.125rem"
    fontWeight: 600
    lineHeight: 1.2
  body:
    fontFamily: "Raleway, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "Raleway, system-ui, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 600
    lineHeight: 1.4
rounded:
  s: "10px"
  m: "18px"
  l: "26px"
spacing:
  s: "8px"
  m: "18px"
  l: "40px"
components:
  button-primary:
    backgroundColor: "{colors.mar}"
    textColor: "#ffffff"
    rounded: "{rounded.s}"
    height: "56px"
    padding: "0 20px"
  button-outline:
    backgroundColor: "#ffffff"
    textColor: "{colors.mar}"
    rounded: "{rounded.s}"
    height: "44px"
    padding: "0 20px"
  card-panel:
    backgroundColor: "#ffffff"
    textColor: "{colors.tinta}"
    rounded: "{rounded.l}"
    padding: "34px 40px"
  card-indicator:
    backgroundColor: "#ffffff"
    textColor: "{colors.tinta}"
    rounded: "{rounded.m}"
    padding: "26px 28px"
---

# Design System: Kala

## Overview

**Creative North Star: "El Cuaderno de Campo"**

Kala se lee como una hoja de campo bien hecha: fondo hueso plano, una tipografía serif con carácter para lo que importa, un solo verde profundo como voz y líneas finas en lugar de cajas. La incertidumbre nunca se esconde: cada estimación se dibuja con su banda, y un grupo que no llega al mínimo es un velo rayado que explica por qué no hay cifra.

La interfaz explica lo que hace. En el login, un diagrama de puntos muestra cómo la respuesta de cada persona se mezcla con ruido antes de salir; en el panel, un medidor y una simulación enseñan cómo el margen baja al sumar respuestas. El tono es calmo y cálido, nunca clínico.

**Key Characteristics:**
- Fondo hueso plano y casi sin sombras; la profundidad viene de líneas de 1,5px y de un levantamiento de 2px al pasar el mouse.
- Lora para títulos y cifras grandes, Raleway para texto; cifras siempre alineadas (lnum, tnum).
- Un solo medidor para todos los indicadores: eje, banda simétrica, corchetes, estimación.
- El movimiento cuenta algo (puntos que cambian, odómetro, banda que se abre), nunca decora.

## Colors

Un hueso cálido de base, tinta casi negra y un verde mar profundo como única voz.

### Primary
- **Verde Mar** (#0b5f73): cifras clave, equipo activo, botón primario, enlaces. Contraste ≥ 6,6:1 sobre hueso.
- **Turquesa** (#0891b2): bandas de incertidumbre, foco y brillos; nunca texto pequeño.

### Neutral
- **Hueso** (#f6f6f1) y **Hueso oscuro** (#ecece5): fondo y tarjetas de equipo.
- **Tinta** (#0f172a), **Tinta suave** (#475569), **Tinta tenue** (#566272): texto principal, secundario y notas (≥ 4,5:1).
- **Línea** (#dcddd4): bordes y separadores de 1,5px.
- **Noche** (#03222c) y **Bruma** (#cfe6ec): el campo de marca del login y su texto.
- **Marrón quemado** (#8a3b12): solo errores de formulario; nunca resultados bajos.

### Named Rules
**The One Voice Rule.** El verde mar es la única voz de color; el turquesa solo aparece en lo que mide la incertidumbre.
**The No Alarm Rule.** Ningún resultado bajo, incompleto o ausente se pinta de rojo ni de color de alarma.

## Typography

**Display, Headline y Title:** Lora (serif). **Body y Label:** Raleway.

**Character:** una serif cálida para lo importante y una sans geométrica y abierta para leer.

### Hierarchy
- **Display** (600, 6rem, 1): la cifra de la semana, con odómetro.
- **Headline** (600, 3.25rem, 1.05): nombre del equipo y títulos de pantalla.
- **Title** (600, 1.125rem): nombres de equipo y de indicador.
- **Body** (400, 1rem, 1.5): texto, con medida máxima de 60ch.
- **Label** (600, 0.8125rem): ejes, notas y rótulos.

### Named Rules
**The Lining Figures Rule.** Toda cifra usa numerales alineados y de ancho fijo. En CSS no se usa el atajo `font:`, porque borra esa petición y el 4 baja de la línea.

## Layout

Cabecera horizontal con marca, pestañas subrayadas y cuenta; la ayuda es siempre el último elemento, en la misma esquina. Debajo, columna lateral de 300px (equipos y ficha de la semana) y centro fluido hasta 1440px. El login reparte dos columnas sobre tres filas compartidas (arriba, medio, abajo), así logo y Ayuda, titular y formulario, datos y nota quedan alineados. Bajo 900–1000px todo se apila. Objetivos táctiles de al menos 44px.

## Elevation & Depth

Plano por defecto. Las tarjetas se separan por borde y no por sombra; solo al pasar el mouse suben 2px con borde más oscuro y una sombra suave (blur 24px, 8% de tinta). El campo de marca del login usa un resplandor en una esquina de cinco pasos y una capa de grano estático por debajo del texto.

### Named Rules
**The Flat-By-Default Rule.** Sin sombras en reposo; la profundidad se gana al interactuar.

## Shapes

Tres radios con función: 10px en botones y campos, 18px en tarjetas, 26px en la tarjeta principal. Las bandas son cápsulas completas. El velo es un rayado diagonal a 135° con borde discontinuo.

## Components

### Buttons
- **Primary:** verde mar, 56px; sube 1px al pasar el mouse y se hunde 1px al pulsar. **Outline:** borde turquesa para Ayuda.

### Medidor (signature component)
Eje con marcas cada paso, una banda simétrica centrada en el intervalo al 95 %, un halo más ancho, corchetes con sus valores y el punto de la estimación. Se abre desde el centro al cargar; al pasar el mouse o enfocarlo, explica que el valor real puede estar en cualquier punto de la banda. En la simulación, la banda se desliza a su nuevo ancho.

### Odómetro
Cada dígito rueda en una cinta 0–9 triplicada: la unidad da dos vueltas y la decena una (1,2 s, sin rebote).

### Flujo de ruido
Diagrama del login: puntos (llenos = sí, vacíos = no) que viajan, se frenan en la puerta del ruido y a 1 de cada 4 se les cambia el valor, con un anillo que se desvanece. Debajo, los valores esperados: el real, el que se observa en promedio y el que recupera el panel.

### Selector de equipos
Lista con un único indicador verde que se desliza al equipo elegido (solo transform). Los equipos sin resultado van rayados.

### Check-in móvil
Una pregunta por pantalla, botones de 56px y un momento de privacidad antes de enviar: "Tu respuesta" (se queda en el teléfono) y "Lo que se envía" (ya con ruido). La ayuda va siempre arriba a la derecha.

## Do's and Don'ts

### Do:
- **Do** mostrar siempre el margen junto a cada estimación, con el mismo medidor.
- **Do** rayar y explicar un grupo sin resultado; nunca dejar un hueco.
- **Do** decir lo que no se sabe: no inventar un conteo que el servidor no envía.
- **Do** respetar "reducir movimiento": todo queda en su estado final y quieto.

### Don't:
- **Don't** usar rojo ni colores de alarma para resultados bajos.
- **Don't** usar tono clínico, gamificación ni celebraciones con confeti, racha o puntos.
- **Don't** mostrar nunca información de una persona individual.
- **Don't** animar con el scroll ni dar movimiento sin propósito.
