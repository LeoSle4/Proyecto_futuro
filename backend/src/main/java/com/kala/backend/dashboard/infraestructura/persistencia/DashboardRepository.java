package com.kala.backend.dashboard.infraestructura.persistencia;

import com.kala.backend.dashboard.aplicacion.puerto.LecturaDashboard.FilaItem;
import com.kala.backend.dashboard.aplicacion.puerto.LecturaDashboard;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// Lee los agregados de varios módulos con SQL directo, sin importar sus clases.
@Repository
public class DashboardRepository implements LecturaDashboard {

    private final JdbcClient jdbc;

    public DashboardRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean equipoPerteneceAEmpresa(Long equipoId, Long empresaId) {
        return jdbc.sql("SELECT COUNT(*) FROM equipo WHERE id = :equipoId AND empresa_id = :empresaId")
                .param("equipoId", equipoId)
                .param("empresaId", empresaId)
                .query(Long.class)
                .single() > 0;
    }

    @Override
    public List<FilaItem> agregadosDeVentana(Long equipoId, LocalDate desde, LocalDate hasta) {
        return jdbc.sql("""
                        SELECT a.item_id, i.tema, a.configuracion_ldp_id, c.mecanismo, c.probabilidad_verdad, c.epsilon,
                               e.valor_min, e.valor_max,
                               SUM(a.suma_reportada)   AS suma,
                               SUM(a.total_respuestas) AS n
                        FROM agregado_item a
                        JOIN configuracion_ldp c ON c.id = a.configuracion_ldp_id
                        JOIN item_cuestionario i ON i.id = a.item_id
                        JOIN escala_respuesta e  ON e.id = i.escala_id
                        WHERE a.equipo_id = :equipoId AND a.fecha BETWEEN :desde AND :hasta
                        GROUP BY a.item_id, i.tema, a.configuracion_ldp_id, c.mecanismo, c.probabilidad_verdad, c.epsilon,
                                 e.valor_min, e.valor_max
                        ORDER BY a.item_id
                        """)
                .param("equipoId", equipoId)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, fila) -> new FilaItem(
                        rs.getLong("item_id"),
                        rs.getString("tema"),
                        rs.getLong("configuracion_ldp_id"),
                        rs.getString("mecanismo"),
                        rs.getBigDecimal("probabilidad_verdad"),
                        rs.getBigDecimal("epsilon"),
                        rs.getInt("valor_min"),
                        rs.getInt("valor_max"),
                        rs.getBigDecimal("suma"),
                        rs.getLong("n")))
                .list();
    }
}
