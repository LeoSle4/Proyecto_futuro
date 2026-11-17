
package com.kala.backend.organizacion.infraestructura.persistencia;

import com.kala.backend.organizacion.aplicacion.puerto.Empresas;
import com.kala.backend.organizacion.dominio.Empresa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmpresaRepository extends JpaRepository<Empresa, Long>, Empresas {

    @Query("""
            SELECT e
            FROM Empresa e
            WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
            ORDER BY e.nombre ASC
            """)
    List<Empresa> buscarPorNombre(@Param("texto") String texto);

    @Query("""
            SELECT e
            FROM Empresa e
            WHERE e.id = :empresaId
              AND LOWER(e.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
            ORDER BY e.nombre ASC
            """)
    List<Empresa> buscarPorNombreEnEmpresa(
            @Param("empresaId") Long empresaId,
            @Param("texto") String texto);

    @Override
    default List<Empresa> todas() {
        return findAll();
    }

    @Override
    default Optional<Empresa> porId(Long id) {
        return findById(id);
    }

    @Override
    default boolean existe(Long id) {
        return existsById(id);
    }

    @Override
    default Empresa guardar(Empresa empresa) {
        return save(empresa);
    }

    @Override
    default void eliminar(Empresa empresa) {
        delete(empresa);
    }
}
