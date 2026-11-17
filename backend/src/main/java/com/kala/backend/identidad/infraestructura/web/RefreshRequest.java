package com.kala.backend.identidad.infraestructura.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cuerpo de /api/auth/refresh y /api/auth/logout. El token opaco mide 43 caracteres; el tope evita buscar basura.
public record RefreshRequest(@NotBlank @Size(max = 128) String refreshToken) {

    @Override
    public String toString() {
        return "RefreshRequest[redactado]";
    }
}
