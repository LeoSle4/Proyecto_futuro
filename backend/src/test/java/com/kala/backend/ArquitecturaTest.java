package com.kala.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

// Reglas de la arquitectura modular con capas hexagonales. Lee los imports del código fuente (sin dependencias
// nuevas) y falla con la lista de infractores. Cada módulo tiene tres capas:
//   dominio          reglas y modelo; no conoce a nadie hacia afuera
//   aplicacion       casos de uso y puertos (aplicacion.puerto); solo conoce el dominio y los puertos
//   infraestructura  controladores, JPA, JDBC y configuración; implementa los puertos
// Los módulos se hablan solo a través de dominio y aplicacion.puerto (y del núcleo puro privacidad.dominio).
class ArquitecturaTest {

    private static final Path FUENTES = Path.of("src/main/java/com/kala/backend");
    private static final Pattern IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+?)(?:\\.\\*)?;", Pattern.MULTILINE);
    private static final Pattern PAQUETE_PROPIO = Pattern.compile(
            "^com\\.kala\\.backend\\.(\\w+)\\.(dominio|aplicacion|infraestructura)((?:\\.\\w+)*)$");

    private record Archivo(Path ruta, String modulo, String capa, String subpaquete, List<String> imports) {
        String nombre() {
            return FUENTES.relativize(ruta).toString();
        }
    }

    private List<Archivo> archivos() throws IOException {
        List<Archivo> resultado = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(FUENTES)) {
            for (Path ruta : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String relativa = FUENTES.relativize(ruta).toString().replace('\\', '/');
                String[] partes = relativa.split("/");
                if (partes.length < 3) {
                    continue; // BackendApplication, en la raíz
                }
                String texto = Files.readString(ruta);
                List<String> imports = new ArrayList<>();
                Matcher m = IMPORT.matcher(texto);
                while (m.find()) {
                    imports.add(m.group(1));
                }
                String sub = partes.length > 3 ? partes[2] : "";
                resultado.add(new Archivo(ruta, partes[0], partes[1], sub, imports));
            }
        }
        assertThat(resultado).as("archivos fuente encontrados").hasSizeGreaterThan(100);
        return resultado;
    }

    private void sinInfractores(String regla, List<String> infractores) {
        assertThat(infractores).as(regla).isEmpty();
    }

    @Test
    void elNucleoDePrivacidadEsPuro() throws IOException {
        List<String> infractores = new ArrayList<>();
        for (Archivo a : archivos()) {
            if (!a.modulo().equals("privacidad") || !a.capa().equals("dominio")) {
                continue;
            }
            for (String i : a.imports()) {
                if (i.startsWith("org.springframework") || i.startsWith("jakarta.") || i.startsWith("com.kala.backend.")) {
                    infractores.add(a.nombre() + " importa " + i);
                }
            }
        }
        sinInfractores("privacidad.dominio no puede importar Spring, Jakarta ni otros módulos", infractores);
    }

    @Test
    void elDominioNoConoceAplicacionNiInfraestructuraNiFrameworksWeb() throws IOException {
        List<String> infractores = new ArrayList<>();
        for (Archivo a : archivos()) {
            if (!a.capa().equals("dominio")) {
                continue;
            }
            for (String i : a.imports()) {
                Matcher m = PAQUETE_PROPIO.matcher(i.replaceAll("\\.[A-Z]\\w*(\\.[A-Z]\\w*)*$", ""));
                boolean haciaAdentro = m.matches() && !m.group(2).equals("dominio");
                boolean framework = i.startsWith("org.springframework.data") || i.startsWith("org.springframework.jdbc")
                        || i.startsWith("org.springframework.security")
                        || (i.startsWith("org.springframework.web")
                        && !i.equals("org.springframework.web.bind.annotation.ResponseStatus"));
                if (haciaAdentro || framework) {
                    infractores.add(a.nombre() + " importa " + i);
                }
            }
        }
        sinInfractores("el dominio solo depende de dominio (y de @ResponseStatus en sus excepciones)", infractores);
    }

    @Test
    void laAplicacionNoDependeDeInfraestructuraNiDeFrameworksDePersistenciaOWeb() throws IOException {
        List<String> infractores = new ArrayList<>();
        for (Archivo a : archivos()) {
            if (!a.capa().equals("aplicacion")) {
                continue;
            }
            for (String i : a.imports()) {
                boolean infra = i.contains(".infraestructura.");
                boolean framework = i.startsWith("org.springframework.data") || i.startsWith("org.springframework.jdbc")
                        || i.startsWith("org.springframework.web") || i.startsWith("org.springframework.http")
                        || i.startsWith("jakarta.persistence");
                if (infra || framework) {
                    infractores.add(a.nombre() + " importa " + i);
                }
            }
        }
        sinInfractores("aplicacion no puede importar infraestructura, Spring Data/JDBC/Web ni JPA", infractores);
    }

    @Test
    void losModulosSoloSeHablanPorDominioYPuertos() throws IOException {
        List<String> infractores = new ArrayList<>();
        for (Archivo a : archivos()) {
            if (a.modulo().equals("compartido")) {
                continue;
            }
            for (String i : a.imports()) {
                Matcher m = PAQUETE_PROPIO.matcher(i.replaceAll("\\.[A-Z]\\w*(\\.[A-Z]\\w*)*$", ""));
                if (!m.matches() || m.group(1).equals(a.modulo())) {
                    continue;
                }
                String otraCapa = m.group(2);
                String otroSub = m.group(3);
                boolean permitido = otraCapa.equals("dominio")
                        || (otraCapa.equals("aplicacion") && otroSub.equals(".puerto"));
                if (!permitido) {
                    infractores.add(a.nombre() + " importa " + i);
                }
            }
        }
        sinInfractores("un módulo solo importa el dominio o los puertos de otro módulo", infractores);
    }
}
