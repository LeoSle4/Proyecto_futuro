package com.kala.backend.checkin.aplicacion.puerto;

import java.util.Optional;

// Puerto de salida: lo que el check-in necesita saber de un ítem, sin importar las clases del módulo de instrumentos.
public interface ItemsCuestionario {

    // itemsPorEnvio es nulo cuando el envío incluye todos los ítems; totalItems es el tamaño del cuestionario.
    record ItemCuestionarioInfo(Long cuestionarioId, Long configuracionLdpId, String mecanismo,
                                int cadenciaDias, Integer itemsPorEnvio, int totalItems) {

        public static ItemCuestionarioInfo pulso(Long cuestionarioId, Long configuracionLdpId) {
            return new ItemCuestionarioInfo(cuestionarioId, configuracionLdpId, "RANDOMIZED_RESPONSE", 7, null, 1);
        }

        // Cuántos ítems puede traer un envío: k, o todos si el cuestionario no define k.
        public int maximoPorEnvio() {
            return itemsPorEnvio == null ? totalItems : itemsPorEnvio;
        }
    }

    Optional<ItemCuestionarioInfo> buscar(Long itemId);
}
