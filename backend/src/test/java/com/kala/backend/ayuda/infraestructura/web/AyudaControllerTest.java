package com.kala.backend.ayuda.infraestructura.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kala.backend.ayuda.aplicacion.AyudaService;
import com.kala.backend.ayuda.dominio.ContactoAyuda;
import com.kala.backend.ayuda.dominio.SolicitudAyuda;
import com.kala.backend.ayuda.dominio.SolicitudAyudaEstadoException;
import com.kala.backend.ayuda.dominio.SolicitudAyudaNoEncontradaException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// Matriz de roles del canal de ayuda, con jwt() y sin depender del SecurityConfig real.
@WebMvcTest(AyudaController.class)
class AyudaControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class SeguridadDeMetodo {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AyudaService service;

    private RequestPostProcessor como(String rol) {
        return jwt().jwt(t -> t.claim("uid", 11L).claim("empresaId", 1L).claim("roles", List.of("ROLE_" + rol)))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Test
    void empleadoPideAyuda_devuelve201_yLaPersonaSaleDelToken() throws Exception {
        when(service.solicitar(eq(11L), any())).thenReturn(new SolicitudAyuda(11L, "hola"));

        mockMvc.perform(post("/api/ayuda/solicitudes").with(como("EMPLEADO")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"mensaje\": \"hola\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        verify(service).solicitar(11L, "hola");
    }

    @Test
    void rrhhNoPuedePedirAyudaNiVerSolicitudes() throws Exception {
        mockMvc.perform(post("/api/ayuda/solicitudes").with(como("RRHH")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/ayuda/solicitudes").with(como("RRHH")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void empleadoNoVeLasSolicitudesDeLaEmpresa() throws Exception {
        mockMvc.perform(get("/api/ayuda/solicitudes").with(como("EMPLEADO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordinadorListaLasSolicitudesDeSuEmpresa() throws Exception {
        when(service.solicitudesDeEmpresa(1L, "PENDIENTE")).thenReturn(List.of(new SolicitudAyuda(5L, null)));

        mockMvc.perform(get("/api/ayuda/solicitudes").param("estado", "PENDIENTE").with(como("COORDINADOR_BIENESTAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(5));
    }

    @Test
    void coordinadorAtiende_conContactoOSinCuerpo() throws Exception {
        when(service.atender(eq(1L), eq(11L), eq(7L), any())).thenReturn(new SolicitudAyuda(5L, null));

        mockMvc.perform(patch("/api/ayuda/solicitudes/7/atender").with(como("COORDINADOR_BIENESTAR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"contactoId\": 3 }"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/ayuda/solicitudes/7/atender").with(como("COORDINADOR_BIENESTAR")).with(csrf()))
                .andExpect(status().isOk());

        verify(service).atender(1L, 11L, 7L, 3L);
        verify(service).atender(1L, 11L, 7L, null);
    }

    @Test
    void solicitudAjenaEs404_yYaAtendidaEs409() throws Exception {
        when(service.atender(eq(1L), eq(11L), eq(7L), any())).thenThrow(new SolicitudAyudaNoEncontradaException(7L));
        when(service.cerrar(1L, 11L, 8L)).thenThrow(new SolicitudAyudaEstadoException(8L, "CERRADA"));

        mockMvc.perform(patch("/api/ayuda/solicitudes/7/atender").with(como("COORDINADOR_BIENESTAR")).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/ayuda/solicitudes/8/cerrar").with(como("COORDINADOR_BIENESTAR")).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void empleadoVeSoloContactosActivos_yPersonalTodos() throws Exception {
        when(service.listarContactos(1L, true)).thenReturn(List.of(new ContactoAyuda(1L, "Linea", "LINEA_CRISIS", "113", null)));
        when(service.listarContactos(1L, false)).thenReturn(List.of());

        mockMvc.perform(get("/api/ayuda/contactos").with(como("EMPLEADO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nombre").value("Linea"));
        mockMvc.perform(get("/api/ayuda/contactos").with(como("RRHH")))
                .andExpect(status().isOk());

        verify(service).listarContactos(1L, true);
        verify(service).listarContactos(1L, false);
    }

    @Test
    void soloElPersonalCreaContactos_yElCuerpoSeValida() throws Exception {
        when(service.crearContacto(eq(1L), any(), any(), any(), any()))
                .thenReturn(new ContactoAyuda(1L, "Dra. Ruiz", "PSICOLOGO", "999", null));

        mockMvc.perform(post("/api/ayuda/contactos").with(como("EMPLEADO")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"Dra. Ruiz\", \"tipo\": \"PSICOLOGO\", \"telefono\": \"999\" }"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/ayuda/contactos").with(como("RRHH")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"\", \"tipo\": \"PSICOLOGO\" }"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/ayuda/contactos").with(como("RRHH")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"Dra. Ruiz\", \"tipo\": \"PSICOLOGO\", \"telefono\": \"999\" }"))
                .andExpect(status().isCreated());
    }

    @Test
    void sinClaimDeEmpresa_devuelve403() throws Exception {
        mockMvc.perform(get("/api/ayuda/contactos")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_RRHH"))))
                .andExpect(status().isForbidden());
    }
}
