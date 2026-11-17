package com.kala.backend.compartido.infraestructura.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Activa las tareas programadas (@Scheduled) de la aplicación.
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ProgramacionConfig {
}
