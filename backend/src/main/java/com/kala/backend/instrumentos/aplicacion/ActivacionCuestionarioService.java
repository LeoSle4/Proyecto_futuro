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
import com.kala.backend.instrumentos.dominio.PresupuestoPrivacidad;
import com.kala.backend.organizacion.aplicacion.puerto.Empresas;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import com.kala.backend.privacidad.dominio.CalculadoraPresupuesto;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // El presupuesto vigente del mes y lo que gastaría hoy una persona con los cuestionarios activos.
    public record ResultadoPresupuesto(Long empresaId, String periodo, double epsilonMaximo, double epsilonMensual) {
    }

    // ε máximo por persona que se admite fijar: la columna es NUMERIC(6,3).
    private static final BigDecimal EPSILON_MAXIMO_ADMITIDO = new BigDecimal("999.999");

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
        verificarEmpresa(empresaId);
        Cuestionario nuevo = cuestionarios.porId(cuestionarioId)
                .orElseThrow(() -> new CuestionarioNoEncontradoException(cuestionarioId));

        double total = gastoMensualSin(empresaId, cuestionarioId) + epsilonMensualDe(nuevo);
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

    // Apaga un cuestionario. Es idempotente: si nunca estuvo activo no escribe nada. Devuelve el gasto que queda.
    @Transactional
    public ResultadoActivacion desactivar(Long empresaId, Long cuestionarioId) {
        verificarEmpresa(empresaId);
        cuestionarios.porId(cuestionarioId).orElseThrow(() -> new CuestionarioNoEncontradoException(cuestionarioId));

        activaciones.porId(empresaId, cuestionarioId).filter(EmpresaCuestionario::isActivo).ifPresent(activacion -> {
            activacion.setActivo(false);
            activaciones.guardar(activacion);
        });

        double gasto = gastoMensualSin(empresaId, cuestionarioId);
        return new ResultadoActivacion(empresaId, cuestionarioId, gasto, epsilonMaximo(empresaId));
    }

    @Transactional(readOnly = true)
    public ResultadoPresupuesto consultarPresupuesto(Long empresaId) {
        verificarEmpresa(empresaId);
        return new ResultadoPresupuesto(empresaId, periodoActual(), epsilonMaximo(empresaId), gastoMensualSin(empresaId, null));
    }

    // Fija el presupuesto del mes actual; sigue vigente los meses siguientes. No se admite uno menor que lo que ya
    // gastan los cuestionarios activos: primero habría que desactivar alguno.
    @Transactional
    public ResultadoPresupuesto fijarPresupuesto(Long empresaId, BigDecimal epsilonMaximoPersona) {
        verificarEmpresa(empresaId);
        if (epsilonMaximoPersona == null || epsilonMaximoPersona.signum() <= 0
                || epsilonMaximoPersona.compareTo(EPSILON_MAXIMO_ADMITIDO) > 0) {
            throw new IllegalArgumentException("El presupuesto debe ser mayor que 0 y no superar " + EPSILON_MAXIMO_ADMITIDO);
        }
        BigDecimal maximo = epsilonMaximoPersona.setScale(3, RoundingMode.HALF_UP);
        if (maximo.signum() <= 0) {
            throw new IllegalArgumentException("El presupuesto debe ser mayor que 0 y no superar " + EPSILON_MAXIMO_ADMITIDO);
        }
        double gasto = gastoMensualSin(empresaId, null);
        if (gasto > maximo.doubleValue()) {
            throw new PresupuestoExcedidoException(gasto, maximo.doubleValue());
        }
        String periodo = periodoActual();
        presupuestos.guardar(new PresupuestoPrivacidad(empresaId, periodo, maximo));
        return new ResultadoPresupuesto(empresaId, periodo, maximo.doubleValue(), gasto);
    }

    private void verificarEmpresa(Long empresaId) {
        if (!empresas.existe(empresaId)) {
            throw new EmpresaNoEncontradaException(empresaId);
        }
    }

    // Lo que gasta una persona al mes con todo lo activo, sin contar el cuestionario indicado (puede ser null).
    private double gastoMensualSin(Long empresaId, Long excluidoId) {
        return cuestionarios.activosDeEmpresa(empresaId).stream()
                .filter(activo -> !activo.getId().equals(excluidoId))
                .mapToDouble(this::epsilonMensualDe)
                .sum();
    }

    private String periodoActual() {
        return YearMonth.now(reloj).toString();
    }

    private double epsilonMaximo(Long empresaId) {
        return presupuestos.vigenteEn(empresaId, periodoActual())
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
