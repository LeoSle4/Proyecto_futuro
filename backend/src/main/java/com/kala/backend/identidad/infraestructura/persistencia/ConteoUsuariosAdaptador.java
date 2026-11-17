package com.kala.backend.identidad.infraestructura.persistencia;

import com.kala.backend.identidad.aplicacion.puerto.ConteoUsuarios;
import org.springframework.stereotype.Component;

// Adaptador de ConteoUsuarios sobre los repositorios de Spring Data de identidad.
@Component
public class ConteoUsuariosAdaptador implements ConteoUsuarios {

    private final UsuarioRepository usuarios;
    private final EmpleadoEquipoRepository empleados;
    private final PersonalEmpresaRepository personal;
    private final AdministradorPlataformaRepository administradores;

    public ConteoUsuariosAdaptador(UsuarioRepository usuarios, EmpleadoEquipoRepository empleados,
                                   PersonalEmpresaRepository personal,
                                   AdministradorPlataformaRepository administradores) {
        this.usuarios = usuarios;
        this.empleados = empleados;
        this.personal = personal;
        this.administradores = administradores;
    }

    @Override
    public long usuarios() {
        return usuarios.count();
    }

    @Override
    public long empleados() {
        return empleados.count();
    }

    @Override
    public long personal() {
        return personal.count();
    }

    @Override
    public long administradores() {
        return administradores.count();
    }
}
