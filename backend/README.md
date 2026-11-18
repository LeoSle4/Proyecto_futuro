# Kala — backend

API de bienestar corporativo con **privacidad diferencial local**. Cada persona responde en su dispositivo, la
respuesta se perturba **antes** de salir (Randomized Response para sí/no, Laplace para escalas) y el servidor solo
guarda sumas y hashes. RRHH ve estimaciones de un equipo, nunca la respuesta de una persona.

Stack: Java 25, Spring Boot 4.1, PostgreSQL (Neon) con Flyway, JWT.

## Arrancar

```bash
cd backend
export KALA_DB_URL=jdbc:postgresql://<host>/<base>?sslmode=require
export KALA_DB_USER=<usuario>
export KALA_DB_PASSWORD=<clave>
export KALA_SESSION_SECRET=<secreto del HMAC del antiduplicado>
export JWT_SECRET_BASE64=<clave de firma de los tokens, base64, 32 bytes o más>
export KALA_ADMIN_EMAIL=<correo del administrador inicial>
export KALA_ADMIN_PASSWORD=<clave de 12 caracteres o más>
./mvnw spring-boot:run
```

Escucha en el puerto `8090`. Flyway crea el esquema al arrancar. Si faltan `KALA_ADMIN_EMAIL` y
`KALA_ADMIN_PASSWORD` no se crea administrador. Nunca pongas secretos en el repositorio.

Salud: `GET /api/publico/estado`. Imagen: `docker build -t kala-backend .` (variables al ejecutarla, ver el `Dockerfile`).

## Probar

```bash
./mvnw test                                  # unitarias y de controladores; las de base de datos se omiten sin variables
KALA_TEST_DB_URL=... KALA_TEST_DB_USER=... KALA_TEST_DB_PASSWORD=... ./mvnw test   # todo, contra una rama de pruebas
KALA_DEMO_FINAL=1 ./mvnw test -Dtest=FlujoDemoFinalTest                            # guion de la demo final (~4 min)
```

Las pruebas con base de datos corren dentro de una transacción que se revierte, salvo las que verifican commits y
concurrencia (`RefrescoRevocacionPersistenteTest`, `PresupuestoConcurrenciaTest`), que escriben y limpian sus filas.
Tras editar archivos de prueba usa `./mvnw clean test-compile`: el build incremental puede ocultar errores.

## Arquitectura

Monolito modular con capas hexagonales por módulo: `organizacion`, `identidad`, `instrumentos`, `privacidad`, `checkin`,
`dashboard`, `ayuda` y `compartido`.

```
<modulo>/dominio           modelo (entidades JPA) y errores de negocio; algoritmos puros en privacidad
<modulo>/aplicacion        casos de uso; aplicacion/puerto = interfaces que la aplicación necesita
<modulo>/infraestructura   web (controladores y DTO), persistencia (Spring Data, JdbcClient), config, seguridad
```

- Los repositorios de Spring Data implementan los puertos con métodos `default`.
- `MecanismoLdp` (Strategy) decide cómo se suma, se estima y cuánto ε gasta cada mecanismo. Para agregar uno basta con una
  clase y registrarla en `MecanismosLdp`.
- Los módulos se hablan solo por `dominio` y `aplicacion.puerto`. `ArquitecturaTest` lo verifica leyendo los imports.

## Privacidad

| Regla | Cómo se cumple |
|---|---|
| El servidor no guarda quién respondió qué | `agregado_item` solo tiene sumas; no hay clave hacia `usuario` |
| Una respuesta por persona y ventana | `sesion_checkin` guarda un HMAC de un solo uso; ventana según la cadencia del cuestionario |
| Sin hora en las tablas del check-in | solo `DATE`, nunca `TIMESTAMP` |
| No se muestran grupos pequeños | `kala.privacidad.n-minimo` (30): menos respuestas devuelve `DATOS_INSUFICIENTES` |
| Presupuesto de ε por persona | se valida al activar un cuestionario (422 si lo excede); ver `PUT /api/empresas/{id}/presupuesto` |
| El canal de ayuda es aparte del check-in | `solicitud_ayuda` no tiene ninguna relación con las tablas del check-in |
| Retención | `sesion_checkin` se purga pasados 60 días (mínimo 30); los agregados no |

Mecanismos: Randomized Response (pulso, `p = 0.75`) y Laplace (WHO-5, PHQ-8 y GAD-7, ε = 1 por ítem). El dispositivo
manda `respuestaRuidosa` (sí/no) o `valorRuidoso` (número, sin recortar a la escala); el servidor valida que corresponda
al mecanismo del ítem y que esté dentro de la cota de la escala.

## Autenticación

- `POST /api/auth/login` devuelve un JWT de **30 minutos** (`accessToken`) y un `refreshToken` de **30 días**.
- `POST /api/auth/refresh` rota el refresh token y devuelve un JWT nuevo con la identidad actual. Reutilizar un token ya
  usado o revocado revoca toda su familia (401). **La app debe serializar sus renovaciones.**
