export interface ErroresEquipo {
  nombre?: string
}

export function validarEquipo(nombre: string): ErroresEquipo {
  const limpio = nombre.trim()
  if (!limpio) return { nombre: 'Escribe el nombre del equipo.' }
  if (limpio.length > 150) return { nombre: 'El nombre no puede tener más de 150 caracteres.' }
  return {}
}
