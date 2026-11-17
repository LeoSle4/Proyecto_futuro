package com.kala.backend.identidad.aplicacion;

import com.kala.backend.identidad.aplicacion.puerto.RefreshTokens;
import com.kala.backend.identidad.dominio.CredencialesInvalidasException;
import com.kala.backend.identidad.dominio.RefreshToken;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Sesiones de larga duración (por defecto 30 días, renovables en cada uso). Cada renovación rota el token: el anterior
// queda usado. Si alguien presenta un token que ya se usó o se revocó, se asume robo y se revoca toda la familia.
@Service
public class RefrescoDeSesionService {

    // Un token opaco mide 43 caracteres; algo mucho más largo no puede ser uno y no se busca en la base.
    private static final int LARGO_MAXIMO = 128;
    private static final int DIAS_DE_GRACIA_PARA_PURGAR = 7;

    public record Sesion(Long usuarioId, String refreshToken, long expiraEnSegundos) {

        @Override
        public String toString() {
            return "Sesion[usuarioId=" + usuarioId + ", refreshToken=redactado]";
        }
    }

    private final RefreshTokens tokens;
    private final int dias;
    private final Clock reloj;

    public RefrescoDeSesionService(RefreshTokens tokens, @Value("${kala.refresh.dias:30}") int dias, Clock reloj) {
        if (dias < 1) {
            throw new IllegalArgumentException("kala.refresh.dias debe ser positivo");
        }
        this.tokens = tokens;
        this.dias = dias;
        this.reloj = reloj;
    }

    // Abre una familia nueva (un inicio de sesión) y devuelve el token en claro, que no se vuelve a poder leer.
    @Transactional
    public Sesion iniciar(Long usuarioId) {
        return emitir(usuarioId, UUID.randomUUID().toString(), ahora());
    }

    // noRollbackFor: al detectar una reutilización se revoca la familia y se responde 401; esa revocación debe quedar
    // guardada aunque se lance la excepción.
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public Sesion rotar(String tokenPlano) {
        if (tokenPlano == null || tokenPlano.isBlank() || tokenPlano.length() > LARGO_MAXIMO) {
            throw new CredencialesInvalidasException();
        }
        RefreshToken actual = tokens.porHash(TokenOpaco.hash(tokenPlano)).orElseThrow(CredencialesInvalidasException::new);
        LocalDateTime ahora = ahora();
        if (actual.getRevocadoEn() != null || !ahora.isBefore(actual.getExpiraEn())) {
            throw new CredencialesInvalidasException();
        }
        if (actual.getUsadoEn() != null || !tokens.marcarUsado(actual.getId(), ahora)) {
            tokens.revocarFamilia(actual.getFamilia(), ahora);
            throw new CredencialesInvalidasException();
        }
        return emitir(actual.getUsuarioId(), actual.getFamilia(), ahora);
    }

    // Cerrar sesión: revoca la familia del token. Es idempotente y un token desconocido no es un error.
    @Transactional
    public void cerrar(String tokenPlano) {
        if (tokenPlano == null || tokenPlano.isBlank() || tokenPlano.length() > LARGO_MAXIMO) {
            return;
        }
        tokens.porHash(TokenOpaco.hash(tokenPlano))
                .ifPresent(token -> tokens.revocarFamilia(token.getFamilia(), ahora()));
    }

    @Transactional
    public void revocarTodos(Long usuarioId) {
        tokens.revocarDeUsuario(usuarioId, ahora());
    }

    // Borra los tokens vencidos hace más de una semana.
    @Transactional
    public int purgarVencidos() {
        return tokens.borrarExpiradosAntesDe(ahora().minusDays(DIAS_DE_GRACIA_PARA_PURGAR));
    }

    private Sesion emitir(Long usuarioId, String familia, LocalDateTime ahora) {
        String plano = TokenOpaco.generar();
        tokens.guardar(new RefreshToken(usuarioId, familia, TokenOpaco.hash(plano), ahora, ahora.plusDays(dias)));
        return new Sesion(usuarioId, plano, dias * 24L * 3600L);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
    }
}
