import type { ElementType, ReactNode } from 'react'
import './Velo.css'

interface Props {
  as?: ElementType
  className?: string
  children: ReactNode
}

// A group that cannot show a result yet is a hatched veil, not an empty hole.
export function Velo({ as: Etiqueta = 'div', className = '', children }: Props) {
  return <Etiqueta className={`velo ${className}`.trim()}>{children}</Etiqueta>
}
