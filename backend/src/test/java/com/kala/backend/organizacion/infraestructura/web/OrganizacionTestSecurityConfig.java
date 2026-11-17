package com.kala.backend.organizacion.infraestructura.web;

import com.kala.backend.identidad.infraestructura.config.JwtConfig;
import com.kala.backend.identidad.infraestructura.config.SecurityConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@TestConfiguration
@EnableMethodSecurity
@Import({SecurityConfig.class, JwtConfig.class})
public class OrganizacionTestSecurityConfig {

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }
}
