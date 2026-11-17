package com.kala.backend.identidad.aplicacion;

import com.kala.backend.identidad.dominio.DemasiadosIntentosException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

// Limita los inicios de sesión fallidos por correo: con maxFallos fallos dentro de la ventana, ese correo queda
// bloqueado durante el tiempo de bloqueo. Vive en memoria, así que vale para una sola instancia del servidor; con
// varias habría que moverlo a un almacén compartido. El bloqueo por correo (y no por IP) evita que un cliente detrás de
// un proxy bloquee a todos, a costa de que alguien pueda bloquear temporalmente la cuenta de otra persona.
public class LimitadorIntentosLogin {

    // Con tantas entradas se empieza a podar las vencidas, como mucho una vez por segundo para que un flujo de
    // fallos no obligue a recorrer la tabla en cada petición.
    private static final int PODA_DESDE = 10_000;
    // Tope duro: pasado este número no se rastrean correos nuevos (los ya rastreados siguen limitándose), así un flujo
    // de correos inventados no agota la memoria.
    private static final int MAX_ENTRADAS = 50_000;
    private static final int LARGO_MAXIMO_CLAVE = 254;

    private static final class Registro {
        final Deque<Instant> fallos = new ArrayDeque<>();
        Instant bloqueadoHasta;
    }

    private final int maxFallos;
    private final Duration ventana;
    private final Duration bloqueo;
    private final Clock reloj;
    private final ConcurrentHashMap<String, Registro> registros = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> ultimaPoda = new AtomicReference<>(Instant.MIN);

    public LimitadorIntentosLogin(int maxFallos, Duration ventana, Duration bloqueo, Clock reloj) {
        if (maxFallos < 1 || ventana.isZero() || ventana.isNegative() || bloqueo.isZero() || bloqueo.isNegative()) {
            throw new IllegalArgumentException("maxFallos, ventana y bloqueo deben ser positivos");
        }
        this.maxFallos = maxFallos;
        this.ventana = ventana;
        this.bloqueo = bloqueo;
        this.reloj = reloj;
    }

    // Lanza DemasiadosIntentosException si el correo está bloqueado.
    public void verificar(String correo) {
        String clave = normalizar(correo);
        Registro registro = clave.isEmpty() ? null : registros.get(clave);
        if (registro == null) {
            return;
        }
        Instant ahora = reloj.instant();
        synchronized (registro) {
            if (registro.bloqueadoHasta != null && ahora.isBefore(registro.bloqueadoHasta)) {
                throw new DemasiadosIntentosException(Math.max(1, Duration.between(ahora, registro.bloqueadoHasta).toSeconds()));
            }
        }
    }

    public void registrarFallo(String correo) {
        String clave = normalizar(correo);
        if (clave.isEmpty()) {
            return; // sin correo no hay a quién limitar (el cuerpo del login ya lo exige)
        }
        Instant ahora = reloj.instant();
        if (registros.size() >= PODA_DESDE) {
            Instant anterior = ultimaPoda.get();
            if (!ahora.isBefore(anterior.plusSeconds(1)) && ultimaPoda.compareAndSet(anterior, ahora)) {
                podar(ahora);
            }
        }
        if (registros.size() >= MAX_ENTRADAS && !registros.containsKey(clave)) {
            return;
        }
        Registro registro = registros.computeIfAbsent(clave, k -> new Registro());
        synchronized (registro) {
            if (registro.bloqueadoHasta != null && !ahora.isBefore(registro.bloqueadoHasta)) {
                registro.bloqueadoHasta = null;
                registro.fallos.clear();
            }
            while (!registro.fallos.isEmpty() && !registro.fallos.peekFirst().plus(ventana).isAfter(ahora)) {
                registro.fallos.pollFirst();
            }
            registro.fallos.addLast(ahora);
            if (registro.fallos.size() >= maxFallos) {
                registro.bloqueadoHasta = ahora.plus(bloqueo);
            }
        }
    }

    public void registrarExito(String correo) {
        registros.remove(normalizar(correo));
    }

    // Solo para pruebas y diagnóstico.
    int entradasEnMemoria() {
        return registros.size();
    }

    private void podar(Instant ahora) {
        registros.entrySet().removeIf(e -> {
            Registro r = e.getValue();
            synchronized (r) {
                boolean bloqueado = r.bloqueadoHasta != null && ahora.isBefore(r.bloqueadoHasta);
                boolean conFallosRecientes = !r.fallos.isEmpty() && r.fallos.peekLast().plus(ventana).isAfter(ahora);
                return !bloqueado && !conFallosRecientes;
            }
        });
    }

    // Las claves se recortan: un correo no pasa de 254 caracteres y no se guarda texto que elija quien ataca.
    private static String normalizar(String correo) {
        String limpio = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
        return limpio.length() > LARGO_MAXIMO_CLAVE ? limpio.substring(0, LARGO_MAXIMO_CLAVE) : limpio;
    }
}
