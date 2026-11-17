package com.kala.backend.checkin.aplicacion;

import com.kala.backend.checkin.aplicacion.puerto.AgregadosCheckin;
import com.kala.backend.checkin.aplicacion.puerto.ItemsCuestionario.ItemCuestionarioInfo;
import com.kala.backend.checkin.aplicacion.puerto.ItemsCuestionario;
import com.kala.backend.checkin.aplicacion.puerto.SesionesCheckin;
import com.kala.backend.checkin.dominio.CheckInDuplicadoException;
import com.kala.backend.checkin.dominio.CheckInInvalidoException;
import com.kala.backend.checkin.dominio.ConfiguracionLdpNoVigenteException;
import com.kala.backend.checkin.dominio.ItemCuestionarioNoEncontradoException;
import com.kala.backend.privacidad.dominio.MecanismosLdp;
import com.kala.backend.privacidad.dominio.SesionHasher;
import com.kala.backend.privacidad.dominio.VentanaSemanal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Registra un check-in como una sola transacción: o se guardan la sesión y la suma, o no se guarda nada.
// El servidor recibe la respuesta ya perturbada en el dispositivo y solo la suma; no guarda quién respondió qué.
@Service
public class CheckInRegistroService {

    private final ItemsCuestionario items;
    private final SesionesCheckin sesiones;
    private final AgregadosCheckin agregados;
    private final String secreto;
    private final ZoneId zona;
    private final Clock reloj;

    @Autowired
    public CheckInRegistroService(ItemsCuestionario items,
                                  SesionesCheckin sesiones,
                                  AgregadosCheckin agregados,
                                  @Value("${kala.sesion.secreto}") String secreto,
                                  @Value("${kala.ventana.zona}") String zona) {
        this(items, sesiones, agregados, secreto, zona, Clock.systemUTC());
    }

    // Constructor para pruebas, con un reloj controlable.
    CheckInRegistroService(ItemsCuestionario items,
                           SesionesCheckin sesiones,
                           AgregadosCheckin agregados,
                           String secreto,
                           String zona,
                           Clock reloj) {
        this.items = items;
        this.sesiones = sesiones;
        this.agregados = agregados;
        this.secreto = secreto;
        this.zona = ZoneId.of(zona);
        this.reloj = reloj;
    }

    // Un ítem suelto (el pulso, o un cuestionario de k = 1) es un lote de uno.
    @Transactional
    public void registrar(Long usuarioId, Long equipoId, CheckInEnvioRequest request) {
        registrarLote(usuarioId, equipoId, List.of(request));
    }

    // Una batería llega como un lote de hasta k ítems de un mismo cuestionario: se valida todo antes de escribir,
    // se consume una sola sesión de antiduplicado y luego se suma cada ítem, todo en la misma transacción.
    @Transactional
    public void registrarLote(Long usuarioId, Long equipoId, List<CheckInEnvioRequest> respuestas) {
        Map<Long, ItemCuestionarioInfo> infos = new LinkedHashMap<>();
        for (CheckInEnvioRequest r : respuestas) {
            if (infos.containsKey(r.itemId())) {
                throw new CheckInInvalidoException("El envío repite un ítem");
            }
            ItemCuestionarioInfo item = items.buscar(r.itemId())
                    .orElseThrow(() -> new ItemCuestionarioNoEncontradoException(r.itemId()));
            if (!item.configuracionLdpId().equals(r.configuracionLdpId())) {
                throw new ConfiguracionLdpNoVigenteException(r.itemId());
            }
            infos.put(r.itemId(), item);
        }

        ItemCuestionarioInfo primero = infos.values().iterator().next();
        for (ItemCuestionarioInfo item : infos.values()) {
            if (!item.cuestionarioId().equals(primero.cuestionarioId())) {
                throw new CheckInInvalidoException("Todos los ítems de un envío deben ser del mismo cuestionario");
            }
        }
        if (respuestas.size() > primero.maximoPorEnvio()) {
            throw new CheckInInvalidoException("El envío trae más ítems que los permitidos por envío");
        }

        List<BigDecimal> sumas = respuestas.stream()
                .map(r -> aSuma(r, infos.get(r.itemId())))
                .toList();

        Instant ahora = reloj.instant();
        LocalDate ventana = VentanaSemanal.inicioDeVentana(ahora, zona, primero.cadenciaDias());
        String hash = SesionHasher.hashDeSesion(secreto, usuarioId, primero.cuestionarioId(), ventana);

        // 0 filas = el hash ya existía: esta persona ya respondió en esta ventana.
        if (sesiones.insertarSiNoExiste(hash, primero.cuestionarioId(), ventana) == 0) {
            throw new CheckInDuplicadoException();
        }

        // Solo fecha, nunca hora (R5): el agregado no permite correlacionar por tiempo.
        LocalDate hoy = ahora.atZone(zona).toLocalDate();
        for (int i = 0; i < respuestas.size(); i++) {
            CheckInEnvioRequest r = respuestas.get(i);
            agregados.registrarRespuesta(equipoId, r.itemId(), r.configuracionLdpId(), hoy, sumas.get(i));
        }
    }

    // Lo que se suma al agregado lo decide la estrategia del mecanismo del ítem (RR: 1 o 0; Laplace: el valor tal cual).
    private static BigDecimal aSuma(CheckInEnvioRequest r, ItemCuestionarioInfo item) {
        try {
            return MecanismosLdp.de(item.mecanismo()).aSuma(r.respuestaRuidosa(), r.valorRuidoso());
        } catch (IllegalArgumentException e) {
            throw new CheckInInvalidoException(e.getMessage());
        }
    }
}
