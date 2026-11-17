import './Pendiente.css'

// Placeholder for screens that teammates build on top of this base. Each links to its GitHub issue.
export function Pendiente({ titulo, descripcion }: { titulo: string; descripcion: string }) {
  return (
    <section className="pendiente">
      <h1>{titulo}</h1>
      <p>{descripcion}</p>
      <p className="pendiente-nota">Esta pantalla todavía no está construida. Usa los componentes y tokens de la base visual.</p>
    </section>
  )
}
