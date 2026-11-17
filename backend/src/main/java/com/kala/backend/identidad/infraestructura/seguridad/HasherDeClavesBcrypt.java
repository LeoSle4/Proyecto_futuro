package com.kala.backend.identidad.infraestructura.seguridad;

import com.kala.backend.identidad.aplicacion.puerto.HasherDeClaves;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Adaptador de HasherDeClaves sobre el PasswordEncoder (BCrypt) de la aplicación.
@Component
public class HasherDeClavesBcrypt implements HasherDeClaves {

    private final PasswordEncoder encoder;

    public HasherDeClavesBcrypt(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String cifrar(String clave) {
        return encoder.encode(clave);
    }

    @Override
    public boolean coincide(String clave, String hash) {
        return encoder.matches(clave, hash);
    }
}
