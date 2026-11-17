-- Baterias WHO-5, PHQ-8 y GAD-7 con Laplace (epsilon por item = 1). Con k = 1 + 2 + 1 y cadencia quincenal el gasto
-- es 3 x 4 x 1 = 12 por persona al mes; con el pulso semanal (5.5) suma 17.5, dentro del maximo de 20.

INSERT INTO escala_respuesta (codigo, valor_min, valor_max) VALUES ('FRECUENCIA_0_3', 0, 3);
INSERT INTO escala_respuesta (codigo, valor_min, valor_max) VALUES ('WHO5_0_5', 0, 5);

INSERT INTO opcion_escala (escala_id, valor, etiqueta)
SELECT e.id, o.valor, o.etiqueta
FROM escala_respuesta e,
     (VALUES (0, 'Nunca'), (1, 'Varios días'), (2, 'Más de la mitad de los días'), (3, 'Casi todos los días'))
         AS o(valor, etiqueta)
WHERE e.codigo = 'FRECUENCIA_0_3';

INSERT INTO opcion_escala (escala_id, valor, etiqueta)
SELECT e.id, o.valor, o.etiqueta
FROM escala_respuesta e,
     (VALUES (0, 'Nunca'), (1, 'De vez en cuando'), (2, 'Menos de la mitad del tiempo'),
             (3, 'Más de la mitad del tiempo'), (4, 'La mayor parte del tiempo'), (5, 'Todo el tiempo'))
         AS o(valor, etiqueta)
WHERE e.codigo = 'WHO5_0_5';

INSERT INTO configuracion_ldp (mecanismo, epsilon) VALUES ('LAPLACE', 1.000);

INSERT INTO cuestionario (codigo, nombre, cadencia_dias, items_por_envio) VALUES
    ('WHO5', 'Índice de bienestar WHO-5', 14, 1),
    ('PHQ8', 'Cuestionario de salud del paciente PHQ-8', 14, 2),
    ('GAD7', 'Escala de ansiedad generalizada GAD-7', 14, 1);

INSERT INTO item_cuestionario (cuestionario_id, orden, tema, texto, escala_id, configuracion_ldp_id)
SELECT c.id, i.orden, i.tema, i.texto, e.id, l.id
FROM cuestionario c
         JOIN (VALUES
    ('WHO5', 1, 'Ánimo', 'Me he sentido alegre y de buen humor'),
    ('WHO5', 2, 'Calma', 'Me he sentido tranquilo(a) y relajado(a)'),
    ('WHO5', 3, 'Energía', 'Me he sentido activo(a) y enérgico(a)'),
    ('WHO5', 4, 'Descanso', 'Me he despertado sintiéndome fresco(a) y descansado(a)'),
    ('WHO5', 5, 'Interés', 'Mi vida cotidiana ha estado llena de cosas que me interesan')
) AS i(cuestionario, orden, tema, texto) ON i.cuestionario = c.codigo
         JOIN escala_respuesta e ON e.codigo = 'WHO5_0_5'
         JOIN configuracion_ldp l ON l.mecanismo = 'LAPLACE';

INSERT INTO item_cuestionario (cuestionario_id, orden, tema, texto, escala_id, configuracion_ldp_id)
SELECT c.id, i.orden, i.tema, i.texto, e.id, l.id
FROM cuestionario c
         JOIN (VALUES
    ('PHQ8', 1, 'Interés', 'Poco interés o placer en hacer cosas'),
    ('PHQ8', 2, 'Ánimo decaído', 'Se ha sentido decaído(a), deprimido(a) o sin esperanzas'),
    ('PHQ8', 3, 'Sueño', 'Dificultad para quedarse o permanecer dormido(a), o ha dormido demasiado'),
    ('PHQ8', 4, 'Energía', 'Se ha sentido cansado(a) o con poca energía'),
    ('PHQ8', 5, 'Apetito', 'Sin apetito o ha comido en exceso'),
    ('PHQ8', 6, 'Autoestima', 'Se ha sentido mal con usted mismo(a), o que es un fracaso o que ha quedado mal con usted mismo(a) o con su familia'),
    ('PHQ8', 7, 'Concentración', 'Dificultad para concentrarse en cosas como leer el periódico o ver televisión'),
    ('PHQ8', 8, 'Ritmo', 'Se ha estado moviendo o hablando tan lentamente que otras personas podrían notarlo, o por el contrario, muy inquieto(a) o agitado(a)')
) AS i(cuestionario, orden, tema, texto) ON i.cuestionario = c.codigo
         JOIN escala_respuesta e ON e.codigo = 'FRECUENCIA_0_3'
         JOIN configuracion_ldp l ON l.mecanismo = 'LAPLACE';

INSERT INTO item_cuestionario (cuestionario_id, orden, tema, texto, escala_id, configuracion_ldp_id)
SELECT c.id, i.orden, i.tema, i.texto, e.id, l.id
FROM cuestionario c
         JOIN (VALUES
    ('GAD7', 1, 'Nerviosismo', 'Se ha sentido nervioso(a), intranquilo(a) o con los nervios de punta'),
    ('GAD7', 2, 'Control de la preocupación', 'No ha podido dejar de preocuparse o controlar la preocupación'),
    ('GAD7', 3, 'Preocupación excesiva', 'Se ha preocupado demasiado por diferentes cosas'),
    ('GAD7', 4, 'Relajación', 'Ha tenido dificultad para relajarse'),
    ('GAD7', 5, 'Inquietud', 'Se ha sentido tan inquieto(a) que es difícil permanecer sentado(a) tranquilamente'),
    ('GAD7', 6, 'Irritabilidad', 'Se ha molestado o ha estado irritable fácilmente'),
    ('GAD7', 7, 'Miedo', 'Ha sentido miedo como si algo terrible pudiera pasar')
) AS i(cuestionario, orden, tema, texto) ON i.cuestionario = c.codigo
         JOIN escala_respuesta e ON e.codigo = 'FRECUENCIA_0_3'
         JOIN configuracion_ldp l ON l.mecanismo = 'LAPLACE';
