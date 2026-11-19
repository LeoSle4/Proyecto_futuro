package com.kala.backend.instrumentos.aplicacion;

import com.kala.backend.checkin.aplicacion.puerto.SesionesCheckin;
import com.kala.backend.privacidad.dominio.SesionHasher;
import com.kala.backend.privacidad.dominio.VentanaSemanal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Los cuestionarios activos que esta persona todavía no respondió en la ventana actual. Usa la misma huella del
// antiduplicado que el check-in (HMAC de persona, cuestionario y ventana), así que lo que se oculta es exactamente
// lo que el check-in rechazaría con 409. Solo lee: no guarda nada nuevo ni relaciona a la persona con sus respuestas.
@Service
@Transactional(readOnly = true)
public class CuestionariosPorResponderService {

    private final CuestionarioConsultaService consulta;
    private final SesionesCheckin sesiones;
    private final String secreto;
    private final ZoneId zona;
    private final Clock reloj;

    @Autowired
    public CuestionariosPorResponderService(CuestionarioConsultaService consulta,
                                            SesionesCheckin sesiones,
                                            @Value("${kala.sesion.secreto}") String secreto,
                                            @Value("${kala.ventana.zona}") String zona) {
        this(consulta, sesiones, secreto, zona, Clock.systemUTC());
    }

    // Constructor para pruebas, con un reloj controlable.
    CuestionariosPorResponderService(CuestionarioConsultaService consulta,
                                     SesionesCheckin sesiones,
                                     String secreto,
                                     String zona,
                                     Clock reloj) {
        this.consulta = consulta;
        this.sesiones = sesiones;
        this.secreto = secreto;
        this.zona = ZoneId.of(zona);
        this.reloj = reloj;
    }

    public List<CuestionarioPendienteResponse> porResponder(Long empresaId, Long usuarioId) {
        Instant ahora = reloj.instant();
        return consulta.pendientes(empresaId).stream()
                .filter(c -> !yaRespondio(usuarioId, c, ahora))
                .toList();
    }

    private boolean yaRespondio(Long usuarioId, CuestionarioPendienteResponse c, Instant ahora) {
        LocalDate ventana = VentanaSemanal.inicioDeVentana(ahora, zona, c.cadenciaDias());
        return sesiones.existe(SesionHasher.hashDeSesion(secreto, usuarioId, c.id(), ventana));
    }
}
