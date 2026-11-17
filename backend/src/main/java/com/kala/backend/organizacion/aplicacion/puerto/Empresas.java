package com.kala.backend.organizacion.aplicacion.puerto;

import com.kala.backend.organizacion.dominio.Empresa;
import java.util.List;
import java.util.Optional;

// Puerto de salida: las empresas (clientes) que conoce la aplicación.
public interface Empresas {

    List<Empresa> todas();

    Optional<Empresa> porId(Long id);

    boolean existe(Long id);

    // Busca la empresa y toma un bloqueo de escritura sobre su fila hasta que termine la transacción. Sirve para
    // serializar operaciones que deciden según el estado de la empresa (por ejemplo el presupuesto de privacidad).
    Optional<Empresa> bloquear(Long id);

    List<Empresa> buscarPorNombre(String texto);

    List<Empresa> buscarPorNombreEnEmpresa(Long empresaId, String texto);

    Empresa guardar(Empresa empresa);

    void eliminar(Empresa empresa);
}
