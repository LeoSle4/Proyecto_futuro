package com.kala.backend.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokenOpacoTest {

    @Test
    void generaTokensLargosYUrlSeguros() {
        String token = TokenOpaco.generar();

        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void nuncaRepiteUnToken() {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 5_000; i++) {
            assertThat(vistos.add(TokenOpaco.generar())).isTrue();
        }
    }

    @Test
    void elHashEsSha256EnHexadecimal_deterministaYDistintoDelToken() {
        String hash = TokenOpaco.hash("abc");

        assertThat(hash).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(TokenOpaco.hash("abc")).isEqualTo(hash);
        assertThat(TokenOpaco.hash("abd")).isNotEqualTo(hash);
    }
}
