package com.kala.backend.ayuda.infraestructura.web;

import com.kala.backend.ayuda.dominio.ContactoAyuda;
import com.kala.backend.ayuda.dominio.SolicitudAyuda;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

// DTOs del canal de ayuda. El mensaje de una solicitud es sensible: se redacta en toString y nunca se registra.
public final class AyudaDtos {

    private AyudaDtos() {
    }

    public record ContactoRequest(
            @NotBlank @Size(max = 150) String nombre,
            @NotBlank String tipo,
            @Size(max = 20) String telefono,
            @Size(max = 150) String email
    ) {
    }

    public record ContactoResponse(Long id, String nombre, String tipo, String telefono, String email, boolean activo) {

        public static ContactoResponse de(ContactoAyuda c) {
            return new ContactoResponse(c.getId(), c.getNombre(), c.getTipo(), c.getTelefono(), c.getEmail(),
                    c.isActivo());
        }
    }

    public record SolicitudRequest(@Size(max = 2000) String mensaje) {

        @Override
        public String toString() {
            return "SolicitudRequest[redactado]";
        }
    }

    public record AtenderRequest(Long contactoId) {
    }

    public record SolicitudResponse(Long id, Long usuarioId, Long contactoId, String mensaje, String estado,
                                    LocalDateTime creadoEn, Long atendidoPor, LocalDateTime atendidoEn) {

        public static SolicitudResponse de(SolicitudAyuda s) {
            return new SolicitudResponse(s.getId(), s.getUsuarioId(), s.getContactoId(), s.getMensaje(), s.getEstado(),
                    s.getCreadoEn(), s.getAtendidoPor(), s.getAtendidoEn());
        }

        @Override
        public String toString() {
            return "SolicitudResponse[redactado]";
        }
    }
}
