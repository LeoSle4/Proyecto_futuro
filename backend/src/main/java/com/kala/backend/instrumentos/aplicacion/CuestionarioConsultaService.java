package com.kala.backend.instrumentos.aplicacion;

import com.kala.backend.instrumentos.aplicacion.CuestionarioPendienteResponse.Escala;
import com.kala.backend.instrumentos.aplicacion.CuestionarioPendienteResponse.ItemPendiente;
import com.kala.backend.instrumentos.aplicacion.CuestionarioPendienteResponse.Opcion;
import com.kala.backend.instrumentos.aplicacion.puerto.ConfiguracionesLdp;
import com.kala.backend.instrumentos.aplicacion.puerto.Cuestionarios;
import com.kala.backend.instrumentos.aplicacion.puerto.EscalasRespuesta;
import com.kala.backend.instrumentos.aplicacion.puerto.ItemsDeCuestionario;
import com.kala.backend.instrumentos.aplicacion.puerto.OpcionesEscala;
import com.kala.backend.instrumentos.dominio.ConfiguracionLdp;
import com.kala.backend.instrumentos.dominio.Cuestionario;
import com.kala.backend.instrumentos.dominio.EscalaRespuesta;
import com.kala.backend.instrumentos.dominio.ItemCuestionario;
import com.kala.backend.organizacion.aplicacion.puerto.Empresas;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CuestionarioConsultaService {

    private final Empresas empresas;
    private final Cuestionarios cuestionarios;
    private final ItemsDeCuestionario items;
    private final EscalasRespuesta escalas;
    private final OpcionesEscala opciones;
    private final ConfiguracionesLdp configuraciones;

    public CuestionarioConsultaService(
            Empresas empresas,
            Cuestionarios cuestionarios,
            ItemsDeCuestionario items,
            EscalasRespuesta escalas,
            OpcionesEscala opciones,
            ConfiguracionesLdp configuraciones) {
        this.empresas = empresas;
        this.cuestionarios = cuestionarios;
        this.items = items;
        this.escalas = escalas;
        this.opciones = opciones;
        this.configuraciones = configuraciones;
    }

    public List<Cuestionario> listarActivos(Long empresaId) {
        verificarEmpresa(empresaId);
        return cuestionarios.activosDeEmpresa(empresaId);
    }

    // Cuestionarios activos de la empresa con sus ítems, escala y configuración LDP.
    public List<CuestionarioPendienteResponse> pendientes(Long empresaId) {
        verificarEmpresa(empresaId);
        return cuestionarios.activosDeEmpresa(empresaId).stream()
                .map(this::conItems)
                .toList();
    }

    private void verificarEmpresa(Long empresaId) {
        if (!empresas.existe(empresaId)) {
            throw new EmpresaNoEncontradaException(empresaId);
        }
    }

    private CuestionarioPendienteResponse conItems(Cuestionario c) {
        List<ItemPendiente> itemsDto = items.deCuestionario(c.getId()).stream()
                .map(this::itemPendiente)
                .toList();
        return new CuestionarioPendienteResponse(c.getId(), c.getCodigo(), c.getNombre(), c.getCadenciaDias(),
                c.getItemsPorEnvio() == null ? null : c.getItemsPorEnvio().intValue(), itemsDto);
    }

    private ItemPendiente itemPendiente(ItemCuestionario item) {
        EscalaRespuesta escala = escalas.porId(item.getEscalaId()).orElseThrow();
        ConfiguracionLdp config = configuraciones.porId(item.getConfiguracionLdpId()).orElseThrow();
        List<Opcion> opcionesDto = opciones.deEscala(escala.getId()).stream()
                .map(o -> new Opcion(o.getId().getValor(), o.getEtiqueta()))
                .toList();
        return new ItemPendiente(item.getId(), item.getOrden(), item.getTema(), item.getTexto(),
                new Escala(escala.getCodigo(), escala.getValorMin(), escala.getValorMax(), opcionesDto),
                config.getId(), config.getMecanismo(), config.getProbabilidadVerdad());
    }
}
