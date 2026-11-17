package com.kala.backend.compartido.infraestructura.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.kala.backend.checkin.dominio.CheckInDuplicadoException;
import com.kala.backend.checkin.dominio.ConfiguracionLdpNoVigenteException;
import com.kala.backend.checkin.dominio.ItemCuestionarioNoEncontradoException;
import com.kala.backend.dashboard.dominio.SemanaInvalidaException;
import com.kala.backend.identidad.dominio.CredencialesInvalidasException;
import com.kala.backend.identidad.dominio.DemasiadosIntentosException;
import com.kala.backend.identidad.dominio.EmailEnUsoException;
import com.kala.backend.identidad.dominio.UsuarioNoEncontradoException;
import com.kala.backend.instrumentos.dominio.CuestionarioNoEncontradoException;
import com.kala.backend.instrumentos.dominio.PresupuestoExcedidoException;
import com.kala.backend.organizacion.dominio.EmpresaConEquiposException;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import com.kala.backend.organizacion.dominio.EquipoNoEncontradoException;
import com.kala.backend.organizacion.dominio.NombreDuplicadoException;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

// Una fila por excepcion de negocio: el handler unico debe devolver el codigo HTTP de su @ResponseStatus
// y el cuerpo ErrorResponse con el mensaje y la ruta.
class GlobalExceptionHandlerMapeoTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    static Stream<Arguments> excepciones() {
        return Stream.of(
                Arguments.of(new EmpresaNoEncontradaException(1L), 404),
                Arguments.of(new EquipoNoEncontradoException(1L), 404),
                Arguments.of(new CuestionarioNoEncontradoException(1L), 404),
                Arguments.of(new ItemCuestionarioNoEncontradoException(1L), 404),
                Arguments.of(new NombreDuplicadoException("Acme"), 409),
                Arguments.of(new EmpresaConEquiposException(1L), 409),
                Arguments.of(new CheckInDuplicadoException(), 409),
                Arguments.of(new PresupuestoExcedidoException(32.96, 20.0), 422),
                Arguments.of(new ConfiguracionLdpNoVigenteException(1L), 422),
                Arguments.of(new SemanaInvalidaException(LocalDate.of(2026, 3, 10)), 400),
                Arguments.of(new IllegalArgumentException("argumento"), 400),
                Arguments.of(new CredencialesInvalidasException(), 401),
                Arguments.of(new EmailEnUsoException(), 409),
                Arguments.of(new DemasiadosIntentosException(60), 429),
                Arguments.of(new UsuarioNoEncontradoException(8L), 404)
        );
    }

    @ParameterizedTest
    @MethodSource("excepciones")
    void cadaExcepcionDeNegocioSeTraduceAlCodigoDeSuResponseStatus(RuntimeException ex, int esperado) {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/prueba");

        ResponseEntity<ErrorResponse> respuesta = invocar(ex, peticion);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(esperado);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().estado()).isEqualTo(esperado);
        assertThat(respuesta.getBody().mensaje()).isEqualTo(ex.getMessage());
        assertThat(respuesta.getBody().ruta()).isEqualTo("/api/prueba");
        var anotacion = ex.getClass().getAnnotation(org.springframework.web.bind.annotation.ResponseStatus.class);
        if (anotacion != null) {
            assertThat(anotacion.value().value()).isEqualTo(esperado);
        }
    }

    private ResponseEntity<ErrorResponse> invocar(RuntimeException ex, MockHttpServletRequest peticion) {
        if (ex instanceof EmpresaNoEncontradaException || ex instanceof EquipoNoEncontradoException
                || ex instanceof CuestionarioNoEncontradoException || ex instanceof ItemCuestionarioNoEncontradoException
                || ex instanceof UsuarioNoEncontradoException) {
            return handler.noEncontrado(ex, peticion);
        }
        if (ex instanceof NombreDuplicadoException || ex instanceof EmpresaConEquiposException
                || ex instanceof CheckInDuplicadoException || ex instanceof EmailEnUsoException) {
            return handler.conflicto(ex, peticion);
        }
        if (ex instanceof PresupuestoExcedidoException || ex instanceof ConfiguracionLdpNoVigenteException) {
            return handler.reglaDePrivacidad(ex, peticion);
        }
        if (ex instanceof DemasiadosIntentosException d) {
            return handler.demasiadosIntentos(d, peticion);
        }
        if (ex instanceof CredencialesInvalidasException c) {
            return handler.credencialesInvalidas(c, peticion);
        }
        return handler.argumentoInvalido(ex, peticion);
    }

    @org.junit.jupiter.api.Test
    void demasiadosIntentos_incluyeRetryAfterConLosSegundosDeEspera() {
        var respuesta = handler.demasiadosIntentos(new DemasiadosIntentosException(840), new MockHttpServletRequest("POST", "/api/auth/login"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(429);
        assertThat(respuesta.getHeaders().getFirst("Retry-After")).isEqualTo("840");
    }

    @org.junit.jupiter.api.Test
    void unaViolacionDeRestriccionDeLaBase_es409ConMensajeFijoSinElSql() {
        var respuesta = handler.integridadDeDatos(
                new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"empresa_ruc_key\" Detail: (ruc)=(20100000001)"),
                new MockHttpServletRequest("POST", "/api/empresas"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(409);
        assertThat(respuesta.getBody().mensaje())
                .isEqualTo("La operación viola una restricción de integridad de los datos")
                .doesNotContain("20100000001")
                .doesNotContain("empresa_ruc_key");
    }
}
