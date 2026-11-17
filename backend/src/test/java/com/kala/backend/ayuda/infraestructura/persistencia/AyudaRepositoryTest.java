package com.kala.backend.ayuda.infraestructura.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kala.backend.FilasPadre;
import com.kala.backend.ayuda.dominio.ContactoAyuda;
import com.kala.backend.ayuda.dominio.SolicitudAyuda;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// Contra la rama de pruebas de Neon. Cada prueba se revierte al terminar.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "KALA_TEST_DB_URL", matches = ".+")
class AyudaRepositoryTest {

    @Autowired
    private ContactoAyudaRepository contactos;
    @Autowired
    private SolicitudAyudaRepository solicitudes;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager em;

    // Dos empresas con un equipo y un empleado cada una; devuelve {empresaA, usuarioA, empresaB, usuarioB}.
    private long[] dosEmpresasConEmpleado() {
        long[] r = new long[4];
        for (int i = 0; i < 2; i++) {
            long empresa = FilasPadre.empresa(jdbc);
            long equipo = FilasPadre.equipo(jdbc, empresa);
            long usuario = FilasPadre.usuario(jdbc);
            jdbc.update("INSERT INTO empleado_equipo (usuario_id, equipo_id) VALUES (?, ?)", usuario, equipo);
            r[i * 2] = empresa;
            r[i * 2 + 1] = usuario;
        }
        return r;
    }

    @Test
    void elContactoDeOtraEmpresaNoSeEncuentra() {
        long[] d = dosEmpresasConEmpleado();
        ContactoAyuda c = contactos.save(new ContactoAyuda(d[0], "Dra. Ruiz", "PSICOLOGO", "999", null));

        assertThat(contactos.findByIdAndEmpresaId(c.getId(), d[0])).isPresent();
        assertThat(contactos.findByIdAndEmpresaId(c.getId(), d[2])).isEmpty();
    }

    @Test
    void laBaseExigeUnMedioDeContacto_yUnTipoValido() {
        long[] d = dosEmpresasConEmpleado();

        assertThatThrownBy(() -> {
            contactos.saveAndFlush(new ContactoAyuda(d[0], "Sin medio", "OTRO", null, null));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lasSolicitudesSeAislanPorEmpresa() {
        long[] d = dosEmpresasConEmpleado();
        SolicitudAyuda deA = solicitudes.save(new SolicitudAyuda(d[1], "ayuda A"));
        SolicitudAyuda deB = solicitudes.save(new SolicitudAyuda(d[3], "ayuda B"));
        em.flush();
        em.clear();

        assertThat(solicitudes.listarDeEmpresa(d[0])).extracting(SolicitudAyuda::getId).containsExactly(deA.getId());
        assertThat(solicitudes.buscarDeEmpresa(deB.getId(), d[0])).isEmpty();
        assertThat(solicitudes.buscarDeEmpresa(deB.getId(), d[2])).isPresent();
        assertThat(solicitudes.findByUsuarioIdOrderByCreadoEnDesc(d[1])).hasSize(1);
    }

    @Test
    void atenderPersisteElEstado_yLaBaseRechazaUnEstadoInvalido() {
        long[] d = dosEmpresasConEmpleado();
        SolicitudAyuda s = solicitudes.saveAndFlush(new SolicitudAyuda(d[1], null));

        assertThatThrownBy(() -> jdbc.update("UPDATE solicitud_ayuda SET estado = 'INVENTADO' WHERE id = ?", s.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // R8: el canal de ayuda no puede enlazar a una persona con una respuesta del check-in.
    @Test
    void solicitudAyudaNoTieneNingunaClaveForaneaHaciaElCheckin() {
        List<String> destinos = jdbc.queryForList("""
                SELECT DISTINCT ccu.table_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name
                WHERE tc.table_name IN ('solicitud_ayuda', 'contacto_ayuda') AND tc.constraint_type = 'FOREIGN KEY'
                """, String.class);

        assertThat(destinos).doesNotContain("agregado_item", "sesion_checkin");
        assertThat(destinos).contains("empleado_equipo", "contacto_ayuda", "personal_empresa", "empresa");
    }
}
