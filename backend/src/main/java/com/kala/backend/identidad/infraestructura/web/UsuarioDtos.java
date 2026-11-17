package com.kala.backend.identidad.infraestructura.web;

import com.kala.backend.identidad.dominio.RolPersonal;
import com.kala.backend.identidad.dominio.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// DTOs de la gestión de usuarios. Las claves nunca se devuelven y nunca llegan a un log: toString se redacta.
public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    public record NuevoPersonalRequest(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 150) String nombre,
            @NotBlank String clave,
            @NotNull RolPersonal rol
    ) {

        @Override
        public String toString() {
            return "NuevoPersonalRequest[redactado]";
        }
    }

    public record NuevoEmpleadoRequest(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 150) String nombre,
            @NotBlank String clave,
            @NotNull Long equipoId
    ) {

        @Override
        public String toString() {
            return "NuevoEmpleadoRequest[redactado]";
        }
    }

    public record CambioClaveRequest(@NotBlank String claveActual, @NotBlank String claveNueva) {

        @Override
        public String toString() {
            return "CambioClaveRequest[redactado]";
        }
    }

    public record UsuarioCreadoResponse(Long id, String email, String nombre) {

        public static UsuarioCreadoResponse de(Usuario usuario) {
            return new UsuarioCreadoResponse(usuario.getId(), usuario.getEmail(), usuario.getNombre());
        }
    }
}
