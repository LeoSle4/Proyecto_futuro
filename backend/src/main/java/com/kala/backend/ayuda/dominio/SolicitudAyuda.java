package com.kala.backend.ayuda.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// Petición de una persona que pide hablar con alguien. Es el único canal identificado de Kala: la persona se
// identifica a propósito, y esta tabla nunca se relaciona con agregado_item ni con sesion_checkin (R8).
@Entity
@Table(name = "solicitud_ayuda")
public class SolicitudAyuda {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_ATENCION = "EN_ATENCION";
    public static final String CERRADA = "CERRADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "contacto_id")
    private Long contactoId;

    @Column(name = "mensaje")
    private String mensaje;

    @Column(name = "estado", nullable = false, length = 15)
    private String estado = PENDIENTE;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "atendido_por")
    private Long atendidoPor;

    @Column(name = "atendido_en")
    private LocalDateTime atendidoEn;

    protected SolicitudAyuda() {
        // Requerido por JPA.
    }

    public SolicitudAyuda(Long usuarioId, String mensaje) {
        this.usuarioId = usuarioId;
        this.mensaje = mensaje;
    }

    // PENDIENTE -> EN_ATENCION: el coordinador la toma y, si quiere, la deriva a un contacto.
    public void atender(Long personalId, Long contactoId, LocalDateTime ahora) {
        this.estado = EN_ATENCION;
        this.atendidoPor = personalId;
        this.contactoId = contactoId;
        this.atendidoEn = ahora;
    }

    public void cerrar(Long personalId, LocalDateTime ahora) {
        this.estado = CERRADA;
        if (this.atendidoPor == null) {
            this.atendidoPor = personalId;
            this.atendidoEn = ahora;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getContactoId() {
        return contactoId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public Long getAtendidoPor() {
        return atendidoPor;
    }

    public LocalDateTime getAtendidoEn() {
        return atendidoEn;
    }
}
