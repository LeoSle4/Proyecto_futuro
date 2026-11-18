package com.kala.backend.instrumentos.infraestructura.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kala.backend.instrumentos.aplicacion.ActivacionCuestionarioService.ResultadoActivacion;
import com.kala.backend.instrumentos.aplicacion.ActivacionCuestionarioService.ResultadoPresupuesto;
import com.kala.backend.instrumentos.aplicacion.ActivacionCuestionarioService;
import com.kala.backend.instrumentos.aplicacion.CuestionarioConsultaService;
import com.kala.backend.instrumentos.aplicacion.CuestionarioPendienteResponse.Escala;
import com.kala.backend.instrumentos.aplicacion.CuestionarioPendienteResponse.ItemPendiente;
import com.kala.backend.instrumentos.aplicacion.CuestionarioPendienteResponse.Opcion;
import com.kala.backend.instrumentos.aplicacion.CuestionarioPendienteResponse;
import com.kala.backend.instrumentos.dominio.Cuestionario;
import com.kala.backend.instrumentos.dominio.CuestionarioNoEncontradoException;
import com.kala.backend.instrumentos.dominio.PresupuestoExcedidoException;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import com.kala.backend.organizacion.infraestructura.web.OrganizacionTestSecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// Matriz 401 / 403 / 200 / 404 / 422 de los endpoints de cuestionarios, con los services como mocks.
@WebMvcTest(CuestionarioController.class)
@AutoConfigureMockMvc
@Import(OrganizacionTestSecurityConfig.class)
@TestPropertySource(properties = {
        "app.jwt.issuer=kala-api",
        "app.jwt.ttl-minutes=30",
        "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class CuestionarioControllerTest {

    private static final String ACTIVAR = "/api/empresas/1/cuestionarios/10/activar";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivacionCuestionarioService activacion;

    @MockitoBean
    private CuestionarioConsultaService consulta;

    static RequestPostProcessor jwtDe(String rol, Long empresaId) {
        return jwt().jwt(jwt -> {
            jwt.claim("roles", List.of(rol));
            jwt.claim("uid", 5L);
            if (empresaId != null) {
                jwt.claim("empresaId", empresaId);
            }
        }).authorities(new SimpleGrantedAuthority(rol));
    }

    private static Cuestionario pulso() {
        Cuestionario c = new Cuestionario("PULSO", "Pulso semanal", (short) 7, null);
        ReflectionTestUtils.setField(c, "id", 10L);
        return c;
    }

    @Test
    void activar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post(ACTIVAR)).andExpect(status().isUnauthorized());
    }

    @Test
    void activar_soloAdmin_losDemasRolesRecibenForbidden() throws Exception {
        for (String rol : List.of("ROLE_RRHH", "ROLE_COORDINADOR_BIENESTAR", "ROLE_EMPLEADO")) {
            mockMvc.perform(post(ACTIVAR).with(jwtDe(rol, 1L))).andExpect(status().isForbidden());
        }
        verify(activacion, never()).activar(any(), any());
    }

    @Test
    void activar_comoAdmin_devuelve200ConElGastoMensual() throws Exception {
        when(activacion.activar(1L, 10L)).thenReturn(new ResultadoActivacion(1L, 10L, 5.49, 20.0));

        mockMvc.perform(post(ACTIVAR).with(jwtDe("ROLE_ADMIN", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaId").value(1))
                .andExpect(jsonPath("$.cuestionarioId").value(10))
                .andExpect(jsonPath("$.epsilonMensual").value(5.49))
                .andExpect(jsonPath("$.epsilonMaximo").value(20.0));
    }

    @Test
    void activar_empresaOCuestionarioInexistente_devuelve404() throws Exception {
        when(activacion.activar(99L, 10L)).thenThrow(new EmpresaNoEncontradaException(99L));
        when(activacion.activar(1L, 99L)).thenThrow(new CuestionarioNoEncontradoException(99L));

        mockMvc.perform(post("/api/empresas/99/cuestionarios/10/activar").with(jwtDe("ROLE_ADMIN", null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/empresas/1/cuestionarios/99/activar").with(jwtDe("ROLE_ADMIN", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void activar_siExcedeElPresupuesto_devuelve422() throws Exception {
        when(activacion.activar(1L, 10L)).thenThrow(new PresupuestoExcedidoException(32.96, 20.0));

        mockMvc.perform(post(ACTIVAR).with(jwtDe("ROLE_ADMIN", null)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void listar_sinToken_devuelve401_yUnEmpleadoRecibe403() throws Exception {
        mockMvc.perform(get("/api/empresas/1/cuestionarios")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/empresas/1/cuestionarios").with(jwtDe("ROLE_EMPLEADO", 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listar_adminYRrhhDeLaEmpresa_devuelven200() throws Exception {
        when(consulta.listarActivos(1L)).thenReturn(List.of(pulso()));

        mockMvc.perform(get("/api/empresas/1/cuestionarios").with(jwtDe("ROLE_ADMIN", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("PULSO"))
                .andExpect(jsonPath("$[0].cadenciaDias").value(7))
                .andExpect(jsonPath("$[0].itemsPorEnvio").doesNotExist());
        mockMvc.perform(get("/api/empresas/1/cuestionarios").with(jwtDe("ROLE_RRHH", 1L)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/empresas/1/cuestionarios").with(jwtDe("ROLE_COORDINADOR_BIENESTAR", 1L)))
                .andExpect(status().isOk());
    }

    @Test
    void listar_empresaInexistenteParaElAdmin_devuelve404() throws Exception {
        when(consulta.listarActivos(99L)).thenThrow(new EmpresaNoEncontradaException(99L));

        mockMvc.perform(get("/api/empresas/99/cuestionarios").with(jwtDe("ROLE_ADMIN", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void conIncluirInactivosListaTodoElCatalogoConSuEstado() throws Exception {
        Cuestionario activo = pulso();
        Cuestionario libre = new Cuestionario("WHO5", "Bienestar", (short) 14, null);
        when(consulta.listarConEstado(1L)).thenReturn(List.of(
                new CuestionarioConsultaService.CuestionarioConEstado(activo, true),
                new CuestionarioConsultaService.CuestionarioConEstado(libre, false)));

        mockMvc.perform(get("/api/empresas/1/cuestionarios").param("incluirInactivos", "true")
                        .with(jwtDe("ROLE_RRHH", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("PULSO"))
                .andExpect(jsonPath("$[0].activo").value(true))
                .andExpect(jsonPath("$[1].codigo").value("WHO5"))
                .andExpect(jsonPath("$[1].activo").value(false));
    }

    @Test
    void sinIncluirInactivosSigueListandoSoloLosActivosYLosMarcaActivos() throws Exception {
        when(consulta.listarActivos(1L)).thenReturn(List.of(pulso()));

        mockMvc.perform(get("/api/empresas/1/cuestionarios").with(jwtDe("ROLE_RRHH", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activo").value(true));
    }

    @Test
    void incluirInactivosDeOtraEmpresaSigueDandoNotFoundParaRrhh() throws Exception {
        mockMvc.perform(get("/api/empresas/9/cuestionarios").param("incluirInactivos", "true")
                        .with(jwtDe("ROLE_RRHH", 1L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void pendientes_devuelveLosItemsConLaConfiguracionParaAleatorizar() throws Exception {
        when(consulta.pendientes(1L)).thenReturn(List.of(new CuestionarioPendienteResponse(
                10L, "PULSO", "Pulso semanal", 7, null, List.of(new ItemPendiente(
                        3L, 1, "Pulso", "¿Hoy te sentiste bien?",
                        new Escala("BINARIA", 0, 1, List.of(new Opcion(0, "No"), new Opcion(1, "Sí"))),
                        2L, "RANDOMIZED_RESPONSE", new BigDecimal("0.750"), null)))));

        mockMvc.perform(get("/api/cuestionarios/pendientes").with(jwtDe("ROLE_EMPLEADO", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("PULSO"))
                .andExpect(jsonPath("$[0].items[0].itemId").value(3))
                .andExpect(jsonPath("$[0].items[0].configuracionLdpId").value(2))
                .andExpect(jsonPath("$[0].items[0].mecanismo").value("RANDOMIZED_RESPONSE"))
                .andExpect(jsonPath("$[0].items[0].probabilidadVerdad").value(0.75))
                .andExpect(jsonPath("$[0].items[0].escala.opciones[1].etiqueta").value("Sí"));
    }

    @Test
    void pendientes_enUnItemLaplaceIncluyeElEpsilonParaEscalarElRuido() throws Exception {
        when(consulta.pendientes(1L)).thenReturn(List.of(new CuestionarioPendienteResponse(
                11L, "WHO5", "Bienestar", 14, null, List.of(new ItemPendiente(
                        4L, 1, "Calma", "Me he sentido tranquilo(a)",
                        new Escala("WHO5", 0, 5, List.of(new Opcion(0, "Nunca"), new Opcion(5, "Siempre"))),
                        3L, "LAPLACE", null, new BigDecimal("1.0"))))));

        mockMvc.perform(get("/api/cuestionarios/pendientes").with(jwtDe("ROLE_EMPLEADO", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items[0].mecanismo").value("LAPLACE"))
                .andExpect(jsonPath("$[0].items[0].epsilon").value(1.0))
                .andExpect(jsonPath("$[0].items[0].escala.valorMax").value(5));
    }

    @Test
    void pendientes_sinToken401_yLosDemasRoles403() throws Exception {
        mockMvc.perform(get("/api/cuestionarios/pendientes")).andExpect(status().isUnauthorized());
        for (String rol : List.of("ROLE_ADMIN", "ROLE_RRHH", "ROLE_COORDINADOR_BIENESTAR")) {
            mockMvc.perform(get("/api/cuestionarios/pendientes").with(jwtDe(rol, 1L)))
                    .andExpect(status().isForbidden());
        }
    }

    // ---- desactivar y presupuesto de privacidad ----

    @Test
    void desactivar_soloAdmin_yDevuelveElGastoQueQueda() throws Exception {
        when(activacion.desactivar(1L, 10L)).thenReturn(new ResultadoActivacion(1L, 10L, 5.49, 20.0));

        mockMvc.perform(post("/api/empresas/1/cuestionarios/10/desactivar").with(jwtDe("ROLE_ADMIN", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.epsilonMensual").value(5.49));
        for (String rol : List.of("ROLE_RRHH", "ROLE_COORDINADOR_BIENESTAR", "ROLE_EMPLEADO")) {
            mockMvc.perform(post("/api/empresas/1/cuestionarios/10/desactivar").with(jwtDe(rol, 1L)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/empresas/1/cuestionarios/10/desactivar")).andExpect(status().isUnauthorized());
        verify(activacion, org.mockito.Mockito.times(1)).desactivar(any(), any());
    }

    @Test
    void presupuesto_adminVeCualquierEmpresa_yElPersonalSoloLaSuya() throws Exception {
        when(activacion.consultarPresupuesto(1L))
                .thenReturn(new ActivacionCuestionarioService.ResultadoPresupuesto(1L, "2026-03", 20.0, 5.49));

        mockMvc.perform(get("/api/empresas/1/presupuesto").with(jwtDe("ROLE_ADMIN", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.epsilonMaximo").value(20.0))
                .andExpect(jsonPath("$.periodo").value("2026-03"));
        mockMvc.perform(get("/api/empresas/1/presupuesto").with(jwtDe("ROLE_RRHH", 1L)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/empresas/1/presupuesto").with(jwtDe("ROLE_COORDINADOR_BIENESTAR", 2L)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/empresas/1/presupuesto").with(jwtDe("ROLE_EMPLEADO", 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void fijarPresupuesto_soloAdmin() throws Exception {
        when(activacion.fijarPresupuesto(eq(1L), any()))
                .thenReturn(new ActivacionCuestionarioService.ResultadoPresupuesto(1L, "2026-03", 25.0, 5.49));

        mockMvc.perform(put("/api/empresas/1/presupuesto").with(jwtDe("ROLE_ADMIN", null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"epsilonMaximoPersona\": 25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.epsilonMaximo").value(25.0));
        for (String rol : List.of("ROLE_RRHH", "ROLE_COORDINADOR_BIENESTAR", "ROLE_EMPLEADO")) {
            mockMvc.perform(put("/api/empresas/1/presupuesto").with(jwtDe(rol, 1L))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"epsilonMaximoPersona\": 25}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void fijarPresupuesto_conCuerpoInvalido_devuelve400() throws Exception {
        for (String cuerpo : List.of("{}", "{\"epsilonMaximoPersona\": 0}", "{\"epsilonMaximoPersona\": -3}",
                "{\"epsilonMaximoPersona\": 1000}", "{\"epsilonMaximoPersona\": \"mucho\"}")) {
            mockMvc.perform(put("/api/empresas/1/presupuesto").with(jwtDe("ROLE_ADMIN", null))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
        verify(activacion, never()).fijarPresupuesto(any(), any());
    }

    @Test
    void fijarPresupuesto_pordebajoDelGastoActual_devuelve422() throws Exception {
        when(activacion.fijarPresupuesto(eq(1L), any())).thenThrow(new PresupuestoExcedidoException(8.49, 5.0));

        mockMvc.perform(put("/api/empresas/1/presupuesto").with(jwtDe("ROLE_ADMIN", null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"epsilonMaximoPersona\": 5}"))
                .andExpect(status().isUnprocessableContent());
    }
}
