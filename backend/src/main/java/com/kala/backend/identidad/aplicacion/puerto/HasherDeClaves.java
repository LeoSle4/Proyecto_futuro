package com.kala.backend.identidad.aplicacion.puerto;

// Puerto de salida: cifrado de claves. La aplicación nunca guarda ni registra una clave en claro.
public interface HasherDeClaves {

    String cifrar(String clave);

    boolean coincide(String clave, String hash);
}
