package com.kala.backend.dashboard.aplicacion;

import com.kala.backend.dashboard.aplicacion.puerto.AuditoriaDashboard;
import com.kala.backend.dashboard.aplicacion.puerto.LecturaDashboard.FilaItem;
import com.kala.backend.dashboard.aplicacion.puerto.LecturaDashboard;
import com.kala.backend.dashboard.dominio.ConsultaDashboard;
import com.kala.backend.dashboard.dominio.SemanaInvalidaException;
import com.kala.backend.organizacion.dominio.EquipoNoEncontradoException;
import com.kala.backend.privacidad.dominio.MecanismosLdp;
import com.kala.backend.privacidad.dominio.ParametrosLdp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    public static final String OK = "OK";
    public static final String DATOS_INSUFICIENTES = "DATOS_INSUFICIENTES";

    // Estimación de un ítem. Randomized Response llena proporcionEstimada (0 a 1); Laplace llena mediaEstimada
    // (en la escala del ítem). El estimado y el margen son nulos si el ítem no alcanza el n mínimo.
    public record ItemEstimado(Long itemId, String tema, Double proporcionEstimada, Double margenError95,
                               Double mediaEstimada) {

        public ItemEstimado(Long itemId, String tema, Double proporcionEstimada, Double margenError95) {
            this(itemId, tema, proporcionEstimada, margenError95, null);
        }
    }

    // totalRespuestas es el mayor n entre los ítems de la ventana (0 si no hay respuestas).
    public record ResultadoDashboard(Long equipoId, LocalDate semanaInicio, String resultado,
                                     long totalRespuestas, List<ItemEstimado> items) {
    }

    private final LecturaDashboard repositorio;
    private final AuditoriaDashboard auditoria;
    private final int nMinimo;

    public DashboardService(
            LecturaDashboard repositorio,
            AuditoriaDashboard auditoria,
            @Value("${kala.privacidad.n-minimo}") int nMinimo) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
        this.nMinimo = nMinimo;
    }

    // Una sola ventana semanal. Toda consulta válida queda auditada en la misma transacción, con datos
    // suficientes o sin ellos. Una semana inválida o un equipo ajeno se rechazan antes de auditar.
    @Transactional
    public ResultadoDashboard consultar(Long usuarioId, Long empresaId, Long equipoId, LocalDate semana) {
        if (semana.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new SemanaInvalidaException(semana);
        }
        if (!repositorio.equipoPerteneceAEmpresa(equipoId, empresaId)) {
            throw new EquipoNoEncontradoException(equipoId);
        }

        LocalDate hasta = semana.plusDays(6);
        List<FilaItem> filas = repositorio.agregadosDeVentana(equipoId, semana, hasta);

        List<ItemEstimado> items = filas.stream().map(this::estimar).toList();
        long total = filas.stream().mapToLong(FilaItem::n).max().orElse(0L);
        boolean hayDatosSuficientes = filas.stream().anyMatch(f -> f.n() >= nMinimo);
        String resultado = hayDatosSuficientes ? OK : DATOS_INSUFICIENTES;

        auditoria.guardar(new ConsultaDashboard(usuarioId, equipoId, semana, hasta, resultado));
        return new ResultadoDashboard(equipoId, semana, resultado, total, items);
    }

    private ItemEstimado estimar(FilaItem fila) {
        if (fila.n() < nMinimo) {
            return new ItemEstimado(fila.itemId(), fila.tema(), null, null);
        }
        ParametrosLdp parametros = new ParametrosLdp(fila.probabilidadVerdad(), fila.epsilon());
        return MecanismosLdp.buscar(fila.mecanismo())
                .flatMap(m -> m.estimar(parametros, fila.valorMin(), fila.valorMax(), fila.suma(), fila.n()))
                .map(e -> new ItemEstimado(fila.itemId(), fila.tema(), e.proporcionEstimada(), e.margenError95(),
                        e.mediaEstimada()))
                .orElseGet(() -> new ItemEstimado(fila.itemId(), fila.tema(), null, null));
    }
}
