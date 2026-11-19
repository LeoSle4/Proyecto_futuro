// "1 pregunta", "7 preguntas": the noun agrees with the number.
export function cantidad(n: number, singular: string, plural: string): string {
  return `${n} ${n === 1 ? singular : plural}`
}
