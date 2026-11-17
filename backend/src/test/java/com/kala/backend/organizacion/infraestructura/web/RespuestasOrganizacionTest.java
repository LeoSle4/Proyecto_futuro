package com.kala.backend.organizacion.infraestructura.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.kala.backend.organizacion.dominio.Empresa;
import com.kala.backend.organizacion.dominio.Equipo;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// Fija el formato JSON que ya consumen los clientes (colecciones de Postman y la app): los mismos campos que antes,
// pero sin exponer la entidad de la base de datos.
class RespuestasOrganizacionTest {

    private static List<String> campos(Class<? extends Record> tipo) {
        return Arrays.stream(tipo.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    @Test
    void empresaResponse_tieneLosMismosCamposQueLaEntidadSerializada() {
        assertThat(campos(EmpresaResponse.class)).containsExactly("id", "ruc", "nombre", "rubro", "creadoEn");
    }

    @Test
    void equipoResponse_tieneLosMismosCamposQueLaEntidadSerializada() {
        assertThat(campos(EquipoResponse.class)).containsExactly("id", "nombre", "empresaId", "creadoEn");
    }

    @Test
    void seCopianLosValoresDeLaEntidad() {
        Empresa empresa = new Empresa("20123456789", "ACME", "TI");
        ReflectionTestUtils.setField(empresa, "id", 5L);
        Equipo equipo = new Equipo("Ventas", 5L);
        ReflectionTestUtils.setField(equipo, "id", 9L);

        EmpresaResponse e = EmpresaResponse.de(empresa);
        EquipoResponse q = EquipoResponse.de(equipo);

        assertThat(e.id()).isEqualTo(5L);
        assertThat(e.ruc()).isEqualTo("20123456789");
        assertThat(e.nombre()).isEqualTo("ACME");
        assertThat(e.rubro()).isEqualTo("TI");
        assertThat(q.id()).isEqualTo(9L);
        assertThat(q.empresaId()).isEqualTo(5L);
        assertThat(q.nombre()).isEqualTo("Ventas");
    }
}
