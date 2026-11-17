package com.kala.backend.ayuda.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Contacto curado por la empresa (psicólogo, línea de crisis, EPS) al que se deriva una solicitud de ayuda.
@Entity
@Table(name = "contacto_ayuda")
public class ContactoAyuda {

    public static final java.util.Set<String> TIPOS = java.util.Set.of("PSICOLOGO", "LINEA_CRISIS", "EPS", "OTRO");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    protected ContactoAyuda() {
        // Requerido por JPA.
    }

    public ContactoAyuda(Long empresaId, String nombre, String tipo, String telefono, String email) {
        this.empresaId = empresaId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.telefono = telefono;
        this.email = email;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActivo() {
        return activo;
    }
}
