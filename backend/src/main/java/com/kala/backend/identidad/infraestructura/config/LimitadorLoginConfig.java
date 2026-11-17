package com.kala.backend.identidad.infraestructura.config;

import com.kala.backend.identidad.aplicacion.LimitadorIntentosLogin;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Límite de inicios de sesión fallidos. Se ajusta con kala.login.max-fallos y kala.login.bloqueo-minutos.
@Configuration(proxyBeanMethods = false)
public class LimitadorLoginConfig {

    @Bean
    LimitadorIntentosLogin limitadorIntentosLogin(
            @Value("${kala.login.max-fallos:5}") int maxFallos,
            @Value("${kala.login.bloqueo-minutos:15}") long bloqueoMinutos,
            Clock reloj) {
        Duration bloqueo = Duration.ofMinutes(bloqueoMinutos);
        return new LimitadorIntentosLogin(maxFallos, bloqueo, bloqueo, reloj);
    }
}
