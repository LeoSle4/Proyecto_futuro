
package com.kala.backend.organizacion.aplicacion;

import com.kala.backend.organizacion.aplicacion.puerto.Equipos;
import com.kala.backend.organizacion.dominio.Equipo;
import com.kala.backend.organizacion.dominio.EquipoNoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipoService {

    private final Equipos equipoRepository;
    private final EmpresaService empresaService;

    public EquipoService(
            Equipos equipoRepository,
            EmpresaService empresaService) {
        this.equipoRepository = equipoRepository;
        this.empresaService = empresaService;
    }

    @Transactional(readOnly = true)
    public List<Equipo> listarPorEmpresa(Long empresaId) {
        if (empresaId == null) {
            return equipoRepository.todos();
        }

        return equipoRepository.deEmpresa(empresaId);
    }

    @Transactional(readOnly = true)
    public Equipo buscarPorId(Long id) {
        return equipoRepository.porId(id)
                .orElseThrow(() -> new EquipoNoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public Equipo buscarPorIdEnEmpresa(Long id, Long empresaId) {
        return equipoRepository.porIdEnEmpresa(id, empresaId)
                .orElseThrow(() -> new EquipoNoEncontradoException(id));
    }

    @Transactional
    public Equipo crear(EquipoRequest request) {
        validar(request);

        Equipo equipo = new Equipo(
                request.nombre(),
                request.empresaId()
        );

        return equipoRepository.guardar(equipo);
    }

    @Transactional
    public Equipo actualizar(Long id, EquipoRequest request) {
        validar(request);

        Equipo equipo = buscarPorId(id);
        equipo.setNombre(request.nombre());
        equipo.setEmpresaId(request.empresaId());

        return equipoRepository.guardar(equipo);
    }

    @Transactional
    public Equipo actualizarEnEmpresa(Long id, EquipoRequest request, Long empresaId) {
        if (!empresaId.equals(request.empresaId())) {
            throw new EquipoNoEncontradoException(id);
        }

        validar(request);
        Equipo equipo = buscarPorIdEnEmpresa(id, empresaId);
        equipo.setNombre(request.nombre());
        return equipoRepository.guardar(equipo);
    }

    @Transactional
    public void eliminar(Long id) {
        Equipo equipo = buscarPorId(id);
        equipoRepository.eliminar(equipo);
    }

    @Transactional
    public void eliminarEnEmpresa(Long id, Long empresaId) {
        Equipo equipo = buscarPorIdEnEmpresa(id, empresaId);
        equipoRepository.eliminar(equipo);
    }

    private void validar(EquipoRequest request) {
        if (request.empresaId() == null) {
            throw new IllegalArgumentException(
                    "empresaId es obligatorio"
            );
        }

        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del equipo es obligatorio"
            );
        }

        empresaService.buscarPorId(request.empresaId());
    }
}
