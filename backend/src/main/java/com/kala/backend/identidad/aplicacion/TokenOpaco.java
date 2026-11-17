package com.kala.backend.identidad.aplicacion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

// Tokens opacos para las sesiones de larga duración: 256 bits aleatorios (43 caracteres seguros para URL). Lo que se
// guarda es solo su SHA-256: con 256 bits de entropía no hace falta un hash lento, y así la base nunca tiene un token
// utilizable.
final class TokenOpaco {

    private static final SecureRandom AZAR = new SecureRandom();

    private TokenOpaco() {
    }

    static String generar() {
        byte[] bytes = new byte[32];
        AZAR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
