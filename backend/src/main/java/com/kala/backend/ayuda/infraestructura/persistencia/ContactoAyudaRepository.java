package com.kala.backend.ayuda.infraestructura.persistencia;

import com.kala.backend.ayuda.aplicacion.puerto.ContactosAyuda;
import com.kala.backend.ayuda.dominio.ContactoAyuda;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoAyudaRepository extends JpaRepository<ContactoAyuda, Long>, ContactosAyuda {

    List<ContactoAyuda> findByEmpresaIdOrderByNombre(Long empresaId);

    List<ContactoAyuda> findByEmpresaIdAndActivoTrueOrderByNombre(Long empresaId);

    // Un contacto de otra empresa no se encuentra: el aislamiento por tenant vive en la consulta.
    Optional<ContactoAyuda> findByIdAndEmpresaId(Long id, Long empresaId);

    @Override
    default List<ContactoAyuda> activosDeEmpresa(Long empresaId) {
        return findByEmpresaIdAndActivoTrueOrderByNombre(empresaId);
    }

    @Override
    default List<ContactoAyuda> deEmpresa(Long empresaId) {
        return findByEmpresaIdOrderByNombre(empresaId);
    }

    @Override
    default Optional<ContactoAyuda> buscarDeEmpresa(Long id, Long empresaId) {
        return findByIdAndEmpresaId(id, empresaId);
    }

    @Override
    default ContactoAyuda guardar(ContactoAyuda contacto) {
        return save(contacto);
    }
}
