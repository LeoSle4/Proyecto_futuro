package com.kala.backend.identidad.infraestructura.web;

// Respuesta del login: nunca incluye la contraseña ni su hash.
public record TokenResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds
) {
}
