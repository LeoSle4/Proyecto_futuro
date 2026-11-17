-- Canal "Hablar con alguien". Esta separado del check-in a proposito (R8): solicitud_ayuda apunta a empleado_equipo
-- porque la persona se identifica al pedir ayuda, y no tiene ningun camino de FK hacia agregado_item ni sesion_checkin.
CREATE TABLE contacto_ayuda (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id  BIGINT       NOT NULL,
    nombre      VARCHAR(150) NOT NULL,
    tipo        VARCHAR(20)  NOT NULL,
    telefono    VARCHAR(20)  NULL,
    email       VARCHAR(150) NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_contacto_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT ck_contacto_tipo CHECK (tipo IN ('PSICOLOGO', 'LINEA_CRISIS', 'EPS', 'OTRO')),
    CONSTRAINT ck_contacto_medio CHECK (telefono IS NOT NULL OR email IS NOT NULL)
);

CREATE INDEX ix_contacto_empresa ON contacto_ayuda (empresa_id);

CREATE TABLE solicitud_ayuda (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id    BIGINT      NOT NULL,
    contacto_id   BIGINT      NULL,
    mensaje       TEXT        NULL,
    estado        VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
    creado_en     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atendido_por  BIGINT      NULL,
    atendido_en   TIMESTAMP   NULL,
    CONSTRAINT fk_solicitud_usuario  FOREIGN KEY (usuario_id)   REFERENCES empleado_equipo(usuario_id),
    CONSTRAINT fk_solicitud_contacto FOREIGN KEY (contacto_id)  REFERENCES contacto_ayuda(id),
    CONSTRAINT fk_solicitud_atiende  FOREIGN KEY (atendido_por) REFERENCES personal_empresa(usuario_id),
    CONSTRAINT ck_solicitud_estado CHECK (estado IN ('PENDIENTE', 'EN_ATENCION', 'CERRADA'))
);

CREATE INDEX ix_solicitud_usuario ON solicitud_ayuda (usuario_id);
CREATE INDEX ix_solicitud_estado ON solicitud_ayuda (estado);
