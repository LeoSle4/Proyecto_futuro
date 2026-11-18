import { describe, expect, it } from 'vitest'
import { buscarPorPrefijo, moverIndice } from './selector'

describe('moverIndice', () => {
  it('baja y sube de uno en uno', () => {
    expect(moverIndice(0, 'ArrowDown', 4)).toBe(1)
    expect(moverIndice(2, 'ArrowUp', 4)).toBe(1)
  })
  it('se queda en los extremos en lugar de dar la vuelta', () => {
    expect(moverIndice(3, 'ArrowDown', 4)).toBe(3)
    expect(moverIndice(0, 'ArrowUp', 4)).toBe(0)
  })
  it('Inicio y Fin saltan al primero y al último', () => {
    expect(moverIndice(2, 'Home', 5)).toBe(0)
    expect(moverIndice(1, 'End', 5)).toBe(4)
  })
  it('con la lista vacía devuelve -1 y con una tecla desconocida no mueve', () => {
    expect(moverIndice(0, 'ArrowDown', 0)).toBe(-1)
    expect(moverIndice(2, 'x', 5)).toBe(2)
  })
  it('si no había nada activo, bajar elige el primero y subir el último', () => {
    expect(moverIndice(-1, 'ArrowDown', 3)).toBe(0)
    expect(moverIndice(-1, 'ArrowUp', 3)).toBe(2)
  })
})

describe('buscarPorPrefijo', () => {
  const opciones = [{ valor: '1', texto: 'Operaciones Lima' }, { valor: '2', texto: 'Ventas' }, { valor: '3', texto: 'Administración' }, { valor: '4', texto: 'Soporte' }, { valor: '5', texto: 'Seguridad' }]
  it('encuentra la primera opción que empieza con lo escrito, sin distinguir mayúsculas ni tildes', () => {
    expect(buscarPorPrefijo(opciones, 'ad', 0)).toBe(2)
    expect(buscarPorPrefijo(opciones, 'ADMINISTRACION', 0)).toBe(2)
  })
  it('al repetir la misma letra recorre las opciones que empiezan con ella', () => {
    expect(buscarPorPrefijo(opciones, 's', 3)).toBe(4)
    expect(buscarPorPrefijo(opciones, 's', 4)).toBe(3)
  })
  it('devuelve -1 si nada coincide', () => {
    expect(buscarPorPrefijo(opciones, 'zzz', 0)).toBe(-1)
  })
})
