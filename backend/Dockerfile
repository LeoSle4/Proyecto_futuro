# Imagen del backend de Kala. Se construye desde la carpeta backend:
#   docker build -t kala-backend .
#
# Variables de entorno que necesita al arrancar (nunca van en la imagen ni en el repositorio):
#   KALA_DB_URL, KALA_DB_USER, KALA_DB_PASSWORD   base de datos PostgreSQL (por ejemplo Neon)
#   KALA_SESSION_SECRET                           secreto del HMAC del antiduplicado
#   JWT_SECRET_BASE64                             clave de firma de los tokens (base64, 32 bytes o más)
#   KALA_ADMIN_EMAIL, KALA_ADMIN_PASSWORD         administrador inicial (la clave, de 12 caracteres o más)
# Chequeo de salud para el orquestador: GET /api/publico/estado en el puerto 8090.

FROM eclipse-temurin:25-jdk AS construccion
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 --no-create-home kala
COPY --from=construccion /app/target/*.jar app.jar
USER kala
EXPOSE 8090
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
