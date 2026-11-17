package com.kala.backend.privacidad.dominio;

import java.util.List;
import java.util.Optional;

// Registro de los mecanismos conocidos. Para agregar uno nuevo basta con sumarlo a esta lista.
public final class MecanismosLdp {

    private static final List<MecanismoLdp> CONOCIDOS = List.of(new RandomizedResponse(), new Laplace());

    private MecanismosLdp() {
    }

    public static Optional<MecanismoLdp> buscar(String codigo) {
        return CONOCIDOS.stream().filter(m -> m.codigo().equals(codigo)).findFirst();
    }

    public static MecanismoLdp de(String codigo) {
        return buscar(codigo).orElseThrow(() -> new IllegalArgumentException("Mecanismo desconocido: " + codigo));
    }
}
