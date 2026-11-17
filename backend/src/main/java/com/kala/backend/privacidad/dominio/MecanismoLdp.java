package com.kala.backend.privacidad.dominio;

import java.math.BigDecimal;
import java.util.Optional;

// Estrategia de un mecanismo de privacidad diferencial local. Agregar un mecanismo nuevo es agregar una clase y
// registrarla en MecanismosLdp: el check-in, el dashboard y el presupuesto no cambian.
public interface MecanismoLdp {

    // Código tal como se guarda en configuracion_ldp.mecanismo.
    String codigo();

    // ε que gasta una persona cada vez que envía este ítem.
    double epsilonItem(ParametrosLdp parametros);

    // Lo que se suma al agregado a partir de lo que mandó el dispositivo. Los parámetros y la escala del ítem permiten
    // validar el valor. Lanza IllegalArgumentException si el envío no trae el campo que corresponde a este mecanismo o
    // si el valor es imposible; el mensaje nunca incluye los valores recibidos.
    BigDecimal aSuma(Boolean respuestaRuidosa, BigDecimal valorRuidoso, ParametrosLdp parametros,
                     double valorMin, double valorMax);

    // Estimación desesgada a partir de la suma y el total reportados. Vacío si faltan parámetros para estimar.
    Optional<Estimacion> estimar(ParametrosLdp parametros, double valorMin, double valorMax, BigDecimal suma, long total);
}
