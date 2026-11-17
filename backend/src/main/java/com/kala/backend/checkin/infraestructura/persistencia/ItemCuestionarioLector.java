package com.kala.backend.checkin.infraestructura.persistencia;

import com.kala.backend.checkin.aplicacion.puerto.ItemsCuestionario.ItemCuestionarioInfo;
import com.kala.backend.checkin.aplicacion.puerto.ItemsCuestionario;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

// Lee de item_cuestionario (tabla de otro módulo) solo lo que el check-in necesita saber del ítem.
// Se usa SQL directo con JdbcClient para no importar clases de ese módulo.
@Component
public class ItemCuestionarioLector implements ItemsCuestionario {

    private final JdbcClient jdbc;

    public ItemCuestionarioLector(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ItemCuestionarioInfo> buscar(Long itemId) {
        return jdbc.sql("""
                        SELECT i.cuestionario_id, i.configuracion_ldp_id, c.mecanismo, q.cadencia_dias,
                               q.items_por_envio,
                               (SELECT COUNT(*) FROM item_cuestionario x WHERE x.cuestionario_id = i.cuestionario_id) AS total_items
                        FROM item_cuestionario i
                        JOIN configuracion_ldp c ON c.id = i.configuracion_ldp_id
                        JOIN cuestionario q ON q.id = i.cuestionario_id
                        WHERE i.id = :itemId
                        """)
                .param("itemId", itemId)
                .query((rs, fila) -> new ItemCuestionarioInfo(
                        rs.getLong("cuestionario_id"),
                        rs.getLong("configuracion_ldp_id"),
                        rs.getString("mecanismo"),
                        rs.getInt("cadencia_dias"),
                        rs.getObject("items_por_envio") == null ? null : rs.getInt("items_por_envio"),
                        rs.getInt("total_items")))
                .optional();
    }
}
