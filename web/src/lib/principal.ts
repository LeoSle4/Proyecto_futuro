import type { ItemDashboard } from '../api/tipos'

// The headline panel draws a yes/no share, so only an indicator on a 0-to-1 scale can be the headline. Without scale
// information (older servers) an item without a mean keeps its old role.
function esSiNo(item: ItemDashboard): boolean {
  if (item.valorMin != null && item.valorMax != null) return item.valorMax - item.valorMin === 1
  return item.mediaEstimada === null
}

export function separarPrincipal(items: ItemDashboard[]): { principal: ItemDashboard | null; resto: ItemDashboard[] } {
  const indice = items.findIndex(esSiNo)
  if (indice < 0) return { principal: null, resto: items }
  return { principal: items[indice], resto: items.filter((_, i) => i !== indice) }
}
