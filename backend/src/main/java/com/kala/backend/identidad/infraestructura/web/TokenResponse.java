package com.kala.backend.identidad.infraestructura.web;

// Respuesta del login y del refresh: nunca incluye la contraseña ni su hash. refreshToken es el token de sesión de
// larga duración; se entrega una sola vez y solo se guarda su hash.
public record TokenResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        String refreshToken,
        long refreshExpiresInSeconds
) {

    public TokenResponse(String tokenType, String accessToken, long expiresInSeconds) {
        this(tokenType, accessToken, expiresInSeconds, null, 0L);
    }

    // El refresh token es una credencial: nunca debe llegar a un log.
    @Override
    public String toString() {
        return "TokenResponse[redactado]";
    }
}
