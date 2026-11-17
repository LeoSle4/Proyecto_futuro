package com.kala.backend.instrumentos.aplicacion.puerto;

import com.kala.backend.instrumentos.dominio.ItemCuestionario;
import java.util.List;

// Puerto de salida: los ítems de un cuestionario, en orden.
public interface ItemsDeCuestionario {

    List<ItemCuestionario> deCuestionario(Long cuestionarioId);
}
