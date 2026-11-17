package com.kala.backend.identidad.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// Una sesión de larga duración de una persona. Guarda solo el hash del token (nunca el token). Las fechas están en UTC.
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "familia", nullable = false, length = 36)
    private String familia;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "usado_en")
    private LocalDateTime usadoEn;

    @Column(name = "revocado_en")
    private LocalDateTime revocadoEn;

    protected RefreshToken() {
        // Requerido por JPA.
    }

    public RefreshToken(Long usuarioId, String familia, String tokenHash, LocalDateTime creadoEn, LocalDateTime expiraEn) {
        this.usuarioId = usuarioId;
        this.familia = familia;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
    }

    public void marcarUsado(LocalDateTime cuando) {
        this.usadoEn = cuando;
    }

    public void revocar(LocalDateTime cuando) {
        this.revocadoEn = cuando;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getFamilia() {
        return familia;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getExpiraEn() {
        return expiraEn;
    }

    public LocalDateTime getUsadoEn() {
        return usadoEn;
    }

    public LocalDateTime getRevocadoEn() {
        return revocadoEn;
    }
}
