package com.kala.backend.identidad.infraestructura.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kala.backend.identidad.aplicacion.GestionUsuariosService;
import com.kala.backend.identidad.dominio.CredencialesInvalidasException;
import com.kala.backend.identidad.dominio.EmailEnUsoException;
import com.kala.backend.identidad.dominio.RolPersonal;
import com.kala.backend.identidad.dominio.Usuario;
import com.kala.backend.identidad.dominio.UsuarioNoEncontradoException;
import com.kala.backend.organizacion.dominio.EquipoNoEncontradoException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// Matriz de roles del alta de cuentas y del cambio de clave, con jwt() y sin depender del SecurityConfig real.
@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    private static final String CLAVE = "una-clave-larga-123";
    private static final String PERSONAL = "{\"email\":\"ana@kala.test\",\"nombre\":\"Ana\",\"clave\":\"" + CLAVE
            + "\",\"rol\":\"RRHH\"}";
    private static final String EMPLEADO = "{\"email\":\"luis@kala.test\",\"nombre\":\"Luis\",\"clave\":\"" + CLAVE
            + "\",\"equipoId\":9}";

    @TestConfiguration
    @EnableMethodSecurity
    static class SeguridadDeMetodo {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GestionUsuariosService service;

    private RequestPostProcessor como(String rol) {
        return jwt().jwt(t -> t.claim("uid", 11L).claim("empresaId", 4L).claim("roles", List.of("ROLE_" + rol)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    // ---- personal de empresa (solo ADMIN) ----

    @Test
    void adminCreaPersonal_devuelve201SinLaClave() throws Exception {
        when(service.crearPersonal(eq(4L), eq("ana@kala.test"), eq("Ana"), eq(CLAVE), eq(RolPersonal.RRHH)))
                .thenReturn(new Usuario("ana@kala.test", "hash", "Ana"));

        mockMvc.perform(post("/api/admin/empresas/4/personal").with(como("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(PERSONAL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@kala.test"))
                .andExpect(jsonPath("$.clave").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void soloElAdminCreaPersonal() throws Exception {
        for (String rol : List.of("RRHH", "COORDINADOR_BIENESTAR", "EMPLEADO")) {
            mockMvc.perform(post("/api/admin/empresas/4/personal").with(como(rol)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(PERSONAL))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test
    void personalConCuerpoInvalido_devuelve400SinLlegarAlServicio() throws Exception {
        for (String cuerpo : List.of(
                "{\"email\":\"no-es-correo\",\"nombre\":\"Ana\",\"clave\":\"" + CLAVE + "\",\"rol\":\"RRHH\"}",
                "{\"email\":\"ana@kala.test\",\"nombre\":\"Ana\",\"rol\":\"RRHH\"}",
                "{\"email\":\"ana@kala.test\",\"nombre\":\"Ana\",\"clave\":\"" + CLAVE + "\",\"rol\":\"SUPERUSUARIO\"}")) {
            mockMvc.perform(post("/api/admin/empresas/4/personal").with(como("ADMIN")).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void correoRepetido_devuelve409SinRepetirElCorreo() throws Exception {
        when(service.crearPersonal(any(), any(), any(), any(), any())).thenThrow(new EmailEnUsoException());

        mockMvc.perform(post("/api/admin/empresas/4/personal").with(como("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(PERSONAL))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("ana@kala.test"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(CLAVE))));
    }

    // ---- empleados (RRHH o coordinador de la empresa del token) ----

    @Test
    void personalCreaEmpleado_laEmpresaSaleDelToken() throws Exception {
        when(service.crearEmpleado(eq(4L), eq(9L), eq("luis@kala.test"), eq("Luis"), eq(CLAVE)))
                .thenReturn(new Usuario("luis@kala.test", "hash", "Luis"));

        for (String rol : List.of("RRHH", "COORDINADOR_BIENESTAR")) {
            mockMvc.perform(post("/api/empleados").with(como(rol)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(EMPLEADO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("luis@kala.test"));
        }
        verify(service, org.mockito.Mockito.times(2)).crearEmpleado(4L, 9L, "luis@kala.test", "Luis", CLAVE);
    }

    @Test
    void ningunOtroRolCreaEmpleados() throws Exception {
        for (String rol : List.of("ADMIN", "EMPLEADO")) {
            mockMvc.perform(post("/api/empleados").with(como(rol)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(EMPLEADO))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test
    void equipoDeOtraEmpresa_devuelve404() throws Exception {
        when(service.crearEmpleado(any(), any(), any(), any(), any())).thenThrow(new EquipoNoEncontradoException(9L));

        mockMvc.perform(post("/api/empleados").with(como("RRHH")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(EMPLEADO))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinClaimDeEmpresa_devuelve403() throws Exception {
        mockMvc.perform(post("/api/empleados")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RRHH"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(EMPLEADO))
                .andExpect(status().isForbidden());
    }

    // ---- cambio de la propia clave ----

    @Test
    void cualquierPersonaAutenticadaCambiaSuClave() throws Exception {
        for (String rol : List.of("ADMIN", "RRHH", "COORDINADOR_BIENESTAR", "EMPLEADO")) {
            mockMvc.perform(put("/api/usuarios/me/clave").with(como(rol)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"claveActual\":\"la-clave-actual-1\",\"claveNueva\":\"otra-clave-nueva-2\"}"))
                    .andExpect(status().isNoContent());
        }
        verify(service, org.mockito.Mockito.times(4)).cambiarClave(11L, "la-clave-actual-1", "otra-clave-nueva-2");
    }

    @Test
    void sinTokenNoSeCambiaNingunaClave() throws Exception {
        mockMvc.perform(put("/api/usuarios/me/clave").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"x\",\"claveNueva\":\"y\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void claveActualIncorrecta_devuelve401() throws Exception {
        doThrow(new CredencialesInvalidasException()).when(service).cambiarClave(any(), any(), any());

        mockMvc.perform(put("/api/usuarios/me/clave").with(como("EMPLEADO")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"equivocada-123456\",\"claveNueva\":\"otra-clave-nueva-2\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("equivocada"))));
    }

    @Test
    void claveNuevaInvalida_devuelve400SinRepetirla() throws Exception {
        doThrow(new IllegalArgumentException("La clave debe tener entre 12 y 72 caracteres"))
                .when(service).cambiarClave(any(), any(), any());

        mockMvc.perform(put("/api/usuarios/me/clave").with(como("EMPLEADO")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"la-clave-actual-1\",\"claveNueva\":\"corta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("corta\""))));
    }

    // ---- desactivar y reactivar ----

    @Test
    void adminPersonalYCoordinadorPuedenCambiarElEstado_yElEmpleadoNo() throws Exception {
        for (String rol : List.of("ADMIN", "RRHH", "COORDINADOR_BIENESTAR")) {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/usuarios/8/desactivar")
                            .with(como(rol)).with(csrf()))
                    .andExpect(status().isNoContent());
        }
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/usuarios/8/activar")
                        .with(como("RRHH")).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/usuarios/8/desactivar")
                        .with(como("EMPLEADO")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/usuarios/8/desactivar")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
        verify(service).cambiarEstado(11L, true, null, 8L, false);
        verify(service, org.mockito.Mockito.times(2)).cambiarEstado(11L, false, 4L, 8L, false);
        verify(service).cambiarEstado(11L, false, 4L, 8L, true);
    }

    @Test
    void siElUsuarioNoEsDeLaEmpresaOExiste_devuelve404() throws Exception {
        doThrow(new com.kala.backend.identidad.dominio.UsuarioNoEncontradoException(8L))
                .when(service).cambiarEstado(any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any(),
                        org.mockito.ArgumentMatchers.anyBoolean());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/usuarios/8/desactivar")
                        .with(como("RRHH")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void elPersonalSinClaimDeEmpresa_devuelve403() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/usuarios/8/desactivar")
                        .with(jwt().jwt(t -> t.claim("uid", 11L).claim("roles", List.of("ROLE_RRHH")))
                                .authorities(new SimpleGrantedAuthority("ROLE_RRHH")))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