- `POST /api/auth/logout` revoca la sesión. Cambiar la clave o desactivar la cuenta revoca todas las sesiones.
- Claims del JWT: `sub` (correo), `uid`, `roles` (`ROLE_ADMIN`, `ROLE_RRHH`, `ROLE_COORDINADOR_BIENESTAR`,
  `ROLE_EMPLEADO`), `empresaId` (no para ADMIN) y `equipoId` (solo empleados).
- Cinco inicios de sesión fallidos de un correo en 15 minutos lo bloquean 15 minutos (429 con `Retry-After`).
- Claves de 12 a 72 caracteres, guardadas con BCrypt. Los correos se normalizan en minúsculas.

## Endpoints

Todo requiere `Authorization: Bearer <accessToken>` salvo `login`, `refresh`, `logout` y `/api/publico/**`. La empresa y la
persona salen siempre del token; un recurso de otra empresa responde 404.

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `POST /api/auth/login` · `POST /api/auth/refresh` · `POST /api/auth/logout` | público | Sesión (ver arriba) |
| `GET /api/auth/me` | autenticado | Lo que dice el token de quien consulta |
| `GET /api/publico/estado` | público | Chequeo de salud |
| `PUT /api/usuarios/me/clave` | autenticado | Cambia la propia clave (exige la actual) |
| `POST /api/admin/empresas/{id}/personal` | ADMIN | Crea RRHH o coordinador en una empresa |
| `POST /api/empleados` | RRHH, coordinador | Crea un empleado en un equipo de su empresa |
| `GET /api/empleados` | RRHH, coordinador | Lista los empleados de su empresa: nombre, correo, equipo y si la cuenta está activa |
| `PATCH /api/usuarios/{id}/desactivar` · `…/activar` | ADMIN (cualquiera menos él), RRHH y coordinador (empleados de su empresa) | Desactiva o reactiva una cuenta |
| `GET /api/admin/reporte` | ADMIN | Fecha del servidor y total de usuarios por tipo |
| `GET/POST /api/empresas` · `GET /api/empresas/buscar` · `POST /api/empresas/con-equipos` | ADMIN (RRHH y coordinador solo leen la suya) | Empresas |
| `GET/PUT/DELETE /api/empresas/{id}` | ADMIN (RRHH y coordinador leen la suya) | Una empresa |
| `GET/POST /api/equipos` · `GET/PUT/DELETE /api/equipos/{id}` | ADMIN y RRHH escriben; coordinador lee | Equipos |
| `GET /api/empresas/{id}/cuestionarios` | ADMIN, RRHH, coordinador | Cuestionarios activos |
| `POST /api/empresas/{id}/cuestionarios/{cid}/activar` · `…/desactivar` | ADMIN | Activa (422 si excede el presupuesto) o apaga un cuestionario |
| `GET/PUT /api/empresas/{id}/presupuesto` | GET: ADMIN, RRHH, coordinador; PUT: ADMIN | ε máximo por persona al mes (sigue vigente los meses siguientes) |
| `GET /api/cuestionarios/pendientes` | EMPLEADO | Cuestionarios activos con ítems, escala y configuración LDP |
| `POST /api/checkin` | EMPLEADO | Un ítem ya perturbado (202; 409 si ya respondió; 422 si la configuración no es la vigente) |
| `POST /api/checkin/lote` | EMPLEADO | Hasta k ítems de una batería con una sola sesión |
| `GET /api/dashboard/{equipoId}?semana=<lunes>` | RRHH, coordinador | Estimación de una semana del equipo (400 si no es lunes) |
| `GET/POST /api/ayuda/contactos` · `PATCH /api/ayuda/contactos/{id}/desactivar` | EMPLEADO lee los activos; RRHH y coordinador gestionan | Contactos de ayuda de la empresa |
| `POST /api/ayuda/solicitudes` · `GET /api/ayuda/solicitudes/mias` | EMPLEADO | Pide ayuda y ve sus solicitudes |
| `GET /api/ayuda/solicitudes` · `PATCH …/{id}/atender` · `PATCH …/{id}/cerrar` | solo COORDINADOR_BIENESTAR | RRHH no ve quién pidió ayuda |

Errores: `400` cuerpo o parámetros inválidos, `401` sin sesión o credenciales inválidas, `403` rol sin permiso, `404` no
existe o es de otra empresa, `409` conflicto (correo repetido, ya respondió, estado), `422` regla de privacidad,
`429` demasiados intentos de login. El cuerpo es `{ fecha, estado, error, mensaje, ruta }` y nunca repite una clave,
un valor ruidoso ni un mensaje de ayuda.

## Operación

- **CI:** `.github/workflows/backend-ci.yml` corre `./mvnw test` en cada PR. Para que corran también las pruebas con base
  de datos, carga `KALA_TEST_DB_URL`, `KALA_TEST_DB_USER` y `KALA_TEST_DB_PASSWORD` como secretos.
- **Tareas diarias:** 03:30 purga de `sesion_checkin` y 03:45 purga de refresh tokens vencidos (zona `kala.ventana.zona`).
  Se apagan con `kala.retencion.habilitada=false`.
- **Una sola instancia:** el límite de login vive en memoria. Con varias instancias hay que moverlo a un almacén compartido.
- **Configuración:** `application.properties` (`kala.*`, `app.jwt.*`). Cada valor con variable de entorno se puede cambiar
  sin recompilar.
