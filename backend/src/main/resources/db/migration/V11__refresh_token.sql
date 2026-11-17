-- Sesiones de larga duracion (refresh tokens). Solo se guarda el SHA-256 del token, nunca el token. Cada inicio de
-- sesion abre una "familia"; cada renovacion rota el token dentro de la familia. Reutilizar un token ya usado revoca
-- toda la familia. Vive en el modulo de identidad: no tiene relacion con las tablas del check-in.
CREATE TABLE refresh_token (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL,
    familia     VARCHAR(36) NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    creado_en   TIMESTAMP   NOT NULL,
    expira_en   TIMESTAMP   NOT NULL,
    usado_en    TIMESTAMP   NULL,
    revocado_en TIMESTAMP   NULL,
    CONSTRAINT uk_refresh_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE INDEX ix_refresh_usuario ON refresh_token (usuario_id);
CREATE INDEX ix_refresh_familia ON refresh_token (familia);
CREATE INDEX ix_refresh_expira ON refresh_token (expira_en);
