
package com.kala.backend.organizacion.aplicacion;

import com.kala.backend.organizacion.aplicacion.puerto.Empresas;
import com.kala.backend.organizacion.aplicacion.puerto.Equipos;
import com.kala.backend.organizacion.dominio.Empresa;
import com.kala.backend.organizacion.dominio.EmpresaConEquiposException;
import com.kala.backend.organizacion.dominio.EmpresaNoEncontradaException;
import com.kala.backend.organizacion.dominio.Equipo;
import com.kala.backend.organizacion.dominio.NombreDuplicadoException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaService {

    private final Empresas empresaRepository;
    private final Equipos equipoRepository;

    public EmpresaService(
            Empresas empresaRepository,
            Equipos equipoRepository) {
        this.empresaRepository = empresaRepository;
        this.equipoRepository = equipoRepository;
    }

    @Transactional(readOnly = true)
    public List<Empresa> listar() {
        return empresaRepository.todas();
    }

    @Transactional(readOnly = true)
    public Empresa buscarPorId(Long id) {
        return empresaRepository.porId(id)
                .orElseThrow(() -> new EmpresaNoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public List<Empresa> buscarPorNombre(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                    "El texto de búsqueda no puede estar vacío");
        }
        return empresaRepository.buscarPorNombre(texto.trim());
    }

    @Transactional(readOnly = true)
    public List<Empresa> buscarPorNombre(String texto, Long empresaId) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                    "El texto de búsqueda no puede estar vacío");
        }
        return empresaRepository.buscarPorNombreEnEmpresa(empresaId, texto.trim());
    }

    @Transactional
    public Empresa crear(EmpresaRequest request) {
        Empresa empresa = new Empresa(
                request.ruc(),
                request.nombre(),
                request.rubro()
        );
        return empresaRepository.guardar(empresa);
    }


@Transactional
public Empresa crearConEquipos(EmpresaConEquiposRequest request) {
    if (request == null) {
        throw new IllegalArgumentException(
                "Los datos de la empresa son obligatorios");
    }

    Empresa empresa = new Empresa(
            request.ruc(),
            request.nombre(),
            request.rubro()
    );

    Empresa guardada = empresaRepository.guardar(empresa);
    Set<String> nombres = new HashSet<>();

    for (String nombreOriginal : request.equipos()) {
        String nombre = nombreOriginal.trim();
        String nombreNormalizado = nombre.toLowerCase(Locale.ROOT);

        if (!nombres.add(nombreNormalizado)) {
            throw new NombreDuplicadoException(nombre);
        }

        Equipo equipo = new Equipo(nombre, guardada.getId());
        equipoRepository.guardar(equipo);
    }

    return guardada;
}


    @Transactional
    public Empresa actualizar(Long id, EmpresaRequest request) {
        Empresa empresa = buscarPorId(id);
        empresa.setRuc(request.ruc());
        empresa.setNombre(request.nombre());
        empresa.setRubro(request.rubro());
        return empresaRepository.guardar(empresa);
    }

    @Transactional
    public void eliminar(Long id) {
        Empresa empresa = buscarPorId(id);
        if (equipoRepository.tieneEquipos(id)) {
            throw new EmpresaConEquiposException(id);
        }
        empresaRepository.eliminar(empresa);
    }
}
