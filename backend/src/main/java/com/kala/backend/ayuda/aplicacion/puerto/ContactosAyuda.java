package com.kala.backend.ayuda.aplicacion.puerto;

import com.kala.backend.ayuda.dominio.ContactoAyuda;
import java.util.List;
import java.util.Optional;

// Puerto de salida: contactos de ayuda curados por cada empresa.
public interface ContactosAyuda {

    List<ContactoAyuda> activosDeEmpresa(Long empresaId);

    List<ContactoAyuda> deEmpresa(Long empresaId);

    // Un contacto de otra empresa no se encuentra: el aislamiento por empresa vive en esta consulta.
    Optional<ContactoAyuda> buscarDeEmpresa(Long id, Long empresaId);

    ContactoAyuda guardar(ContactoAyuda contacto);
}
