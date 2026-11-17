package com.kala.backend.instrumentos.aplicacion;

import com.kala.backend.instrumentos.aplicacion.puerto.Activaciones;
import com.kala.backend.instrumentos.aplicacion.puerto.ConfiguracionesLdp;
import com.kala.backend.instrumentos.aplicacion.puerto.Cuestionarios;
import com.kala.backend.instrumentos.aplicacion.puerto.ItemsDeCuestionario;
import com.kala.backend.instrumentos.aplicacion.puerto.Presupuestos;
import com.kala.backend.instrumentos.dominio.ConfiguracionLdp;
import com.kala.backend.instrumentos.dominio.Cuestionario;
import com.kala.backend.instrumentos.dominio.CuestionarioNoEncontradoException;
import com.kala.backend.instrumentos.dominio.EmpresaCuestionario;
import com.kala.backend.instrumentos.dominio.ItemCuestionario;
import com.kala.backend.instrumentos.dominio.PresupuestoExcedidoException;
import com.kala.backend.organizacion.aplicacion.puerto.Empresas;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import com.kala.backend.privacidad.dominio.CalculadoraPresupuesto;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivacionCuestionarioService {

    // Resultado de una activación: lo que gastaría una persona al mes con todo lo activo, y el máximo vigente.
    public record ResultadoActivacion(Long empresaId, Long cuestionarioId, double epsilonMensual, double epsilonMaximo) {
    }

    private final CalculadoraPresupuesto calculadora = new CalculadoraPresupuesto();

    private final Empresas empresas;
    private final Cuestionarios cuestionarios;
    private final ItemsDeCuestionario items;
    private final ConfiguracionesLdp configuraciones;
    private final Activaciones activaciones;
    private final Presupuestos presupuestos;
    private final Clock reloj;
    private final double epsilonMaximoPorDefecto;

    public ActivacionCuestionarioService(
            Empresas empresas,
            Cuestionarios cuestionarios,
            ItemsDeCuestionario items,
            ConfiguracionesLdp configuraciones,
            Activaciones activaciones,
            Presupuestos presupuestos,
            Clock reloj,
            @Value("${kala.privacidad.epsilon-maximo-persona}") double epsilonMaximoPorDefecto) {
        this.empresas = empresas;
        this.cuestionarios = cuestionarios;
        this.items = items;
        this.configuraciones = configuraciones;
        this.activaciones = activaciones;
        this.presupuestos = presupuestos;
        this.reloj = reloj;
        this.epsilonMaximoPorDefecto = epsilonMaximoPorDefecto;
    }

    // Activa el cuestionario si el ε mensual por persona (lo ya activo más el nuevo) no excede el máximo del mes.
    // Si lo excede lanza PresupuestoExcedidoException y no se escribe nada.
    @Transactional
    public ResultadoActivacion activar(Long empresaId, Long cuestionarioId) {
        if (!empresas.existe(empresaId)) {
            throw new EmpresaNoEncontradaException(empresaId);
        }
        Cuestionario nuevo = cuestionarios.porId(cuestionarioId)
                .orElseThrow(() -> new CuestionarioNoEncontradoException(cuestionarioId));

        double gastoActual = cuestionarios.activosDeEmpresa(empresaId).stream()
                .filter(activo -> !activo.getId().equals(cuestionarioId))
                .mapToDouble(this::epsilonMensualDe)
                .sum();
        double total = gastoActual + epsilonMensualDe(nuevo);
        double maximo = epsilonMaximo(empresaId);

        if (total > maximo) {
            throw new PresupuestoExcedidoException(total, maximo);
        }

        EmpresaCuestionario activacion = activaciones.porId(empresaId, cuestionarioId)
                .orElseGet(() -> new EmpresaCuestionario(empresaId, cuestionarioId, true));
        activacion.setActivo(true);
        activaciones.guardar(activacion);

        return new ResultadoActivacion(empresaId, cuestionarioId, total, maximo);
    }

    private double epsilonMaximo(Long empresaId) {
        String periodo = YearMonth.now(reloj).toString();
        return presupuestos.deEmpresaYPeriodo(empresaId, periodo)
                .map(p -> p.getEpsilonMaximoPersona().doubleValue())
                .orElse(epsilonMaximoPorDefecto);
    }

    private double epsilonMensualDe(Cuestionario cuestionario) {
        List<Double> epsilons = items.deCuestionario(cuestionario.getId()).stream()
                .map(this::epsilonDe)
                .toList();
        Integer k = cuestionario.getItemsPorEnvio() != null ? cuestionario.getItemsPorEnvio().intValue() : null;
        return calculadora.epsilonMensual(cuestionario.getCadenciaDias(), k, epsilons);
    }

    private double epsilonDe(ItemCuestionario item) {
        ConfiguracionLdp config = configuraciones.porId(item.getConfiguracionLdpId())
                .orElseThrow(() -> new IllegalStateException(
                        "El ítem " + item.getId() + " referencia una configuración inexistente"));
        return calculadora.epsilonItem(config.getMecanismo(), config.getProbabilidadVerdad(), config.getEpsilon());
    }
}
