package com.kala.backend.dashboard.infraestructura.web;

import java.time.LocalDate;
import java.util.List;

// Lo que devuelve GET /api/dashboard/{equipoId}?semana=: la estimación de la semana del equipo.
// Randomized Response llena proporcionEstimada; Laplace llena mediaEstimada. Estimado y margen son nulos cuando
// los datos son insuficientes.
public record DashboardResponse(
        Long equipoId,
        LocalDate semanaInicio,
        String resultado,
        long totalRespuestas,
        List<ItemDashboard> items
) {

    public record ItemDashboard(
            Long itemId,
            String tema,
            Double proporcionEstimada,
            Double margenError95,
            Double mediaEstimada,
            Integer valorMin,
            Integer valorMax
    ) {
    }
}
